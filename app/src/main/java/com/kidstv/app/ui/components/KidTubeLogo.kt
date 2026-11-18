package com.kidstv.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun KidTubeEmblem(
    size: Dp = 32.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        val cyan = Color(0xFF38BDF8)
        val shell = Color(0xFFE2E8F0)
        val screen = Color(0xFF0B1120)
        val tubeRed = Color(0xFFF43F5E)

        // Antennas
        drawLine(
            color = cyan,
            start = Offset(w * 0.40f, h * 0.25f),
            end = Offset(w * 0.28f, h * 0.10f),
            strokeWidth = w * 0.06f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = cyan,
            start = Offset(w * 0.60f, h * 0.25f),
            end = Offset(w * 0.72f, h * 0.10f),
            strokeWidth = w * 0.06f,
            cap = StrokeCap.Round
        )
        drawCircle(color = cyan, radius = w * 0.045f, center = Offset(w * 0.28f, h * 0.10f))
        drawCircle(color = cyan, radius = w * 0.045f, center = Offset(w * 0.72f, h * 0.10f))

        // TV Outer Frame
        drawRoundRect(
            color = shell,
            topLeft = Offset(w * 0.08f, h * 0.24f),
            size = Size(w * 0.84f, h * 0.60f),
            cornerRadius = CornerRadius(w * 0.16f, w * 0.16f)
        )

        // Inner Dark Screen
        drawRoundRect(
            color = screen,
            topLeft = Offset(w * 0.14f, h * 0.30f),
            size = Size(w * 0.72f, h * 0.48f),
            cornerRadius = CornerRadius(w * 0.11f, w * 0.11f)
        )

        // Red Play Capsule
        drawRoundRect(
            color = tubeRed,
            topLeft = Offset(w * 0.28f, h * 0.38f),
            size = Size(w * 0.44f, h * 0.32f),
            cornerRadius = CornerRadius(w * 0.10f, w * 0.10f)
        )

        // Play Triangle
        val playPath = Path().apply {
            moveTo(w * 0.44f, h * 0.45f)
            lineTo(w * 0.61f, h * 0.54f)
            lineTo(w * 0.44f, h * 0.63f)
            close()
        }
        drawPath(path = playPath, color = Color.White)

        // Channel indicator dot
        drawCircle(color = cyan, radius = w * 0.032f, center = Offset(w * 0.79f, h * 0.38f))

        // TV Stand Bar
        drawRoundRect(
            color = cyan,
            topLeft = Offset(w * 0.34f, h * 0.89f),
            size = Size(w * 0.32f, h * 0.07f),
            cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
        )
    }
}

@Composable
fun KidTubeLogo(
    emblemSize: Dp = 32.dp,
    fontSize: TextUnit = 20.sp,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        KidTubeEmblem(size = emblemSize)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = Color(0xFFF1F5F9), fontWeight = FontWeight.ExtraBold)) {
                        append("Kid")
                    }
                },
                fontSize = fontSize
            )
            Surface(
                color = Color(0xFFF43F5E),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.padding(start = 2.dp)
            ) {
                Text(
                    text = "Tube",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = fontSize * 0.88f,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                )
            }
        }
    }
}
