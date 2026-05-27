package com.lastdone.app.feedback

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.lastdone.app.BuildConfig

private const val FEEDBACK_EMAIL = "hyunsung1987@gmail.com"

fun buildFeedbackMailIntent(@Suppress("UNUSED_PARAMETER") context: Context): Intent {
    val body = """
        ─────────────
        앱 버전: ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})
        기기: ${Build.MANUFACTURER} ${Build.MODEL}
        Android: ${Build.VERSION.RELEASE}
        ─────────────

        (여기에 내용을 작성해 주세요)

    """.trimIndent()
    return Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf(FEEDBACK_EMAIL))
        putExtra(Intent.EXTRA_SUBJECT, "[언제했지 v${BuildConfig.VERSION_NAME}] 문의/제안")
        putExtra(Intent.EXTRA_TEXT, body)
    }
}
