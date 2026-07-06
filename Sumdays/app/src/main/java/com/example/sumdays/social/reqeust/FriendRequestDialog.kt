package com.example.sumdays.social.reqeust

import FriendRequestAdapter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.sumdays.databinding.DialogFriendRequestBinding
import com.google.android.material.tabs.TabLayout
import com.example.sumdays.social.SocialViewModel

class FriendRequestDialog : DialogFragment() {

    private var _binding: DialogFriendRequestBinding? = null
    private val binding get() = _binding!!
    private lateinit var requestAdapter: FriendRequestAdapter
    private lateinit var viewModel: SocialViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogFriendRequestBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[SocialViewModel::class.java]
        setupAdapter()
        setupTabLayout()

        // X 버튼 누르면 닫기
        binding.btnClose.setOnClickListener {
            dismiss()
        }

        lifecycleScope.launch {
            // 1. 받은 요청 목록이 바뀌면 자동으로 현재 탭 리스트 갱신
            launch {
                viewModel.receivedRequests.collect {
                    showList(binding.tabLayout.selectedTabPosition)
                }
            }
            // 2. 보낸 요청 목록이 바뀌면 자동으로 현재 탭 리스트 갱신
            launch {
                viewModel.sentRequests.collect {
                    showList(binding.tabLayout.selectedTabPosition)
                }
            }
            // 3. 에러 상태가 바뀌면 자동으로 현재 탭 리스트 갱신
            launch {
                viewModel.requestLoadFailed.collect {
                    showList(binding.tabLayout.selectedTabPosition)
                }
            }
        }

        // 초기 데이터 로드 (첫 번째 탭: 일반 친구 요청)
        showList(0)
    }

    // adapter 초기화 (버튼 action 등)
    private fun setupAdapter() {
        requestAdapter = FriendRequestAdapter(
            onAccept = { request -> viewModel.acceptFriendRequest(requireContext(), request) },
            onReject = { request -> viewModel.rejectFriendRequest(requireContext(), request) },
            onCancel = { request -> viewModel.cancelSentFriendRequest(requireContext(), request) }
        )

        binding.rvRequests.adapter = requestAdapter
    }

    private fun setupTabLayout() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                showList(tab?.position ?: 0)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun showList(position: Int) {
        // ✅ 1. 무조건 먼저 타입 동기화
        requestAdapter.updateType(if (position == 0) "received" else "sent")

        // 🌟 [수정] 자체 변수가 아닌 ViewModel의 StateFlow 값을 가져오도록 변경
        val currentList = if (position == 0) viewModel.receivedRequests.value else viewModel.sentRequests.value
        val requestsLoadFailed = viewModel.requestLoadFailed.value
        val emptyText = if (position == 0) "받은 친구 요청이 없습니다." else "보낸 친구 요청이 없습니다."

        // 3. 상태 처리
        when {
            requestsLoadFailed -> {
                binding.tvEmptyMessage.text = "데이터를 불러오지 못했습니다."
                binding.tvEmptyMessage.visibility = View.VISIBLE
                requestAdapter.submitList(emptyList())
            }

            currentList.isEmpty() -> {
                binding.tvEmptyMessage.text = emptyText
                binding.tvEmptyMessage.visibility = View.VISIBLE
                requestAdapter.submitList(emptyList())
            }

            else -> {
                binding.tvEmptyMessage.visibility = View.GONE
                requestAdapter.submitList(currentList)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // 다이얼로그 크기를 화면에 맞게 조정
    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}