package com.mapleserver

import android.content.Context
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
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
fun ExportCompose(context: Context, navController: NavHostController) {
    val dbFile = context.getDatabasePath("cosmic")
    val configYamlFile = File(context.filesDir, "config.yaml")

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
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    exportFileToDownload(context, dbFile)
                    navController.navigate("main_screen")
                }
            ) {
                Text("Export Database")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    exportFileToDownload(context, configYamlFile)
                    navController.navigate("main_screen")
                }
            ) {
                Text("Export Config")
            }
        }
    }
}

@Composable
fun BackButton(navController: NavHostController) {
    IconButton(
        onClick = {
            navController.navigate("main_screen")
        }
    ) {
        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back Button")
    }
}

fun exportFileToDownload(context: Context, exportFileName: File) {
    if (exportFileName.exists()) {
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

            showToast(context, "File has been exported successfully to /sdcard/Download !")
        } catch (e: IOException) {
            showToast(context, "File export failed !")
            e.printStackTrace()
        }
    } else {
        showToast(context, "File cosmic not found in app folder!")
    }
}

fun showToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}