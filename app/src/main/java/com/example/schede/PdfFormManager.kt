package com.example.schede

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDAcroForm
import java.io.IOException

object PdfFormManager {

    fun init(context: Context) {
        PDFBoxResourceLoader.init(context.applicationContext)
    }

    /**
     * Extracts the names of all form fields from a given PDF.
     * @param context The application context.
     * @param pdfUri The URI of the input PDF file.
     * @return A list of form field names.
     */
    fun getFormFields(context: Context, pdfUri: Uri): List<String> {
        val fieldNames = mutableListOf<String>()
        try {
            context.contentResolver.openInputStream(pdfUri)?.use { inputStream ->
                val document = PDDocument.load(inputStream)
                val acroForm: PDAcroForm? = document.documentCatalog.acroForm

                acroForm?.fields?.forEach { field ->
                    fieldNames.add(field.fullyQualifiedName)
                }
                document.close()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return fieldNames
    }

    /**
     * Fills a PDF form with the given values and saves it to a new file.
     * @param context The application context.
     * @param inputPdfUri The URI of the template PDF file.
     * @param outputPdfUri The URI where the filled PDF will be saved.
     * @param fieldValues A map where the key is the field name and the value is the text to fill.
     */
    fun fillForm(
        context: Context,
        inputPdfUri: Uri,
        outputPdfUri: Uri,
        fieldValues: Map<String, String>
    ) {
        try {
            context.contentResolver.openInputStream(inputPdfUri)?.use { inputStream ->
                context.contentResolver.openOutputStream(outputPdfUri)?.use { outputStream ->
                    val document = PDDocument.load(inputStream)
                    val acroForm: PDAcroForm? = document.documentCatalog.acroForm

                    if (acroForm != null) {
                        fieldValues.forEach { (fieldName, value) ->
                            val field = acroForm.getField(fieldName)
                            field?.setValue(value)
                        }
                        acroForm.flatten()
                    }
                    document.save(outputStream)
                    document.close()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
