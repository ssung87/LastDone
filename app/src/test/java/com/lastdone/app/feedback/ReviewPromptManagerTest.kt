package com.lastdone.app.feedback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewPromptManagerTest {

    @Test
    fun `under threshold returns false`() {
        assertFalse(ReviewPromptManager.shouldRequest(doneCount = 6, lastReviewedVersion = null, currentVersion = 5))
    }

    @Test
    fun `at threshold with never-prompted returns true`() {
        assertTrue(ReviewPromptManager.shouldRequest(doneCount = 7, lastReviewedVersion = null, currentVersion = 5))
    }

    @Test
    fun `already prompted same version returns false`() {
        assertFalse(ReviewPromptManager.shouldRequest(doneCount = 100, lastReviewedVersion = 5, currentVersion = 5))
    }

    @Test
    fun `prompted older version returns true`() {
        assertTrue(ReviewPromptManager.shouldRequest(doneCount = 100, lastReviewedVersion = 4, currentVersion = 5))
    }

    @Test
    fun `boundary same version different count returns false`() {
        assertFalse(ReviewPromptManager.shouldRequest(doneCount = 7, lastReviewedVersion = 5, currentVersion = 5))
    }
}
