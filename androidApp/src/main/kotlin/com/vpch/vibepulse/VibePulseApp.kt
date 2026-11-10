package com.vpch.vibepulse

import android.app.Application
import com.vpch.vibepulse.di.initKoin
class VibePulseApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(this)
    }
}