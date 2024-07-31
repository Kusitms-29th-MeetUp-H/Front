package com.example.sync_front.ui.sync

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import com.example.sync_front.databinding.ActivitySyncBinding
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContentProviderCompat.requireContext
import com.google.android.material.tabs.TabLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.sync_front.R
import com.example.sync_front.api_server.ReviewManager
import com.example.sync_front.data.model.BookmarkRequest
import com.example.sync_front.data.model.Sync
import com.example.sync_front.databinding.PopupJoinSyncBinding
import com.example.sync_front.databinding.PopupOpenSyncBinding
import com.example.sync_front.ui.main.MainActivity
import com.example.sync_front.ui.main.home.SyncAdapter
import java.util.function.LongFunction


class SyncActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySyncBinding
    private lateinit var circleGraphView: CircleGraphView
    private lateinit var viewModel: SyncViewModel
    private lateinit var reviewAdapter: ReviewAdapter
    private lateinit var sameSyncAdapter: SyncAdapter
    private var syncId: Long = 0
    private var smallerDataName: String = "외국인"
    private var token: String? = null
    private var bookmark: Boolean = false // 북마크 중: true, 기본: false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        syncId = intent.getLongExtra("syncId", -1L)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_sync)
        viewModel = ViewModelProvider(this).get(SyncViewModel::class.java)

        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        viewModel.fetchSyncDetail(syncId)
        viewModel.fetchGraphData("national", syncId)

        // 저장된 토큰 읽어오기
        val sharedPreferences = getSharedPreferences("my_token", Context.MODE_PRIVATE)
        token = sharedPreferences.getString("auth_token", null)




        circleGraphView = binding.circle  // circleGraphView 초기화
        setToolbarButton()
        setupTabs(binding.root)
        subscribeUi()
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
        updateGraphTextViews("${smallerDataName}보다 ", "의 비율이 더 높은 편이에요")
    }

    private fun subscribeUi() {
        viewModel.fetchReviews(syncId, 3)
        viewModel.fetchSameSyncData(syncId, 3)
    }

    private fun observeViewModel() {
        viewModel.reviews.observe(this, Observer {
            //reviewAdapter.updateReviews(it)
            if (it.isEmpty()) {
                binding.syncLinear4.visibility = View.GONE
            } else {
                binding.syncLinear4.visibility = View.VISIBLE
                reviewAdapter.updateReviews(it)
            }
        })
        viewModel.sames.observe(this, Observer {
            sameSyncAdapter.updateSyncs(it)
        })
        // ViewModel 의 LiveData 관찰자 설정
        viewModel.syncDetail.observe(this, Observer { syncDetail ->
            Glide.with(binding.ivSyncImg.context)
                .load(syncDetail.syncImage)
                .into(binding.ivSyncImg)

            Glide.with(binding.profileImg.context)
                .load(syncDetail.userImage)
                .into(binding.profileImg)

            // regularDate가 null인지 체크
            if (syncDetail.regularDate == null) {
                // regularDate가 null이면, "일시"로 텍스트 설정
                binding.tvDateTitle.text = "일시"
                binding.tvDate.text = syncDetail.date
                binding.syncLinear4.visibility = View.GONE // 일회성은 리뷰 제거
            } else {
                // regularDate가 null이 아니면, regularDate 값을 텍스트로 설정
                binding.tvDate.text = "${syncDetail.regularDate}\n첫 모임 날짜: ${syncDetail.date}"
            }
            binding.tvCnt.text = "최소 ${syncDetail.userCnt}명 최대 ${syncDetail.totalCnt}명"

            //가입 버튼
            //꽉차거나, 가입했거나, 주인장이면 false
            if (syncDetail.isFull || syncDetail.isJoin || syncDetail.isOwner) {
                binding.btnJoin.isEnabled = false
                binding.btnJoin.setBackgroundResource(R.drawable.btn_gray_10)
                binding.btnJoin.setTextColor(getResources().getColor(R.color.gray_70))
                binding.btnJoin.setTextAppearance(R.style.Body1_400) // 스타일 변경

                if (syncDetail.isFull) {
                    binding.btnJoin.text = "이미 꽉 찬 싱크예요"
                } else if (syncDetail.isJoin) {
                    binding.btnJoin.text = "이미 가입한 싱크예요"
                } else if (syncDetail.isOwner) {
                    binding.btnJoin.text = "내가 개설한 싱크예요"
                }
            }
            //북마크 한 싱크면 채워지게
            binding.btnBookmark.isSelected = syncDetail.isMarked
            bookmark = syncDetail.isMarked // 북마크 여부 체크
        })
        viewModel.graphDetails.observe(this, Observer { details ->
            val graphData = details.data.sortedByDescending { it.percent }
            val sortedData = graphData.map { it.percent.toFloat() }
            Log.d("GraphUpdate", "Updating graph with data: $sortedData")
            when (sortedData.size) {
                0 -> setupCirCleGraphView(0f, 0f)  // 모든 데이터가 없는 경우
                1 -> setupCirCleGraphView(sortedData[0], 0f)
                2 -> {
                    setupCirCleGraphView(sortedData[0], sortedData[1])
                    smallerDataName =
                        if (graphData[0].percent > graphData[1].percent) graphData[1].name else graphData[0].name
                    Log.d("GraphDetail", "Smaller data name: $smallerDataName")
                }

                3 -> setupCirCleGraphView(sortedData[0], sortedData[1], sortedData[2])
                else -> {
                    val topThree = sortedData.take(3)
                    val others = sortedData.drop(3).sum()
                    setupCirCleGraphView(topThree[0], topThree[1], topThree[2], others)
                }
            }
        })
    }

    private fun setupClickListeners() {
        binding.btnBookmark.setOnClickListener {
            if (!bookmark) {
                // 북마크 등록
                sendBookMark(syncId, bookmark)
            } else {
                // 북마크 취소
                sendBookMark(syncId, bookmark)
            }
        }
        binding.btnJoin.setOnClickListener {
            if (it.isEnabled) {
                viewModel.fetchJoinSync(syncId)
                showPopup()
            }
        }
    }

    private fun setToolbarButton() {
        binding.syncToolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupTabs(view: View) {
        val tabLayout = binding.syncTabs
        tabLayout.addTab(tabLayout.newTab().setText("내외국인"))
        tabLayout.addTab(tabLayout.newTab().setText("성별"))
        tabLayout.addTab(tabLayout.newTab().setText("대학"))
        tabLayout.addTab(tabLayout.newTab().setText("참여수"))

        // 탭 선택 리스너 추가
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        viewModel.fetchGraphData("national", syncId)
                        updateGraphTextViews("${smallerDataName}보다 ", "의 비율이 더 높은 편이에요")
                    }

                    1 -> {
                        viewModel.fetchGraphData("gender", syncId)
                        updateGraphTextViews("", "의 참여율이 더 높은 편이에요.")
                    }

                    2 -> {
                        viewModel.fetchGraphData("university", syncId)
                        updateGraphTextViews("", "의 참여율이 더 높은 편이에요.")
                    }

                    3 -> {
                        viewModel.fetchGraphData("participate", syncId)
                        updateGraphTextViews("싱크에 ", " 멤버들이 가장 많은 편이에요.")
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
        // 첫 번째 탭을 프로그래매틱하게 선택
        tabLayout.post {
            tabLayout.getTabAt(0)?.select()
        }
    }

    private fun updateGraphTextViews(prefix: String, suffix: String) {
        binding.tvGraph1.text = prefix
        viewModel.graphDetails.observe(this, Observer { details ->
            if (details.data.isNotEmpty()) {
                binding.tvGraph2.text = details.status
                // details.status가 "요"로 끝나는지 확인
                if (details.status.endsWith("요")) {
                    binding.tvGraph3.text = ""
                } else {
                    binding.tvGraph3.text = suffix
                }
            }
        })
        //binding.tvGraph3.text = suffix
    }


    private fun setupCirCleGraphView(
        first: Float,
        second: Float,
        third: Float? = 0f,
        fourth: Float? = 0f
    ) {
        circleGraphView = binding.circle
        circleGraphView.animateSections(first, second, third, fourth)
    }

    private fun setupRecyclerView() {
        reviewAdapter = ReviewAdapter(listOf(), token!!)
        binding.syncReviewRecyclerView.apply {
            adapter = reviewAdapter
            layoutManager = LinearLayoutManager(context)
        }
        sameSyncAdapter = SyncAdapter(listOf(), object : SyncAdapter.OnSyncClickListener {
            override fun onSyncClick(sync: Sync) {
                openSyncActivity(sync)
            }
        })
        binding.syncSameDayRecyclerView.apply {
            adapter = sameSyncAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun openSyncActivity(sync: Sync) {
        val intent = Intent(this, SyncActivity::class.java).apply {
            putExtra("syncId", sync.syncId)
        }
        startActivity(intent)
    }

    private fun showPopup() {
        val popupLayoutBinding = PopupJoinSyncBinding.inflate(layoutInflater)
        val popupView = popupLayoutBinding.root

        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setView(popupView)

        val alertDialog = alertDialogBuilder.create()
        alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        alertDialog.show()
        popupLayoutBinding.doneBtn.setOnClickListener {


            alertDialog.dismiss() // 팝업 닫기
            // 현재 액티비티 종료
            this.finish()

            // MainActivity의 MyFragment로 이동
            val mainActivityIntent = Intent(this, MainActivity::class.java)
            mainActivityIntent.putExtra(
                "fragment",
                "MyFragment"
            ) // MyFragment로 이동하는 것을 나타내는 플래그나 데이터를 전달할 수 있습니다.
            startActivity(mainActivityIntent)
        }

    }

    private fun sendBookMark(syncId: Long, isBookmarked: Boolean) {
        val request = BookmarkRequest(syncId, isBookmarked)
        ReviewManager.sendBookmark(token!!, request) {
            if (it!!.status == 200) {
                binding.btnBookmark.isSelected = it.data == true
                bookmark = !isBookmarked
            }
        }
    }
}