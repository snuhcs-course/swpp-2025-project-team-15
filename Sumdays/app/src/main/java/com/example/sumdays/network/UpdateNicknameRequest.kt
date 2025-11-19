package com.example.sumdays.network

data class UpdateNicknameRequest(val newNickname: String)

data class UpdateNicknameResponse(
    val success: Boolean,
    val message: String
)