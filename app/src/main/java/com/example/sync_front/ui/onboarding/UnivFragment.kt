package com.example.sync_front.ui.onboarding

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.sync_front.R
import com.example.sync_front.api_server.Onboarding
import com.example.sync_front.databinding.FragmentUnivBinding

class UnivFragment : Fragment() {
    lateinit var binding: FragmentUnivBinding
    private lateinit var language: String
    private var profile: String ?= null
    private lateinit var name: String
    private lateinit var national: String
    private lateinit var gender: String
    private lateinit var univ: String
    private val args: UnivFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragmentUnivBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.doneBtn.isEnabled = false

        // 전달된 데이터 읽기
        language = args.onboarding.language!!
        profile = args.onboarding.profile
        name = args.onboarding.userName!!
        national = args.onboarding.countryName!!
        gender = args.onboarding.gender!!

        setupClickListeners()
        setUpChangedListener()
    }

    private fun setupClickListeners() {
        binding.doneBtn.setOnClickListener {
            if (binding.doneBtn.isEnabled) {
                univ = binding.univ.text.toString()
                // 대학명 확인 요청 API 추가

                val action = UnivFragmentDirections.actionUnivFragmentToEmailFragment(
                    Onboarding(language, profile, name, national, gender, univ, null, null)
                )
                findNavController().navigate(action)
            }
        }

        binding.beforeBtn.setOnClickListener {
            findNavController().navigateUp() // 이전 프래그먼트로
        }

        binding.textCancel.setOnClickListener {
            binding.univ.setText("") // 텍스트 초기화
        }

        binding.root.setOnClickListener {
            hideKeyboard()
        }
    }

    private fun setUpChangedListener() { // 이름에 값이 들어갈 때 다음 버튼 활성화
        binding.univ.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                binding.doneBtn.isEnabled = s?.isNotBlank() ?: false
                updateDoneButtonBackground()
            }
        })
    }

    private fun updateDoneButtonBackground() {
        if (binding.doneBtn.isEnabled) { // 다음 버튼 스타일 변경
            binding.doneBtn.setTextColor(context!!.resources.getColor(R.color.white))
            binding.doneBtn.setBackgroundResource(R.drawable.btn_default)
        } else {
            binding.doneBtn.setTextColor(context!!.resources.getColor(R.color.gray_70))
            binding.doneBtn.setBackgroundResource(R.drawable.btn_gray_10)
        }
    }

    private fun hideKeyboard() {
        binding.univ.clearFocus()
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }
}