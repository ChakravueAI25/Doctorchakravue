package com.org.doctorchakravue

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.*
import kotlin.random.Random

/* =========================================================
   DOCTOR CHAKRAVUE — BRAHMI GLYPH SPLASH ANIMATION

   Phase A: Primordial Soup (wandering glyphs)
   Phase B: Alignment (4 leaders rotate, particles rush inward)
   Phase C: Logo Lock (180° rotation, snap to logo positions)
   Phase D: Flash & Reveal (white flash, logo zoom)
   ========================================================= */

private enum class Phase {
    PRIMORDIAL_SOUP,  // Wandering Brahmi characters
    ALIGNMENT,        // 4 leaders appear, particles accelerate inward
    LOGO_LOCK,        // 180° rotation, snap to logo positions
    FLASH_REVEAL      // White flash, logo appears
}

/* ---------------- BRAHMI GLYPH PATHS ---------------- */

private val brahmiGlyphs = listOf(
    // Triangle glyph
    Path().apply {
        moveTo(0f, -12f)
        lineTo(10f, 10f)
        lineTo(-10f, 10f)
        close()
    },
    // Diamond glyph
    Path().apply {
        moveTo(0f, -14f)
        lineTo(12f, 0f)
        lineTo(0f, 14f)
        lineTo(-12f, 0f)
        close()
    },
    // Bar glyph
    Path().apply {
        addRect(Rect(-4f, -12f, 4f, 12f))
    },
    // Cross glyph
    Path().apply {
        addRect(Rect(-3f, -12f, 3f, 12f))
        addRect(Rect(-10f, -3f, 10f, 3f))
    },
    // Arc glyph
    Path().apply {
        moveTo(-10f, 8f)
        quadraticTo(0f, -14f, 10f, 8f)
        lineTo(6f, 8f)
        quadraticTo(0f, -6f, -6f, 8f)
        close()
    }
)

@Composable
fun DoctorChakravueSplashScreen(onAnimationFinished: () -> Unit) {

    /* ---------------- CONFIG ---------------- */

    val particleCount = 48
    val leaderCount = 4
    val logoRadius = 160f

    /* ---------------- PARTICLE DATA ---------------- */

    data class Particle(
        val id: Int,
        val glyphIndex: Int,
        val baseAngle: Float,
        val wanderOffset: Float,
        val wanderSpeed: Float,
        val homeAngle: Float,      // Final angle in logo circle
        val homeRotation: Float,   // Final rotation of glyph
        val isLeader: Boolean
    )

    val particles = remember {
        List(particleCount) { i ->
            val homeAngle = (i.toFloat() / particleCount) * 2f * PI.toFloat()
            Particle(
                id = i,
                glyphIndex = i % brahmiGlyphs.size,
                baseAngle = Random.nextFloat() * 2f * PI.toFloat(),
                wanderOffset = Random.nextFloat() * 1000f,
                wanderSpeed = Random.nextFloat() * 0.5f + 0.3f,
                homeAngle = homeAngle,
                homeRotation = homeAngle * 57.3f + 90f,
                isLeader = i < leaderCount
            )
        }
    }

    /* ---------------- ANIMATION STATES ---------------- */

    var phase by remember { mutableStateOf(Phase.PRIMORDIAL_SOUP) }
    val progress = remember { Animatable(0f) }
    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(1.3f) }
    val flashAlpha = remember { Animatable(0f) }
    val leaderAlpha = remember { Animatable(0f) }

    // Continuous time for wandering animation
    val infiniteTransition = rememberInfiniteTransition(label = "wander")
    val wanderTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(100000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wanderTime"
    )

    /* ---------------- TIMELINE ---------------- */

    LaunchedEffect(Unit) {

        // Phase A: Primordial Soup (2 seconds of wandering)
        phase = Phase.PRIMORDIAL_SOUP
        delay(2000)

        // Phase B: Alignment (leaders appear, particles rush inward)
        phase = Phase.ALIGNMENT
        leaderAlpha.animateTo(1f, tween(300))
        progress.snapTo(0f)
        progress.animateTo(1f, tween(2000, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)))

        // Phase C: Logo Lock (180° rotation, snap with spring)
        phase = Phase.LOGO_LOCK
        progress.snapTo(0f)
        progress.animateTo(1f, tween(1500, easing = FastOutSlowInEasing))

        // Phase D: Flash & Reveal
        phase = Phase.FLASH_REVEAL
        flashAlpha.animateTo(1f, tween(150))
        logoAlpha.animateTo(1f, tween(200))
        logoScale.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
        flashAlpha.animateTo(0f, tween(500))

        delay(1000)
        onAnimationFinished()
    }

    /* ---------------- DRAW ---------------- */

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {

        Canvas(modifier = Modifier.fillMaxSize()) {

            val center = Offset(size.width / 2f, size.height / 2f)
            val maxWanderRadius = min(size.width, size.height) * 0.45f

            particles.forEach { p ->

                // Calculate position and rotation based on phase
                val (pos, rotation, alpha, scale) = when (phase) {

                    Phase.PRIMORDIAL_SOUP -> {
                        // Wandering behavior with depth-based alpha
                        val wanderAngle = p.baseAngle + sin(wanderTime * p.wanderSpeed + p.wanderOffset) * 0.8f
                        val wanderRadius = maxWanderRadius * (0.3f + 0.7f * sin(wanderTime * 0.3f + p.wanderOffset).absoluteValue)
                        val x = center.x + cos(wanderAngle) * wanderRadius
                        val y = center.y + sin(wanderAngle) * wanderRadius
                        val distFromCenter = sqrt((x - center.x).pow(2) + (y - center.y).pow(2))
                        val depthAlpha = 0.4f + 0.6f * (1f - distFromCenter / maxWanderRadius)
                        val rot = wanderTime * 20f * p.wanderSpeed + p.wanderOffset

                        ParticleState(Offset(x, y), rot, depthAlpha, 1f)
                    }

                    Phase.ALIGNMENT -> {
                        val t = progress.value

                        if (p.isLeader) {
                            // Leaders rotate clockwise in a circle
                            val leaderAngle = p.baseAngle + t * 2f * PI.toFloat()
                            val leaderRadius = 200f
                            val x = center.x + cos(leaderAngle) * leaderRadius
                            val y = center.y + sin(leaderAngle) * leaderRadius
                            val rot = leaderAngle * 57.3f

                            ParticleState(Offset(x, y), rot, leaderAlpha.value, 2.5f)
                        } else {
                            // Other particles accelerate inward (inward acceleration curve)
                            val easedT = t.pow(2.5f) // Accelerating curve
                            val startRadius = maxWanderRadius * (0.5f + 0.5f * p.wanderOffset / 1000f)
                            val endRadius = logoRadius + 50f
                            val currentRadius = startRadius + (endRadius - startRadius) * easedT
                            val currentAngle = p.baseAngle + easedT * 4f
                            val x = center.x + cos(currentAngle) * currentRadius
                            val y = center.y + sin(currentAngle) * currentRadius
                            val rot = currentAngle * 57.3f + wanderTime * 10f * p.wanderSpeed

                            ParticleState(Offset(x, y), rot, 0.7f + 0.3f * easedT, 1f)
                        }
                    }

                    Phase.LOGO_LOCK -> {
                        val t = progress.value

                        // 180° rotation during formation
                        val formationRotation = PI.toFloat() * t

                        // Interpolate to home position with overshoot
                        val overshoot = if (t > 0.8f) sin((t - 0.8f) * 5f * PI.toFloat()) * 5f * (1f - t) else 0f
                        val targetRadius = logoRadius + overshoot

                        // Current angle interpolates to home angle
                        val startAngle = p.baseAngle + 4f // End angle from alignment phase
                        val currentAngle = startAngle + (p.homeAngle - startAngle) * t + formationRotation

                        val x = center.x + cos(currentAngle) * targetRadius
                        val y = center.y + sin(currentAngle) * targetRadius

                        // Glyph rotation snaps to home rotation
                        val rot = p.homeRotation * t + (1f - t) * (startAngle * 57.3f)

                        val glyphScale = if (p.isLeader) 2.5f - 1.5f * t else 1f

                        ParticleState(Offset(x, y), rot, 1f, glyphScale)
                    }

                    Phase.FLASH_REVEAL -> {
                        // Glyphs stay in logo formation
                        val x = center.x + cos(p.homeAngle) * logoRadius
                        val y = center.y + sin(p.homeAngle) * logoRadius

                        ParticleState(Offset(x, y), p.homeRotation, 1f - flashAlpha.value * 0.5f, 1f)
                    }
                }

                // Draw the glyph
                withTransform({
                    translate(pos.x, pos.y)
                    rotate(rotation, pivot = Offset.Zero)
                    scale(scale, scale, pivot = Offset.Zero)
                }) {
                    drawPath(
                        path = brahmiGlyphs[p.glyphIndex],
                        color = Color.White.copy(alpha = alpha.coerceIn(0f, 1f))
                    )
                }
            }

            // Central glow during logo lock and flash
            if (phase == Phase.LOGO_LOCK || phase == Phase.FLASH_REVEAL) {
                val glowAlpha = when (phase) {
                    Phase.LOGO_LOCK -> progress.value * 0.6f
                    Phase.FLASH_REVEAL -> 0.6f + flashAlpha.value * 0.4f
                    else -> 0f
                }
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color.White.copy(alpha = glowAlpha), Color.Transparent),
                        center = center,
                        radius = logoRadius * 0.8f
                    ),
                    radius = logoRadius * 0.8f,
                    center = center
                )
            }

            // White flash
            if (flashAlpha.value > 0f) {
                drawRect(
                    brush = Brush.radialGradient(
                        listOf(
                            Color.White.copy(alpha = flashAlpha.value),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.minDimension * 0.7f
                    ),
                    size = size
                )
            }
        }

        // Logo image (appears during flash)
        if (phase == Phase.FLASH_REVEAL && logoAlpha.value > 0f) {
            Image(
                painter = painterResource(id = R.drawable.app_icon_foreground),
                contentDescription = null,
                modifier = Modifier
                    .size(220.dp)
                    .graphicsLayer {
                        alpha = logoAlpha.value
                        scaleX = logoScale.value
                        scaleY = logoScale.value
                    }
            )
        }
    }
}

/* ---------------- HELPER DATA CLASS ---------------- */

private data class ParticleState(
    val position: Offset,
    val rotation: Float,
    val alpha: Float,
    val scale: Float
)
