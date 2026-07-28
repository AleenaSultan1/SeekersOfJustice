package com.seekersofjustice.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [CaseEntity::class, MemoEntity::class, UserEntity::class],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun caseDao(): CaseDao
    abstract fun memoDao(): MemoDao
    abstract fun userDao(): UserDao

    companion object {
        // v2: per-statement speaker + recorded-by officer, and the users table.
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE memos ADD COLUMN speakerName TEXT")
                db.execSQL("ALTER TABLE memos ADD COLUMN speakerRelation TEXT")
                db.execSQL("ALTER TABLE memos ADD COLUMN recordedByUserId INTEGER")
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS users (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        pinHash TEXT NOT NULL,
                        pinSalt TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )"""
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_users_name ON users(name)")
            }
        }

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "seekers.db")
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
