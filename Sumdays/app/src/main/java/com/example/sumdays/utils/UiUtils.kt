package com.example.sumdays.utils

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import android.view.Window // Window 클래스를 명시적으로 import 해주는 것이 좋습니다.


fun Activity.setupEdgeToEdge(rootView: View) {
    // 1. Edge-to-Edge 모드 활성화 및 시스템 바 색상 투명 설정
    // 'window'는 Activity의 속성으로 바로 접근 가능합니다.
    WindowCompat.setDecorFitsSystemWindows(window, false)
    window.statusBarColor = Color.TRANSPARENT
    window.navigationBarColor = Color.TRANSPARENT

    // 2. 내비게이션 바 오버레이(Scrim) 제거 (API 29 이상에서 미세한 색상 차이 제거)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        // 명암 대비 강제 적용을 해제하여 투명도를 100%로 만듭니다.
        window.isNavigationBarContrastEnforced = false
    }

    // 3. 시스템 아이콘 밝기 설정 (어두운 배경을 가정하여 아이콘을 밝게 유지: false)
    val controller = ViewCompat.getWindowInsetsController(window.decorView)
    if (controller != null) {
        // isAppearanceLight... = true 일 때 아이콘이 어두워집니다.
        // 배경이 어두우므로 아이콘을 밝게 유지(false)합니다.
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
    }

    // 4. 인셋 리스너를 통한 패딩 설정
    ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
        val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
        val navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom

        // updatePadding 함수를 사용하여 기존 좌우 패딩 값은 유지하고 상하 패딩만 업데이트합니다.
        view.updatePadding(
            top = statusBarHeight,
            bottom = navBarHeight
        )

        insets // 인셋을 반환하여 시스템 처리를 완료합니다.
    }
}