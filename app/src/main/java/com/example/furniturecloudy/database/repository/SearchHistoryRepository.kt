package com.example.furniturecloudy.database.repository

import com.example.furniturecloudy.database.dao.SearchHistoryDao
import com.example.furniturecloudy.database.entity.SearchHistory
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchHistoryRepository @Inject constructor(
    private val searchHistoryDao: SearchHistoryDao
) {

    fun getRecentSearches(): Flow<List<String>> {
        return searchHistoryDao.getRecentSearches()
    }

    fun getRecentSearches(limit: Int): Flow<List<SearchHistory>> {
        return searchHistoryDao.getRecentSearchesWithLimit(limit)
    }

    suspend fun getSuggestions(prefix: String): List<String> {
        return if (prefix.isBlank()) {
            emptyList()
        } else {
            searchHistoryDao.getSuggestions(prefix)
        }
    }

    suspend fun addSearch(query: String) {
        if (query.isNotBlank()) {
            val searchHistory = SearchHistory(query = query.trim())
            searchHistoryDao.insertSearch(searchHistory)

            // Delete searches older than 90 days
            val ninetyDaysAgo = System.currentTimeMillis() - (90 * 24 * 60 * 60 * 1000L)
            searchHistoryDao.deleteOldSearches(ninetyDaysAgo)
        }
    }

    suspend fun deleteSearch(query: String) {
        searchHistoryDao.deleteSearch(query)
    }

    suspend fun deleteSearch(searchHistory: SearchHistory) {
        searchHistoryDao.deleteSearch(searchHistory.query)
    }

    suspend fun clearHistory() {
        searchHistoryDao.clearHistory()
    }
}
