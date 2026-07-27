package com.niranjan.medqueue.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [RequestEntity::class], version = 6, exportSchema = false)
@TypeConverters(RequestConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun requestDao(): RequestDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /** Migration 1→2: add createdAt column with current timestamp as default. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE requests ADD COLUMN createdAt INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()}"
                )
            }
        }

        /** Migration 2→3: emergency flag moves from in-memory state onto the row. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE requests ADD COLUMN isEmergency INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        /** Migration 3→4: optional prescription photo path. */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE requests ADD COLUMN prescriptionPath TEXT")
            }
        }

        /**
         * Migration 4→5: when the customer was last messaged.
         *
         * Nullable with no default — existing rows genuinely have no answer,
         * and back-filling them with a timestamp would claim every historical
         * customer had already been told.
         */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE requests ADD COLUMN notifiedAt INTEGER")
            }
        }

        /** Migration 5→6: which medicine lines have arrived. */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE requests ADD COLUMN readyItems TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            // Double-checked locking: the second read inside the lock is what stops
            // two racing callers from each building their own database.
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "medqueue.db"
                )
                    .addMigrations(
                        MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6
                    )
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

