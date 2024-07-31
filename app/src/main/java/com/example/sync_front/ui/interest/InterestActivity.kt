package com.example.sync_front.ui.interest

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sync_front.R
import com.example.sync_front.data.model.Sync
import com.example.sync_front.databinding.ActivityInterestBinding
import com.example.sync_front.ui.sync.SyncActivity
import com.example.sync_front.ui.type.SyncSquareAdapter
import com.google.android.material.tabs.TabLayout

class InterestActivity : AppCompatActivity() {
    private lateinit var binding: ActivityInterestBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var syncAdapter: SyncSquareAdapter2
    private val interestViewModel: InterestViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInterestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setToolbarButton()

        // 인텐트에서 탭 선택 정보 받기
        val selectedTab = intent.getStringExtra("selectedTab")
        setupTabs(selectedTab)
        interestViewModel.fetchInterestSyncs(type = selectedTab)

        setupRecyclerView()
        observeViewModel()

    }

    private fun setToolbarButton() {
        binding.listToolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupTabs(selectedTab: String?) {
        binding.tabLayout1.apply {
            val tabIndex = when (selectedTab) {
                "외국어" -> 0
                "문화/예술" -> 1
                "여행/동행" -> 2
                "액티비티" -> 3
                "푸드드링크" -> 4
                else -> 5
            }
            selectTab(getTabAt(tabIndex)) // 탭 활성화
        }
        binding.tabLayout1.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val type = when (tab?.position) {
                    0 -> "외국어"
                    1 -> "문화/예술"
                    2 -> "여행/동행"
                    3 -> "액티비티"
                    4 -> "푸드드링크"
                    else -> "기타"
                }
                type.let {
                    // 타입에 따라 ViewModel에서 데이터 불러오기
                    interestViewModel.fetchInterestSyncs(type = it)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                // 탭이 선택 해제되었을 때 필요한 작업
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                // 탭이 다시 선택되었을 때 필요한 작업
            }
        })
    }

    private fun setupRecyclerView() {
        recyclerView = binding.recyclerView
        syncAdapter = SyncSquareAdapter2(listOf(), object : SyncSquareAdapter2.OnSyncClickListener {
            override fun onSyncClick(sync: Sync) {
                openSyncActivity(sync)
            }
        })
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@InterestActivity)
            adapter = syncAdapter
        }
    }

    private fun openSyncActivity(sync: Sync) {
        val intent = Intent(this, SyncActivity::class.java).apply {
            putExtra("syncId", sync.syncId)
        }
        startActivity(intent)
    }

    private fun observeViewModel() {
        interestViewModel.interestSyncs.observe(this) { syncs ->
            if (syncs.isNullOrEmpty()) {
                syncAdapter.updateSyncs(emptyList())
            } else {
                syncAdapter.updateSyncs(syncs)
            }
        }
        interestViewModel.errorMessage.observe(
            this,
            { error -> Toast.makeText(this, "해당 싱크가 없습니다", Toast.LENGTH_SHORT).show() }
        )
    }
}