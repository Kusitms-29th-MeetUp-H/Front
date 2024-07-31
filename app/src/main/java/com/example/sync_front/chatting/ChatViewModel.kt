package com.example.sync_front.chatting

import android.app.Application
import android.content.Context
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.sync_front.api_server.RetrofitClient
import com.example.sync_front.data.model.ChatTransModel
import com.example.sync_front.data.model.ChatTransResponse

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val _response = MutableLiveData<ChatTransResponse>()
    val response: LiveData<ChatTransResponse> get() = _response

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error
    val authToken: String?

    init {
        val sharedPreferences = application.getSharedPreferences("my_token", Context.MODE_PRIVATE)
        authToken = sharedPreferences.getString("auth_token", null)
    }

    fun chatTrans(requestBody: ChatTransModel) {
        RetrofitClient.instance.chatTransService.chatTrans(authToken, requestBody)
            .enqueue(object : Callback<ChatTransResponse> {
                override fun onResponse(
                    call: Call<ChatTransResponse>,
                    response: Response<ChatTransResponse>
                ) {
                    if (response.isSuccessful) {
                        _response.value = response.body()
                    } else {
                        _error.value = "Error: ${response.code()}"
                    }
                }

                override fun onFailure(call: Call<ChatTransResponse>, t: Throwable) {
                    _error.value = t.message
                }
            })
    }
}