package com.example.sync_front.ui.alarm

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.sync_front.api_server.RetrofitClient
import com.example.sync_front.data.model.NotificationResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AlarmViewModel(application: Application) : AndroidViewModel(application) {
    private val _notifications = MutableLiveData<NotificationResponse>()
    val notifications: LiveData<NotificationResponse>
        get() = _notifications
    val authToken: String?
    val errorMessage = MutableLiveData<String>()

    init {
        val sharedPreferences = application.getSharedPreferences("my_token", Context.MODE_PRIVATE)
        authToken = sharedPreferences.getString("auth_token", null)
    }

    fun fetchNotifications(topCategory: String) {
        RetrofitClient.instance.alarmService.fetchNotifications(
            authorization = authToken,
            topCategory = topCategory
        ).enqueue(object : Callback<NotificationResponse> {
            override fun onResponse(
                call: Call<NotificationResponse>,
                response: Response<NotificationResponse>
            ) {
                if (response.isSuccessful) {
                    _notifications.postValue(response.body())
                } else {
                    _notifications.postValue(null)
                    errorMessage.postValue("Error: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<NotificationResponse>, t: Throwable) {
                _notifications.postValue(null)
                errorMessage.postValue(t.message ?: "Unknown error")
            }
        })
    }
}
