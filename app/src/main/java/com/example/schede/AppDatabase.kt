package com.example.schede

import android.content.Context
import androidx.room.*

@Dao
interface InterventoDao {
    @Query("SELECT * FROM interventi ORDER BY id DESC")
    fun getAll(): List<Intervento>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(intervento: Intervento)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(interventi: List<Intervento>)

    @Delete
    fun delete(intervento: Intervento)

    @Query("DELETE FROM interventi")
    fun deleteAll()
}

@Database(entities = [Intervento::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun interventoDao(): InterventoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "schede_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
