package com.vpch.vibepulse.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.vpch.vibepulse.data.local.dao.ActivityDao
import com.vpch.vibepulse.data.local.entity.ActivityEntity
import com.vpch.vibepulse.data.local.entity.ActivityTrackEntity
import com.vpch.vibepulse.data.local.entity.CadenceSampleEntity
import com.vpch.vibepulse.data.local.entity.UserEntity
import com.vpch.vibepulse.data.local.entity.WeatherSnapshotEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Database(
    entities = [
        UserEntity::class,
        WeatherSnapshotEntity::class,
        ActivityEntity::class,
        CadenceSampleEntity::class,
        ActivityTrackEntity::class
    ],
    version = 1,
    exportSchema = true
)
@ConstructedBy(VibePulseDatabaseConstructor::class)
abstract class VibePulseDatabase : RoomDatabase() {
    abstract fun activityDao(): ActivityDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object VibePulseDatabaseConstructor : RoomDatabaseConstructor<VibePulseDatabase> {
    override fun initialize(): VibePulseDatabase
}

fun getRoomDatabase(
    builder: RoomDatabase.Builder<VibePulseDatabase>
): VibePulseDatabase {
    return builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}