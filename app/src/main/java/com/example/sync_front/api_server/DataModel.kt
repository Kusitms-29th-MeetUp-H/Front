package com.example.sync_front.api_server

import com.google.gson.annotations.SerializedName

data class User(
    val userId: Int,
    val email: String,
    val name: String,
    val picture: String,
    val accessToken: String,
    val refreshToken: String,
    val isFirst: Int,
    val sessionId: String
)

data class Platform(
    val platform: String
)

data class LogInResponse(
    val status: Int,
    val message: String,
    val data: User
)

data class Chatting(
    var chattingId: String? = null,
    var user: String? = null,
    var text: String? = null,
    var time: String? = null
)

data class ChattingRoom(
    var roomId: String? = null,
    var title: String? = null,
    var lastText: String? = null,
    var lastTime: String? = null
)

data class ChatMessageRequestDto(
    val chatSession: String,
    val fromUserName: String,
    val toRoomName: String,
    val content: String
)

data class ChatMessageListRequestDto(
    val chatSession: String,
    val fromUserName: String,
    val toUserName: String,
)

data class RoomMessageListResponseDto(
    val users: List<ChatUserResponseDto>,
    val chatMessageList: List<RoomMessageElementResponseDto> = emptyList()
)

data class ChatUserResponseDto(
    val sessionId: String,
    val name: String,
    val type: String,
    val profile: String
)

data class RoomMessageElementResponseDto(
    val user: ChatUserResponseDto,
    val content: String,
    val time: String
)

data class LoginGoogleRequestModel(
    @SerializedName("grant_type")
    private val grant_type: String,
    @SerializedName("client_id")
    private val client_id: String,
    @SerializedName("client_secret")
    private val client_secret: String,
    @SerializedName("code")
    private val code: String
)

data class LoginGoogleResponseModel(
    @SerializedName("access_token") var access_token: String,
)
