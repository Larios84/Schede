package com.example.schede

import android.content.Context
import android.net.Uri
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.InputStream
import java.io.OutputStream

class KmExcelManager(private val context: Context) {

    fun compilaExcelKm(
        templateInputStream: InputStream,
        outputStream: OutputStream,
        auto: Auto,
        rimborso: RimborsoMese,
        viaggi: List<Viaggio>,
        userName: String
    ) {
        val workbook = XSSFWorkbook(templateInputStream)
        val sheet = workbook.getSheetAt(0)

        // C5 -> Row 4, Cell 2 (0-indexed)
        val rowC5 = sheet.getRow(4) ?: sheet.createRow(4)
        val cellC5 = rowC5.getCell(2) ?: rowC5.createCell(2)
        val infoAuto = if (auto.annoImmatricolazione.isNotEmpty()) "${auto.modello} - ${auto.annoImmatricolazione}" else auto.modello
        cellC5.setCellValue(infoAuto)

        // C6 -> Row 5, Cell 2
        val rowC6 = sheet.getRow(5) ?: sheet.createRow(5)
        val cellC6 = rowC6.getCell(2) ?: rowC6.createCell(2)
        cellC6.setCellValue(auto.combustibile)

        // C7 -> Row 6, Cell 2
        val rowC7 = sheet.getRow(6) ?: sheet.createRow(6)
        val cellC7 = rowC7.getCell(2) ?: rowC7.createCell(2)
        cellC7.setCellValue(auto.targa)

        // A9 -> Row 8, Cell 0
        val rowA9 = sheet.getRow(8) ?: sheet.createRow(8)
        val cellA9 = rowA9.getCell(0) ?: rowA9.createCell(0)
        cellA9.setCellValue(userName)

        // Tabella Viaggi -> Inizio riga 12 (Index 11), Massimo riga 40 (Index 39)
        var currentRowIndex = 11
        val maxRowIndex = 39
        
        for (viaggio in viaggi) {
            if (currentRowIndex > maxRowIndex) break
            
            val row = sheet.getRow(currentRowIndex) ?: sheet.createRow(currentRowIndex)
            
            // Usiamo getCell + setCellValue per non distruggere lo stile (bordi, font) della tabella
            
            // Colonna A (Giorno)
            (row.getCell(0) ?: row.createCell(0)).setCellValue(viaggio.giorno.toDouble())
            
            // Colonna B (Percorso)
            (row.getCell(1) ?: row.createCell(1)).setCellValue("${viaggio.partenza} - ${viaggio.destinazione}")
            
            // Colonna C (Km)
            (row.getCell(2) ?: row.createCell(2)).setCellValue(viaggio.km)
            
            // Colonna D (Motivazione/Causale)
            (row.getCell(3) ?: row.createCell(3)).setCellValue(viaggio.causale)
            
            currentRowIndex++
        }

        workbook.write(outputStream)
        workbook.close()
    }
}
