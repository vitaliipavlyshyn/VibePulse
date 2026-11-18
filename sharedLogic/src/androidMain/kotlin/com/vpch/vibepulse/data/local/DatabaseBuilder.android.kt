package com.vpch.vibepulse.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

fun getDatabaseBuilder(context: Context): RoomDatabase.Builder<VibePulseDatabase> {
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath("vibepulse.db")
    return Room.databaseBuilder<VibePulseDatabase>(
        context = appContext,
        name = dbFile.absolutePath
    )
}