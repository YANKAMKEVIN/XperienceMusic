package com.xperiencemusic.shared.ui.brand

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xperiencemusic.shared.ui.theme.BrandColors

/**
 * The XM monogram: white X, gradient M whose right leg is a note. Same drawing as the app icon
 * (design/make_icon.swift), in a 1000x1000 design space with y pointing down.
 */
@Composable
fun XmMonogram(modifier: Modifier = Modifier, size: Dp = 96.dp) {
    Canvas(modifier.size(size)) {
        val unit = this.size.minDimension / 1000f
        // Centre the drawing (it spans x 150..935, y 200..760 once flipped).
        translate(left = (500f - 542f) * unit, top = (500f - 480f) * unit) {
            scale(unit, unit, pivot = Offset.Zero) { drawMonogram() }
        }
    }
}

private val snow = Color(0xFFF4F3FA)

private fun DrawScope.drawMonogram() {
    // Design coordinates were y-up; flip once here.
    fun p(x: Float, y: Float) = Offset(x, 1000f - y)

    val x = Path().apply {
        moveTo(150f, 1000f - 300f); lineTo(420f, 1000f - 700f)
        moveTo(150f, 1000f - 700f); lineTo(420f, 1000f - 300f)
    }
    drawPath(x, snow, style = Stroke(width = 96f, cap = StrokeCap.Butt))

    val gradient = Brush.linearGradient(
        listOf(BrandColors.Purple, BrandColors.Indigo, BrandColors.Blue),
        start = p(480f, 820f),
        end = p(900f, 240f),
    )
    val m = Path().apply {
        moveTo(505f, 1000f - 300f); lineTo(505f, 1000f - 700f); lineTo(640f, 1000f - 500f); lineTo(775f, 1000f - 700f)
    }
    // Flat top at the X's height instead of the miter's spike.
    // Explicit bounds: the defaults would be the canvas size in pixels, not design units.
    clipRect(left = 0f, top = 1000f - 733f, right = 1000f, bottom = 1000f) {
        drawPath(m, gradient, style = Stroke(width = 92f, cap = StrokeCap.Butt, join = StrokeJoin.Miter, miter = 10f))
    }
    // Right leg doubles as the note's stem.
    drawRect(gradient, topLeft = p(729f, 800f), size = Size(92f, 470f))
    val flag = Path().apply {
        moveTo(805f, 1000f - 785f)
        cubicTo(845f, 1000f - 730f, 935f, 1000f - 690f, 885f, 1000f - 600f)
    }
    drawPath(flag, gradient, style = Stroke(width = 56f, cap = StrokeCap.Round))
    // Note head, tilted like a printed note.
    rotate(degrees = -24f, pivot = p(708f, 322f)) {
        val c = p(708f, 322f)
        drawOval(gradient, topLeft = Offset(c.x - 100f, c.y - 70f), size = Size(200f, 140f))
    }
}

