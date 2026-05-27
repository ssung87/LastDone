package com.lastdone.app.feedback

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private const val PACKAGE_NAME = "com.lastdone"
private const val PLAY_STORE_MARKET_URI = "market://details?id=$PACKAGE_NAME"
private const val PLAY_STORE_WEB_URL = "https://play.google.com/store/apps/details?id=$PACKAGE_NAME"

suspend fun requestInAppReview(activity: Activity) {
    val manager = ReviewManagerFactory.create(activity.applicationContext)
    val reviewInfo: ReviewInfo? = suspendCancellableCoroutine { cont ->
        manager.requestReviewFlow().addOnCompleteListener { task ->
            cont.resume(if (task.isSuccessful) task.result else null)
        }
    }
    if (reviewInfo == null) {
        openPlayStoreListing(activity)
        return
    }
    suspendCancellableCoroutine<Unit> { cont ->
        manager.launchReviewFlow(activity, reviewInfo).addOnCompleteListener {
            cont.resume(Unit)
        }
    }
}

fun openPlayStoreListing(context: Context) {
    val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_MARKET_URI)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(marketIntent)
        return
    } catch (_: ActivityNotFoundException) {
        // fall through to web
    }

    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_WEB_URL)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(webIntent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "Play 스토어를 열 수 없습니다", Toast.LENGTH_SHORT).show()
    }
}
