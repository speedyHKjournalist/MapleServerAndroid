package com.mapleserver.ui.theme

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.mapleserver.FileViewModel
import java.io.File
import java.io.FileOutputStream


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerConfigScreen(context: Context, navController: NavHostController) {
    val screenHeight = LocalConfiguration.current.screenHeightDp
    val textFieldHeight = (screenHeight * 0.8).dp
    val fileModel: FileViewModel = viewModel()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        TextField(
            value = fileModel.fileContent.value,
            onValueChange = { newText ->
                fileModel.fileContent.value = newText
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(textFieldHeight)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = {
                    saveToSharedStorage(context, "config.yaml", fileModel.fileContent.value)
                    navController.popBackStack()
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Save")
            }
            Button(
                onClick = {
                    navController.popBackStack()
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
        }
    }
}

fun saveToSharedStorage(context: Context, fileName: String, content: String) {
    val outputStream = FileOutputStream(File(context.dataDir, fileName))
    val bytes = content.toByteArray()
    outputStream.write(bytes)
    outputStream.close()
}