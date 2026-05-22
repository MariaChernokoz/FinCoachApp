package com.example.fincoach

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        // Держим сплэш пока не будет готово
        splashScreen.setKeepOnScreenCondition { false }

        // Сразу переходим в MainActivity
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}