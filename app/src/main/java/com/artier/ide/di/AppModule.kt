package com.artier.ide.di

import android.content.Context
import com.artier.ide.data.remote.DaemonApi
import com.artier.ide.data.remote.DaemonClient
import com.artier.ide.data.remote.LspClient
import com.artier.ide.data.remote.WebSocketClient
import com.artier.ide.data.repository.ChatRepository
import com.artier.ide.data.repository.DatabaseRepository
import com.artier.ide.data.repository.EditorRepository
import com.artier.ide.data.repository.FileRepository
import com.artier.ide.data.repository.SkillRepository
import com.artier.ide.proot.DaemonManager
import com.artier.ide.proot.ProotManager
import com.artier.ide.proot.SystemInitializer
import com.artier.ide.ui.terminal.TerminalManager
import com.artier.ide.utils.SecureStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideWebSocketClient(): WebSocketClient {
        return WebSocketClient()
    }

    @Provides
    @Singleton
    fun provideDaemonApi(webSocketClient: WebSocketClient): DaemonApi {
        return DaemonApi(webSocketClient)
    }

    @Provides
    @Singleton
    fun provideDaemonClient(
        daemonApi: DaemonApi,
        webSocketClient: WebSocketClient
    ): DaemonClient {
        return DaemonClient(daemonApi, webSocketClient)
    }

    @Provides
    @Singleton
    fun provideFileRepository(daemonApi: DaemonApi): FileRepository {
        return FileRepository(daemonApi)
    }

    @Provides
    @Singleton
    fun provideEditorRepository(): EditorRepository {
        return EditorRepository()
    }

    @Provides
    @Singleton
    fun provideProotManager(@ApplicationContext context: Context): ProotManager {
        return ProotManager(context)
    }

    @Provides
    @Singleton
    fun provideDaemonManager(
        @ApplicationContext context: Context,
        prootManager: ProotManager
    ): DaemonManager {
        return DaemonManager(context, prootManager)
    }

    @Provides
    @Singleton
    fun provideSystemInitializer(@ApplicationContext context: Context): SystemInitializer {
        return SystemInitializer(context)
    }

    @Provides
    @Singleton
    fun provideTerminalManager(webSocketClient: WebSocketClient): TerminalManager {
        return TerminalManager(webSocketClient)
    }

    @Provides
    @Singleton
    fun provideLspClient(webSocketClient: WebSocketClient): LspClient {
        return LspClient(webSocketClient)
    }

    @Provides
    @Singleton
    fun provideDatabaseRepository(webSocketClient: WebSocketClient): DatabaseRepository {
        return DatabaseRepository(webSocketClient)
    }

    @Provides
    @Singleton
    fun provideSkillRepository(webSocketClient: WebSocketClient): SkillRepository {
        return SkillRepository(webSocketClient)
    }

    @Provides
    @Singleton
    fun provideChatRepository(): ChatRepository {
        return ChatRepository()
    }

    @Provides
    @Singleton
    fun provideSecureStorage(@ApplicationContext context: Context): SecureStorage {
        return SecureStorage(context)
    }
}
