package com.example.sync_front.ui.alarm

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sync_front.databinding.ActivityAlarmBinding
import com.google.android.material.tabs.TabLayout

class AlarmActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAlarmBinding
    private val viewModel: AlarmViewModel by viewModels()
    private val adapter = AlarmAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlarmBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupTabLayout()
        subscribeToViewModel()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.listToolbar)
        binding.listToolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun setupTabLayout() {
        binding.tabLayout1.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val category = when (tab?.position) {
                    0 -> "활동"
                    1 -> "내싱크"
                    else -> "활동"
                }
                viewModel.fetchNotifications(category)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun subscribeToViewModel() {
        viewModel.notifications.observe(this) { notificationResponse ->
            adapter.setAlarms(notificationResponse.alarmData)
        }

        viewModel.errorMessage.observe(this) { errorMessage ->
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
        }
    }
}
