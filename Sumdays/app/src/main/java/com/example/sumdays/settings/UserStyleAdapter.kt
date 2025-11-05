package com.example.sumdays.settings

// com.example.sumdays.settings.UserStyleAdapter.kt

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.sumdays.databinding.ItemUserStyleBinding
import com.example.sumdays.data.style.UserStyle // Room Entity로 가정된 클래스

class UserStyleAdapter(
    private val onStyleSelected: (styleId: Long) -> Unit, // 스타일 선택 (RadioButton 클릭)
    private val onDeleteClicked: (style: UserStyle) -> Unit // 삭제 버튼 클릭
) : RecyclerView.Adapter<UserStyleAdapter.StyleViewHolder>() {

    private val stylesList = mutableListOf<UserStyle>()
    // 현재 활성화된 스타일의 ID (UserStats Prefs에서 가져옴)
    private var activeStyleId: Long? = null

    fun updateList(newStyles: List<UserStyle>, newActiveStyleId: Long?) {
        stylesList.clear()
        stylesList.addAll(newStyles)
        activeStyleId = newActiveStyleId
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StyleViewHolder {
        val binding = ItemUserStyleBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StyleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StyleViewHolder, position: Int) {
        val style = stylesList[position]
        holder.bind(style, activeStyleId)
    }

    override fun getItemCount(): Int = stylesList.size

    inner class StyleViewHolder(private val binding: ItemUserStyleBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(style: UserStyle, activeId: Long?) {
            binding.styleNameTextView.text = style.styleName

            // StylePrompt에서 요약 정보 추출 (예: emotional_tone과 formality)
            binding.styleSummaryTextView.text =
                "톤: ${style.stylePrompt.emotional_tone}, 형식: ${style.stylePrompt.formality}"

            // --- 1. 활성 스타일 표시 (라디오 버튼) ---
            val isSelected = style.styleId == activeId
            binding.styleRadioButton.isChecked = isSelected

            // --- 2. 리스너 설정 ---

            // 스타일 선택 (라디오 버튼 또는 항목 클릭 시 활성화)
            binding.root.setOnClickListener {
                if (!isSelected) {
                    onStyleSelected(style.styleId) // Activity로 활성 스타일 변경 요청
                }
            }
            binding.styleRadioButton.setOnClickListener {
                if (!isSelected) {
                    onStyleSelected(style.styleId)
                }
            }

            // 3. 삭제 버튼
            binding.deleteStyleButton.setOnClickListener {
                if (style.styleId == activeId) {
                    Toast.makeText(binding.root.context, "활성 스타일은 삭제할 수 없습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    onDeleteClicked(style)
                }
            }
        }
    }
}