package com.example.sync_front.ui.main.community

import android.annotation.SuppressLint
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.sync_front.R
import com.example.sync_front.api_server.CommunityManager
import com.example.sync_front.api_server.TranslationManager
import com.example.sync_front.api_server.TranslationManager.translate
import com.example.sync_front.data.model.CheckLanguageRequest
import com.example.sync_front.data.model.Comment
import com.example.sync_front.data.model.Content
import com.example.sync_front.data.model.TranslateRequest
import com.example.sync_front.databinding.ActivityCommunityDetailBinding

class CommunityDetailActivity : AppCompatActivity() {
    lateinit var binding: ActivityCommunityDetailBinding
    private lateinit var commentList: List<Comment>
    private lateinit var commentAdapter: CommentAdapter
    private lateinit var imageAdapter: MultiImageLoadAdapter
    private lateinit var imageList: List<String>
    private var authToken: String ?= null // 로그인 토큰
    private var communityId: Int ?= -1
    private var likedByUser: Boolean = false
    private var likeCnt: Int ?= 0
    private var trans: Boolean = false // 번역 중: true, 기본: false
    private var originalTitle: String? = null // 원래 제목
    private var originalContent: String? = null // 원래 내용

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommunityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initialSetting()
        setupClickListeners()
    }

    private fun initialSetting() {
        communityId = intent.getIntExtra("communityId", -1)

        // 저장된 토큰 읽어오기
        val sharedPreferences = getSharedPreferences("my_token", Context.MODE_PRIVATE)
        authToken = sharedPreferences.getString("auth_token", null)

        commentAdapter = CommentAdapter(emptyList<Comment>(), authToken!!)
        binding.commentRecyclerview.layoutManager = LinearLayoutManager(this)
        binding.commentRecyclerview.adapter = commentAdapter

        binding.imgRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        imageAdapter = MultiImageLoadAdapter(emptyList(), this)
        binding.imgRecyclerView.adapter = imageAdapter

        loadCommunity()
    }

    private fun setupClickListeners() {
        binding.sendBtn.setOnClickListener {
            sendComment()
        }

        binding.likeBtn.setOnClickListener {
            sendLike()
        }

        binding.translateBtn.setOnClickListener {
            if (!trans) {
                getTranslate()
            } else {
                returnOriginalText()
            }
        }

        binding.backBtn.setOnClickListener {
            finish()
        }

        binding.root.setOnClickListener {
            hideKeyboard()
        }
    }

    private fun loadCommunity() {
        if (communityId != -1) {
            // 내용 불러오기
            CommunityManager.getCommunityDetail(authToken!!, communityId!!) { response ->
                response?.let {
                    Log.d("my log", "커뮤니티 상세")

                    binding.name.text = it.writerName
                    binding.time.text = it.createdDate
                    binding.title.text = it.title
                    binding.content.text = it.content
                    binding.likeCount.text = it.likeCnt.toString()
                    binding.commentCount.text = it.commentCnt.toString()

                    originalTitle = it.title
                    originalContent = it.content

                    if (!it.writerImage.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(it.writerImage)
                            .into(binding.profile)
                    }

                    if (!it.imageUrls.isNullOrEmpty()) {
                        val imageString = it.imageUrls

                        imageAdapter = MultiImageLoadAdapter(imageString, applicationContext)
                        binding.imgRecyclerView.adapter = imageAdapter
                    }

                    likeCnt = it.likeCnt

                    likedByUser = it.likedByUser // 좋아요 클릭 여부
                    setLikeButtonState(likedByUser)

                    loadComment() // 댓글 목록 불러오기
                }
            }
        }
    }

    private fun loadComment() {
        if (communityId != -1) {
            CommunityManager.getCommentList(authToken!!, communityId!!) { response ->
                response?.let {
                    Log.d("my log", "댓글 목록")

                    commentAdapter.updateData(it)
                }
            }
        }
    }

    private fun sendComment() {
        val content = binding.addCommentText.text.toString()

        if (!content.isNullOrEmpty()) {
            CommunityManager.postComment(authToken!!, communityId!!, Content(content)) { response ->
                if (response == 200) {
                    hideKeyboard()
                    binding.addCommentText.text.clear()
                    loadComment()
                }
            }
        } else {
            Toast.makeText(applicationContext, getString(R.string.input_comment), Toast.LENGTH_LONG).show()
        }
    }

    private fun getTranslate() {
        val title = binding.title.text.toString()
        val content = binding.content.text.toString()

        sendTranslate(title) { transTitle ->
            sendTranslate(content) { transContent ->
                binding.title.text = transTitle
                binding.content.text = transContent
                trans = !trans
                setTransButtonState(trans)
            }
        }
    }

    private fun sendTranslate(text: String, callback: (String) -> Unit) {
        TranslationManager.checkLanguage(authToken!!, CheckLanguageRequest(text)) { response ->
            if (response != null && response.status == 200) {
                val target = if (response.data.langCode == "en") {
                    "ko"
                } else { "en" }

                val request = TranslateRequest(response.data.langCode, target, text)
                translate(authToken!!, request) { translateResponse ->
                    if (translateResponse != null && translateResponse.status == 200) {
                        val result = translateResponse.data
                        callback(result.translatedText)
                    } else {
                        Log.e("TranslationError", "Translation response is null or status is not 200")
                        Toast.makeText(applicationContext, getString(R.string.cant_trans), Toast.LENGTH_LONG).show()
                        callback(text)
                    }
                }
            } else {
                Log.e("TranslationError", "CheckLanguage response is null or status is not 200")
                Toast.makeText(applicationContext, getString(R.string.cant_trans), Toast.LENGTH_LONG).show()
                callback(text)
            }
        }
    }

    private fun returnOriginalText() {
        binding.title.text = originalTitle
        binding.content.text = originalContent
        trans = false
        setTransButtonState(trans)
    }

    private fun sendLike() {
        if (!likedByUser) {
            CommunityManager.postCommunityLike(authToken!!, communityId!!) { response ->
                if (response == 200 ) {
                    likeCnt = likeCnt?.plus(1)
                    likedByUser = !likedByUser
                    setLikeButtonState(likedByUser)
                }
            }
        } else {
            CommunityManager.deleteCommunityLike(authToken!!, communityId!!) { response ->
                if (response == 200) {
                    likeCnt = likeCnt?.minus(1)
                    likedByUser = !likedByUser
                    setLikeButtonState(likedByUser)
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setLikeButtonState(liked: Boolean) {
        if (liked) {
            binding.likeBtn.setBackgroundResource(R.drawable.label_white)
            binding.likeIc.setBackgroundResource(R.drawable.ic_thumbs_up)
            binding.likeCount.text = likeCnt.toString()
            binding.likeCount.setTextColor(this.resources.getColor(R.color.primary))
        } else {
            binding.likeBtn.setBackgroundResource(R.drawable.label_gray10)
            binding.likeIc.setBackgroundResource(R.drawable.ic_thumbs_up_gray)
            binding.likeCount.text = likeCnt.toString()
            binding.likeCount.setTextColor(this.resources.getColor(R.color.gray_50))
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setTransButtonState(trans: Boolean) {
        if (trans) {
            binding.translateBtn.setBackgroundResource(R.drawable.label_white)
            binding.translateIc.setBackgroundResource(R.drawable.ic_translation2)
            binding.translate.setTextColor(this.resources.getColor(R.color.primary))
        } else {
            binding.translateBtn.setBackgroundResource(R.drawable.label_gray10)
            binding.translateIc.setBackgroundResource(R.drawable.ic_translation2)
            binding.translate.setTextColor(this.resources.getColor(R.color.gray_50))
        }
    }

    private fun hideKeyboard() {
        binding.addCommentText.clearFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }


    override fun onResume() {
        super.onResume()

        loadComment() // 댓글 리스트 업데이트
    }
}