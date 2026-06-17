package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PostEntity::class, CommentEntity::class, StoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class InstagramDatabase : RoomDatabase() {
    abstract val dao: InstagramDao

    companion object {
        @Volatile
        private var INSTANCE: InstagramDatabase? = null

        fun getDatabase(context: Context): InstagramDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    InstagramDatabase::class.java,
                    "instagram_clone_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
