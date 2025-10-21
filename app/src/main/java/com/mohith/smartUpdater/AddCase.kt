package com.mohith.smartUpdater

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mohith.smartUpdater.ExcelUtil.getPoliceStationNames
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCase(
    navController: NavController
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // --- UI States ---
    var crNo by remember { mutableStateOf("") }
    var scNo by remember { mutableStateOf("") }
    var sectionOfLaw by remember { mutableStateOf("") }
    var dateOfPosting by remember { mutableStateOf("") }
    var presentStage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // --- Police Station States ---
    // Full list of police stations from the Excel file.
    var allPoliceStations by remember { mutableStateOf<Set<String>>(emptySet()) }
    // The text currently typed or selected in the TextField.
    var policeStationInput by remember { mutableStateOf("") }
    // Controls if the dropdown is visible.
    var isDropdownExpanded by remember { mutableStateOf(false) }

    // This derived state will automatically update the filtered list when the user types.
    val filteredPoliceStations by remember(allPoliceStations, policeStationInput) {
        derivedStateOf {
            if (policeStationInput.isEmpty()) {
                allPoliceStations.toList() // Show all if input is empty
            } else {
                allPoliceStations.filter {
                    it.contains(policeStationInput, ignoreCase = true)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        isLoading = true
        getPoliceStationNames(context)
            .onSuccess { names ->
                allPoliceStations = names
            }
            .onFailure { e ->
                Toast.makeText(
                    context,
                    "Error reading police stations: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Case") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(value = crNo, onValueChange = { crNo = it }, label = { Text("CR-No") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = scNo, onValueChange = { scNo = it }, label = { Text("SC-No") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = sectionOfLaw, onValueChange = { sectionOfLaw = it }, label = { Text("Section of Law") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))

                // --- Autocomplete Police Station Dropdown ---
                ExposedDropdownMenuBox(
                    expanded = isDropdownExpanded,
                    onExpandedChange = { isDropdownExpanded = !isDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = policeStationInput,
                        // User can now type in the field
                        onValueChange = {
                            policeStationInput = it
                            // Keep the dropdown open while typing
                            isDropdownExpanded = true
                        },
                        label = { Text("Police Station") },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    // Show dropdown only if there are suggestions and the user is interacting
                    if (filteredPoliceStations.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = isDropdownExpanded,
                            onDismissRequest = { isDropdownExpanded = false }
                        ) {
                            filteredPoliceStations.forEach { station ->
                                DropdownMenuItem(
                                    text = { Text(station) },
                                    onClick = {
                                        policeStationInput = station // Set text field to selection
                                        isDropdownExpanded = false   // Close dropdown
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = dateOfPosting, onValueChange = { dateOfPosting = it }, label = { Text("Date of Posting") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = presentStage, onValueChange = { presentStage = it }, label = { Text("Present Stage") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        // The `policeStationInput` is now the source of truth
                        val caseDetails = CaseDetails(crNo, scNo, sectionOfLaw, policeStationInput.trim(), dateOfPosting, presentStage)
                        isLoading = true
                        coroutineScope.launch {
                            ExcelUtil.addCase(context, caseDetails)
                                .onSuccess {
                                    Toast.makeText(context, "Case Added Successfully!", Toast.LENGTH_SHORT).show()
                                    // Reset all fields
                                    crNo = ""; scNo = ""; sectionOfLaw = ""; policeStationInput = ""; dateOfPosting = ""; presentStage = ""
                                }
                                .onFailure {
                                    Toast.makeText(context, "Failed to add case: ${it.message}", Toast.LENGTH_LONG).show()
                                }
                            isLoading = false
                        }
                    },
                    enabled = !isLoading && policeStationInput.isNotBlank() && crNo.isNotBlank() && scNo.isNotBlank() && sectionOfLaw.isNotBlank()
                ) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    else Text("Add Case")
                }
            }
        }
    }
}
