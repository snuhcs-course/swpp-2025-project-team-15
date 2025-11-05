package com.example.sumdays

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.sumdays.databinding.ActivitySettingsMainBinding
import com.example.sumdays.settings.AccountSettingsActivity
import com.example.sumdays.settings.DiaryStyleSettingsActivity
import com.example.sumdays.settings.NotificationSettingsActivity
import com.example.sumdays.settings.prefs.UserStatsPrefs
import org.threeten.bp.LocalDate

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsMainBinding
    private lateinit var userStatsPrefs: UserStatsPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 2. UserStatsPrefs 인스턴스 초기화
        userStatsPrefs = UserStatsPrefs(this)

        // 3. 닉네임 로드 및 표시 함수 호출
        loadAndDisplayNickname()

        setSettingsBtnListener()
        setNavigationBtnListener()
    }

    /**
     * SharedPreferences에서 닉네임을 가져와 UI에 표시하는 함수
     */
    private fun loadAndDisplayNickname() {
        // 4. 저장된 닉네임 값을 가져옵니다.
        val nickname = userStatsPrefs.getNickname()

        // 5. 가져온 닉네임을 UI의 TextView(userNickname)에 설정합니다.
        // ActivitySettingsMainBinding.xml 레이아웃에 userBlock 내부에
        // userNicknameTextView와 같은 ID를 가진 TextView가 있다고 가정합니다.
        binding.nickname.text = nickname
    }

    private fun setNavigationBtnListener() = with(binding.navigationBar) {
        btnCalendar.setOnClickListener {
            val intent = Intent(this@SettingsActivity, CalendarActivity::class.java)
            startActivity(intent)
        }
        btnDaily.setOnClickListener {
            val today = LocalDate.now().toString()
            val intent = Intent(this@SettingsActivity, DailyWriteActivity::class.java)
            intent.putExtra("date", today)
            startActivity(intent)
        }
        btnInfo.setOnClickListener {

        }
    }

    private fun setSettingsBtnListener() = with(binding) {

        binding.notificationBlock.setOnClickListener {
            startActivity(Intent(this@SettingsActivity, NotificationSettingsActivity::class.java))
        }

        binding.diaryStyleBlock.setOnClickListener {
            startActivity(Intent(this@SettingsActivity, DiaryStyleSettingsActivity::class.java))
        }

        binding.accountBlock.setOnClickListener {
            startActivity(Intent(this@SettingsActivity, AccountSettingsActivity::class.java))
        }
    }
}