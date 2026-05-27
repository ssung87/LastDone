package com.lastdone.app.feedback

object ReviewPromptManager {
    const val REVIEW_THRESHOLD = 7

    fun shouldRequest(doneCount: Int, lastReviewedVersion: Int?, currentVersion: Int): Boolean =
        doneCount >= REVIEW_THRESHOLD && lastReviewedVersion != currentVersion
}
