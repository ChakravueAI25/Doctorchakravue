package com.org.doctorchakravue.core.navigation

sealed class NavRoutes(val route: String) {
    object Dashboard : NavRoutes("dashboard")
    object Patients : NavRoutes("patients")
    object PatientDetail : NavRoutes("patientDetail/{patientId}") {
        fun createRoute(patientId: String) = "patientDetail/$patientId"
    }
    object Submissions : NavRoutes("submissions")
    object Adherence : NavRoutes("adherence")
    object Notifications : NavRoutes("notifications")
    object Call : NavRoutes("call")
}
