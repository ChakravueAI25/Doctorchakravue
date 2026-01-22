package com.org.doctorchakravue.platform

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
