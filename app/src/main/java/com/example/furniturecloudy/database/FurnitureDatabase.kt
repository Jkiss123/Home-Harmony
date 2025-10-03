package com.example.furniturecloudy.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.furniturecloudy.database.dao.RecentlyViewedDao
import com.example.furniturecloudy.database.dao.SearchHistoryDao
import com.example.furniturecloudy.database.entity.RecentlyViewed
import com.example.furniturecloudy.database.entity.SearchHistory

@Database(
    entities = [
        SearchHistory::class,
        RecentlyViewed::class
    ],
    version = 1,
    exportSchema = false
)
abstract class FurnitureDatabase : RoomDatabase() {
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun recentlyViewedDao(): RecentlyViewedDao
}
