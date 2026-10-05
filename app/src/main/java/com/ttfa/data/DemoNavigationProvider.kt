package com.ttfa.data

import com.ttfa.domain.*

/** Fictional fixtures, not real-road instructions. Never usable for live driving. */
class DemoNavigationProvider : NavigationProvider {
    override val id = "demo-fixtures"
    override val simulationOnly = true
    val origin = Coordinate(37.7749, -122.4194)
    private val places = listOf(
        Destination("coffee", "Common Sense Coffee · demo", Coordinate(37.7829, -122.4114)),
        Destination("park", "Wrong Turn Park · demo", Coordinate(37.7809, -122.4074)),
        Destination("diner", "The Reconnect Diner · demo", Coordinate(37.7849, -122.4134)),
    )
    override suspend fun search(query: String) = places.filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
    override suspend fun route(request: RoutingRequest): Route {
        require(request.simulation) { "Demo provider cannot supply real driving directions." }
        val turn = Coordinate(request.destination.location.latitude, request.origin.longitude)
        return fixture("original", request, listOf(request.origin, turn, request.destination.location), 180)
    }
    private fun fixture(name: String, request: RoutingRequest, points: List<Coordinate>, seconds: Int, meters: Double? = null) = Route(
        name, id, request.destination.id, points, meters ?: points.zipWithNext().sumOf { distance(it.first, it.second) }, seconds,
        listOf(Maneuver(points[1], if (name == "normal-reroute") "At the demo intersection, turn left" else "At the demo intersection, turn right"), Maneuver(points.last(), "Arrive at your demo destination")),
        RouteTrust.SIMULATION_ONLY, true, true,
    )
    override suspend fun reroute(request: RoutingRequest): RerouteOptions {
        require(request.simulation)
        val o = request.origin; val d = request.destination.location
        val direct = listOf(o, Coordinate(d.latitude, o.longitude), d)
        val normal = listOf(o, Coordinate(o.latitude - .012, o.longitude), Coordinate(o.latitude - .012, d.longitude + .012), Coordinate(d.latitude, d.longitude + .012), d)
        return RerouteOptions(
            fixture("normal-reroute", request, normal, 960, 13197.0),
            listOf(fixture("demo-reconnect", request, direct, 120, 966.0)),
        )
    }
}
