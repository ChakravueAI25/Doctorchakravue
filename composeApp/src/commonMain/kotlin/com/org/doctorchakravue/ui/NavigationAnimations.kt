package com.org.doctorchakravue.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.navigation.NavBackStackEntry

/**
 * Global navigation animations for the entire app.
 * Provides smooth slide + fade transitions for all screen navigations.
 */
object NavigationAnimations {
    private const val DURATION = 300

    /**
     * Animation when navigating forward to a new screen.
     * Slides in from right + fades in.
     */
    val enterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition) = {
        slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Left,
            animationSpec = tween(DURATION)
        ) + fadeIn(animationSpec = tween(DURATION))
    }

    /**
     * Animation for the current screen when navigating forward.
     * Slides out to left + fades out.
     */
    val exitTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition) = {
        slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Left,
            animationSpec = tween(DURATION)
        ) + fadeOut(animationSpec = tween(DURATION))
    }

    /**
     * Animation when navigating back (previous screen coming back).
     * Slides in from left + fades in.
     */
    val popEnterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition) = {
        slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Right,
            animationSpec = tween(DURATION)
        ) + fadeIn(animationSpec = tween(DURATION))
    }

    /**
     * Animation for the current screen when pressing back.
     * Slides out to right + fades out.
     */
    val popExitTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition) = {
        slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Right,
            animationSpec = tween(DURATION)
        ) + fadeOut(animationSpec = tween(DURATION))
    }
}
