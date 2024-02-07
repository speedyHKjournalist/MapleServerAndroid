package com.mapleserver

import android.content.Context
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

@Composable
fun ExportDBCompose(context: Context, navController: NavHostController) {
    var exportFileName by remember { mutableStateOf(context.getDatabasePath("cosmic")) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        IconButton(
            onClick = {
                navController.popBackStack()
            }
        ) {
            Icon(imageVector = Icons.Default.Menu, contentDescription = "Drawer Toggle Button")
        }
        Button(
            onClick = {
                exportFileToDownload(context, exportFileName)
                navController.popBackStack()
            },
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Export Database")
            }
        }
    }
}

fun exportFileToDownload(context: Context, exportFileName: File) {
    if(exportFileName.exists()) {
        val sourcePath = exportFileName.absolutePath
        val destinationDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val destinationPath = File(destinationDirectory, exportFileName.name)

        try {
            val inputStream = FileInputStream(sourcePath)
            val outputStream = FileOutputStream(destinationPath)

            val buffer = ByteArray(1024)
            var length: Int
            while (inputStream.read(buffer).also { length = it } > 0) {
                outputStream.write(buffer, 0, length)
            }

            inputStream.close()
            outputStream.close()

            showToast(context, "Database cosmic has been exported successfully to /sdcard/Download !")
        } catch (e: IOException) {
            showToast(context, "Database export failed !")
            e.printStackTrace()
        }
    } else {
        showToast(context, "Database cosmic not found in app !")
    }
}
fun showToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}