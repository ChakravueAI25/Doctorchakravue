package com.org.doctorchakravue

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform