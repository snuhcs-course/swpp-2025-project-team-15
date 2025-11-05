package com.example.sumdays.network

import com.example.sumdays.data.style.StylePrompt

data class StyleExtractionResponse(
    val success: Boolean,
    val style_vector: List<Float>,
    val style_examples: List<String>,
    val style_prompt: StylePrompt,
    val message: String? = null // 실패 시 메시지
)