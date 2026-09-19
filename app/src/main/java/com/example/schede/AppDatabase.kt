package com.example.schede

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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

@Database(entities = [Intervento::class, Auto::class, RimborsoMese::class, Viaggio::class], version = 7, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun interventoDao(): InterventoDao
    abstract fun autoDao(): AutoDao
    abstract fun rimborsoMeseDao(): RimborsoMeseDao
    abstract fun viaggioDao(): ViaggioDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `auto` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `modello` TEXT NOT NULL, `targa` TEXT NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `rimborsi_mese` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `mese` INTEGER NOT NULL, `anno` INTEGER NOT NULL, `autoId` INTEGER NOT NULL, FOREIGN KEY(`autoId`) REFERENCES `auto`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `viaggi` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `rimborsoMeseId` INTEGER NOT NULL, `giorno` INTEGER NOT NULL, `causale` TEXT NOT NULL, `km` REAL NOT NULL, `partenza` TEXT NOT NULL, `destinazione` TEXT NOT NULL, FOREIGN KEY(`rimborsoMeseId`) REFERENCES `rimborsi_mese`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `auto` ADD COLUMN `annoImmatricolazione` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `auto` ADD COLUMN `combustibile` TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "schede_database"
                )
                .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
