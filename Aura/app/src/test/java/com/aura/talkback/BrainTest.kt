package com.aura.talkback

import com.aura.talkback.ai.AiClient
import com.aura.talkback.ai.Brain
import com.aura.talkback.ai.NudgeContext
import com.aura.talkback.data.AuraSettings
import com.aura.talkback.data.Memory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrainTest {
    private val brain = Brain(AiClient())

    @Test
    fun `late night window wraps past midnight`() {
        val s = AuraSettings(nightStartHour = 23, nightEndHour = 5)
        assertTrue(s.isNight(23))
        assertTrue(s.isNight(2))
        assertFalse(s.isNight(5))
        assertFalse(s.isNight(14))
    }

    @Test
    fun `offline chat understands lock commands`() = runBlocking {
        val reply = brain.chat(AuraSettings(), emptyList(), "Lock Instagram for 1 hour", emptyList(), null, emptyList(), emptyList(), emptyList())
        val action = reply.actions.single()
        assertEquals("lock", action.type)
        assertEquals("instagram", action.app)
        assertEquals(60, action.minutes)
    }

    @Test
    fun `offline chat remembers goals`() = runBlocking {
        val reply = brain.chat(AuraSettings(), emptyList(), "I want to study DSA tonight", emptyList(), null, emptyList(), emptyList(), emptyList())
        assertEquals(listOf("I want to study DSA tonight"), reply.remember)
    }

    @Test
    fun `template talk-back uses name minutes and the latest goal`() {
        val s = AuraSettings(userName = "Rahul")
        val ctx = NudgeContext(
            app = "Instagram", sessionMin = 37, todayAppMin = 80, notifications = 12, night = true,
            memories = listOf(Memory(1, System.currentTimeMillis(), "Wants to study DSA")),
            todaysNudges = emptyList(), snoozesForApp = 0,
        )
        val msg = brain.templateNudge(s, ctx).message
        assertTrue(msg, msg.contains("Rahul") && msg.contains("37") && msg.contains("Instagram") && msg.contains("Wants to study DSA"))
    }
}
