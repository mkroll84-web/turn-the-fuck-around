package com.ttfa.domain

import kotlin.math.*

data class Coordinate(val latitude: Double, val longitude: Double)
data class Destination(val id: String, val name: String, val location: Coordinate)
enum class RouteTrust { UNVERIFIED, SIMULATION_ONLY, PROVIDER_VERIFIED }
data class Maneuver(val at: Coordinate, val instruction: String)
data class Route(
    val id: String,
    val providerId: String,
    val destinationId: String,
    val points: List<Coordinate>,
    val distanceMeters: Double,
    val durationSeconds: Int,
    val maneuvers: List<Maneuver>,
    val trust: RouteTrust,
    val legalForCurrentTravelDirection: Boolean,
    val restrictionAware: Boolean,
)
data class RoutingRequest(val origin: Coordinate, val bearing: Double?, val destination: Destination, val simulation: Boolean)
data class RerouteOptions(val normal: Route, val alternatives: List<Route>)
/** Adapters must obtain legality/restrictions from the provider, never from coordinate geometry. */
interface NavigationProvider {
    val id: String
    val simulationOnly: Boolean
    suspend fun search(query: String): List<Destination>
    suspend fun route(request: RoutingRequest): Route
    suspend fun reroute(request: RoutingRequest): RerouteOptions
}
data class WtfThresholds(val minimumMetersSaved: Double = 400.0, val minimumSecondsSaved: Int = 60) {
    init { require(minimumMetersSaved.isFinite() && minimumMetersSaved >= 0 && minimumSecondsSaved >= 0) }
}
fun Route.usableFor(simulation: Boolean): Boolean =
    legalForCurrentTravelDirection && restrictionAware && points.size >= 2 && maneuvers.isNotEmpty() &&
    distanceMeters.isFinite() && distanceMeters > 0 && durationSeconds > 0 &&
    points.all { it.latitude.isFinite() && it.latitude in -90.0..90.0 && it.longitude.isFinite() && it.longitude in -180.0..180.0 } &&
    (trust == RouteTrust.PROVIDER_VERIFIED || (simulation && trust == RouteTrust.SIMULATION_ONLY))
data class RouteDecision(val route: Route, val won: Boolean, val metersSaved: Double, val secondsSaved: Int)
object WtfPolicy {
    fun choose(options: RerouteOptions, enabled: Boolean, simulation: Boolean, thresholds: WtfThresholds): RouteDecision {
        val normal = options.normal
        require(normal.usableFor(simulation)) { "Normal reroute is not verified for this navigation mode." }
        val candidates = if (!enabled) emptyList() else options.alternatives.filter {
            it.providerId == normal.providerId && it.destinationId == normal.destinationId &&
                it.usableFor(simulation) &&
                normal.distanceMeters - it.distanceMeters >= thresholds.minimumMetersSaved &&
                normal.durationSeconds - it.durationSeconds >= thresholds.minimumSecondsSaved
        }
        val chosen = candidates.minByOrNull { it.durationSeconds } ?: normal
        return RouteDecision(chosen, chosen !== normal, normal.distanceMeters - chosen.distanceMeters, normal.durationSeconds - chosen.durationSeconds)
    }
}
fun distance(a: Coordinate, b: Coordinate): Double {
    val lat = Math.toRadians(b.latitude - a.latitude)
    val lon = Math.toRadians(b.longitude - a.longitude)
    val h = sin(lat / 2).pow(2) + cos(Math.toRadians(a.latitude)) * cos(Math.toRadians(b.latitude)) * sin(lon / 2).pow(2)
    return 6371000.0 * 2 * atan2(sqrt(h), sqrt((1 - h).coerceAtLeast(0.0)))
}
/** Geometry detects deviation only. It never manufactures legal road connections. */
fun distanceToRoute(location: Coordinate, points: List<Coordinate>): Double {
    if (points.isEmpty()) return Double.POSITIVE_INFINITY
    if (points.size == 1) return distance(location, points.first())
    val scale = cos(Math.toRadians(location.latitude))
    fun project(p: Coordinate) = Pair((p.longitude - location.longitude) * scale * 111320, (p.latitude - location.latitude) * 111320)
    return points.zipWithNext().minOf { (a, b) ->
        val (ax, ay) = project(a); val (bx, by) = project(b)
        val dx = bx - ax; val dy = by - ay
        val length = dx * dx + dy * dy
        val t = if (length == 0.0) 0.0 else ((-ax * dx - ay * dy) / length).coerceIn(0.0, 1.0)
        hypot(ax + t * dx, ay + t * dy)
    }
}
class DeviationDetector(private val thresholdMeters: Double = 45.0, private val requiredSamples: Int = 3) {
    private var samples = 0
    fun update(location: Coordinate, route: Route, accuracyMeters: Float): Boolean {
        if (accuracyMeters > 35f || !accuracyMeters.isFinite() || accuracyMeters < 0) { samples = 0; return false }
        samples = if (distanceToRoute(location, route.points) > thresholdMeters + accuracyMeters) samples + 1 else 0
        return samples >= requiredSamples
    }
    fun reset() { samples = 0 }
}
enum class Personality(val label: String) { CALM("Calm"), DRY("Dry wit"), ROAST("Full roast") }
fun missedTurnMessage(personality: Personality) = when (personality) {
    Personality.CALM -> "Missed turn. Finding another route."
    Personality.DRY -> "A scenic detour. Bold choice."
    Personality.ROAST -> "You missed the fucking turn. Let’s sort it out safely."
}
