package com.example.finalproject.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [RideEntity::class],
    version = 1,
    exportSchema = false
)
abstract class RideDatabase : RoomDatabase() {
    
    abstract fun rideDao(): RideDao
    
    companion object {
        @Volatile
        private var INSTANCE: RideDatabase? = null
        
        fun getDatabase(context: Context): RideDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RideDatabase::class.java,
                    "ride_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
} 