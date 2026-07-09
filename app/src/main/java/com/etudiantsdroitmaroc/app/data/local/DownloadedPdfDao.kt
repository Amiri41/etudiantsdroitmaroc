package com.etudiantsdroitmaroc.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadedPdfDao {

    @Query("SELECT * FROM downloaded_pdfs ORDER BY downloadedAt DESC")
    fun getAll(): Flow<List<DownloadedPdfEntity>>

    @Query("SELECT * FROM downloaded_pdfs WHERE subjectId = :subjectId")
    fun getForSubject(subjectId: String): Flow<List<DownloadedPdfEntity>>

    @Query("SELECT * FROM downloaded_pdfs WHERE pdfId = :pdfId LIMIT 1")
    suspend fun getById(pdfId: String): DownloadedPdfEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DownloadedPdfEntity)

    @Delete
    suspend fun delete(entity: DownloadedPdfEntity)

    @Query("DELETE FROM downloaded_pdfs WHERE pdfId = :pdfId")
    suspend fun deleteById(pdfId: String)
}
