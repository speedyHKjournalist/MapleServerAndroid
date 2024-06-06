package com.mapleserver

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

@Composable
fun ImportCompose(context: Context, navController: NavHostController, importViewModel: ImportViewModel, serverParameter: ServerParameter) {
    val updateCompose by importViewModel.shouldRecompose.collectAsState()
    var showAlertDialog by remember { mutableStateOf(false) }
    var selectedDbFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedConfigFileUri by remember { mutableStateOf<Uri?>(null) }
    var dbFileName by remember { mutableStateOf(TextFieldValue()) }
    var configFileName by remember { mutableStateOf(TextFieldValue()) }

    val dbLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        selectedDbFileUri = uri
        dbFileName = TextFieldValue(getFileNameFromUri(context, uri) ?: "")
        if (dbFileName.text.isNotEmpty()) {
            if (dbFileName.text != "cosmic") {
                showAlertDialog = true
            } else {
                importDatabase(context, uri)
                importViewModel.updateComposeState(true)
            }
        }
    }

    val configLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        selectedConfigFileUri = uri
        configFileName = TextFieldValue(getFileNameFromUri(context, uri) ?: "")
        if (configFileName.text.isNotEmpty()) {
            if (configFileName.text != "config.yaml") {
                showAlertDialog = true
            } else {
                importConfigYaml(context, uri)
                importViewModel.updateComposeState(true)
                //reload
                serverParameter.serverConfig = serverParameter.yamlMapper.readValue(File(context.filesDir, "config.yaml"), ServerConfig::class.java)
            }
        }
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Row {
            BackButton(navController)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(modifier = Modifier.weight(1f)) {
                ImportDatabaseButton(dbLauncher)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                ImportConfigYAMLButton(configLauncher)
            }
        }
        if (updateCompose) {
            selectedDbFileUri = null
            selectedConfigFileUri = null
            dbFileName = TextFieldValue()
            configFileName = TextFieldValue()
        }

        if (showAlertDialog) {
            AlertDialog(
                onDismissRequest = { showAlertDialog = false },
                title = { Text("Invalid File Name") },
                text = { Text("The selected file name must be 'cosmic' or 'config.yaml'. Please select the correct file.") },
                confirmButton = {
                    Button(
                        onClick = { showAlertDialog = false }
                    ) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

@Composable
fun ImportDatabaseButton(launcher: ManagedActivityResultLauncher<String, Uri?>) {
    Button(
        onClick = { launcher.launch("*/*") }) {
        Text("Select DB File")
    }
}

@Composable
fun ImportConfigYAMLButton(launcher: ManagedActivityResultLauncher<String, Uri?>) {
    Button(
        onClick = {
            launcher.launch("*/*")
        }) {
        Text("Select Config File")
    }
}

fun importDatabase(context: Context, uri: Uri?) {
    if (uri == null) {
        return
    }

    val contentResolver: ContentResolver = context.contentResolver
    val inputStream: InputStream? = contentResolver.openInputStream(uri)
    if (inputStream == null) {
        showToast(context, "Failed to open input stream")
        return
    }

    try {
        val destinationFile = File(context.getDatabasePath("cosmic").absolutePath)
        val outputStream = FileOutputStream(destinationFile)
        val buffer = ByteArray(1024)
        var length: Int

        while (inputStream.read(buffer).also { length = it } > 0) {
            outputStream.write(buffer, 0, length)
        }
        outputStream.close()
        showToast(context, "Import Database cosmic succeed !")
    } catch (e: IOException) {
        showToast(context, "Import Database cosmic failed !")
        e.printStackTrace()
    } finally {
        inputStream.close()
    }
}

fun importConfigYaml(context: Context, uri: Uri?) {
    if (uri == null) {
        return
    }

    val contentResolver: ContentResolver = context.contentResolver
    val inputStream: InputStream? = contentResolver.openInputStream(uri)
    if (inputStream == null) {
        showToast(context, "Failed to open input stream")
        return
    }

    try {
        val destinationFile = File(context.filesDir, "config.yaml").absolutePath
        val outputStream = FileOutputStream(destinationFile)
        val buffer = ByteArray(1024)
        var length: Int

        while (inputStream.read(buffer).also { length = it } > 0) {
            outputStream.write(buffer, 0, length)
        }
        outputStream.close()
        showToast(context, "Import server config.yaml succeed !")
    } catch (e: IOException) {
        showToast(context, "Import server config.yaml failed !")
        e.printStackTrace()
    } finally {
        inputStream.close()
    }
}

private fun getFileNameFromUri(context: Context, uri: Uri?): String? {
    val documentFile = uri?.let { DocumentFile.fromSingleUri(context, it) }
    return documentFile?.name
}
