package com.org.doctorchakravue.domain

data class Doctor(
    val id: String,
    val name: String,
    val specialization: String
)

data class Patient(
    val id: String,
    val name: String,
    val age: Int,
    val condition: String
)
