package com.mapleserver.ui.theme

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun StartButton(
    text: String,
    startMapleServer: () -> Unit,
    isButtonEnabled: Boolean
) {
    Button(
        onClick = {
            startMapleServer() },
        colors = ButtonDefaults.buttonColors(
            contentColor = Color.White
        ),
        enabled = isButtonEnabled,
        modifier = Modifier.padding(16.dp)
    ) {
        Text(text = text)
    }
}