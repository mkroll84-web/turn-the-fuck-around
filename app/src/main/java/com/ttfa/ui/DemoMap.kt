package com.ttfa.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import com.ttfa.domain.Coordinate

/** Offline fictional street diagram. No road geometry in this view is routing evidence. */
@Composable
fun DemoMap(state: UiState, modifier: Modifier = Modifier) {
    Box(modifier) {
        Canvas(Modifier.matchParentSize()) {
            drawRect(Color(0xFFE7ECE4))
            val all = (state.route?.points ?: emptyList()) + state.location + listOf(Coordinate(37.774, -122.423), Coordinate(37.787, -122.403))
            val minLat = all.minOf { it.latitude } - .0015; val maxLat = all.maxOf { it.latitude } + .0015
            val minLon = all.minOf { it.longitude } - .0015; val maxLon = all.maxOf { it.longitude } + .0015
            fun project(p: Coordinate) = Offset(((p.longitude - minLon) / (maxLon - minLon) * size.width).toFloat(), ((maxLat - p.latitude) / (maxLat - minLat) * size.height).toFloat())
            drawRoundRect(Color(0xFFC4DCB5), Offset(size.width * .64f, size.height * .12f), Size(size.width * .25f, size.height * .28f), androidx.compose.ui.geometry.CornerRadius(20f))
            for (i in 0..9) {
                val x = size.width * i / 9f; val y = size.height * i / 9f
                drawLine(Color.White, Offset(x, 0f), Offset(x, size.height), 16f)
                drawLine(Color.White, Offset(0f, y), Offset(size.width, y), 16f)
            }
            state.route?.let { route ->
                val path = Path()
                route.points.forEachIndexed { index, p -> val point = project(p); if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y) }
                drawPath(path, Color.White, style = Stroke(19f))
                drawPath(path, Color(0xFF007F75), style = Stroke(12f))
            }
            state.selected?.let {
                val p = project(it.location)
                drawCircle(Color.White, 18f, p); drawCircle(Color(0xFF101C20), 12f, p)
            }
            val driver = project(state.location)
            drawCircle(Color(0x33FF6945), 30f, driver)
            drawCircle(Color.White, 18f, driver)
            drawCircle(Color(0xFFFF6945), 13f, driver)
        }
        Text("OFFLINE DEMO MAP", Modifier.align(Alignment.BottomStart).padding(12.dp), color = Color(0xFF49615A), fontWeight = FontWeight.Bold)
    }
}
