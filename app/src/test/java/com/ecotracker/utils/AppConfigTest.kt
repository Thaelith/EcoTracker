package com.ecotracker.utils

import org.junit.Assert.*
import org.junit.Test

class AppConfigTest {

    @Test
    fun `cache TTL millis matches days constant`() {
        val expectedMillis = AppConfig.CACHE_TTL_DAYS * 24 * 60 * 60 * 1000L
        assertEquals(expectedMillis, AppConfig.CACHE_TTL_MILLIS)
    }

    @Test
    fun `username regex accepts valid usernames`() {
        assertTrue(AppConfig.USERNAME_REGEX.matches("John_Doe"))
        assertTrue(AppConfig.USERNAME_REGEX.matches("user-123"))
        assertTrue(AppConfig.USERNAME_REGEX.matches("hello world"))
    }

    @Test
    fun `username regex rejects special characters`() {
        assertFalse(AppConfig.USERNAME_REGEX.matches("user<script>"))
        assertFalse(AppConfig.USERNAME_REGEX.matches("user@name"))
        assertFalse(AppConfig.USERNAME_REGEX.matches(""))
    }

    @Test
    fun `username regex rejects usernames exceeding max length`() {
        val tooLong = "a".repeat(AppConfig.USERNAME_MAX_LENGTH + 1)
        assertFalse(AppConfig.USERNAME_REGEX.matches(tooLong))
    }
}
