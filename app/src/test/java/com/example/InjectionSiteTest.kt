package com.example

import com.example.data.model.InjectionSite
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InjectionSiteTest {

    @Test
    fun `injection site route labels match requested specifications`() {
        assertEquals("IM", InjectionSite.LEFT_GLUTE.routeLabel)
        assertEquals("IM", InjectionSite.RIGHT_GLUTE.routeLabel)
        assertFalse(InjectionSite.LEFT_GLUTE.isSubQFriendly)
        assertFalse(InjectionSite.RIGHT_GLUTE.isSubQFriendly)

        assertEquals("IM", InjectionSite.LEFT_DELTOID.routeLabel)
        assertEquals("IM", InjectionSite.RIGHT_DELTOID.routeLabel)
        assertFalse(InjectionSite.LEFT_DELTOID.isSubQFriendly)
        assertFalse(InjectionSite.RIGHT_DELTOID.isSubQFriendly)

        assertEquals("SubQ", InjectionSite.ABDOMEN.routeLabel)
        assertTrue(InjectionSite.ABDOMEN.isSubQFriendly)

        assertEquals("SubQ / IM", InjectionSite.LEFT_THIGH.routeLabel)
        assertEquals("SubQ / IM", InjectionSite.RIGHT_THIGH.routeLabel)
        assertTrue(InjectionSite.LEFT_THIGH.isSubQFriendly)
        assertTrue(InjectionSite.RIGHT_THIGH.isSubQFriendly)
    }

    @Test
    fun `subq preset filter only contains thighs and abdomen`() {
        val subqSites = InjectionSite.entries.filter { it.isSubQFriendly }.toSet()
        assertEquals(
            setOf(InjectionSite.LEFT_THIGH, InjectionSite.RIGHT_THIGH, InjectionSite.ABDOMEN),
            subqSites
        )
    }
}
