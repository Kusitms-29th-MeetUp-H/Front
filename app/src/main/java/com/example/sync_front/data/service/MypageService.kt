package com.example.sync_front.data.service

import com.example.sync_front.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

interface MypageService {

    @GET("mypage")
    fun mypage(
        @Header("Content-Type") application: String,
        @Header("Authorization") accessToken: String,
        @Query("language") language: String,
    ): Call<MypageResponse>

//    @Multipart
//    @PATCH("mypage")
//    fun modMypage(
//        @Header("Content-Type") contentType: String,
//        @Header("Authorization") accessToken: String,
//        @Part image: MultipartBody.Part?,
//        @Part("name") name: ModName,
//        @Part("gender") gender: ModGender,
//        @Part("syncType") syncType: ModSyncType,
//        @Part("detailTypes") detailTypes: ModDetailTypes
//    ): Call<ModMypageResponse>

//    @Multipart
//    @PATCH("mypage")
//    fun modMypage(
//        @Header("Authorization") accessToken: String,
//        @Part image: MultipartBody.Part?,
//        @Part("name") name: String,
//        @Part("gender") gender: String,
//        @Part("syncType") syncType: String,
//        @Part("detailTypes") detailTypes: List<String>
//    ): Call<ModMypageResponse>

    @Multipart
    @PATCH("mypage")
    fun modMypage(
        @Header("Authorization") accessToken: String,
        @Part profileImage: MultipartBody.Part?,
        @Part("editProfileRequest") editProfileRequest: RequestBody
    ): Call<ModMypageResponse>

    @PATCH("mypage/profile")
    fun modNotProfileMypage(
        @Header("Content-Type") application: String,
        @Header("Authorization") accessToken: String,
        @Body request: ModProfileRequest
    ): Call<ModMypageResponse>

    @GET("mypage/mysync")
    fun mySyncList(
        @Header("Content-Type") application: String,
        @Header("Authorization") accessToken: String
    ): Call<MySyncResponse>

    @GET("mypage/join")
    fun myJoinList(
        @Header("Content-Type") application: String,
        @Header("Authorization") accessToken: String
    ): Call<MySyncResponse>

    @GET("mypage/bookmark")
    fun bookmarkList(
        @Header("Content-Type") application: String,
        @Header("Authorization") accessToken: String
    ): Call<MySyncResponse>

    @Multipart
    @POST("mypage/image")
    fun imageUpload(
        @Header("Authorization") accessToken: String,
        @Part file: MultipartBody.Part?
    ): Call<ImageUploadResponse>
}