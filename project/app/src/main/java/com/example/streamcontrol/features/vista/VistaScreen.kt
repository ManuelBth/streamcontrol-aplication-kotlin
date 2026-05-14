package com.example.streamcontrol.features.vista

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun VistaScreen(
    viewModel: VistaViewModel
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        ExplanationCard()

        Spacer(modifier = Modifier.height(8.dp))

        InfoCardsGrid(state)

        Spacer(modifier = Modifier.height(12.dp))

        TriacVisualizationCanvas(
            power = state.power,
            frequency = state.frequency,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        Spacer(modifier = Modifier.height(8.dp))

        LegendRow()

        Spacer(modifier = Modifier.height(8.dp))

        PowerSlider(
            power = state.power,
            onPowerChange = { viewModel.setPower(it) }
        )

        FrequencySlider(
            frequency = state.frequency,
            onFrequencyChange = { viewModel.setFrequency(it) }
        )
    }
}

@Composable
private fun LegendRow() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(color = Color(0xFF378ADD), label = "Señal AC")
            Spacer(modifier = Modifier.width(12.dp))
            LegendItem(color = Color(0xFFE24B4A), label = "Cruce por cero")
            Spacer(modifier = Modifier.width(12.dp))
            LegendItem(color = Color(0xFFEF9F27), label = "Pulso gate (~100µs)")
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(color = Color(0xFF7F77DD), label = "TRIAC latch")
            Spacer(modifier = Modifier.width(12.dp))
            LegendItem(color = Color(0xFF1D9E75), label = "Corriente carga")
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(14.dp)
                .height(3.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PowerSlider(
    power: Int,
    onPowerChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Potencia: ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "$power%",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(48.dp)
        )
        Slider(
            value = power.toFloat(),
            onValueChange = { onPowerChange(it.toInt()) },
            valueRange = 5f..95f,
            steps = 0,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
private fun FrequencySlider(
    frequency: Int,
    onFrequencyChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Frecuencia: ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "$frequency Hz",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(60.dp)
        )
        Slider(
            value = frequency.toFloat(),
            onValueChange = { onFrequencyChange(it.toInt()) },
            valueRange = 50f..60f,
            steps = 0,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
private fun TriacVisualizationCanvas(
    power: Int,
    frequency: Int,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val bgColor = if (isDarkTheme) Color(0xFF1c1c1a) else Color(0xFFf5f4f0)
    val gridColor = if (isDarkTheme) Color.White.copy(alpha = 0.07f) else Color.Black.copy(alpha = 0.07f)
    val axisColor = if (isDarkTheme) Color.White.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.18f)
    val textColor = if (isDarkTheme) Color(0xFF888780) else Color(0xFF73726c)

    Canvas(modifier = modifier.background(bgColor, RoundedCornerShape(8.dp))) {
        val W = size.width
        val H = size.height

        val PL = 58.dp.toPx()
        val PR = 18.dp.toPx()
        val PT = 24.dp.toPx()
        val PB = 36.dp.toPx()
        val PW = W - PL - PR
        val PH = H - PT - PB

        // Calculate TRIAC parameters
        val semi = 1.0f / (frequency * 2f)
        val totalT = semi * 5f
        val alpha = PI.toFloat() * (1f - power / 100f)
        val fireT = semi * (1f - power / 100f)
        val pulseW = minOf(0.0001f, semi * 0.015f)

        // Row positions
        val rowAC = PT + PH * 0.30f
        val rowACAmp = PH * 0.24f
        val rowLatchY0 = PT + PH * 0.60f
        val rowLatchY1 = PT + PH * 0.78f
        val rowGateY0 = PT + PH * 0.82f
        val rowGateY1 = PT + PH * 0.97f

        // Helper functions
        fun tx(t: Float) = PL + (t / totalT) * PW
        fun tyAC(v: Float) = rowAC - v * rowACAmp

        // Draw grid
        for (g in 0..4) {
            val gy = PT + g * PH / 4
            drawLine(gridColor, Offset(PL, gy), Offset(PL + PW, gy), strokeWidth = 0.5f)
        }
        for (gt in 0..5) {
            val gx = PL + gt * PW / 5
            drawLine(gridColor, Offset(gx, PT), Offset(gx, PT + PH), strokeWidth = 0.5f)
        }

        // Zero line (dashed)
        drawLine(
            axisColor,
            Offset(PL, rowAC),
            Offset(PL + PW, rowAC),
            strokeWidth = 0.8f
        )

        // Axes
        drawLine(axisColor, Offset(PL, PT), Offset(PL, PT + PH), strokeWidth = 0.8f)
        drawLine(axisColor, Offset(PL, PT + PH), Offset(PL + PW, PT + PH), strokeWidth = 0.8f)

        // Y labels - ON inside conduction zone, OFF below baseline
        drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply {
                color = textColor.hashCode()
                textSize = 10.sp.toPx()
                textAlign = android.graphics.Paint.Align.RIGHT
            }
            drawText("+V", PL - 4, tyAC(1f) + 4, paint)
            drawText("0", PL - 4, tyAC(0f) + 4, paint)
            drawText("-V", PL - 4, tyAC(-1f) + 4, paint)
            // Latch row - ON centered in bar, OFF below
            val latchBarCenter = rowLatchY0 + (rowLatchY1 - rowLatchY0) / 2
            drawText("ON", PL - 4, latchBarCenter + 3, paint)
            drawText("OFF", PL - 4, rowLatchY1 + 12, paint)
            // Gate row - ON centered in bar, OFF below
            val gateBarCenter = rowGateY0 + (rowGateY1 - rowGateY0) / 2
            drawText("ON", PL - 4, gateBarCenter + 3, paint)
            drawText("OFF", PL - 4, rowGateY1 + 12, paint)

            val leftPaint = android.graphics.Paint().apply {
                color = textColor.hashCode()
                textSize = 10.sp.toPx()
                textAlign = android.graphics.Paint.Align.LEFT
            }
            drawText("Señal AC", PL + 4, PT + 12, leftPaint)
            leftPaint.color = Color(0xFF7F77DD).hashCode()
            drawText("TRIAC latch", PL + 4, rowLatchY0 - 4, leftPaint)
            leftPaint.color = Color(0xFFEF9F27).hashCode()
            drawText("Pulso gate ESP32", PL + 4, rowGateY0 - 4, leftPaint)
        }

        // X labels (time in ms)
        for (gt in 0..5) {
            val tv = (totalT * gt / 5) * 1000
            val label = String.format("%.1fms", tv)
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = textColor.hashCode()
                    textSize = 10.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                drawText(label, PL + (gt / 5f) * PW, PT + PH + 14, paint)
            }
        }

        val N = 1000
        val freqFloat = frequency.toFloat()
        val piFloat = PI.toFloat()

        // AC sine fill
        val sineFillPath = Path()
        sineFillPath.moveTo(tx(0f), rowAC)
        for (i in 0 until N) {
            val t = (i.toFloat() / (N - 1)) * totalT
            val v = sin(2f * piFloat * freqFloat * t)
            val x = tx(t)
            val y = tyAC(v)
            sineFillPath.lineTo(x, y)
        }
        sineFillPath.lineTo(tx(totalT), rowAC)
        sineFillPath.lineTo(PL, rowAC)
        sineFillPath.close()
        drawPath(sineFillPath, Color(0xFF378ADD).copy(alpha = 0.10f))

        // AC sine line
        val sinePath = Path()
        for (i in 0 until N) {
            val t = (i.toFloat() / (N - 1)) * totalT
            val v = sin(2f * piFloat * freqFloat * t)
            val x = tx(t)
            val y = tyAC(v)
            if (i == 0) sinePath.moveTo(x, y) else sinePath.lineTo(x, y)
        }
        drawPath(sinePath, Color(0xFF378ADD), style = Stroke(width = 1.8f))

        // Load current (green) - conducting portions only
        for (s in 0 until 5) {
            val tS = s * semi + fireT
            val tE = (s + 1) * semi
            if (tS >= totalT) break
            val tEClamped = minOf(tE, totalT)

            val loadPath = Path()
            loadPath.moveTo(tx(tS), rowAC)
            for (k in 0..200) {
                val tt = tS + (tEClamped - tS) * k / 200f
                val vv = sin(2f * piFloat * freqFloat * tt)
                loadPath.lineTo(tx(tt), tyAC(vv))
            }
            loadPath.lineTo(tx(tEClamped), rowAC)
            loadPath.close()
            drawPath(loadPath, Color(0xFF1D9E75).copy(alpha = 0.20f))

            // Green line
            val loadLinePath = Path()
            loadLinePath.moveTo(tx(tS), rowAC)
            for (k in 0..200) {
                val tt = tS + (tEClamped - tS) * k / 200f
                val vv = sin(2f * piFloat * freqFloat * tt)
                loadLinePath.lineTo(tx(tt), tyAC(vv))
            }
            drawPath(loadLinePath, Color(0xFF1D9E75), style = Stroke(width = 2.2f))
        }

        // TRIAC latch bar (purple)
        for (s in 0 until 5) {
            val tS = s * semi + fireT
            val tE = (s + 1) * semi
            if (tS >= totalT) break
            val tEClamped = minOf(tE, totalT)
            val x0 = tx(tS)
            val x1 = tx(tEClamped)
            val condW = x1 - x0

            drawRect(
                Color(0xFF7F77DD).copy(alpha = 0.25f),
                Offset(x0, rowLatchY0),
                Size(condW, rowLatchY1 - rowLatchY0)
            )
            // Top line only (conducting = HIGH)
            drawLine(
                Color(0xFF7F77DD),
                Offset(x0, rowLatchY0),
                Offset(x1, rowLatchY0),
                strokeWidth = 1.5f
            )
            // Rising edge
            drawLine(
                Color(0xFF7F77DD),
                Offset(x0, rowLatchY1),
                Offset(x0, rowLatchY0),
                strokeWidth = 1.5f
            )
            // Falling edge
            drawLine(
                Color(0xFF7F77DD),
                Offset(x1, rowLatchY0),
                Offset(x1, rowLatchY1),
                strokeWidth = 1.5f
            )

            // Annotation on first cycle
            if (s == 0 && condW > 30) {
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = Color(0xFF7F77DD).hashCode()
                        textSize = 10.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                    drawText("conducción autónoma", x0 + condW / 2, rowLatchY1 + 14, paint)
                }
            }
        }
        // LOW baseline latch
        drawLine(
            Color(0xFF7F77DD).copy(alpha = 0.4f),
            Offset(PL, rowLatchY1),
            Offset(PL + PW, rowLatchY1),
            strokeWidth = 1f
        )

        // Gate pulse (orange) - very narrow
        val visiblePulseW = maxOf(tx(semi * 0.025f) - tx(0f), 4f)
        for (s in 0 until 5) {
            val tF = s * semi + fireT
            if (tF >= totalT) break
            val xf = tx(tF)

            drawRect(
                Color(0xFFEF9F27).copy(alpha = 0.35f),
                Offset(xf, rowGateY0),
                Size(visiblePulseW, rowGateY1 - rowGateY0)
            )
            drawLine(
                Color(0xFFEF9F27),
                Offset(xf, rowGateY1),
                Offset(xf, rowGateY0),
                strokeWidth = 1.5f
            )
            drawLine(
                Color(0xFFEF9F27),
                Offset(xf, rowGateY0),
                Offset(xf + visiblePulseW, rowGateY0),
                strokeWidth = 1.5f
            )
            drawLine(
                Color(0xFFEF9F27),
                Offset(xf + visiblePulseW, rowGateY0),
                Offset(xf + visiblePulseW, rowGateY1),
                strokeWidth = 1.5f
            )

            if (s == 0) {
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = Color(0xFFEF9F27).hashCode()
                        textSize = 10.sp.toPx()
                        textAlign = android.graphics.Paint.Align.LEFT
                    }
                    drawText("~100µs", xf + visiblePulseW + 4, rowGateY1 + 14, paint)
                }
            }
        }
        // LOW baseline gate
        drawLine(
            Color(0xFFEF9F27).copy(alpha = 0.4f),
            Offset(PL, rowGateY1),
            Offset(PL + PW, rowGateY1),
            strokeWidth = 1f
        )

        // Zero crossing markers
        for (s in 0..5) {
            val zt = s * semi
            if (zt > totalT) break
            val zx = tx(zt)

            drawCircle(
                Color(0xFFE24B4A),
                radius = 5f,
                center = Offset(zx, rowAC)
            )
            // Vertical dashed line at ZC
            drawLine(
                Color(0xFFE24B4A).copy(alpha = 0.3f),
                Offset(zx, PT),
                Offset(zx, PT + PH),
                strokeWidth = 0.8f
            )
        }

        // Alpha annotation on first cycle
        if (fireT > semi * 0.05) {
            val xZC = tx(0f)
            val xFire = tx(fireT)
            val my = rowAC - 20

            drawLine(
                Color(0xFFEF9F27).copy(alpha = 0.6f),
                Offset(xFire, rowAC - 30),
                Offset(xFire, rowGateY1 + 5),
                strokeWidth = 0.8f
            )
            drawLine(
                Color(0xFFEF9F27).copy(alpha = 0.7f),
                Offset(xZC + 3, my),
                Offset(xFire - 3, my),
                strokeWidth = 1f
            )

            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = textColor.hashCode()
                    textSize = 10.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                val delayMs = (fireT * 1000).toFloat()
                drawText("α retardo = ${String.format("%.1f", delayMs)}ms", (xZC + xFire) / 2, my - 5, paint)
            }
        }
    }
}

@Composable
private fun ExplanationCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
    ) {
        Text(
            text = "Por qué el pulso es tan corto?\nEl TRIAC hace latch: con recibir el pulso de gate una sola vez, queda en conducción hasta el próximo cruce por cero donde la corriente cae a cero sola. La barra morada muestra ese período de conducción autónoma — la ESP32 ya no interviene durante ese tiempo.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InfoCardsGrid(state: VistaState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        InfoCard(
            label = "Ángulo α",
            value = "${state.alphaDegrees.toInt()}°",
            modifier = Modifier.weight(1f)
        )
        InfoCard(
            label = "Retardo disparo",
            value = "${String.format("%.1f", state.fireDelayMs)} ms",
            modifier = Modifier.weight(1f)
        )
        InfoCard(
            label = "Conducción autónoma",
            value = "${String.format("%.1f", state.autonomousConductionMs)} ms",
            modifier = Modifier.weight(1f)
        )
        InfoCard(
            label = "Potencia RMS",
            value = "${state.rmsPower}%",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun InfoCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(64.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                RoundedCornerShape(8.dp)
            )
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

private fun Color.luminance(): Float {
    return (0.299f * red + 0.587f * green + 0.114f * blue)
}