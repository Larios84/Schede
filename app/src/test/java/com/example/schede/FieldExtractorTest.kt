package com.example.schede

import com.tom_roush.pdfbox.pdmodel.PDDocument
import org.junit.Test
import java.io.File

class FieldExtractorTest {
    @Test
    fun listFields() {
        val pdfFile = File("src/main/assets/RIMBORSO GL.pdf")
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
                println("--- PDF FIELD LIST FOR RIMBORSO GL ---")
                acroForm.fields.forEach { field ->
                    println("Field: '${field.fullyQualifiedName}' (Type: ${field.fieldType})")
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
