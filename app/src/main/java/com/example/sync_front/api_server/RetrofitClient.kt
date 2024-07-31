package com.example.sync_front.api_server

import com.example.sync_front.BuildConfig
import com.example.sync_front.data.service.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create
import java.text.SimpleDateFormat
import java.util.*

class RetrofitClient {

    val interceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    val client = OkHttpClient().newBuilder()
        .addNetworkInterceptor(interceptor)
        .build()

    val retrofit = Retrofit.Builder()
        .baseUrl("${BuildConfig.BASE_URL}")
        .addConverterFactory(GsonConverterFactory.create(getGson()))
        .client(client)
        .build()

    companion object {
        val instance: RetrofitClient by lazy { RetrofitClient() }
    }

    fun getGson(): Gson {
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault()) // 원하는 시간 형식 지정

        return GsonBuilder()
            .setDateFormat("yyyy-MM-dd") // Date 형식 지정
            .create()
    }

    val loginService = retrofit.create(LoginService::class.java)
    val homeService: HomeService = retrofit.create(HomeService::class.java)
    val syncDetailService: SyncDetailService = retrofit.create(SyncDetailService::class.java)
    val countriesService = retrofit.create(CountriesService::class.java)
    val emailService = retrofit.create(EmailService::class.java)
    val communityService = retrofit.create(CommunityService::class.java)
    val translationService = retrofit.create(TranslationService::class.java)
    val mypageService = retrofit.create(MypageService::class.java)
    val openSyncService: OpenSyncService = retrofit.create(OpenSyncService::class.java)
    val reviewService = retrofit.create(ReviewService::class.java)
    val alarmService: AlarmSaveService = retrofit.create(AlarmSaveService::class.java)
    val chatTransService: ChatTransService = retrofit.create(ChatTransService::class.java)
}