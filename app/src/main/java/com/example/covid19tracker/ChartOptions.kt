package com.example.covid19tracker

enum class Metrics {
    NEGATIVE, POSITIVE, DEATH
}

enum class TimeScale (val numsDays: Int) {
    WEEK (7),
    MONTH (30),
    MAX(-1)
}