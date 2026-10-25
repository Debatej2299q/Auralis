package dev.auralis.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Track::class, PlaylistEntity::class, PlaylistTrackEntity::class, HistoryEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class MusicDatabase : RoomDatabase() {

    abstract fun dao(): MusicDao

    companion object {
        @Volatile
        private var instance: MusicDatabase? = null

        fun build(context: Context): MusicDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MusicDatabase::class.java,
                    "auralis.db",
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
