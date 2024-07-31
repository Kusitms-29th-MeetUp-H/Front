package com.example.sync_front.ui.main.home

import SyncPagerAdapter
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.example.sync_front.R
import com.example.sync_front.api_server.MypageManager
import com.example.sync_front.data.model.Sync
import com.example.sync_front.databinding.FragmentHomeBinding
import com.example.sync_front.ui.alarm.AlarmActivity
import com.example.sync_front.ui.associate.AssociateActivity
import com.example.sync_front.ui.friend.FriendActivity
import com.example.sync_front.ui.interest.InterestActivity
import com.example.sync_front.ui.main.MainActivity
import com.example.sync_front.ui.open.OpenActivity
import com.example.sync_front.ui.sync.SyncActivity
import com.example.sync_front.ui.type.TypeListActivity
import java.util.*

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: HomeViewModel
    private lateinit var syncAdapter: SyncAdapter
    private lateinit var associateAdapter: AssociateSyncAdapter
    private lateinit var name: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        binding.viewModel = viewModel  // ViewModel을 Binding에 연결
        binding.lifecycleOwner = viewLifecycleOwner  // LifecycleOwner 설정

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUser()
        setupRecyclerView()
        subscribeUi()
        setupClickListeners()
        fetchAllData()  // 초기 데이터 로딩

        // 현재 언어가 영어(en)일 경우 로딩 이미지 표시
        if (loadLanguageSetting() == "en") {
            showLoadingImageForDuration(5000)
        }
    }

    private fun setupUser() {
        // 소셜 로그인으로 얻은 유저이름, 프로필 꺼내기
        val sharedPreferences =
            requireActivity().getSharedPreferences("my_token", Context.MODE_PRIVATE)
        name = sharedPreferences.getString("name", null)!!
        val authToken = sharedPreferences.getString("auth_token", null)

        MypageManager.mypage(authToken!!, "한국어") { response ->
            if (response?.status == 200) {
                binding.homeUsername.text = response.data.name
            }
        }
    }

    private fun getCurrentLanguageForApi(): String? {
        val currentLocale = loadLanguageSetting() ?: Locale.getDefault().language
        return when (currentLocale) {
            "en" -> "영어"
            "ko" -> "한국어"
            else -> null
        }
    }

    private fun showLoadingImageForDuration(duration: Long) {
        val activity = activity as MainActivity
        activity.showLoadingOverlay(duration)
    }

    private fun setupRecyclerView() {
        syncAdapter = SyncAdapter(listOf(), object : SyncAdapter.OnSyncClickListener {
            override fun onSyncClick(sync: Sync) {
                openSyncActivity(sync)
            }
        })
        binding.homeRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = syncAdapter
        }
        associateAdapter = AssociateSyncAdapter(listOf(), object : SyncAdapter.OnSyncClickListener {
            override fun onSyncClick(sync: Sync) {
                openSyncActivity(sync)
            }
        })
        binding.homeDiscountRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = associateAdapter
        }
    }

    private fun openSyncActivity(sync: Sync) {
        val intent = Intent(context, SyncActivity::class.java).apply {
            putExtra("syncId", sync.syncId)
        }
        startActivity(intent)
    }

    private fun fetchAllData() {
        val language = getCurrentLanguageForApi() // 현재 언어 설정을 동적으로 받아옵니다.
        viewModel.fetchSyncs(3, language = language)
        viewModel.fetchAssociateSyncs(3, language = language)
        viewModel.fetchRecommendSyncs(2, language = language)
    }

    private fun subscribeUi() {
        // 추천 Syncs 데이터 관찰
        viewModel.recommendSyncs.observe(viewLifecycleOwner) { recommendSyncs ->
            Log.d("HomeFragment", "Recommended Syncs observed: ${recommendSyncs.size}")
            if (recommendSyncs.isNotEmpty()) {
                // ViewPager를 추천 Syncs로 설정
                setupViewPager(recommendSyncs)
            }
        }
        viewModel.syncs.observe(viewLifecycleOwner) { syncs ->
            Log.d("HomeFragment", "Syncs observed: ${syncs.size}")
            if (syncs.isNotEmpty()) {
                syncAdapter.updateSyncs(syncs)
            }
        }
        viewModel.associateSyncs.observe(viewLifecycleOwner) { associateSyncs ->
            if (associateSyncs.isNotEmpty()) {
                associateAdapter.updateSyncs(associateSyncs)
            }
        }
        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            Log.e("HomeFragment", "Error observed: $message")
        }


    }

    private fun setupViewPager(syncList: List<Sync>) {
        val adapter = SyncPagerAdapter(syncList, object : SyncAdapter.OnSyncClickListener {
            override fun onSyncClick(sync: Sync) {
                openSyncActivity(sync)
            }
        })
        binding.homeVp1.adapter = adapter
        binding.homeVp1.orientation = ViewPager2.ORIENTATION_HORIZONTAL

        // ViewPager에 패딩 추가 및 clipToPadding 설정
        val pageMarginPx = resources.getDimensionPixelOffset(R.dimen.pageMargin)
        val offsetPx = resources.getDimensionPixelOffset(R.dimen.offset)

        // MIN_SCALE과 MIN_ALPHA 값을 여기에 정의
        val MIN_SCALE = 0.85f
        val MIN_ALPHA = 0.5f

        binding.homeVp1.apply {
            setPageTransformer { page, position ->
                val myOffset = position * -(2 * offsetPx + pageMarginPx)
                page.translationX = myOffset
                val scaleFactor = Math.max(MIN_SCALE, 1 - Math.abs(position))
                page.scaleX = scaleFactor
                page.scaleY = scaleFactor
                page.alpha =
                    MIN_ALPHA + ((scaleFactor - MIN_SCALE) / (1 - MIN_SCALE) * (1 - MIN_ALPHA))
            }
            setPadding(offsetPx, 0, offsetPx, 0)
        }
    }

    //설정 언어 바꾸기

    private fun saveLanguageSetting(languageCode: String) {
        val prefs = requireActivity().getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        prefs.edit().putString("AppLanguage", languageCode).apply()
    }

    private fun loadLanguageSetting(): String? {
        val prefs = requireActivity().getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        return prefs.getString("AppLanguage", Locale.getDefault().language)
    }


    private fun setLocale(languageCode: String) {
        saveLanguageSetting(languageCode)  // 언어 설정 저장
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
        restartActivity()  // 액티비티 재시작
    }

    private fun toggleLanguage() {
        val currentLanguage = loadLanguageSetting() ?: Locale.getDefault().language
        val newLanguage = if (currentLanguage == "en") "ko" else "en"
        setLocale(newLanguage)
    }


    private fun restartActivity() {
        val currentActivity = activity
        val intent = Intent(currentActivity, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        currentActivity?.startActivity(intent)
        currentActivity?.finish()
    }


    private fun setupClickListeners() {
        binding.alarm.setOnClickListener {
            val intent = Intent(context, AlarmActivity::class.java)
            startActivity(intent)
        }
        binding.earth.setOnClickListener {
            // 언어를 변경하는거
            toggleLanguage()
        }
        binding.fabOpen.setOnClickListener {
            viewModel.authToken?.let { authToken ->
                openOpenActivity(authToken)
            }
        }

        binding.boxOnetime.setOnClickListener {
            val intent = Intent(context, TypeListActivity::class.java).apply {
                putExtra("selectedTab", "일회성") // "onetime" 탭 선택
            }
            startActivity(intent)
        }
        binding.boxPersistence.setOnClickListener {
            val intent = Intent(context, TypeListActivity::class.java).apply {
                putExtra("selectedTab", "지속성") // "persistence" 탭 선택
            }
            startActivity(intent)
        }
        binding.boxForeignLanguage.setOnClickListener {
            val intent = Intent(context, InterestActivity::class.java).apply {
                putExtra("selectedTab", "외국어")
            }
            startActivity(intent)
        }
        binding.boxCulture.setOnClickListener {
            val intent = Intent(context, InterestActivity::class.java).apply {
                putExtra("selectedTab", "문화/예술")
            }
            startActivity(intent)
        }
        binding.boxTravel.setOnClickListener {
            val intent = Intent(context, InterestActivity::class.java).apply {
                putExtra("selectedTab", "여행/동행")
            }
            startActivity(intent)
        }
        binding.boxActivity.setOnClickListener {
            val intent = Intent(context, InterestActivity::class.java).apply {
                putExtra("selectedTab", "액티비티")
            }
            startActivity(intent)
        }
        binding.boxFood.setOnClickListener {
            val intent = Intent(context, InterestActivity::class.java).apply {
                putExtra("selectedTab", "푸드드링크")
            }
            startActivity(intent)
        }
        binding.boxEtc.setOnClickListener {
            val intent = Intent(context, InterestActivity::class.java).apply {
                putExtra("selectedTab", "기타")
            }
            startActivity(intent)
        }
        binding.friendAll.setOnClickListener {
            val intent = Intent(context, FriendActivity::class.java)
            startActivity(intent)
        }
        binding.associateAll.setOnClickListener {
            val intent = Intent(context, AssociateActivity::class.java)
            startActivity(intent)
        }
    }

    private fun openOpenActivity(token: String) {
        val intent = Intent(context, OpenActivity::class.java).apply {
            putExtra("token", token)
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}