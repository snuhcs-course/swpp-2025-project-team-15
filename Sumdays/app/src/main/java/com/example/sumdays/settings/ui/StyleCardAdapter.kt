package com.example.sumdays.settings.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.sumdays.R
import com.example.sumdays.daily.memo.MemoPayload
import com.example.sumdays.daily.memo.MergeRequest
import com.example.sumdays.data.style.UserStyle
import com.example.sumdays.databinding.ItemStyleAddCardBinding
import com.example.sumdays.databinding.ItemStyleCardBinding
import com.example.sumdays.network.ApiClient
import com.example.sumdays.settings.ui.model.SampleMemo
import com.example.sumdays.daily.memo.MemoMergeAdapter
import com.example.sumdays.daily.memo.MemoMergeUtils.convertStylePromptToMap
import com.example.sumdays.daily.memo.MemoMergeUtils.extractMergedText
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StyleCardAdapter(
    private val onSelect: (UserStyle?) -> Unit,
    private val onRename: (UserStyle, String) -> Unit,
    private val onDelete: (UserStyle) -> Unit,
    private val onAdd: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<UserStyle>()
    private val sampleCache = mutableMapOf<Long, String>()
    private var activeId: Long? = null

    private val sampleMemos = listOf(
        SampleMemo(1, "아침에 일어나서 조금 멍했다.", 1),
        SampleMemo(2, "카페에서 라떼를 마셨다.", 2),
        SampleMemo(3, "오늘 하루는 그냥 조용히 지나간 것 같다.", 3)
    )

    companion object {
        private const val TYPE_STYLE = 0
        private const val TYPE_ADD = 1
    }

    fun submit(list: List<UserStyle>, activeStyleId: Long?) {
        items.clear()
        items.addAll(list)
        this.activeId = activeStyleId
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int) =
        if (position == items.size) TYPE_ADD else TYPE_STYLE

    override fun getItemCount(): Int = items.size + 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inf = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_STYLE) {
            StyleVH(ItemStyleCardBinding.inflate(inf, parent, false))
        } else {
            AddVH(ItemStyleAddCardBinding.inflate(inf, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is StyleVH) holder.bind(items[position])
        else (holder as AddVH).bind()
    }

    inner class StyleVH(private val b: ItemStyleCardBinding) : RecyclerView.ViewHolder(b.root) {
        private var flipped = false
        private var isFront = true

        fun bind(style: UserStyle) {
            // 항상 초기 상태는 앞면
            b.front.visibility = View.VISIBLE
            b.back.visibility = View.GONE
            isFront = true

            // filp 애니메이션 효과
            b.flipContainer.setOnClickListener {
                isFront = !isFront
                applyFlipAnimation(isFront)
            }

            // 선택 상태에 따른 테두리 강조 효과
            if (style.styleId == activeId) {
                b.cardRoot.strokeWidth = (4 * b.root.resources.displayMetrics.density).toInt() // 2dp
            } else {
                b.cardRoot.strokeWidth = 0
            }

            // 기본 스타일 이름
            b.styleName.text = style.styleName

            // 샘플 일기 표시 (캐시 → 없다면 생성)
            if (sampleCache.containsKey(style.styleId)) {
                b.sampleDiary.text = sampleCache[style.styleId]
            } else {
                b.sampleDiary.text = "샘플 생성 중..."
                generateSampleDiary(style) { diary ->
                    sampleCache[style.styleId] = diary
                    b.sampleDiary.text = diary
                }
            }

            // 프롬프트(요약) – style.stylePrompt 객체를 문자열로 요약 표시 (필드명은 프로젝트 정의에 맞게)
            val p = style.stylePrompt
            b.promptBody.text = buildString {
                p?.tone?.let { append("• Tone: $it\n") }
                p?.formality?.let { append("• Formality: $it\n") }
                p?.sentence_length?.let { append("• Sentence Length: $it\n") }
                p?.sentence_structure?.let { append("• Structure: $it\n") }

                p?.sentence_endings?.takeIf { it.isNotEmpty() }?.let {
                    append("• Endings: ${it.joinToString(", ")}\n")
                }

                p?.lexical_choice?.let { append("• Word Choice: $it\n") }

                p?.common_phrases?.takeIf { it.isNotEmpty() }?.let {
                    append("• Common Phrases: ${it.joinToString(", ")}\n")
                }

                p?.emotional_tone?.let { append("• Emotional Tone: $it\n") }
                p?.irony_or_sarcasm?.let { append("• Irony/Sarcasm: $it\n") }
                p?.slang_or_dialect?.let { append("• Slang/Dialect: $it\n") }
                p?.pacing?.let { append("• Pacing: $it\n") }
            }


            // flip
            b.flipContainer.setOnClickListener {
                flipped = !flipped
                b.front.visibility = if (flipped) View.GONE else View.VISIBLE
                b.back.visibility = if (flipped) View.VISIBLE else View.GONE
            }

            // ⋮ 메뉴
            b.moreButton.setOnClickListener { v ->
                PopupMenu(v.context, v).apply {
                    menuInflater.inflate(R.menu.menu_style_card, menu)
                    setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            R.id.action_rename -> {
                                showRenameDialog(style)
                                true
                            }
                            R.id.action_delete -> { onDelete(style); true }
                            else -> false
                        }
                    }
                }.show()
            }

            // 선택 상태(외부 버튼에서 처리하므로 카드에는 시각 피드백만)
            val isActive = (style.styleId == activeId)
            b.cardRoot.strokeWidth = if (isActive) 2 else 0
            b.cardRoot.strokeColor = 0xFF8B008B.toInt()
        }

        private fun applyFlipAnimation(showFront: Boolean) {
            val scale = b.root.resources.displayMetrics.density
            b.flipContainer.cameraDistance = 6000 * scale

            val flipOut = ObjectAnimator.ofFloat(b.flipContainer, "rotationY", 0f, 90f).apply {
                duration = 200
            }
            val flipIn = ObjectAnimator.ofFloat(b.flipContainer, "rotationY", -90f, 0f).apply {
                duration = 200
            }

            flipOut.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (showFront) {
                        b.back.visibility = View.GONE
                        b.front.visibility = View.VISIBLE
                    } else {
                        b.front.visibility = View.GONE
                        b.back.visibility = View.VISIBLE
                    }
                    flipIn.start()
                }
            })

            flipOut.start()
        }

        private fun showRenameDialog(style: UserStyle) {
            val ctx = b.root.context
            val input = android.widget.EditText(ctx).apply {
                setText(style.styleName)
                setSelection(text.length)
            }
            AlertDialog.Builder(ctx)
                .setTitle("스타일 이름 변경")
                .setView(input)
                .setPositiveButton("확인") { _, _ ->
                    onRename(style, input.text.toString().ifBlank { defaultName(adapterPosition) })
                }
                .setNegativeButton("취소", null)
                .show()
        }

        private fun defaultName(idx: Int) = "스타일 ${idx + 1}"

        private fun dummySample(s: UserStyle): CharSequence {
            // stylePrompt 필드가 있으면 맛만 보기로 반영, 없으면 고정 문구
            val tone = s.stylePrompt?.emotional_tone ?: "담담한"
            return "아침 공기가 ${tone} 느낌이었고,\n커피 한 잔으로 마음을 고르며\n하루를 가볍게 지나보냈다."
        }
    }

    inner class AddVH(private val b: ItemStyleAddCardBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind() { b.addRoot.setOnClickListener { onAdd() } }
    }

    /** 외부에서 현재 스냅된 포지션의 스타일을 얻고 싶을 때 */
    fun styleAt(position: Int): UserStyle? = items.getOrNull(position)

    fun updateActiveStyleId(activeId: Long?) {
        this.activeId = activeId
        notifyDataSetChanged() // 즉시 UI 갱신
    }

    private fun generateSampleDiary(
        style: UserStyle,
        callback: (String) -> Unit
    ) {
        val promptMap = convertStylePromptToMap(style.stylePrompt)
        val examples = style.styleExamples

        val memosPayload = sampleMemos.map {
            MemoPayload(id = it.id, content = it.content, order = it.order)
        }

        val request = MergeRequest(
            memos = memosPayload,
            endFlag = true, // ✅ 1-shot 완성 모드
            stylePrompt = promptMap,
            styleExamples = examples
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.api.mergeMemos(request)
                val json = response.body()
                val diary = extractMergedText(json ?: JsonObject())

                withContext(Dispatchers.Main) { callback(diary) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { callback("샘플 생성 실패 :(") }
            }
        }
    }

    fun removeCache(styleId: Long?) {
        if (styleId != null) {
            sampleCache.remove(styleId)
        }
    }
}
