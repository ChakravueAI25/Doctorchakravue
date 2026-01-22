package com.org.doctorchakravue.app

/**
 * Navigation constants and utilities.
 * Contains the list of screens that should show the bottom navigation bar.
 */
object Navigator {
    // Screens that display the bottom navigation bar
    val bottomNavScreens = setOf(
        "dashboard",
        "patients",
        "notifications",
        "adherence"
    )

    fun shouldShowBottomNav(currentRoute: String?): Boolean {
        return currentRoute in bottomNavScreens
    }
}
