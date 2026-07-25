package dev.lumen.launcher.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.lumen.launcher.core.data.db.GridDao
import dev.lumen.launcher.core.data.db.LumenDatabase
import dev.lumen.launcher.core.data.prefs.PrefsSerializer
import dev.lumen.launcher.core.data.prefs.UsageSerializer
import dev.lumen.launcher.core.data.proto.LumenPrefs
import dev.lumen.launcher.core.data.proto.UsageStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    /** One long-lived scope for repositories; a launcher process lives as long as the session. */
    @Provides
    @Singleton
    fun appScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): LumenDatabase =
        Room.databaseBuilder(context, LumenDatabase::class.java, "lumen.db")
            // A corrupt or downgraded database yields an empty home screen the user can rebuild;
            // a crash-loop yields a phone with no home screen at all.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun gridDao(db: LumenDatabase): GridDao = db.gridDao()

    @Provides
    @Singleton
    fun prefsStore(
        @ApplicationContext context: Context,
        scope: CoroutineScope,
    ): DataStore<LumenPrefs> = DataStoreFactory.create(
        serializer = PrefsSerializer,
        scope = CoroutineScope(scope.coroutineContext + Dispatchers.IO + SupervisorJob()),
    ) { context.dataStoreFile("lumen_prefs.pb") }

    @Provides
    @Singleton
    fun usageStore(
        @ApplicationContext context: Context,
        scope: CoroutineScope,
    ): DataStore<UsageStats> = DataStoreFactory.create(
        serializer = UsageSerializer,
        scope = CoroutineScope(scope.coroutineContext + Dispatchers.IO + SupervisorJob()),
    ) { context.dataStoreFile("lumen_usage.pb") }
}
