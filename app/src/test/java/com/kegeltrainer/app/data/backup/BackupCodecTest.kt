package com.kegeltrainer.app.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

class BackupCodecTest {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    @Test
    fun roundtripSerializeAndDeserialize() {
        val payload = BackupPayload(
            version = 1,
            exportedAt = 1700000000000L,
            profile = BackupProfile(
                ageRange = "UNDER_30",
                experience = "REGULAR",
                goals = listOf("CONTROL", "PROSTATE"),
                dailyBudget = "MEDIUM",
                baselineHoldSec = 10,
                planStartedEpochDay = 20000L,
                onboardedAt = 1699999000000L,
            ),
            planDays = listOf(
                BackupPlanDay(dayIndex = 0, workoutId = "mixed_beginner_1", week = 1),
            ),
            sessions = listOf(
                BackupSession(
                    workoutId = "mixed_beginner_1",
                    startedAt = 1700000000000L,
                    endedAt = 1700000300000L,
                    durationMs = 300000L,
                    completed = true,
                    isPlanSession = true,
                    contractionCount = 15,
                    epochDay = 20000L,
                ),
            ),
            settings = BackupSettings(
                voiceEnabled = true,
                completionSoundEnabled = false,
                hapticEnabled = false,
                stealthEnabled = true,
                backgroundPlayEnabled = true,
                reminderMinutes = listOf(540, 1260),
                themeMode = "DARK",
            ),
        )

        val encoded = json.encodeToString(payload)
        val decoded = json.decodeFromString<BackupPayload>(encoded)

        assertEquals(1, decoded.version)
        assertNotNull(decoded.profile)
        assertEquals("UNDER_30", decoded.profile?.ageRange)
        assertEquals(listOf("CONTROL", "PROSTATE"), decoded.profile?.goals)
        assertEquals(1, decoded.planDays.size)
        assertEquals(1, decoded.sessions.size)
        assertEquals(false, decoded.settings.completionSoundEnabled)
        assertTrue(decoded.settings.stealthEnabled)
        assertEquals(listOf(540, 1260), decoded.settings.reminderMinutes)
    }

    @Test
    fun handlesMissingOptionalFieldsGracefully() {
        val minimalJson = """
            {
              "version": 1,
              "exportedAt": 1700000000000
            }
        """.trimIndent()

        val decoded = json.decodeFromString<BackupPayload>(minimalJson)
        assertEquals(1, decoded.version)
        assertEquals(null, decoded.profile)
        assertTrue(decoded.planDays.isEmpty())
        assertTrue(decoded.sessions.isEmpty())
        assertTrue(decoded.settings.voiceEnabled)
    }
}
