package com.ttfa.domain

import org.junit.Assert.*
import org.junit.Test
import kotlinx.coroutines.runBlocking
import com.ttfa.data.DemoNavigationProvider

class NavigationTest {
    private val a = Coordinate(37.0, -122.0)
    private val b = Coordinate(37.01, -122.0)
    private fun route(id: String = "normal", meters: Double = 3000.0, seconds: Int = 600, trust: RouteTrust = RouteTrust.PROVIDER_VERIFIED, legal: Boolean = true, restrictions: Boolean = true, provider: String = "test", destination: String = "d") =
        Route(id, provider, destination, listOf(a, b), meters, seconds, listOf(Maneuver(b, "Arrive")), trust, legal, restrictions)
    @Test fun legalAlternativeWinsOnlyWhenBothThresholdsMet() {
        val normal = route(); val alternative = route("short", 1000.0, 180)
        val choice = WtfPolicy.choose(RerouteOptions(normal, listOf(alternative)), true, false, WtfThresholds())
        assertTrue(choice.won); assertEquals(alternative, choice.route); assertEquals(2000.0, choice.metersSaved, .01)
        assertFalse(WtfPolicy.choose(RerouteOptions(normal, listOf(route("tinyTime", 1000.0, 570))), true, false, WtfThresholds()).won)
        assertFalse(WtfPolicy.choose(RerouteOptions(normal, listOf(route("tinyDistance", 2900.0, 180))), true, false, WtfThresholds()).won)
    }
    @Test fun unsafeUnverifiedMismatchedAlternativesAreRejected() {
        val normal = route()
        val alternatives = listOf(
            route("illegal", 1000.0, 180, legal = false),
            route("unknownRestrictions", 1000.0, 180, restrictions = false),
            route("simulation", 1000.0, 180, trust = RouteTrust.SIMULATION_ONLY),
            route("unverified", 1000.0, 180, trust = RouteTrust.UNVERIFIED),
            route("badTime", 1000.0, -180),
            route("wrongProvider", 1000.0, 180, provider = "other"),
            route("wrongDestination", 1000.0, 180, destination = "other"),
        )
        assertEquals(normal, WtfPolicy.choose(RerouteOptions(normal, alternatives), true, false, WtfThresholds()).route)
    }
    @Test fun disabledModeAndNoAlternativeUseNormal() {
        val normal = route(); val short = route("short", 1000.0, 180)
        assertFalse(WtfPolicy.choose(RerouteOptions(normal, listOf(short)), false, false, WtfThresholds()).won)
        assertFalse(WtfPolicy.choose(RerouteOptions(normal, emptyList()), true, false, WtfThresholds()).won)
    }
    @Test fun fastestEligibleAlternativeIsSelected() {
        val fast = route("fast", 2000.0, 120)
        assertEquals(fast, WtfPolicy.choose(RerouteOptions(route(), listOf(route("slow", 1000.0, 180), fast)), true, false, WtfThresholds()).route)
    }
    @Test fun deviationRequiresThreeGoodFixesAndResetsOnRoute() {
        val detector = DeviationDetector(); val off = Coordinate(37.005, -122.01)
        assertFalse(detector.update(off, route(), 5f)); assertFalse(detector.update(off, route(), 5f)); assertTrue(detector.update(off, route(), 5f))
        assertFalse(detector.update(a, route(), 5f)); assertFalse(detector.update(off, route(), 5f))
    }
    @Test fun poorAccuracyNeverTriggersReroute() {
        val detector = DeviationDetector(); val off = Coordinate(37.005, -122.01)
        repeat(10) { assertFalse(detector.update(off, route(), 80f)) }
        assertFalse(detector.update(off, route(), Float.NaN))
    }
    @Test fun segmentDistanceUsesSegmentNotJustVertices() {
        assertEquals(0.0, distanceToRoute(Coordinate(37.005, -122.0), listOf(a, b)), .1)
        assertTrue(distanceToRoute(Coordinate(37.005, -122.01), listOf(a, b)) > 800)
        assertTrue(distanceToRoute(a, emptyList()).isInfinite())
        assertEquals(0.0, distanceToRoute(a, listOf(a, a)), .1)
    }
    @Test fun demoSearchRouteAndRerouteWorkButLiveRoutingIsRejected() = runBlocking {
        val provider = DemoNavigationProvider()
        val destination = provider.search("coffee").single()
        assertTrue(provider.search("missing").isEmpty())
        val request = RoutingRequest(provider.origin, 0.0, destination, true)
        assertEquals(RouteTrust.SIMULATION_ONLY, provider.route(request).trust)
        assertTrue(WtfPolicy.choose(provider.reroute(request), true, true, WtfThresholds()).won)
        try { WtfPolicy.choose(provider.reroute(request), true, false, WtfThresholds()); fail("Demo reroutes must not be accepted for live driving") } catch (_: IllegalArgumentException) { }
        try { provider.route(request.copy(simulation = false)); fail("Live driving must be blocked") } catch (_: IllegalArgumentException) { }
    }
    @Test fun unverifiedNormalRouteIsNotASafeFallback() {
        try { WtfPolicy.choose(RerouteOptions(route(trust = RouteTrust.UNVERIFIED), emptyList()), true, false, WtfThresholds()); fail("Must reject unverified normal route") } catch (_: IllegalArgumentException) { }
    }
    @Test fun invalidThresholdsAreRejected() {
        try { WtfThresholds(-1.0, 60); fail("Negative threshold") } catch (_: IllegalArgumentException) { }
        try { WtfThresholds(Double.NaN, 60); fail("NaN threshold") } catch (_: IllegalArgumentException) { }
    }
    @Test fun personalityNeverMutatesDirections() {
        assertTrue(missedTurnMessage(Personality.ROAST).contains("safely"))
        assertFalse(missedTurnMessage(Personality.CALM).contains("fuck"))
    }
}
