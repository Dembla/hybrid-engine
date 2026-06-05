package com.example.hyroxtraining.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.15.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

fun getScaledTypography(scaleFactor: Float): Typography {
    if (scaleFactor == 1.0f) return Typography
    return Typography(
        displayLarge = Typography.displayLarge.copy(
            fontSize = (Typography.displayLarge.fontSize.value * scaleFactor).sp,
            lineHeight = (Typography.displayLarge.lineHeight.value * scaleFactor).sp
        ),
        displayMedium = Typography.displayMedium.copy(
            fontSize = (Typography.displayMedium.fontSize.value * scaleFactor).sp,
            lineHeight = (Typography.displayMedium.lineHeight.value * scaleFactor).sp
        ),
        titleLarge = Typography.titleLarge.copy(
            fontSize = (Typography.titleLarge.fontSize.value * scaleFactor).sp,
            lineHeight = (Typography.titleLarge.lineHeight.value * scaleFactor).sp
        ),
        titleMedium = Typography.titleMedium.copy(
            fontSize = (Typography.titleMedium.fontSize.value * scaleFactor).sp,
            lineHeight = (Typography.titleMedium.lineHeight.value * scaleFactor).sp
        ),
        bodyLarge = Typography.bodyLarge.copy(
            fontSize = (Typography.bodyLarge.fontSize.value * scaleFactor).sp,
            lineHeight = (Typography.bodyLarge.lineHeight.value * scaleFactor).sp
        ),
        bodyMedium = Typography.bodyMedium.copy(
            fontSize = (Typography.bodyMedium.fontSize.value * scaleFactor).sp,
            lineHeight = (Typography.bodyMedium.lineHeight.value * scaleFactor).sp
        ),
        labelLarge = Typography.labelLarge.copy(
            fontSize = (Typography.labelLarge.fontSize.value * scaleFactor).sp,
            lineHeight = (Typography.labelLarge.lineHeight.value * scaleFactor).sp
        ),
        labelSmall = Typography.labelSmall.copy(
            fontSize = (Typography.labelSmall.fontSize.value * scaleFactor).sp,
            lineHeight = (Typography.labelSmall.lineHeight.value * scaleFactor).sp
        )
    )
}

