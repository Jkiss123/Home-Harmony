package com.example.furniturecloudy.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.furniturecloudy.database.entity.RecentlyViewed
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentlyViewedDao {

    @Query("SELECT * FROM recently_viewed ORDER BY viewedAt DESC LIMIT 10")
    fun getRecentlyViewed(): Flow<List<RecentlyViewed>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertViewed(viewed: RecentlyViewed)

    @Query("DELETE FROM recently_viewed WHERE viewedAt < :timestamp")
    suspend fun deleteOldViews(timestamp: Long)

    @Query("DELETE FROM recently_viewed")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM recently_viewed")
    suspend fun getCount(): Int
}
