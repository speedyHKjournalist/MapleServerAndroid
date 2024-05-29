package com.mapleserver

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
fun ImportDBCompose(context: Context, navController: NavHostController) {
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf(TextFieldValue()) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        selectedFileUri = uri
        fileName = TextFieldValue(getFileNameFromUri(context, uri) ?: "")
    }
    Column() {
        IconButton(
            onClick = {
                navController.navigate("main_screen")
            }
        ) {
            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back Button")
        }
        Column(
            modifier = Modifier
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = { launcher.launch("*/*") }) {
                        Text("Select Database File")
                    }
                    if (selectedFileUri != null) {
                        Text("Selected File: ${fileName.text}")
                        Button(
                            onClick = {
                                if (selectedFileUri != null) {
                                    importDatabase(context, selectedFileUri!!)
                                    navController.popBackStack()
                                }
                            }
                        ) {
                            Text("Import Database")
                        }
                    }
                }
            }
        }
    }
}

fun importDatabase(context: Context, uri: Uri) {
    val contentResolver: ContentResolver = context.contentResolver
    val inputStream: InputStream = contentResolver.openInputStream(uri) ?: return
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

private fun getFileNameFromUri(context: Context, uri: Uri?): String? {
    val documentFile = uri?.let { DocumentFile.fromSingleUri(context, it) }
    return documentFile?.name
}
