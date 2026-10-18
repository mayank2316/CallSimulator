package com.example.callsimulator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CallDataTest {
    @Test fun callDefaultsToScheduled() {
        val call = SimCall(name = "Demo", timeMillis = 1_900_000_000_000L)
        assertEquals("Scheduled", call.status)
        assertTrue(call.id.isNotBlank())
    }

    @Test fun callRetainsSimulationDetails() {
        val call = SimCall(name = "Taylor", number = "123", timeMillis = 1234L, vibration = false)
        assertEquals("Taylor", call.name)
        assertEquals("123", call.number)
        assertEquals(false, call.vibration)
    }
}
