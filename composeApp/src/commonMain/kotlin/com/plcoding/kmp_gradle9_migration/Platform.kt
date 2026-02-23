package com.plcoding.kmp_gradle9_migration

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform