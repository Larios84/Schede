package com.example.schede

import com.tom_roush.pdfbox.pdmodel.PDDocument
import org.junit.Test
import java.io.File

class FieldExtractorTest {
    @Test
    fun listFields() {
        val pdfFile = File("/home/larios/Scrivania/DA MINT/Cartella senza nome/Schede 09-08-26  V1.6/MD-46 Scheda verifica giornaliera cabina AVL IZ.pdf")
        if (!pdfFile.exists()) {
            println("File not found at ${pdfFile.absolutePath}")
            return
        }
        
        try {
            val document = PDDocument.load(pdfFile)
            val acroForm = document.documentCatalog.acroForm
            if (acroForm == null) {
                println("No AcroForm found in the PDF.")
            } else {
                println("--- PDF FIELD LIST ---")
                acroForm.fields.forEach { field ->
                    println("Field: ${field.fullyQualifiedName} (Type: ${field.fieldType})")
                }
                println("--- END OF LIST ---")
            }
            document.close()
        } catch (e: Exception) {
            println("Error: ${e.message}")
            e.printStackTrace()
        }
    }
}
