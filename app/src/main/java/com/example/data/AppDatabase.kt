package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SchoolEntity::class,
        UserEntity::class,
        StudentEntity::class,
        BusEntity::class,
        RouteEntity::class,
        StopEntity::class,
        TripEntity::class,
        NotificationEntity::class,
        AuditLogEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun schoolDao(): SchoolDao
    abstract fun userDao(): UserDao
    abstract fun studentDao(): StudentDao
    abstract fun busDao(): BusDao
    abstract fun routeDao(): RouteDao
    abstract fun stopDao(): StopDao
    abstract fun tripDao(): TripDao
    abstract fun notificationDao(): NotificationDao
    abstract fun auditLogDao(): AuditLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "school_track_pro_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
