package com.automatic.main.di

import com.automatic.design.ui.permission.PermissionUtil
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun getPermissionUtil(): PermissionUtil {
        return PermissionUtil()
    }
}