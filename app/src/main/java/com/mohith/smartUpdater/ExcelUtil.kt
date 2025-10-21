package com.mohith.smartUpdater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.add
import androidx.core.content.FileProvider
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class CaseDetails(
    val crNo: String,
    val scNo: String,
    val sectionOfLaw: String,
    val policeStation: String,
    val dateOfPosting: String,
    val presentStage: String
)

data class Case(
    val serialNo: String,
    val dateOfDisposal: String = "",
    val presentStageOfTheCase: String = ""
)

object ExcelUtil {
    var totalCases = 0;
    var addedCases = 0;
    var removedCases = 0;

    suspend fun setTotalCases(context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val fileName = "Copy of ${getName()} of Pocso Court MCR.xlsx"
            val file = File(context.filesDir,fileName)

            val workbook = FileInputStream(file).use { input ->
                XSSFWorkbook(input)
            }
            val sheet = workbook.getSheetAt(0);

            for(i in 2..sheet.lastRowNum) {
                try {
                    sheet.getRow(i).getCell(0).numericCellValue
                } catch (_: Exception) {
                    // Try Again!
                    break;
                }
                totalCases++
                Log.d("TotalCases",totalCases.toString())
            }
        } catch (e : Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error sharing file: ${e.message} Try Again", Toast.LENGTH_LONG).show()
        }
        return@withContext Result.success(Unit)
    }

    suspend fun getPoliceStationNames(context: Context): Result<Set<String>> =
        withContext(Dispatchers.IO) {
            val policeStations = mutableSetOf<String>()
            try {
                val fileName = "Copy of ${getName()} of Pocso Court MCR.xlsx"
                val file = File(context.filesDir, fileName)
                val inputStream = FileInputStream(file)
                val workbook = WorkbookFactory.create(inputStream)
                val sheet = workbook.getSheetAt(0);
                for (i in 2..totalCases) {
                    val row = sheet.getRow(i)
                    val policeStation = row.getCell(4).stringCellValue.trim()
                    policeStations.add(policeStation)
                }
                policeStations.sorted()
                inputStream.close()
                workbook.close()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Try Again: ${e.message}", Toast.LENGTH_LONG).show()
            }
            return@withContext Result.success(policeStations)
        }

    suspend fun addCase(context: Context,case: CaseDetails): Result<Unit> =
        withContext(Dispatchers.IO) {
            // now I need to again add that case at a particular index!

            val fileName = "Copy of ${getName()} of Pocso Court MCR.xlsx"
            val file = File(context.filesDir,fileName)
            // read the contents from the file
            val workbook = FileInputStream(file).use { input ->
                XSSFWorkbook(input)
            }
            val sheet = workbook.getSheetAt(0);
            var toAddIdx = 2;
            var newPoliceStation = true;
            for(i in 2..totalCases+2+addedCases-removedCases) {
                val policeStation = sheet.getRow(i).getCell(4).stringCellValue.trim()
                if(policeStation == case.policeStation) {
                    toAddIdx = i;
                    newPoliceStation = false;
                    break;
                }
            }
            if(!newPoliceStation) toAddIdx++;
            sheet.shiftRows(toAddIdx,sheet.lastRowNum,1);
            val newRow = sheet.createRow(toAddIdx)
            val styleSourceRow = sheet.getRow(2)
            fun createAndStyleCell(colIndex: Int, value: String) {
                val newCell = newRow.createCell(colIndex)
                newCell.setCellValue(value)
                styleSourceRow?.getCell(colIndex)?.cellStyle?.let { style ->
                    newCell.cellStyle = style
                }
            }
            createAndStyleCell(0, "")
            createAndStyleCell(1, case.crNo)
            createAndStyleCell(2, case.scNo)
            createAndStyleCell(3, case.sectionOfLaw)
            createAndStyleCell(4, case.policeStation)
            createAndStyleCell(5, case.dateOfPosting)
            createAndStyleCell(6, case.presentStage)

            if(removedCases > 1) {
                // check the first ABSTRACT!
                // and set the data
                var idx = -1;
                for(i in 2..sheet.lastRowNum) {
                    val row = sheet.getRow(i)
                    val data = row.getCell(4)
                    if(data != null && data.toString().trim() == "ABSTRACT") {
                        idx = i
                        break
                    }
                }
                // Now update the data in the row
                if(idx != -1) {
                    for(i in idx+2..idx+4) {
                        val row = sheet.getRow(i)
                        val cell = row.getCell(5)
                        when (i) {
                            idx+2 -> {
                                cell.setCellValue(addedCases.toString())
                            }
                            idx+3 -> {
                                cell.setCellValue(removedCases.toString())
                            }
                            else -> {
                                cell.setCellValue((totalCases+addedCases-removedCases).toString())
                            }
                        }
                    }
                }
            }


            FileOutputStream(file).use { outputStream ->
                workbook.write(outputStream)
                outputStream.flush()
            }
            workbook.close()
            addedCases++;
            return@withContext Result.success(Unit)
        }
    suspend fun removeCase(context: Context, case: Case): Result<Unit> =
        withContext(Dispatchers.IO) {
            val fileName = "Copy of ${getName()} of Pocso Court MCR.xlsx"
            val file = File(context.filesDir,fileName)
            val workbook = FileInputStream(file).use { input ->
                XSSFWorkbook(input)
            }
            val sheet = workbook.getSheetAt(0);
            var rowToRemoveIndex = -1;
            Log.d("totalCases",totalCases.toString())
            val oldRowData = mutableListOf<String>()
            for(i in 2..totalCases+2+addedCases-removedCases) {
                val row = sheet.getRow(i)
                val dataInCell = row.getCell(0, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)
                if (dataInCell != null && dataInCell.cellType == CellType.NUMERIC) {
                    if (dataInCell.numericCellValue.toInt().toString() == case.serialNo) {
                        Log.d("Value : ",dataInCell.numericCellValue.toInt().toString())
                        rowToRemoveIndex = i
                        for (cellIndex in 0..6) { // Loop through columns 0 to 6
                            val cellValue = row.getCell(cellIndex)?.toString()?.trim() ?: ""
                            oldRowData.add(cellValue)
                        }
                        break
                    }
                }
            }
            if(rowToRemoveIndex == -1) {
                return@withContext Result.failure(Exception("Case not found"))
            } else {
                sheet.removeRow(sheet.getRow(rowToRemoveIndex))
                removedCases++;
                sheet.shiftRows(rowToRemoveIndex+1,sheet.lastRowNum,-1);
            }
            FileOutputStream(file).use { outputStream ->
                workbook.write(outputStream)
                outputStream.flush()
            }
            workbook.close()
            if(oldRowData.isNotEmpty()) addRemovedCase(context,oldRowData,case)
            return@withContext Result.success(Unit)
        }

    private suspend fun addRemovedCase(context: Context, oldRowData: List<String>, case: Case) =
        withContext(Dispatchers.IO) {
            val fileName = "Copy of ${getName()} of Pocso Court MCR.xlsx"
            val file = File(context.filesDir,fileName)
            val workbook = FileInputStream(file).use { input ->
                XSSFWorkbook(input)
            }
            Log.d("Removed Cases:", removedCases.toString())
            var toAddIdx = totalCases+2+addedCases-removedCases;
            val sheet = workbook.getSheetAt(0);
            val headerData = "CASE DISPOSED FOR THE MONTH OF ${getDate()}";
            if(removedCases == 1) {
                sheet.shiftRows(toAddIdx, sheet.lastRowNum, 1);
                val mainHeaderRow = sheet.createRow(toAddIdx);
                val mainHeaderCell = mainHeaderRow.createCell(0);
                mainHeaderCell.setCellValue(headerData)
                val titleStyle = sheet.getRow(0)?.getCell(0)?.cellStyle
                if (titleStyle != null) {
                    mainHeaderCell.cellStyle = titleStyle
                }
                sheet.addMergedRegion(CellRangeAddress(toAddIdx, toAddIdx, 0, 6));
                toAddIdx++;
                sheet.shiftRows(toAddIdx,sheet.lastRowNum,1);
                val subHeaderRow = sheet.createRow(toAddIdx);
                val headerTitles = listOf("SL-No.", "CR-No.", "SC-No.","Section of Law", "Police Station", "Date of Disposal", "Present Stage of the Case");
                val styleSourceRow = sheet.getRow(1)
                headerTitles.forEachIndexed { index, title ->
                    val cell = subHeaderRow.createCell(index);
                    cell.setCellValue(title);
                    styleSourceRow?.getCell(index)?.cellStyle?.let {
                        cell.cellStyle = it
                    };
                }
                sheet.shiftRows(toAddIdx + 1, sheet.lastRowNum, 1)
                var abstractRow = sheet.createRow(toAddIdx + 1)
                val boldFont = workbook.createFont().apply {
                    this.bold = true
                }
                val boldStyle = workbook.createCellStyle().apply {
                    setFont(boldFont)
                    this.alignment = HorizontalAlignment.CENTER
                    this.verticalAlignment = VerticalAlignment.CENTER
                }
                sheet.addMergedRegion(CellRangeAddress(toAddIdx + 1, toAddIdx + 1, 4, 5))
                abstractRow.createCell(4).apply {
                    setCellValue("ABSTRACT")
                    cellStyle = boldStyle
                }
                // total pt cases!
                sheet.shiftRows(toAddIdx + 2, sheet.lastRowNum, 1)
                abstractRow = sheet.createRow(toAddIdx + 2)
                abstractRow.createCell(4).apply {
                    setCellValue("Total PT cases:")
                    cellStyle = boldStyle
                }
                abstractRow.createCell(5).apply {
                    setCellValue(totalCases.toString())
                    cellStyle = boldStyle
                }
                // Received SC nos:
                sheet.shiftRows(toAddIdx + 3, sheet.lastRowNum, 1)
                abstractRow = sheet.createRow(toAddIdx + 3)
                abstractRow.createCell(4).apply {
                    setCellValue("Received SC nos:")
                    cellStyle = boldStyle
                }
                abstractRow.createCell(5).apply {
                    setCellValue(addedCases.toString())
                    cellStyle = boldStyle
                }
                // Disposal Cases
                sheet.shiftRows(toAddIdx + 4, sheet.lastRowNum, 1)
                abstractRow = sheet.createRow(toAddIdx + 4)
                abstractRow.createCell(4).apply {
                    setCellValue("Disposal Cases:")
                    cellStyle = boldStyle
                }
                abstractRow.createCell(5).apply {
                    setCellValue(removedCases.toString())
                    cellStyle = boldStyle
                }
                // Total Cases
                sheet.shiftRows(toAddIdx + 5, sheet.lastRowNum, 1)
                abstractRow = sheet.createRow(toAddIdx + 5)
                abstractRow.createCell(4).apply {
                    setCellValue("Total cases:")
                    cellStyle = boldStyle
                }
                abstractRow.createCell(5).apply {
                    setCellValue((totalCases+addedCases-removedCases).toString())
                    cellStyle = boldStyle
                }
            }
            if(removedCases > 1) {
                // check the first ABSTRACT!
                // and set the data
                // and also set the serial numbers accordingly!
                var idx = -1;
                for(i in 2..sheet.lastRowNum) {
                    val row = sheet.getRow(i)
                    val data = row.getCell(4)
                    if(data != null && data.toString().trim() == "ABSTRACT") {
                        idx = i
                        break
                    }
                }
                // Now update the data in the row
                if(idx != -1) {
                    for(i in idx+2..idx+4) {
                        val row = sheet.getRow(i)
                        val cell = row.getCell(5)
                        when (i) {
                            idx+2 -> {
                                cell.setCellValue(addedCases.toString())
                            }
                            idx+3 -> {
                                cell.setCellValue(removedCases.toString())
                            }
                            else -> {
                                cell.setCellValue((totalCases+addedCases-removedCases).toString())
                            }
                        }
                    }
                }
            }
            toAddIdx = totalCases+4+addedCases-removedCases;
            sheet.shiftRows(toAddIdx, sheet.lastRowNum, 1)
            val newRow = sheet.createRow(toAddIdx)
            val styleSourceRow = sheet.getRow(2)
            fun createAndStyleCell(colIndex: Int, value: String?) {
                val newCell = newRow.createCell(colIndex)
                newCell.setCellValue(value ?: "")
                styleSourceRow?.getCell(colIndex)?.cellStyle?.let { style ->
                    newCell.cellStyle = style
                }
            }

            Log.d("Value In that Row:", oldRowData[1])
            createAndStyleCell(0, removedCases.toString())
            createAndStyleCell(1, oldRowData[1])
            createAndStyleCell(2, oldRowData[2])
            createAndStyleCell(3, oldRowData[3])
            createAndStyleCell(4, oldRowData[4])
            createAndStyleCell(5, case.dateOfDisposal)
            // Use ifBlank for a concise fallback
            val presentStage = case.presentStageOfTheCase.ifBlank { oldRowData[6] }
            createAndStyleCell(6, presentStage)


            // update the serial numbers!
            if(removedCases > 1) {
                var startIdx = -1;
                for(i in 2..sheet.lastRowNum) {
                    val row = sheet.getRow(i)
                    val data = row.getCell(0);
                    if(data != null && data.toString() == "CASE DISPOSED FOR THE MONTH OF ${getDate()}") {
                        startIdx = i+2;
                        break;
                    }
                }
                for(i in 1..removedCases) {
                    val row = sheet.getRow(startIdx+i-1)
                    val cell = row.getCell(0)
                    cell.setCellValue(i.toString())
                }
            }

            FileOutputStream(file).use { outputStream ->
                workbook.write(outputStream)
                outputStream.flush()
            }
            workbook.close()
            return@withContext
        }

    fun getDate(): String {
        val now = LocalDate.now()
        val prev = now.minusMonths(0)
        val fmt = DateTimeFormatter.ofPattern("MMMM-yyyy", java.util.Locale.ENGLISH)
        return prev.format(fmt).uppercase()
    }

    fun modifyCase(
        context: Context,
        case: Case
    ): Result<Unit> {
        val fileName = "Copy of ${getName()} of Pocso Court MCR.xlsx"
        val file = File(context.filesDir, fileName)

        try {
            val workbook = FileInputStream(file).use { input ->
                XSSFWorkbook(input)
            }
            val sheet = workbook.getSheetAt(0)
            var rowToModifyIndex = -1

            for (i in 2..sheet.lastRowNum) {
                val row = sheet.getRow(i) ?: continue
                val cell = row.getCell(0, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)
                if (cell != null && cell.cellType == CellType.NUMERIC) {
                    if (cell.numericCellValue.toInt().toString() == case.serialNo) {
                        rowToModifyIndex = i
                        break
                    }
                }
            }

            if (rowToModifyIndex == -1) {
                workbook.close()
                return Result.failure(Exception("Case with S.No ${case.serialNo} not found."))
            }
            val row = sheet.getRow(rowToModifyIndex)
            if (row != null) {
                val dateCell = row.getCell(5, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK)
                if (case.dateOfDisposal.isNotBlank()) {
                    dateCell.setCellValue(case.dateOfDisposal)
                }
                // Update Present Stage (Column 6)
                val stageCell = row.getCell(6, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK)
                // Use new stage if not blank, otherwise keep the old value
                if (case.presentStageOfTheCase.isNotBlank()) {
                    stageCell.setCellValue(case.presentStageOfTheCase)
                }
            }

            // 3. Write the changes back to the file
            FileOutputStream(file).use { outputStream ->
                workbook.write(outputStream)
                outputStream.flush()
            }
            workbook.close()
            return Result.success(Unit)

        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure(e)
        }
    }
    fun ok(
        context: Context
    ) {
        try {
            val fileName = "Copy of ${getName()} of Pocso Court MCR.xlsx"
            val file = File(context.filesDir, fileName)

            val workbook = FileInputStream(file).use { input ->
                XSSFWorkbook(input)
            }
            val sheet = workbook.getSheetAt(0);
            sheet.getRow(0).getCell(0).setCellValue("POCSO COURT MCR FOR THE MONTH OF ${getDate()}")
            for (i in 2..totalCases + 1 + addedCases - removedCases) {
                val row = sheet.getRow(i)
                row.getCell(0).setCellValue((i - 1).toString())
            }
            FileOutputStream(file).use { outputStream ->
                workbook.write(outputStream)
                outputStream.flush()
            }
            workbook.close()
            val fileUri: Uri = FileProvider.getUriForFile(
                context,
                "com.mohith.smartUpdater.provider", // This must match your AndroidManifest.xml
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                // Specifically target WhatsApp
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            // Use a chooser as a fallback in case WhatsApp is not installed
            val chooser = Intent.createChooser(shareIntent, "Share Excel File Via")
            // Check if WhatsApp is installed before trying to launch it directly
            if (shareIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(shareIntent)
            } else {
                Toast.makeText(context, "WhatsApp is not installed.", Toast.LENGTH_SHORT).show()
                // Optionally, launch the general chooser so the user can pick another app
                 context.startActivity(chooser)
            }
        } catch (e : Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error sharing file: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}