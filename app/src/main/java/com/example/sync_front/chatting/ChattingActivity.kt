package com.example.sync_front.chatting

import ChattingAdapter
import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sync_front.api_server.*
import com.example.sync_front.databinding.ActivityChattingBinding
import com.google.gson.Gson
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import okhttp3.WebSocket
import org.json.JSONObject
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import ua.naiksoftware.stomp.dto.LifecycleEvent
import ua.naiksoftware.stomp.dto.StompHeader
import java.io.PrintWriter
import java.net.InetAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean


class ChattingActivity : AppCompatActivity() {
    lateinit var binding: ActivityChattingBinding
    lateinit var myName : String
    lateinit var chattingList: MutableList<RoomMessageElementResponseDto>
    private lateinit var adapter: ChattingAdapter
    lateinit var roomCode: String
    lateinit var webSocket: WebSocket
    private lateinit var mHandler: Handler
    private lateinit var serverAddr: InetAddress
    private lateinit var socket: Socket
    private lateinit var sendWriter: PrintWriter

    private lateinit var stompClient: StompClient
    private val compositeDisposable = CompositeDisposable()
    val isUnexpectedClosed = AtomicBoolean(false)
    private lateinit var roomName: String

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChattingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // 뒤로가기 버튼 활성화
        supportActionBar?.setDisplayShowTitleEnabled(false) //타이틀 없애기

        val sharedPreferences = getSharedPreferences("my_token", Context.MODE_PRIVATE)
        myName = sharedPreferences.getString("name", "익명")!!

        roomName = "단톡"

        // 어댑터를 설정하고 리사이클러뷰에 연결
        chattingList = mutableListOf<RoomMessageElementResponseDto>()
        adapter = ChattingAdapter(chattingList, myName)
        binding.chattingRecyclerview.layoutManager = LinearLayoutManager(this@ChattingActivity)
        binding.chattingRecyclerview.adapter = adapter


        //val headerList = arrayListOf<StompHeader>()
        initStomp()

//        stompClient.lifecycle().subscribe { lifecycleEvent: LifecycleEvent ->
//            when (lifecycleEvent.type) {
//                LifecycleEvent.Type.OPENED -> {
//                    Log.d(TAG, "Stomp connection opened")
//                    //requestAndSubscribeToChatDetails(roomName)
//                }
//                LifecycleEvent.Type.ERROR -> {
//                    Log.e(TAG, "Error", lifecycleEvent.exception)
//                    if (lifecycleEvent.exception.message!!.contains("EOF")) {
//                        isUnexpectedClosed.set(true)
//                    }
//                }
//                LifecycleEvent.Type.CLOSED -> {
//                    Log.d(TAG, "Stomp connection closed")
//                    isUnexpectedClosed.set(false)
//                }
//                else -> {}
//            }
//        }
//        stompClient.connect()

        //loadChatHistory()
        //requestAndSubscribeToChatDetails("단톡")

        // 메세지 전송 버튼 클릭 시
        binding.sendBtn.setOnClickListener {
            val message = binding.sendTxt.text.toString().trim()
            if (message.isEmpty()) {
                Toast.makeText(this@ChattingActivity, "메시지를 입력하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            else { // 메세지가 있다면 전송
                //saveStore() //전송

                if (stompClient.isConnected()) {
                    Log.d("WebSocket", "연결됨")
                } else {
                    Log.d("WebSocket", "연결되지 않음")
                }

                binding.sendTxt.setText("") // 텍스트창 초기화

                val chatMessage = ChatMessageRequestDto(
                    "123456789012345678901_xxxxxx",
                    "테스트유저1",
                    roomName,
                    message
                )
                val chatId = roomName // 실제 세션 ID로 대체해야 합니다.
                val json = Gson().toJson(chatMessage)
                Log.d("my log","보내는 정보 - ${json}")

                stompClient.send("/pub/room/${chatId}", json).subscribe()

                //getStore() // 채팅창 업데이트
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun initStomp() {
        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, "wss://kusitms28.store/ws")

        stompClient.lifecycle().subscribe { lifecycleEvent: LifecycleEvent ->
            when (lifecycleEvent.type) {
                LifecycleEvent.Type.OPENED -> {
                    Log.d(TAG, "Stomp connection opened")
                    requestAndSubscribeToChatDetails(roomName)
                }
                LifecycleEvent.Type.ERROR -> {
                    Log.e(TAG, "Error", lifecycleEvent.exception)
                    if (lifecycleEvent.exception.message!!.contains("EOF")) {
                        isUnexpectedClosed.set(true)
                    }
                }
                LifecycleEvent.Type.CLOSED -> {
                    Log.d(TAG, "Stomp connection closed")
                    if (isUnexpectedClosed.get()) {
                        initStomp()
                        isUnexpectedClosed.set(false)
                    }
                }
                else -> {}
            }
        }
        stompClient.connect()

        //requestAndSubscribeToChatDetails("단톡")
    }

    @SuppressLint("CheckResult")
    private fun loadChatHistory() {
        Log.d("my log", "채팅 불러오기..")
        val roomName = "123456789012345678901_xxxxxx" // 실제 세션 ID로 대체해야 합니다.
        stompClient.send("/chat/detail/${roomName}").subscribe {
        }
    }

    @SuppressLint("CheckResult")
    private fun requestAndSubscribeToChatDetails(roomName: String) {
        val topic = "/sub/room/$roomName"
        compositeDisposable.add(stompClient.topic(topic)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ stompMessage ->
                Log.d("my log", "${stompMessage}")
                val dataJson = JSONObject(stompMessage.payload).getJSONObject("data").toString()
                Log.d("my log", "data 값 - ${dataJson}")

                val jsonObject = JSONObject(dataJson)

                val chatMessageArray = jsonObject.getJSONArray("chatMessageList")

                val chatMessageList = mutableListOf<RoomMessageElementResponseDto>()

                for (i in 0 until chatMessageArray.length()) {
                    val chatMessageObject = chatMessageArray.getJSONObject(i)
                    val userObject = chatMessageObject.getJSONObject("user")
                    val user = ChatUserResponseDto(
                        sessionId = userObject.getString("sessionId"),
                        name = userObject.getString("name"),
                        type = userObject.getString("type"),
                        profile = userObject.getString("profile")
                    )
                    val content = chatMessageObject.getString("content")
                    val time = chatMessageObject.getString("time")

                    val chatMessage = RoomMessageElementResponseDto(user, content, time)
                    chatMessageList.add(chatMessage)
                }

                adapter.updateData(chatMessageList)

            }, { throwable ->
                Log.e("Stomp subscribe error", "Error on subscribe: ${throwable.localizedMessage}", throwable)
            })
        )

        stompClient.send("/pub/room/detail/$roomName").subscribe({
            Log.d("Chat", "Chat detail request sent for session $roomName")
        }, { throwable ->
            Log.e("Chat", "Failed to send chat detail request: ${throwable.localizedMessage}", throwable)
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        stompClient.disconnect()
    }

}