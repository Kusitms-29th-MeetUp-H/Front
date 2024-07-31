package com.example.sync_front.chatting

import ChattingAdapter
import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.adapters.TextViewBindingAdapter.setText
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.sync_front.R
import com.example.sync_front.api_server.*
import com.example.sync_front.data.model.ChatTransModel
import com.example.sync_front.databinding.ActivityChattingBinding
import com.example.sync_front.ui.main.my.ModProfileActivity
import com.google.android.material.tabs.TabLayout
import com.google.gson.Gson
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.json.JSONObject
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import ua.naiksoftware.stomp.dto.LifecycleEvent
import ua.naiksoftware.stomp.dto.StompCommand
import ua.naiksoftware.stomp.dto.StompMessage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.util.ArrayList
import java.util.concurrent.atomic.AtomicBoolean

class ChattingActivity : AppCompatActivity() {
    lateinit var binding: ActivityChattingBinding
    lateinit var myName: String
    lateinit var tempMsg: String
    lateinit var chattingList: MutableList<RoomMessageElementResponseDto>
    private lateinit var adapter: ChattingAdapter
    private var authToken: String? = null // 로그인 토큰
    private var plusToggle: Boolean = false // 사진 추가 버튼 활성화
    private var selectedImg: String? = null //선택한 이미지

    private lateinit var stompClient: StompClient
    private val compositeDisposable = CompositeDisposable()
    val isUnexpectedClosed = AtomicBoolean(false)
    private var roomName: String? = null
    private var sessionId: String? = null // 세션id

    private lateinit var viewModel: ChatViewModel


    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChattingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupTabs(binding.root)
        initialSetting()
        setupClickListeners()
        observeViewModel()

        //initStomp()
    }

    private fun initialSetting() {
        // 저장된 토큰 읽어오기
        val sharedPreferences = this.getSharedPreferences("my_token", Context.MODE_PRIVATE)
        authToken = sharedPreferences.getString("auth_token", null)
        myName = sharedPreferences.getString("name", "익명")!!
        roomName = intent.getStringExtra("roomName")
        sessionId = sharedPreferences.getString("sessionId", null)

        // 어댑터를 설정하고 리사이클러뷰에 연결
        chattingList = mutableListOf<RoomMessageElementResponseDto>()
        adapter = ChattingAdapter(chattingList, myName)
        binding.chattingRecyclerview.layoutManager = LinearLayoutManager(this@ChattingActivity)
        binding.chattingRecyclerview.adapter = adapter

        val syncName = intent.getStringExtra("syncName")
        binding.chattingTitle.text = syncName

        val total = intent.getIntExtra("total", 0)
        binding.memberCount.text = total.toString()

        if (!::stompClient.isInitialized) {
            initStomp()
        } else {
            // Reconnect if needed
            if (stompClient.isConnected.not()) {
                stompClient.connect()
            }
        }
    }

    private fun setupTabs(view: View) {
        val typeTab = binding.typeTabs
        typeTab.addTab(typeTab.newTab().setText("원문"))
        typeTab.addTab(typeTab.newTab().setText("길게"))
        typeTab.addTab(typeTab.newTab().setText("짧게"))
        typeTab.addTab(typeTab.newTab().setText("친구같이"))
        typeTab.addTab(typeTab.newTab().setText("공손하게"))

        val languageTab = binding.languageTabs
        languageTab.addTab(languageTab.newTab().setText("한국어"))
        languageTab.addTab(languageTab.newTab().setText("영어"))
        languageTab.addTab(languageTab.newTab().setText("일본어"))
        languageTab.addTab(languageTab.newTab().setText("중국어"))
        languageTab.addTab(languageTab.newTab().setText("독일어"))
        languageTab.addTab(languageTab.newTab().setText("프랑스어"))
        languageTab.addTab(languageTab.newTab().setText("스페인어"))

        typeTab.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val selectedType = tab?.text.toString()
                val selectedLanguage =
                    languageTab.getTabAt(languageTab.selectedTabPosition)?.text.toString()

                val requestBody = ChatTransModel(
                    language = selectedLanguage,
                    type = selectedType,
                    message = tempMsg
                )

                // Assuming viewModel is already initialized
                viewModel.chatTrans(requestBody)

            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })

        languageTab.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val selectedLanguage = tab?.text.toString()
                val selectedType = typeTab.getTabAt(typeTab.selectedTabPosition)?.text.toString()

                val requestBody = ChatTransModel(
                    language = selectedLanguage,
                    type = selectedType,
                    message = tempMsg
                )

                // Assuming viewModel is already initialized
                viewModel.chatTrans(requestBody)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })

    }

    private fun observeViewModel() {
        viewModel.response.observe(this, Observer { responseBody ->
            responseBody.data?.let {
                binding.sendTxt.setText(it.message)
            }
            println("Status: ${responseBody.status}")
            println("Message: ${responseBody.message}")
            println("Data: ${responseBody.data?.message}")
        })

        viewModel.error.observe(this, Observer { error ->
            println("Error: $error")
        })
    }

    private fun setupClickListeners() {
        //번역 버튼 클릭 시
        binding.transBtn.setOnClickListener {
            binding.typeTabs.visibility = View.VISIBLE
            binding.languageTabs.visibility = View.VISIBLE
            tempMsg = binding.sendTxt.text.toString()
        }

        // 메세지 전송 버튼 클릭 시
        binding.sendBtn.setOnClickListener {
            val message = binding.sendTxt.text.toString().trim()

            if (message.isEmpty()) {
                Toast.makeText(
                    this@ChattingActivity,
                    getString(R.string.input_message),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            } else { // 메세지가 있다면 전송
                if (stompClient.isConnected()) {
                    Log.d("WebSocket", "연결됨")
                    sendMessage(message, selectedImg) // 전송
                } else {
                    Log.d("WebSocket", "연결되지 않음")
                }
            }
        }

        binding.picBtn.setOnClickListener {
            singleImagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
        }

        // 키보드가 활성화되면 리사이클러뷰의 크기 조정
        binding.chattingRecyclerview.addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            binding.chattingRecyclerview.scrollToPosition(adapter.itemCount - 1)
        }

        binding.plusBtn.setOnClickListener {
            if (!plusToggle) {
                plusToggle = !plusToggle
                binding.picBtn.visibility = View.VISIBLE
            } else {
                plusToggle = !plusToggle
                binding.picBtn.visibility = View.GONE
            }
        }

        binding.backBtn.setOnClickListener {
            finish()
        }
    }

    @SuppressLint("CheckResult")
    private fun initStomp() {
        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, "wss://kusitms28.store/ws")

        stompClient.connect()

        stompClient.lifecycle().subscribe { lifecycleEvent: LifecycleEvent ->
            when (lifecycleEvent.type) {
                LifecycleEvent.Type.OPENED -> {
                    Log.d(TAG, "Stomp connection opened")
                    requestAndSubscribeToChatDetails(roomName!!)
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
        //stompClient.connect()
    }

    private fun sendMessage(message: String, image: String?) {
        binding.sendTxt.setText("") // 텍스트창 초기화
        binding.image.visibility = View.GONE

        val chatMessage = ChatMessageRequestDto(
            sessionId!!,
            myName,
            roomName!!,
            message,
            image
        ) // 채팅 보내는 사람 정보, 텍스트

        val json = Gson().toJson(chatMessage)


        Log.d("my log", "보내는 정보 - ${json}")

        stompClient.send("/pub/room/${roomName}", json).subscribe()
    }

    @SuppressLint("CheckResult")
    private fun requestAndSubscribeToChatDetails(roomName: String) {
        val topic = "/sub/room/$roomName"
        compositeDisposable.add(
            stompClient.topic(topic)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ stompMessage ->
                    Log.d("my log", "${stompMessage}")

                    val messageType = JSONObject(stompMessage.payload).getString("messageType")
                    val dataJson = JSONObject(stompMessage.payload).getJSONObject("data").toString()
                    Log.d("my log", "data 값 - ${dataJson}")

                    val jsonObject = JSONObject(dataJson)

                    if (messageType == "messageDetail") { // 메세지 전체 출력일 때
                        val chatMessageArray = jsonObject.getJSONArray("chatMessageList")

                        val chatMessageList = mutableListOf<RoomMessageElementResponseDto>()

                        for (i in 0 until chatMessageArray.length()) {
                            val chatMessageObject = chatMessageArray.getJSONObject(i)
                            val userObject = chatMessageObject.getJSONObject("user")
                            val user = ChatUserResponseDto(
                                sessionId = userObject.getString("sessionId"),
                                name = userObject.getString("name"),
                                profile = userObject.getString("profile"),
                                isOwner = userObject.getBoolean("isOwner")
                            )
                            val content = chatMessageObject.getString("content")
                            val time = chatMessageObject.getString("time")
                            val image = chatMessageObject.getString("image")

                            Log.d("my log", "$image")

                            val chatMessage =
                                RoomMessageElementResponseDto(user, content, time, image)
                            chatMessageList.add(chatMessage)
                        }

                        adapter.setData(chatMessageList)

                    } else { // 하나씩 들어올 때
                        val user = ChatUserResponseDto( // 수정 필요
                            sessionId = jsonObject.getString("sessionId"),
                            name = jsonObject.getString("userName"),
                            profile = jsonObject.getString("profile"),
                            isOwner = jsonObject.getBoolean("isOwner")
                        )
                        val content = jsonObject.getString("content")
                        val time = jsonObject.getString("time")
                        val image = jsonObject.getString("image")

                        val newChat = RoomMessageElementResponseDto(user, content, time, image)

                        adapter.updateData(newChat)
                    }

                    // RecyclerView의 스크롤을 맨 아래로 이동
                    binding.chattingRecyclerview.scrollToPosition(adapter.itemCount - 1)

                }, { throwable ->
                    Log.e(
                        "Stomp subscribe error",
                        "Error on subscribe: ${throwable.localizedMessage}",
                        throwable
                    )
                })
        )

        Handler(Looper.getMainLooper()).postDelayed({
            compositeDisposable.add(
                stompClient.send("/pub/room/detail/$roomName")
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        {
                            Log.d("Chat", "Chat detail request sent for session $roomName")
                        }, { throwable ->
                            Log.e(
                                "Chat",
                                "Failed to send chat detail request: ${throwable.localizedMessage}",
                                throwable
                            )
                        })
            )
        }, 100)
    }

    private val singleImagePicker =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            if (uri != null) {
                val profileUri = uri

                // URI에 대한 지속적인 권한을 부여
                val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION
                this.contentResolver.takePersistableUriPermission(uri, flag)

                val imagePart: MultipartBody.Part? = profileUri?.let {
                    val file = File(
                        ModProfileActivity.URIPathHelper().getPath(this, it) ?: return@let null
                    )
                    val requestBody = RequestBody.create("image/*".toMediaTypeOrNull(), file)
                    MultipartBody.Part.createFormData("file", file.name, requestBody)
                }

                MypageManager.imageUpload(authToken!!, imagePart) {
                    if (it?.status == 200) {
                        Log.d("my log", "${it.data}")
                        selectedImg = it.data

                        binding.image.visibility = View.VISIBLE
                        Glide.with(binding.image)
                            .load(selectedImg)
                            .into(binding.image)
                        //sendMessage("이미지", it.data )
                    }
                }

            }
        }

    override fun onDestroy() {
        super.onDestroy()
        stompClient.disconnect()
    }

}