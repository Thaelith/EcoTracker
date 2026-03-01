package com.ecotracker.di

import android.content.Context
import androidx.room.Room
import com.ecotracker.data.local.EcoTrackerDatabase
import com.ecotracker.data.local.ScannedProductDao
import com.ecotracker.data.remote.OpenBeautyFactsApiService
import com.ecotracker.data.remote.OpenFoodFactsApiService
import com.ecotracker.data.remote.UPCItemDbApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // ── Network ───────────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "EcoTracker/1.0 (Android)")
                    .build()
                chain.proceed(request)
            }
            .build()

    @Provides
    @Singleton
    fun provideOpenFoodFactsApi(client: OkHttpClient): OpenFoodFactsApiService =
        Retrofit.Builder()
            .baseUrl(OpenFoodFactsApiService.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenFoodFactsApiService::class.java)

    @Provides
    @Singleton
    fun provideUPCItemDbApi(client: OkHttpClient): UPCItemDbApiService =
        Retrofit.Builder()
            .baseUrl(UPCItemDbApiService.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(UPCItemDbApiService::class.java)

    @Provides
    @Singleton
    fun provideOpenBeautyFactsApi(client: OkHttpClient): OpenBeautyFactsApiService =
        Retrofit.Builder()
            .baseUrl(OpenBeautyFactsApiService.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenBeautyFactsApiService::class.java)

    // ── Database ──────────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): EcoTrackerDatabase =
        Room.databaseBuilder(
            context,
            EcoTrackerDatabase::class.java,
            EcoTrackerDatabase.DATABASE_NAME
        ).build()

    @Provides
    @Singleton
    fun provideScannedProductDao(db: EcoTrackerDatabase): ScannedProductDao =
        db.scannedProductDao()
}
