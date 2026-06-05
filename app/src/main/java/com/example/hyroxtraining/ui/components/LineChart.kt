package com.example.hyroxtraining.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LineChart(
    dataPoints: List<Double>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    gradientColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
) {
    if (dataPoints.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = MaterialTheme.shapes.medium
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Log at least 2 workouts to view progress trends",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp
            )
        }
        return
    }

    val maxVal = dataPoints.maxOrNull() ?: 1.0
    val minVal = dataPoints.minOrNull() ?: 0.0
    val diff = (maxVal - minVal).let { if (it == 0.0) 1.0 else it }

    var selectedIndex by remember(dataPoints) { mutableStateOf<Int?>(null) }
    val scrollState = rememberScrollState()

    // Auto-scroll to the end (most recent chronologically) when new logs are loaded
    androidx.compose.runtime.LaunchedEffect(dataPoints) {
        if (dataPoints.size > 6) {
            scrollState.scrollTo(scrollState.maxValue)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Dynamic tooltip selection text above the chart
        if (selectedIndex != null && selectedIndex!! < dataPoints.size) {
            val selectedVal = dataPoints[selectedIndex!!]
            val selectedLabel = labels.getOrNull(selectedIndex!!) ?: ""
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Touch Log: $selectedLabel",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Score: ${selectedVal.toString().removeSuffix(".0")}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        } else {
            // Placeholder hint text
            Text(
                text = "Touch and drag on the graph to view log history numbers",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    shape = MaterialTheme.shapes.medium
                )
                .padding(16.dp)
        ) {
            val canvasModifier = if (dataPoints.size > 6) {
                Modifier
                    .horizontalScroll(scrollState)
                    .width((dataPoints.size * 65).dp)
                    .fillMaxHeight()
            } else {
                Modifier.fillMaxSize()
            }

            Canvas(
                modifier = canvasModifier
                    .pointerInput(dataPoints) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val position = event.changes.firstOrNull()?.position
                                if (position != null) {
                                    val spacing = size.width / (dataPoints.size - 1).coerceAtLeast(1)
                                    val touchedIndex = ((position.x + spacing / 2) / spacing).toInt().coerceIn(0, dataPoints.size - 1)
                                    selectedIndex = touchedIndex
                                }
                            }
                        }
                    }
            ) {
                val width = size.width
                val height = size.height
                val spacing = width / (dataPoints.size - 1).coerceAtLeast(1)

                val points = dataPoints.mapIndexed { index, valData ->
                    val x = index * spacing
                    val ratio = (valData - minVal) / diff
                    // Invert y axis for canvas (0 is top, height is bottom) and add padding for labels
                    val y = height - (ratio.toFloat() * (height - 60f) + 30f)
                    Offset(x, y)
                }

                // Draw grid lines
                val gridColor = lineColor.copy(alpha = 0.08f)
                val gridCount = 4
                for (i in 0..gridCount) {
                    val yGrid = 30f + (height - 60f) * i / gridCount
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, yGrid),
                        end = Offset(width, yGrid),
                        strokeWidth = 2f
                    )
                }

                // Draw area gradient under the curve
                if (points.size > 1) {
                    val fillPath = Path().apply {
                        moveTo(points.first().x, height)
                        points.forEach { point ->
                            lineTo(point.x, point.y)
                        }
                        lineTo(points.last().x, height)
                        close()
                    }
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(gradientColor, Color.Transparent),
                            startY = 0f,
                            endY = height
                        )
                    )
                }

                // Draw Line
                if (points.size > 1) {
                    val strokePath = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            lineTo(points[i].x, points[i].y)
                        }
                    }
                    drawPath(
                        path = strokePath,
                        color = lineColor,
                        style = Stroke(width = 6f, cap = StrokeCap.Round)
                    )
                }

                // Draw vertical guideline if touched
                if (selectedIndex != null && selectedIndex!! < points.size) {
                    val selPoint = points[selectedIndex!!]
                    drawLine(
                        color = lineColor.copy(alpha = 0.4f),
                        start = Offset(selPoint.x, 0f),
                        end = Offset(selPoint.x, height),
                        strokeWidth = 3f,
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                }

                // Draw Point dots and glow
                points.forEachIndexed { idx, point ->
                    val isSelected = idx == selectedIndex

                    drawCircle(
                        color = if (isSelected) lineColor.copy(alpha = 0.5f) else lineColor.copy(alpha = 0.3f),
                        radius = if (isSelected) 24f else 16f,
                        center = point
                    )
                    drawCircle(
                        color = lineColor,
                        radius = if (isSelected) 12f else 8f,
                        center = point
                    )
                    drawCircle(
                        color = Color.White,
                        radius = if (isSelected) 6f else 4f,
                        center = point
                    )

                    // Draw the numerical numbers directly above the point dot on the graph!
                    val numberStr = dataPoints[idx].toString().removeSuffix(".0")
                    val paint = android.graphics.Paint().apply {
                        color = if (isSelected) {
                            android.graphics.Color.argb(255, 235, 94, 40) // Active highlight primary color
                        } else {
                            android.graphics.Color.WHITE
                        }
                        textSize = if (isSelected) 30f else 24f
                        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        numberStr,
                        point.x,
                        point.y - (if (isSelected) 32f else 22f), // adjust offset above circle
                        paint
                    )
                }
            }
        }
    }
}
