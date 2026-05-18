package com.magpie.magpie.ui.screens.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProfileEditScreen(
    paddingValues: PaddingValues,
    displayName: String,
    bio: String?,
    onSave: (String, String?) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(displayName) }
    var body by remember { mutableStateOf(bio.orEmpty()) }

    Column(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Display name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text("Bio") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { onSave(name.trim(), body.ifBlank { null }) }, modifier = Modifier.fillMaxWidth()) { Text("Save") }
        Button(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
    }
}
