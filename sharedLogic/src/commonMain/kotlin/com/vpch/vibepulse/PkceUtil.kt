package com.vpch.vibepulse

import kotlin.random.Random


expect fun generateCodeChallenge(verifier: String): String
object PkceUtil {
    fun generateCodeVerifier(): String {
        val charPool = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"
        return (1..128)
            .map { Random.nextInt(0, charPool.length) }
            .map(charPool::get)
            .joinToString("")
    }
}