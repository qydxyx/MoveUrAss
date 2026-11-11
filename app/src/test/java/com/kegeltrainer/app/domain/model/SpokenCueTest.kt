package com.kegeltrainer.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SpokenCueTest {
    @Test
    fun spokenCuesUseCrispCommands() {
        assertEquals("收", PhaseType.CONTRACT.spokenCue(1_000L))
        assertEquals("收", PhaseType.CONTRACT.spokenCue(5_000L))
        assertEquals("放", PhaseType.RELAX.spokenCue(1_000L))
        assertEquals("放", PhaseType.RELAX.spokenCue(5_000L))
        assertEquals("保持", PhaseType.HOLD.spokenCue(7_000L))
        assertEquals("准备", PhaseType.PREPARE.spokenCue(3_000L))
    }

    @Test
    fun onScreenLabelStaysFullWords() {
        assertEquals("收缩", PhaseType.CONTRACT.displayLabel())
        assertEquals("放松", PhaseType.RELAX.displayLabel())
    }
}
