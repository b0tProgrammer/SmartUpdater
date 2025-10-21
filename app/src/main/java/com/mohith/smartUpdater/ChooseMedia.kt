package com.mohith.smartUpdater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.core.content.edit
import androidx.core.net.toUri
import com.mohith.smartUpdater.ExcelUtil.getPoliceStationNames
import kotlinx.coroutines.launch
import org.apache.poi.openxml4j.opc.StreamHelper.copyStream
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun ChooseMedia(
    navController: NavController
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.DarkGray),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            val buttonText = "Choose The Excel File To Open"
            Text(
                text = buttonText,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp),
                textAlign = TextAlign.Center
            )
            val context = LocalContext.current
            val savedFile = File(context.filesDir, "Copy of ${getName()} of Pocso Court MCR.xlsx")
            val scope = rememberCoroutineScope()
            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri: Uri? ->
                uri?.let {
                    if (copyFileFromUri(context, it, savedFile)) {
                        scope.launch {
                            ExcelUtil.setTotalCases(context)
                                .onSuccess { total ->
                                    Log.d("ChooseMedia", "Successfully processed file. Total cases: $total")
                                    Toast.makeText(context, "File processed successfully!", Toast.LENGTH_SHORT).show()
                                }
                                .onFailure { error ->
                                    // This runs on the main thread after failure
                                    Log.e("ChooseMedia", "Error processing file", error)
                                    Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_LONG).show()
                                }
                        }
                        Log.d("ChooseMedia", "File copied to ${savedFile.absolutePath}")
                        // Navigate immediately after starting the copy
                        navController.navigate("options")
                    } else {
                        Toast.makeText(context, "Failed to copy file.", Toast.LENGTH_LONG).show()
                    }
                    Log.d("ChooseMedia", "File copied to ${savedFile.absolutePath}")
                }
            }
            Button(
                modifier = Modifier.size(250.dp),
                onClick = {
                    // --- The onClick now ONLY launches the file picker ---
                    launcher.launch(
                        arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.upload_icon),
                    contentDescription = "Upload File",
                    modifier = Modifier.size(150.dp),
                    tint = Color.LightGray
                )
            }
        }
    }
}

fun copyFileFromUri(context: Context, sourceUri: Uri, outputFile: File): Boolean {
    return try {
        context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
            FileOutputStream(outputFile).use { outputStream ->
                copyStream(inputStream, outputStream)
            }
        }
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

fun getName() : String {
    val now = LocalDate.now()
    val fmt = DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale.ENGLISH)
    return now.format(fmt).uppercase()
}