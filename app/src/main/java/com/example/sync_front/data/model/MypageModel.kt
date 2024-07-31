package com.example.sync_front.data.model

data class MypageModel(
    val userId: Int,
    val image: String,
    val name: String,
    val university: String,
    val syncType: String,
    val detailTypes: List<String>,
    val gender: String
)

data class MypageResponse(
    val status: Int,
    val message: String,
    val data: MypageModel
)

data class ModMypageRequest(
    val userName: String,
    val gender: String,
    val syncType: String,
    val detailTypes: List<String>
)

data class ModMypageResponse(
    val status: Int,
    val message: String,
    val data: String
)

data class ModProfileRequest(
    val image: String?,
    val name: String,
    val gender: String,
    val syncType: String,
    val detailTypes: List<String>
)

data class MySyncResponse(
    val status: Int,
    val message: String,
    val data: List<Sync>
)

data class ImageUploadResponse(
    val status: Int,
    val message: String,
    val data: String
)

data class ModName(
    val name: String
)

data class ModGender(
    val gender: String
)

data class ModSyncType(
    val syncType: String
)

data class ModDetailTypes(
    val detailTypes: List<String>
)