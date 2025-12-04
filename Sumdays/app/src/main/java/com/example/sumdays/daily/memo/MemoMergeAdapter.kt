package com.example.sumdays.daily.memo

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import android.content.ClipData
import android.content.Context
import android.os.Build
import android.util.Log
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.View
import android.view.View.DragShadowBuilder
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.sumdays.R
import com.example.sumdays.daily.memo.MemoMergeUtils.convertStylePromptToMap
import com.example.sumdays.daily.memo.MemoMergeUtils.extractMergedText
import com.example.sumdays.data.style.StylePrompt
import com.example.sumdays.data.style.UserStyle
import com.example.sumdays.data.dao.UserStyleDao
import com.example.sumdays.network.ApiClient
import com.example.sumdays.settings.prefs.UserStatsPrefs
import com.example.sumdays.settings.prefs.LabsPrefs
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.InputStreamReader

class MemoMergeAdapter(
    private val context: Context, // toast 띄우기 위해서 추가
    private val memoList: MutableList<Memo>,
    private val scope: CoroutineScope,
    private val onAllMergesDone: () -> Unit,
    private val useStableIds: Boolean = true,
    private val userStatsPrefs: UserStatsPrefs,
    private val userStyleDao: UserStyleDao
) : RecyclerView.Adapter<MemoMergeAdapter.VH>() {

    data class MergeRecord(
        val fromIndexBefore: Int,
        val toIndexBefore: Int,
        val fromMemo: Memo,
        val toMemoBefore: Memo,
        val mergedIds: List<Int>,
        val previousIdMap: Map<Int, List<Int>> // 머지 직전 idToMergedIds 스냅샷
    )

    private val undoStack = ArrayDeque<MergeRecord>()

    /** id to mergedIds */
    private val idToMergedIds = mutableMapOf<Int, MutableList<Int>>()
    private val originalMemoMap: Map<Int, Memo> = memoList.associateBy { it.id }

    init {
        memoList.forEach { memo ->
            idToMergedIds[memo.id] = mutableListOf(memo.id)
        }
    }

    init {
        if (useStableIds) {
            try {
                setHasStableIds(true)
            } catch (_: Throwable) {
                // 테스트 환경에서의 NPE 방지
            }
        }
    }

    /**
     * suppose that the timestamp is STABLE (since ID must be STABLE)
     */
    override fun getItemId(position: Int): Long =
        (memoList[position].content + memoList[position].timestamp).hashCode().toLong()

    fun getMemoContent(index: Int): String {
        return memoList[index].content
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_memo, parent, false)

        val memoText = v.findViewById<TextView>(R.id.memo_text)
        val params = memoText.layoutParams
        params.width = dp(parent.context, 190)
        memoText.minHeight = dp(parent.context, 40)
        memoText.layoutParams = params

        return VH(v)
    }

    private fun dp(context: Context, dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.itemView.alpha = 1.0f
        holder.itemView.scaleX = 1.0f
        holder.itemView.scaleY = 1.0f
        val memo = memoList[position]
        holder.bind(memo)

        // 드래그 시작: 시작 index를 localState로 넘김
        holder.itemView.isLongClickable = true
        holder.itemView.setOnLongClickListener { v ->
            val fromIndex = holder.adapterPosition
            if (fromIndex == RecyclerView.NO_POSITION) return@setOnLongClickListener false

            val data = ClipData.newPlainText("memo", "drag")
            val shadow = DragShadowBuilder(v)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                v.startDragAndDrop(data, shadow, fromIndex, 0)
            } else {
                @Suppress("DEPRECATION")
                v.startDrag(data, shadow, fromIndex, 0)
            }
            v.alpha = 0.6f
            true
        }

        // 타깃 리스너
        holder.itemView.setOnDragListener { view, event ->
            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> true

                DragEvent.ACTION_DRAG_ENTERED -> {
                    view.scaleX = 1.03f
                    view.scaleY = 1.03f
                    true
                }

                DragEvent.ACTION_DRAG_EXITED -> {
                    view.scaleX = 1f
                    view.scaleY = 1f
                    true
                }

                DragEvent.ACTION_DROP -> {
                    view.scaleX = 1f
                    view.scaleY = 1f

                    val fromIndex = event.localState as? Int ?: return@setOnDragListener false
                    val toIndex = holder.adapterPosition
                    if (toIndex == RecyclerView.NO_POSITION) return@setOnDragListener false

                    if (fromIndex != toIndex) {
                        val fromId = memoList[fromIndex].id
                        val toId = memoList[toIndex].id

                        val fromIds: List<Int> =
                            idToMergedIds[fromId]?.toList() ?: listOf(fromId)
                        val toIds: List<Int> =
                            idToMergedIds[toId]?.toList() ?: listOf(toId)

                        // 두 그룹을 합치고, 중복 제거
                        val mergedIds: List<Int> = (fromIds + toIds).distinct()

                        // order 순서대로 정렬해서 안정적인 머지 순서 유지
                        val sortedIdsByOrder: List<Int> = mergedIds
                            .mapNotNull { id -> originalMemoMap[id] }
                            .sortedBy { it.order }
                            .map { it.id }

                        mergeByIndex(fromIndex, toIndex, sortedIdsByOrder)
                    }
                    true
                }

                DragEvent.ACTION_DRAG_ENDED -> {
                    holder.itemView.alpha = 1f
                    true
                }

                else -> false
            }
        }
    }

    override fun getItemCount(): Int = memoList.size

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val content: TextView = itemView.findViewById(R.id.memo_text)
        private val timestamp: TextView = itemView.findViewById(R.id.memo_time)

        fun bind(m: Memo) {
            content.text = m.content
            timestamp.text = m.timestamp
        }

        fun updateTextOnly(text: String) {
            content.text = text
        }
    }

    private fun maybeNotifyAllMerged() {
        if (memoList.size <= 1) onAllMergesDone()
    }

    /** 합칠 MemoList index들과 id들을 인자로 주면 두 메모를 하나로 합친다. */
    private fun mergeByIndex(fromIndex: Int, toIndex: Int, mergedIds: List<Int>) {
        if (fromIndex !in memoList.indices || toIndex !in memoList.indices) return

        // 머지 직전 idToMergedIds 스냅샷 저장 (이번 머지에 참여하는 id들만)
        val previousIdMap: Map<Int, List<Int>> = mergedIds.associateWith { id ->
            (idToMergedIds[id] ?: listOf(id)).toList() // defensive copy
        }

        val record = MergeRecord(
            fromIndexBefore = fromIndex,
            toIndexBefore = toIndex,
            fromMemo = memoList[fromIndex],
            toMemoBefore = memoList[toIndex],
            mergedIds = mergedIds,
            previousIdMap = previousIdMap
        )

        scope.launch(Dispatchers.IO) {
            try {
                var targetIndex: Int

                // 먼저 UI에서 fromIndex 제거
                withContext(Dispatchers.Main) {
                    memoList.removeAt(fromIndex)
                    notifyItemRemoved(fromIndex)

                    // 제거 후 올바른 targetIndex 계산
                    targetIndex = if (fromIndex < toIndex) toIndex - 1 else toIndex
                }

                // 스트리밍 머지 시작
                val finalMergedText = mergeTextByIds(mergedIds, endFlag = false) { partial ->
                    scope.launch(Dispatchers.Main) {
                        if (targetIndex in memoList.indices) {
                            memoList[targetIndex] =
                                memoList[targetIndex].copy(content = partial)
                            notifyItemChanged(targetIndex, partial)
                        }
                    }
                }

                // ID 머지 상태 업데이트 (성공 시에만)
                mergedIds.forEach { updateIdMap(it, mergedIds) }

                // 스트림 종료 후 최종 내용 고정
                withContext(Dispatchers.Main) {
                    if (targetIndex in memoList.indices) {
                        memoList[targetIndex] =
                            memoList[targetIndex].copy(content = finalMergedText)
                        notifyItemChanged(targetIndex, finalMergedText)
                    }

                    undoStack.addLast(record)
                    maybeNotifyAllMerged()
                }
            } catch (e: Exception) {
                Log.e("MemoMergeAdapter", "Merge failed", e)

                // 실패 시 idToMergedIds도 롤백
                for ((id, oldList) in record.previousIdMap) {
                    idToMergedIds[id] = oldList.toMutableList()
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "메모 합치기 실패", Toast.LENGTH_SHORT).show()

                    // 삭제했던 from 메모 복원
                    if (fromIndex <= memoList.size) {
                        memoList.add(fromIndex, record.fromMemo)
                        notifyItemInserted(fromIndex)
                    }

                    // to 메모 내용 복원
                    if (toIndex in memoList.indices) {
                        memoList[toIndex] = record.toMemoBefore
                        notifyItemChanged(toIndex)
                    }
                }
            }
        }
    }

    fun updateIdMap(targetId: Int, mergedIds: List<Int>) {
        idToMergedIds[targetId] = mergedIds.toMutableList()
    }

    /** 주어진 Memo Id들에 해당하는 Memo text들을 OpenAI를 이용해 하나로 합친다. 합쳐진 문장 반환 */
    suspend fun mergeTextByIds(
        mergedIds: List<Int>,
        endFlag: Boolean = false,
        onPartial: (String) -> Unit = {}
    ): String {

        // 서버에 보낼 Memo 리스트 구성
        val memos = mutableListOf<MemoPayload>()
        mergedIds.forEach {
            val memo = originalMemoMap[it]
            if (memo != null) {
                memos.add(MemoPayload(memo.id, memo.content, memo.order))
            }
        }

        // 활성 스타일 데이터 로드 또는 더미 데이터 사용
        val activeStyleId = userStatsPrefs.getActiveStyleId()
        val styleData = userStyleDao.getStyleById(activeStyleId)
        val stylePrompt: Map<String, Any>
        val styleExample: List<String>
        val styleVector: List<Float>

        if (styleData != null) {
            // 활성 스타일이 있을 경우: Room DB 실제 데이터 사용
            stylePrompt = convertStylePromptToMap(styleData.stylePrompt)
            styleExample = styleData.styleExamples
            styleVector = styleData.styleVector
        } else {
            // 활성 스타일이 없을 경우: 더미 데이터 사용
            stylePrompt = mapOf(
                "character_concept" to "일상적인 삶을 살아가는 평범한 사람. 소소한 일상을 관찰하고 기록하는 성향을 가진 인물.",
                "emotional_tone" to "감정이 드러나지 않고 중립적인 톤으로, 일상적인 사건을 기록하는 데 집중한다.",
                "formality" to "비격식적인 대화체로, 자연스러운 흐름을 유지하며 친근한 느낌을 준다.",
                "lexical_choice" to "일상적인 단어와 표현을 사용하여 친근함을 느끼게 한다.",
                "pacing" to "느긋하고 여유로운 흐름, 빠르지 않게 사건을 나열.",
                "punctuation_style" to "기본적인 문장 부호 사용, 복잡한 구두점은 없다.",
                "sentence_endings" to listOf("~었다.", "~했다.", "~었다고 생각했다."),
                "sentence_length" to "중간 길이의 문장들이 많으며, 간결하게 표현되어 있다.",
                "sentence_structure" to "주어-서술어 구조가 명확하며, 문장이 단순하고 직관적이다.",
                "special_syntax" to "일상적인 표현을 그대로 사용하며, 특별한 구문은 없음.",
                "speech_quirks" to "특별한 말투의 버릇은 없으며, 대화체적인 표현이 자연스럽다.",
                "tone" to "담담하고 차분한 어조로 일상의 소소한 사건들을 서술."
            )
            styleExample = listOf(
                "일어나서 물을 한 잔 마셨다",
                "조용히 하루가 지나갔다",
                "일기를 쓰고 자야겠다고 생각했다",
                "창문을 여니 바깥 공기가 들어왔다"
            )
            styleVector = emptyList()
        }

        val temperature = LabsPrefs.getTemperature(context)
        val advancedFlag = LabsPrefs.getAdvancedFlag(context)

        val request = MergeRequest(
            memos = memos,
            endFlag = endFlag,
            stylePrompt = stylePrompt,
            styleExamples = styleExample,
            styleVector = styleVector,
            advancedFlag = advancedFlag,
            temperature = temperature
        )

        if (endFlag) {
            // skip button => 마지막 완성 → JSON 반환
            val response = ApiClient.api.mergeMemos(request)
            val json = response.body() ?: throw IllegalStateException("Empty final response")
            return extractMergedText(json)
        }

        // API 호출
        val call = ApiClient.api.mergeMemosStream(request)
        val response = call.execute()

        val stream = response.body()?.byteStream()
            ?: throw IllegalStateException("Empty streaming body")

        val reader = InputStreamReader(stream, Charsets.UTF_8)
        val sb = StringBuilder()
        val charBuffer = CharArray(64)

        while (true) {
            val read = reader.read(charBuffer)
            if (read == -1) break

            val chunk = String(charBuffer, 0, read)
            sb.append(chunk)

            // 스트리밍 중간 결과 콜백
            onPartial(sb.toString())
        }

        return sb.toString()
    }

    /** 마지막 머지를 되돌린다. 성공하면 true */
    fun undoLastMerge(): Boolean {
        val rec = undoStack.removeLastOrNull() ?: return false

        // idToMergedIds를 머지 직전 상태로 복원
        for ((id, oldList) in rec.previousIdMap) {
            idToMergedIds[id] = oldList.toMutableList()
        }

        // 현재 리스트에서 타깃 인덱스 계산
        val toCurrent = if (rec.fromIndexBefore < rec.toIndexBefore)
            rec.toIndexBefore - 1
        else
            rec.toIndexBefore

        // 타깃 메모 내용 복원
        if (toCurrent in memoList.indices) {
            memoList[toCurrent] = rec.toMemoBefore
            notifyItemChanged(toCurrent)
        } else {
            return false
        }

        // from 메모를 원래 위치에 다시 삽입
        val insertIndex = rec.fromIndexBefore.coerceIn(0, memoList.size)
        memoList.add(insertIndex, rec.fromMemo)
        notifyItemInserted(insertIndex)

        return true
    }

    suspend fun mergeAllMemo(): String {
        val idMutableList = mutableListOf<Int>()
        for (entry in originalMemoMap) {
            idMutableList.add(entry.value.id)
        }
        val idList = idMutableList.toList()
        return mergeTextByIds(idList, endFlag = true)
    }

    override fun onBindViewHolder(holder: VH, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty()) {
            val partialText = payloads.last() as String
            holder.updateTextOnly(partialText)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }
}
