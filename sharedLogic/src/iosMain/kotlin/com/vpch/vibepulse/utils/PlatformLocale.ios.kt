package com.vpch.vibepulse.utils

import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.languageCode

actual fun getSystemLanguageCode(): String =
    NSLocale.currentLocale.languageCode