package com.org.doctorchakravue.features.dashboard

import com.org.doctorchakravue.data.Submission

data class DashboardState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val doctorName: String = "",
    val urgentReviews: List<Submission> = emptyList(),
    val history: List<Submission> = emptyList()
)
