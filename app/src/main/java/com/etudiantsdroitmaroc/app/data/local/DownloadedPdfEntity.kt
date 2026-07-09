package com.etudiantsdroitmaroc.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * سطر واحد = ملف PDF محمل محليا فالهاتف (باش يتقرا بلا انترنت)
 */
@Entity(tableName = "downloaded_pdfs")
data class DownloadedPdfEntity(
    @PrimaryKey val pdfId: String,
    val subjectId: String,
    val title: String,
    val localFilePath: String,
    val downloadedAt: Long = System.currentTimeMillis()
)
