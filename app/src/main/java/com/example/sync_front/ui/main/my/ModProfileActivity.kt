package com.example.sync_front.ui.main.my

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContentProviderCompat.requireContext
import com.bumptech.glide.Glide
import com.example.sync_front.R
import com.example.sync_front.api_server.MypageManager
import com.example.sync_front.data.model.ModMypageRequest
import com.example.sync_front.data.model.ModProfileRequest
import com.example.sync_front.data.model.OnboardingRequest
import com.example.sync_front.databinding.ActivityModProfileBinding
import com.google.gson.Gson
import com.kakao.sdk.common.KakaoSdk.type
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File

class ModProfileActivity : AppCompatActivity() {
    lateinit var binding: ActivityModProfileBinding
    val REQUEST_CODE = 1001
    val REQUEST_CODE2 = 1002
    val REQUEST_CODE3 = 1003
    private var profile: Uri ?= null  // 이미지 추가
    private var authToken: String ?= null // 로그인 토큰
    private var existingImageUrl: String? = null  // 서버로부터 가져온 기존 이미지 URL

    private val singleImagePicker =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            if (uri != null) {
                profile = uri

                binding.profileImg.setImageURI(uri)

                // URI에 대한 지속적인 권한을 부여
                val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION
                applicationContext.contentResolver.takePersistableUriPermission(uri, flag)
            } else {
                Toast.makeText(applicationContext, getString(R.string.didnt_select_img), Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityModProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initSetting()
        setupClickListeners()
    }

    private fun initSetting() {
        // 저장된 토큰 읽어오기
        val sharedPreferences = this.getSharedPreferences("my_token", Context.MODE_PRIVATE)
        authToken = sharedPreferences.getString("auth_token", null)

        loadMe()
    }

    private fun loadMe() {
        MypageManager.mypage(authToken!!, "한국어") { response ->
            if (response?.status == 200) {
                binding.username.setText(response.data.name)
                binding.gender.text = response.data.gender
                binding.likePeople.text = response.data.syncType

                val detailTypesString = response.data.detailTypes?.joinToString(", ") ?: ""
                binding.like.text = detailTypesString

                existingImageUrl = response.data.image

                if (!response.data.image.isNullOrEmpty()) {
                    Glide.with(this)
                        .load(response.data.image)
                        .placeholder(R.drawable.img_profile_default)
                        .error(R.drawable.img_profile_default)
                        .into(binding.profileImg)
                } else {
                    binding.profileImg.setImageResource(R.drawable.img_profile_default)
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.profileImg.setOnClickListener {
            singleImagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
        }

        binding.genderLayout.setOnClickListener {
            val itemList = listOf(getString(R.string.woman), getString(R.string.man), getString(R.string.closed))
            val intent = Intent(this, SelectListActivity::class.java)
            intent.putStringArrayListExtra("itemList", ArrayList(itemList))
            startActivityForResult(intent, REQUEST_CODE)
        }

        binding.likePeopleLayout.setOnClickListener {
            val itemList = listOf(
                getString(R.string.type1),
                getString(R.string.type2),
                getString(R.string.type3))
            val intent = Intent(this, SelectListActivity::class.java)
            intent.putStringArrayListExtra("itemList", ArrayList(itemList))
            startActivityForResult(intent, REQUEST_CODE2)
        }

        binding.likeLayout.setOnClickListener {
            val intent = Intent(this, ModInterestActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE3)
        }

        binding.doneBtn.setOnClickListener { // 수정 완료
            modMypage()
        }

        binding.cancelBtn.setOnClickListener {
            finish()
        }

        binding.root.setOnClickListener {
            // 화면의 다른 부분을 클릭하면 EditText의 포커스를 해제하고 키보드를 내림
            hideKeyboard()
        }

        binding.backBtn.setOnClickListener {
            finish()
        }
    }

    private fun modMypage() {
        val name = binding.username.text.toString()
        val gender = binding.gender.text.toString()
        //val syncType = binding.likePeople.text.toString()

        val type: String
        if (binding.likePeople.text.toString() == this.getString(R.string.성향1)) {
            type = "지속성"
        } else if (binding.likePeople.text.toString() == this.getString(R.string.성향2)) {
            type = "일회성"
        } else {
            type = "내친소"
        }
        val detailTypes = binding.like.text.toString().split(", ").map { it.trim() }

        // Prepare the image file if it exists
        val imagePart: MultipartBody.Part? = profile?.let {
            val file = File(URIPathHelper().getPath(this, it) ?: return@let null)
            val requestBody = RequestBody.create("image/*".toMediaTypeOrNull(), file)
            MultipartBody.Part.createFormData("profileImage", file.name, requestBody)
        }

        Log.d("my log", "$name $gender $type")

        val imageUrl = if (profile == null) existingImageUrl else null

        val request = ModMypageRequest(name, gender,type, detailTypes)

        val gson = Gson()
        val requestJson = gson.toJson(request)
        val requestDtoBody = RequestBody.create("application/json".toMediaTypeOrNull(), requestJson)

        if (authToken != null) {
            if (profile != null) {
                MypageManager.modMypage(authToken!!,imagePart, requestDtoBody) { response ->
                    if (response != null) {
                        Log.d("수정 완료", "")
                        finish()
                    }
                }
            }
            else {
                MypageManager.modNotProfileMypage(authToken!!, ModProfileRequest(existingImageUrl, name, gender, type, detailTypes)) {
                    if (it != null) {
                        Log.d("수정 완료", "")
                        finish()
                    }
                }
            }
        }
    }

    class URIPathHelper {
        fun getPath(context: Context, uri: Uri): String? {
            val projection = arrayOf(MediaStore.Images.Media.DATA)
            val cursor: Cursor? = context.contentResolver.query(uri, projection, null, null, null)
            cursor?.use {
                val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                if (it.moveToFirst()) {
                    return it.getString(columnIndex)
                }
            }
            return null
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val selectedItem = data?.getStringExtra("selectedItem")
            Log.d("my log", "전달 받은 값: $selectedItem")
            binding.gender.setText(selectedItem)
        }
        else if (requestCode == REQUEST_CODE2 && resultCode == Activity.RESULT_OK) {
            val selectedItem = data?.getStringExtra("selectedItem")
            Log.d("my log", "전달 받은 값: $selectedItem")
            binding.likePeople.setText(selectedItem)
        }
        else if (requestCode == REQUEST_CODE3 && resultCode == Activity.RESULT_OK) {
            val selectedItems = data?.getStringArrayListExtra("selectedItems")
            Log.d("my log", "전달 받은 값: $selectedItems")

            val selectedItemsText = selectedItems?.joinToString(", ")
            binding.like.setText(selectedItemsText)
        }
    }

    private fun hideKeyboard() {
        binding.username.clearFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }
}