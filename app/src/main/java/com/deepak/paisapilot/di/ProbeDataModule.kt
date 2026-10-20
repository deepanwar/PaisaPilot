package com.deepak.paisapilot.di

import android.content.Context
import androidx.room.Room
import com.deepak.paisapilot.spike.TOOLCHAIN_PROBE_DATABASE_NAME
import com.deepak.paisapilot.spike.ToolchainProbeDao
import com.deepak.paisapilot.spike.ToolchainProbeDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ProbeDataModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ToolchainProbeDatabase =
        Room.databaseBuilder(
            context,
            ToolchainProbeDatabase::class.java,
            TOOLCHAIN_PROBE_DATABASE_NAME,
        ).build()

    @Provides
    fun provideDao(database: ToolchainProbeDatabase): ToolchainProbeDao =
        database.toolchainProbeDao()
}
