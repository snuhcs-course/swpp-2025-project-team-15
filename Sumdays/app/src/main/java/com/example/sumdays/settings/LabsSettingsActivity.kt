package com.example.sumdays.settings

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.sumdays.R
import com.example.sumdays.databinding.ActivitySettingsLabsBinding
import com.example.sumdays.utils.setupEdgeToEdge

class LabsSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsLabsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsLabsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupHeader()

        // 상태바, 네비게이션바 같은 색으로
        val rootView = findViewById<View>(R.id.setting_labs_root)
        setupEdgeToEdge(rootView)
    }

    private fun setupHeader() {
        binding.header.headerTitle.text = "Labs"
        binding.header.headerBackIcon.setOnClickListener { finish() }
    }
}