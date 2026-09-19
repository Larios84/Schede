package com.example.schede

import androidx.room.*

@Dao
interface AutoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(auto: Auto)

    @Delete
    fun delete(auto: Auto)

    @Query("SELECT * FROM auto")
    fun getAll(): List<Auto>
}

@Dao
interface RimborsoMeseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(rimborso: RimborsoMese)

    @Query("SELECT * FROM rimborsi_mese WHERE mese = :mese AND anno = :anno LIMIT 1")
    fun getByMonth(mese: Int, anno: Int): RimborsoMese?

    @Query("""
        SELECT auto.* FROM auto 
        INNER JOIN rimborsi_mese ON auto.id = rimborsi_mese.autoId 
        WHERE rimborsi_mese.mese = :mese AND rimborsi_mese.anno = :anno
        LIMIT 1
    """)
    fun getAutoForMonth(mese: Int, anno: Int): Auto?
}

@Dao
interface ViaggioDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(viaggio: Viaggio)

    @Query("SELECT * FROM viaggi WHERE rimborsoMeseId = :rimborsoId ORDER BY giorno ASC")
    fun getViaggiForRimborso(rimborsoId: Int): List<Viaggio>

    @Delete
    fun delete(viaggio: Viaggio)
}
