package com.deepak.paisapilot.di

import com.deepak.paisapilot.core.data.IdGenerator
import com.deepak.paisapilot.core.data.UuidIdGenerator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TimeModule {
    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()

    @Provides
    @Singleton
    fun provideIdGenerator(): IdGenerator = UuidIdGenerator
}
