package com.vpch.vibepulse

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform