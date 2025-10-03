package com.example.furniturecloudy.di

import android.app.Application
import android.content.Context.MODE_PRIVATE
import androidx.room.Room
import com.example.furniturecloudy.database.FurnitureDatabase
import com.example.furniturecloudy.model.firebase.FirebaseCommon
import com.example.furniturecloudy.util.Constants.INTRODUCTION_SP
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object Module {
    @Singleton
    @Provides
    fun provideAuthFirebase() = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFireStoreDatabase() = Firebase.firestore

    @Provides
    fun provideIntroduction(application: Application) = application.getSharedPreferences(INTRODUCTION_SP,MODE_PRIVATE)

    @Provides
    @Singleton
    fun provideFirebaseCommon(firebaseFirestore: FirebaseFirestore,firebaseAuth: FirebaseAuth) = FirebaseCommon(firebaseFirestore,firebaseAuth)

    @Provides
    @Singleton
    fun provideFirebaseStorage() = FirebaseStorage.getInstance().reference

    @Provides
    @Singleton
    fun provideFurnitureDatabase(application: Application): FurnitureDatabase {
        return Room.databaseBuilder(
            application,
            FurnitureDatabase::class.java,
            "furniture_database"
        ).build()
    }

    @Provides
    @Singleton
    fun provideSearchHistoryDao(database: FurnitureDatabase) = database.searchHistoryDao()

    @Provides
    @Singleton
    fun provideRecentlyViewedDao(database: FurnitureDatabase) = database.recentlyViewedDao()
}