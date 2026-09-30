package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
  entities = [
    InventoryCardEntity::class,
    SavedDeckEntity::class,
    UserCardEntity::class,
    CardEntity::class,
  ],
  version = 4,
  exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun inventoryDao(): InventoryDao
  abstract fun savedDeckDao(): SavedDeckDao
  abstract fun userCardDao(): UserCardDao
  abstract fun cardDao(): CardDao

  companion object {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "tcg_pocket_inventory.db",
        )
          .fallbackToDestructiveMigration(dropAllTables = true)
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
