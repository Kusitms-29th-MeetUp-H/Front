package com.example.sync_front.data.model

data class Sync(
    val syncId: Long,
    val syncType: String,
    val type: String,
    val image: String,
    val userCnt: Int,
    val totalCnt: Int,
    val syncName: String,
    val location: String,
    val date: String,
    val associate: String?,
    val isMarked: Boolean,
)

data class SyncDetail(
    val syncName: String,
    val syncImage: String,
    val syncType: String,
    val type: String,
    val syncIntro: String,
    val date: String,
    val regularDate: String?,
    val location: String,
    val userCnt: Int,
    val totalCnt: Int,
    val userImage: String,
    val userName: String,
    val university: String,
    val userIntro: String,
    val isFull: Boolean,
    val isJoin: Boolean,
    val isMarked: Boolean,
    val isOwner: Boolean,
)

data class GraphData(
    val name: String,
    val percent: Int
)

data class GraphDetails(
    val data: List<GraphData>,
    val status: String
)
data class Review(
    val image: String,
    val name: String,
    val university: String,
    val content: String,
    val date: String
)

data class BookmarkRequest(
    val syncId: Long,
    val isMarked: Boolean
)

data class BookmarkResponse(
    val status: Int,
    val message: String,
    val data: Boolean
)

