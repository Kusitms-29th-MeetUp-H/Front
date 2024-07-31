package com.example.sync_front.ui.interest

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.example.sync_front.api_server.RetrofitClient
import com.example.sync_front.data.model.Sync
import com.example.sync_front.data.service.AssociateSyncRequest
import com.example.sync_front.data.service.SyncResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class InterestViewModel(application: Application) : AndroidViewModel(application) {
    val interestSyncs = MutableLiveData<List<Sync>>()
    val authToken: String?

    init {
        val sharedPreferences = application.getSharedPreferences("my_token", Context.MODE_PRIVATE)
        authToken = sharedPreferences.getString("auth_token", null)
    }

    val errorMessage = MutableLiveData<String>()

    fun fetchInterestSyncs(take: Int? = null, syncType: String? = null, type: String? = null, language: String? = null) {
        val request = AssociateSyncRequest(take, syncType, type, language)
        RetrofitClient.instance.homeService.postTypeSyncs(
            "application/json",
            authToken,
            request
        )
            .enqueue(object : Callback<SyncResponse> {
                override fun onResponse(
                    call: Call<SyncResponse>,
                    response: Response<SyncResponse>
                ) {
                    if (response.isSuccessful) {
                        interestSyncs.postValue(response.body()?.data ?: listOf())
                    } else {
                        interestSyncs.postValue(listOf())
                        if (response.code() == 401) { // 토큰 만료 체크
                            errorMessage.postValue("Token expired. Please log in again.")
                        } else {
                            errorMessage.postValue("Error: ${response.errorBody()?.string()}")
                        }
                    }
                }

                override fun onFailure(call: Call<SyncResponse>, t: Throwable) {
                    interestSyncs.postValue(listOf())
                    errorMessage.postValue(t.message ?: "Unknown error")
                }
            })
    }

}