package com.lastdone.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.lastdone.app.R

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val notoSansKr = GoogleFont("Noto Sans KR")

val NotoSansKr = FontFamily(
    Font(googleFont = notoSansKr, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = notoSansKr, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = notoSansKr, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = notoSansKr, fontProvider = provider, weight = FontWeight.Bold)
)

private val baseTypography = Typography()

val LastDoneTypography = Typography(
    displayLarge = baseTypography.displayLarge.copy(fontFamily = NotoSansKr),
    displayMedium = baseTypography.displayMedium.copy(fontFamily = NotoSansKr),
    displaySmall = baseTypography.displaySmall.copy(fontFamily = NotoSansKr),
    headlineLarge = baseTypography.headlineLarge.copy(fontFamily = NotoSansKr),
    headlineMedium = baseTypography.headlineMedium.copy(fontFamily = NotoSansKr),
    headlineSmall = baseTypography.headlineSmall.copy(fontFamily = NotoSansKr),
    titleLarge = baseTypography.titleLarge.copy(fontFamily = NotoSansKr),
    titleMedium = baseTypography.titleMedium.copy(fontFamily = NotoSansKr),
    titleSmall = baseTypography.titleSmall.copy(fontFamily = NotoSansKr),
    bodyLarge = baseTypography.bodyLarge.copy(fontFamily = NotoSansKr),
    bodyMedium = baseTypography.bodyMedium.copy(fontFamily = NotoSansKr),
    bodySmall = baseTypography.bodySmall.copy(fontFamily = NotoSansKr),
    labelLarge = baseTypography.labelLarge.copy(fontFamily = NotoSansKr),
    labelMedium = baseTypography.labelMedium.copy(fontFamily = NotoSansKr),
    labelSmall = baseTypography.labelSmall.copy(fontFamily = NotoSansKr)
)
