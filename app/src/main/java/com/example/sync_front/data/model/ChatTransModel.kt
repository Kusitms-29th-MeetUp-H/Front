package com.example.sync_front.data.model

// RequestBody
data class ChatTransModel(
    val language: String,
    val type: String,
    val message: String
)

// ResponseBody
data class ChatTransResponse(
    val status: Int,
    val message: String,
    val data: Data? = null // 성공 응답에만 데이터가 포함됨
)

// Data 클래스
data class Data(
    val message: String
)
