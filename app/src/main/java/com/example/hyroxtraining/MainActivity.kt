package com.example.hyroxtraining

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.hyroxtraining.theme.HyroxTrainingTheme

import androidx.compose.runtime.DisposableEffect
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    setContent {
      val sharedPrefs = remember { getSharedPreferences("hyrox_settings", MODE_PRIVATE) }
      val auth = remember { FirebaseAuth.getInstance() }
      var currentUser by remember { mutableStateOf(auth.currentUser) }

      // Listen to auth state changes to dynamically load correct user preferences
      DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
          currentUser = firebaseAuth.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose {
          auth.removeAuthStateListener(listener)
        }
      }

      val uid = currentUser?.uid
      var isDarkTheme by remember(uid) {
        mutableStateOf(
          if (uid != null) {
            sharedPrefs.getBoolean("dark_theme_$uid", true)
          } else {
            true // Login page is agnostic of theme; always dark mode
          }
        )
      }
      var fontScale by remember { mutableStateOf(sharedPrefs.getFloat("font_scale", 1.0f)) }

      HyroxTrainingTheme(darkTheme = isDarkTheme, fontScale = fontScale) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          MainNavigation(
              isDark = isDarkTheme,
              onThemeToggle = {
                val currentUid = currentUser?.uid
                if (currentUid != null) {
                  val key = "dark_theme_$currentUid"
                  val newVal = !isDarkTheme
                  sharedPrefs.edit().putBoolean(key, newVal).apply()
                  isDarkTheme = newVal
                } else {
                  isDarkTheme = !isDarkTheme
                  sharedPrefs.edit().putBoolean("dark_theme", isDarkTheme).apply()
                }
              },
              fontScale = fontScale,
              onFontScaleChange = { newScale ->
                fontScale = newScale
                sharedPrefs.edit().putFloat("font_scale", newScale).apply()
              }
          )
        }
      }
    }
  }
}

