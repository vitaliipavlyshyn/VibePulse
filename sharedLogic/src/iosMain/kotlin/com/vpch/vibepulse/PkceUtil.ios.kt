package com.vpch.vibepulse

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.refTo
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.base64EncodedStringWithOptions
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual fun generateCodeChallenge(verifier: String): String {
    val inputBytes = verifier.encodeToByteArray()
    if (inputBytes.isEmpty()) return ""

    val hash = ByteArray(CC_SHA256_DIGEST_LENGTH)

    inputBytes.usePinned { inputPinned ->
        hash.usePinned { hashPinned ->
            CC_SHA256(
                data = inputPinned.addressOf(0),
                len = inputBytes.size.toUInt(),
                md = hashPinned.addressOf(0).reinterpret()
            )
        }
    }

    val nsData = hash.usePinned { hashPinned ->
        NSData.create(
            bytes = hashPinned.addressOf(0),
            length = hash.size.toULong()
        )
    }

    return nsData.base64EncodedStringWithOptions(0UL)
        .replace("+", "-")
        .replace("/", "_")
        .trimEnd('=')
}