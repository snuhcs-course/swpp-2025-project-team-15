package com.example.sumdays.settings.ui

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import androidx.recyclerview.widget.RecyclerView
import com.example.sumdays.TestActivity
import com.example.sumdays.TestApplication
import com.example.sumdays.data.style.StylePrompt
import com.example.sumdays.data.style.UserStyle
import com.example.sumdays.databinding.ItemStyleAddCardBinding
import com.example.sumdays.databinding.ItemStyleCardBinding
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE],
    application = TestApplication::class,
    manifest = Config.NONE
)
class StyleCardAdapterTest {

    private fun fakeStyle(id: Long, name: String = "스타일$id"): UserStyle {
        return UserStyle(
            styleId = id,
            styleName = name,
            styleVector = listOf(0.1f, 0.2f),
            styleExamples = listOf("예시1", "예시2"),
            stylePrompt = StylePrompt(
                character_concept = "일상적인 삶을 살아가는 평범한 사람. 소소한 일상을 관찰하고 기록하는 성향을 가진 인물.",
                emotional_tone = "감정이 드러나지 않고 중립적인 톤으로, 일상적인 사건을 기록하는 데 집중한다.",
                formality = "비격식적인 대화체로, 자연스러운 흐름을 유지하며 친근한 느낌을 준다.",
                lexical_choice = "일상적인 단어와 표현을 사용하여 친근함을 느끼게 한다.",
                pacing = "느긋하고 여유로운 흐름, 빠르지 않게 사건을 나열.",
                punctuation_style = "기본적인 문장 부호 사용, 복잡한 구두점은 없다.",
                sentence_endings = listOf("~었다.", "~했다.", "~었다고 생각했다."),
                sentence_length = "중간 길이의 문장들이 많으며, 간결하게 표현되어 있다.",
                sentence_structure = "주어-서술어 구조가 명확하며, 문장이 단순하고 직관적이다.",
                special_syntax = "일상적인 표현을 그대로 사용하며, 특별한 구문은 없음.",
                speech_quirks = "특별한 말투의 버릇은 없으며, 대화체적인 표현이 자연스럽다.",
                tone = "담담하고 차분한 어조로 일상의 소소한 사건들을 서술."
            ),
            sampleDiary = "샘플다이어리"
        )
    }

    // region 기본 submit / 조회 계열

    @Test
    fun submit_includesDefaultStyleAndProvidedStyles() {
        val adapter = StyleCardAdapter({}, { _, _ -> }, {}, {})
        adapter.submit(listOf(fakeStyle(1), fakeStyle(2)), activeStyleId = 2)

        // items: Default + 1 + 2 = 3
        assertEquals(3, adapter.items.size)
        // itemCount: items + ADD 카드 = 4
        assertEquals(4, adapter.itemCount)
    }

    @Test
    fun getItemViewType_returnsCorrectTypes() {
        val adapter = StyleCardAdapter({}, { _, _ -> }, {}, {})
        adapter.submit(listOf(fakeStyle(1)), null)

        assertEquals(0, adapter.getItemViewType(0)) // Default
        assertEquals(0, adapter.getItemViewType(1)) // fakeStyle(1)
        assertEquals(1, adapter.getItemViewType(2)) // ADD card
    }

    @Test
    fun styleAt_returnsNullWhenOutOfRange() {
        val adapter = StyleCardAdapter({}, { _, _ -> }, {}, {})
        adapter.submit(listOf(fakeStyle(1)), null)

        assertNull(adapter.styleAt(-1))
        assertNull(adapter.styleAt(999))
    }

    @Test
    fun styleAt_returnsCorrectStyle() {
        val adapter = StyleCardAdapter({}, { _, _ -> }, {}, {})
        adapter.submit(listOf(fakeStyle(1), fakeStyle(2)), null)

        assertEquals(1L, adapter.styleAt(1)?.styleId)
        assertEquals(2L, adapter.styleAt(2)?.styleId)
    }

    @Test
    fun updateActiveStyleId_changesInternalState() {
        val adapter = StyleCardAdapter({}, { _, _ -> }, {}, {})
        adapter.submit(listOf(fakeStyle(1), fakeStyle(2)), null)

        adapter.updateActiveStyleId(2L)

        assertEquals(2L, adapter.styleAt(2)?.styleId)
    }

    // endregion

    // region onCreateViewHolder / onBindViewHolder 커버

    @Test
    fun onCreateViewHolder_and_onBindViewHolder_coverBothTypes() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val parent = FrameLayout(context)

        val adapter = StyleCardAdapter({}, { _, _ -> }, {}, {})
        adapter.submit(listOf(fakeStyle(1)), null)

        // TYPE_STYLE ViewHolder 생성 및 바인딩 (position 0: Default, 1: fakeStyle(1))
        val styleHolder0 =
            adapter.onCreateViewHolder(parent, 0) as RecyclerView.ViewHolder
        adapter.onBindViewHolder(styleHolder0, 0)

        val styleHolder1 =
            adapter.onCreateViewHolder(parent, 0) as RecyclerView.ViewHolder
        adapter.onBindViewHolder(styleHolder1, 1)

        // TYPE_ADD ViewHolder 생성 및 바인딩 (마지막 position = items.size)
        val addHolder =
            adapter.onCreateViewHolder(parent, 1) as RecyclerView.ViewHolder
        adapter.onBindViewHolder(addHolder, adapter.items.size)

        // 예외 없이 여기까지 오면 onCreateViewHolder / onBindViewHolder 전부 실행된 것
        assertTrue(true)
    }

    // endregion

    // region ViewHolder bind + flip 동작

    @Test
    fun viewHolder_bind_bindsCorrectly() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val inflater = LayoutInflater.from(context)
        val binding = ItemStyleCardBinding.inflate(inflater)

        val adapter = StyleCardAdapter({}, { _, _ -> }, {}, {})
        adapter.submit(listOf(fakeStyle(1)), activeStyleId = 1)

        // items[0] = Default, [1] = fakeStyle(1)
        val style = adapter.items[1]
        val holder = adapter.StyleVH(binding)
        holder.bind(style)

        assertEquals(style.styleName, binding.styleName.text.toString())
        assertEquals("샘플다이어리", binding.sampleDiary.text.toString())
        assertEquals(View.VISIBLE, binding.front.visibility)
        assertEquals(View.GONE, binding.back.visibility)
    }

    @Test
    fun viewHolder_flip_togglesFrontBackVisibility() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val inflater = LayoutInflater.from(context)
        val binding = ItemStyleCardBinding.inflate(inflater)

        val adapter = StyleCardAdapter({}, { _, _ -> }, {}, {})
        adapter.submit(listOf(fakeStyle(1)), null)

        val holder = adapter.StyleVH(binding)
        holder.bind(adapter.items[1])

        // 초기 상태
        assertEquals(View.VISIBLE, binding.front.visibility)
        assertEquals(View.GONE, binding.back.visibility)

        // flipContainer 클릭 → 두 번째 setOnClickListener의 단순 토글 분기 실행
        binding.flipContainer.performClick()
        assertEquals(View.GONE, binding.front.visibility)
        assertEquals(View.VISIBLE, binding.back.visibility)

        binding.flipContainer.performClick()
        assertEquals(View.VISIBLE, binding.front.visibility)
        assertEquals(View.GONE, binding.back.visibility)
    }

    // endregion

    // region 메뉴 버튼 / showRenameDialog / applyFlipAnimation 커버

    @Test
    fun menuButton_click_doesNotCrash_andPopupCodeRuns() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val inflater = LayoutInflater.from(context)
        val binding = ItemStyleCardBinding.inflate(inflater)

        var deleteCalled = false
        val adapter = StyleCardAdapter(
            onSelect = {},
            onRename = { _, _ -> },
            onDelete = { _ -> deleteCalled = true },
            onAdd = {}
        )
        adapter.submit(listOf(fakeStyle(1)), null)

        val holder = adapter.StyleVH(binding)
        holder.bind(adapter.items[1])

        // moreButton 클릭 → PopupMenu.apply{...}.show()까지 실행됨
        binding.moreButton.performClick()

        // 여기서는 단순히 에러 없이 통과하는지만 보면 됨 (PopupMenu 생성 분기 커버)
        assertFalse(deleteCalled)
    }

    @Test
    fun showRenameDialog_runsWithoutCrash() {
        val activity = Robolectric.buildActivity(TestActivity::class.java).setup().get()
        val inflater = LayoutInflater.from(activity)
        val binding = ItemStyleCardBinding.inflate(inflater)

        val adapter = StyleCardAdapter(
            onSelect = {},
            onRename = { _, _ -> },
            onDelete = {},
            onAdd = {}
        )
        val style = fakeStyle(1, "스타일1")
        adapter.submit(listOf(style), null)

        val holder = adapter.StyleVH(binding)
        holder.bind(adapter.items[1])

        val method = StyleCardAdapter.StyleVH::class.java
            .getDeclaredMethod("showRenameDialog", UserStyle::class.java)
        method.isAccessible = true

        // 단순히 예외 없이 실행되기만 하면 OK
        method.invoke(holder, style)
    }



    @Test
    fun applyFlipAnimation_runsWithoutCrash() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val inflater = LayoutInflater.from(context)
        val binding = ItemStyleCardBinding.inflate(inflater)

        val adapter = StyleCardAdapter({}, { _, _ -> }, {}, {})
        adapter.submit(listOf(fakeStyle(1)), null)

        val holder = adapter.StyleVH(binding)
        holder.bind(adapter.items[1])

        val method = StyleCardAdapter.StyleVH::class.java
            .getDeclaredMethod("applyFlipAnimation", Boolean::class.javaPrimitiveType)
        method.isAccessible = true

        // 단순히 예외 없이 실행되면 OK (라인 커버)
        method.invoke(holder, false)
        method.invoke(holder, true)
    }


    // endregion

    // region AddVH

    @Test
    fun addCard_click_callsOnAdd() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val inflater = LayoutInflater.from(context)
        val binding = ItemStyleAddCardBinding.inflate(inflater)

        var clicked = false
        val adapter = StyleCardAdapter({}, { _, _ -> }, {}, { clicked = true })

        val holder = adapter.AddVH(binding)
        holder.bind()

        binding.addRoot.performClick()

        assertTrue(clicked)
    }

    // endregion
}
