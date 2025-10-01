package com.example.sumdays.auth

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * MasterKey 및 EncryptedSharedPreferences를 전혀 사용하지 않는 새로운 세션 관리자입니다.
 * AndroidKeyStore를 직접 사용하여 암호화 키를 관리하고, Cipher API로 데이터를 암호화/복호화합니다.
 * 이 방식은 androidx.security:security-crypto 라이브러리에 대한 의존성이 없습니다.
 */
object SessionManager {

    private const val PREFS_NAME = "sumdays_keystore_prefs"
    private const val KEY_USER_ID = "user_id"

    // 암호화된 데이터를 저장할 Key
    private const val ENCRYPTED_AUTH_TOKEN = "encrypted_auth_token"
    // 암호화에 사용된 IV(Initialization Vector)를 저장할 Key
    private const val ENCRYPTION_IV = "encryption_iv"

    // AndroidKeyStore 관련 상수
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "SumdaysKeyAlias"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var keyStore: KeyStore

    /**
     * 이 함수를 Application 클래스나 MainActivity의 onCreate에서 최초 한 번만 호출해야 합니다.
     */
    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // AndroidKeyStore 인스턴스를 로드합니다.
        keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }

        // KeyStore에 암호화 키가 없으면 새로 생성합니다.
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            generateSecretKey()
        }
    }

    private fun generateSecretKey() {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)

        val parameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).apply {
            setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            setKeySize(256) // 256비트 AES 키
            // API 23 이상에서만 사용 가능
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                setUserAuthenticationRequired(false) // 이 예제에서는 지문인증 등은 사용하지 않음
            }
        }.build()

        keyGenerator.init(parameterSpec)
        keyGenerator.generateKey()
    }

    private fun getSecretKey(): SecretKey {
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }

    /**
     * 로그인 성공 시 사용자 정보와 인증 토큰을 암호화하여 저장합니다.
     */
    fun saveSession(userId: Int, token: String) {
        try {
            val secretKey = getSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            val iv = cipher.iv // 암호화에 사용된 IV를 저장해야 합니다.
            val encryptedTokenBytes = cipher.doFinal(token.toByteArray(Charsets.UTF_8))

            // 바이트 배열을 SharedPreferences에 저장하기 위해 Base64 문자열로 인코딩합니다.
            val encryptedTokenString = Base64.encodeToString(encryptedTokenBytes, Base64.DEFAULT)
            val ivString = Base64.encodeToString(iv, Base64.DEFAULT)

            sharedPreferences.edit().apply {
                putInt(KEY_USER_ID, userId)
                putString(ENCRYPTED_AUTH_TOKEN, encryptedTokenString)
                putString(ENCRYPTION_IV, ivString)
                apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // 암호화 실패 처리
        }
    }

    /**
     * 저장된 인증 토큰을 복호화하여 가져옵니다.
     */
    fun getToken(): String? {
        val encryptedTokenString = sharedPreferences.getString(ENCRYPTED_AUTH_TOKEN, null)
        val ivString = sharedPreferences.getString(ENCRYPTION_IV, null)

        if (encryptedTokenString == null || ivString == null) {
            return null
        }

        return try {
            // Base64 문자열을 다시 바이트 배열로 디코딩합니다.
            val encryptedTokenBytes = Base64.decode(encryptedTokenString, Base64.DEFAULT)
            val iv = Base64.decode(ivString, Base64.DEFAULT)

            val secretKey = getSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, iv) // GCM 모드는 IV와 함께 초기화해야 합니다.
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            val decryptedTokenBytes = cipher.doFinal(encryptedTokenBytes)
            String(decryptedTokenBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            // 복호화 실패 시 null 반환
            null
        }
    }

    /**
     * 저장된 사용자 ID를 가져옵니다. (ID는 민감 정보가 아니므로 암호화하지 않음)
     */
    fun getUserId(): Int {
        return sharedPreferences.getInt(KEY_USER_ID, -1)
    }

    /**
     * 로그인 상태인지 확인합니다.
     */
    fun isLoggedIn(): Boolean {
        // 토큰이 성공적으로 복호화되는지로 로그인 상태를 판단합니다.
        return getToken() != null
    }

    /**
     * 로그아웃 시 저장된 모든 세션 정보를 삭제합니다.
     */
    fun clearSession() {
        sharedPreferences.edit().clear().apply()
    }
}
/*
```

### 실제 사용 방법

1.  **초기화:** 앱이 시작될 때 딱 한 번 `init()` 함수를 호출해줍니다. `Application` 클래스나 앱의 첫 화면인 `MainActivity`의 `onCreate`에서 호출하는 것이 좋습니다.
```kotlin
// MainActivity.kt 또는 App.kt
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    SessionManager.init(applicationContext) // 앱 실행 시 초기화

    // ...
}
```

2.  **로그인 성공 시 저장:** `LoginActivity`에서 서버로부터 로그인 성공 응답을 받으면 `saveSession`을 호출합니다.
```kotlin
// LoginActivity.kt - handleLogin() 내부
// ... 로그인 API 호출 성공 후 ...
val userId = response.body()!!.userId // 예시: 응답에 userId가 포함
val token = response.body()!!.token   // 예시: 응답에 token이 포함

// SessionManager를 통해 로그인 정보 저장
SessionManager.saveSession(userId, token)

// 메인 화면으로 이동
// ...
```

3.  **로그인 상태 확인:** 앱의 시작점(예: `MainActivity`)에서 로그인 상태를 확인하고 분기 처리합니다.
```kotlin
// MainActivity.kt - onCreate() 내부
if (SessionManager.isLoggedIn()) {
    // 로그인 상태이므로 메인 컨텐츠를 보여줌
    showMainContent()
} else {
    // 비로그인 상태이므로 로그인 화면으로 이동
    val intent = Intent(this, LoginActivity::class.java)
    startActivity(intent)
    finish()
}
```

4.  **API 요청 시 토큰 사용:** 서버 API를 호출할 때 헤더에 저장된 토큰을 추가합니다. (Retrofit Interceptor 사용 시 편리)
```kotlin
// Retrofit Interceptor 예시
val token = SessionManager.getToken()
if (token != null) {
    request.newBuilder()
        .addHeader("Authorization", "Bearer $token")
        .build()
}
```

5.  **로그아웃:** 로그아웃 버튼을 누르면 `clearSession`을 호출합니다.
```kotlin
// 설정 화면 등에서
logoutButton.setOnClickListener {
    SessionManager.clearSession()
    // 로그인 화면으로 이동
    val intent = Intent(this, LoginActivity::class.java)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    startActivity(intent)
}
*/