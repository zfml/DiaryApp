package com.example.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight

@Composable
fun DisplayAlertDialog(
    title: String,
    message: String,
    dialogOpened: Boolean,
    onClosedDialog: () -> Unit,
    onYesClicked: () -> Unit
) {

    if(dialogOpened) {
        AlertDialog(
            title = {
                Text(
                    text = title,
                    fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = message,
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                )
            },
            onDismissRequest = onClosedDialog,
            dismissButton = {
                 OutlinedButton(onClick = onClosedDialog) {
                     Text(
                         text = "No"
                     )
                 }           
            },
            confirmButton = {
                Button(onClick = {
                    onYesClicked()
                    onClosedDialog()
                }) {
                    Text(
                        text = "Yes"
                    )
                }

            }
        )

    }


}