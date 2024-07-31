package com.example.sync_front.data.service

import com.example.sync_front.data.model.ChatTransModel
import com.example.sync_front.data.model.ChatTransResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface ChatTransService {
    @Headers("Content-Type: application/json")
    @POST("/api/translate")
    fun chatTrans(
        @Header("Authorization") authToken: String?,
        @Body requestBody: ChatTransModel
    ): Call<ChatTransResponse>
}