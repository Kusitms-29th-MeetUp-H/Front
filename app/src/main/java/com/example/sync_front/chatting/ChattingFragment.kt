package com.example.sync_front.chatting

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sync_front.api_server.ChattingRoom
import com.example.sync_front.databinding.FragmentChattingBinding
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.json.JSONObject
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import ua.naiksoftware.stomp.dto.LifecycleEvent
import java.util.concurrent.atomic.AtomicBoolean

class ChattingFragment : Fragment() {
    private var _binding: FragmentChattingBinding? = null
    private val binding get() = _binding!!
    lateinit var roomList: MutableList<ChattingRoom>
    private lateinit var adapter: RoomAdapter
    private var authToken: String ?= null // 로그인 토큰
    private var sessionId: String ?= null //세션Id
    private lateinit var stompClient: StompClient
    private val compositeDisposable = CompositeDisposable()
    private val isUnexpectedClosed = AtomicBoolean(false)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentChattingBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initialSetting()
    }

    private fun initialSetting() {
        // 저장된 토큰 읽어오기
        val sharedPreferences = requireActivity().getSharedPreferences("my_token", Context.MODE_PRIVATE)
        authToken = sharedPreferences.getString("auth_token", null)
        sessionId = sharedPreferences.getString("sessionId", null)

        Log.d("my log","$sessionId")

        // 어댑터를 설정하고 리사이클러뷰에 연결
        roomList = mutableListOf<ChattingRoom>()
        adapter = RoomAdapter(roomList)
        binding.roomRecyclerview.layoutManager = LinearLayoutManager(requireContext())
        binding.roomRecyclerview.adapter = adapter

        if (!::stompClient.isInitialized) {
            initStomp()
        } else {
            if (stompClient.isConnected.not()) {
                stompClient.connect()
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun initStomp() {
        val headers: MutableMap<String, String> = mutableMapOf()
        headers["Content-Type"] = "application/json"
        headers["Authorization"] = authToken!!

        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, "wss://kusitms28.store/ws", headers)

        stompClient.connect()

        stompClient.lifecycle().subscribe { lifecycleEvent: LifecycleEvent ->
            when (lifecycleEvent.type) {
                LifecycleEvent.Type.OPENED -> {
                    Log.d(ContentValues.TAG, "Stomp connection opened")
                    loadChattingRoom()
                }
                LifecycleEvent.Type.ERROR -> {
                    Log.e(ContentValues.TAG, "Error", lifecycleEvent.exception)
                    if (lifecycleEvent.exception.message!!.contains("EOF")) {
                        isUnexpectedClosed.set(true)
                    }
                }
                LifecycleEvent.Type.CLOSED -> {
                    Log.d(ContentValues.TAG, "Stomp connection closed")
                    if (isUnexpectedClosed.get()) {
                        initStomp()
                        isUnexpectedClosed.set(false)
                    }
                }
                else -> {}
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun loadChattingRoom() {
        val topic = "/sub/room/$sessionId"

        compositeDisposable.add(stompClient.topic(topic)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ stompMessage ->
                Log.d("my log", "${stompMessage}")

                val messageType = JSONObject(stompMessage.payload).getString("messageType")
                val dataJson = JSONObject(stompMessage.payload).getJSONObject("data").toString()
                Log.d("my log", "data 값 - ${dataJson}")

                val jsonObject = JSONObject(dataJson)

                if (messageType == "chatList") { // 채팅 리스트 일 때
                    val chatMessageArray = jsonObject.getJSONArray("chatList")
                    Log.d("my log", "채팅 리스트 - ${chatMessageArray}")

                    // roomList 초기화
                    roomList.clear()

                    for (i in 0 until chatMessageArray.length()) {
                        val chatMessageObject = chatMessageArray.getJSONObject(i)

                        val syncName = chatMessageObject.getString("syncName")
                        val roomName = chatMessageObject.getString("roomName")
                        val total = chatMessageObject.getInt("total")
                        val content = chatMessageObject.getString("content")
                        val time = chatMessageObject.getString("time")
                        val image = chatMessageObject.getString("image")
                        val oner = chatMessageObject.getString("ownerSessionId")

                        val chatMessage = ChattingRoom(roomName, syncName, total, content, time, image)
                        roomList.add(chatMessage)
                    }

                    // UI 업데이트
                    if (isAdded) {
                        requireActivity().runOnUiThread {
                            adapter.updateData(roomList)
                        }
                    }
                    //adapter.updateData(roomList)
                }
            }, { throwable ->
                Log.e("Stomp subscribe error", "Error on subscribe: ${throwable.localizedMessage}", throwable)
            })
        )

        Handler(Looper.getMainLooper()).postDelayed({
            compositeDisposable.add(
                stompClient.send("/pub/room/all/$sessionId")
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        Log.d("Chat", "채팅방 리스트 조회")
                    }, { throwable ->
                        Log.e("Chat", "Failed to send chat detail request: ${throwable.localizedMessage}", throwable)
                    })
            )
        }, 100) // 0.1-second delay
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
//        compositeDisposable.clear()
        if (stompClient.isConnected) {
            stompClient.disconnect()
        }
    }

}