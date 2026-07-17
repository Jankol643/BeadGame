package com.kolnberger.beadsgame

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform