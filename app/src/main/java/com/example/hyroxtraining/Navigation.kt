package com.example.hyroxtraining

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.hyroxtraining.ui.main.MainScreen

@Composable
fun MainNavigation(
    isDark: Boolean,
    onThemeToggle: () -> Unit,
    fontScale: Float,
    onFontScaleChange: (Float) -> Unit
) {
  val backStack = rememberNavBackStack(Main)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Main> {
          MainScreen(
              onItemClick = { navKey -> backStack.add(navKey) },
              isDark = isDark,
              onThemeToggle = onThemeToggle,
              fontScale = fontScale,
              onFontScaleChange = onFontScaleChange,
              modifier = Modifier.fillMaxSize()
          )
        }
      },
  )
}
