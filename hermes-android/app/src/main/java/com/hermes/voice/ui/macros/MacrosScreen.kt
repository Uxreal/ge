package com.hermes.voice.ui.macros

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hermes.voice.data.Macro

/**
 * Voice shortcuts: a spoken trigger phrase that expands into a longer prompt,
 * so a routine instruction is three words instead of thirty.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MacrosScreen(
    macros: List<Macro>,
    onSave: (List<Macro>) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voice shortcuts") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onSave(macros + Macro(phrase = "", prompt = "")) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add") },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    text = "Say the phrase and Hermes receives the full prompt instead. " +
                        "Matching is fuzzy, so near-misses from the recognizer still fire.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }

            items(macros, key = { it.id }) { macro ->
                MacroCard(
                    macro = macro,
                    onChange = { updated ->
                        onSave(macros.map { if (it.id == updated.id) updated else it })
                    },
                    onDelete = { onSave(macros.filterNot { it.id == macro.id }) },
                )
            }
        }
    }
}

@Composable
private fun MacroCard(macro: Macro, onChange: (Macro) -> Unit, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = macro.phrase,
                onValueChange = { onChange(macro.copy(phrase = it)) },
                label = { Text("When I say") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = macro.prompt,
                onValueChange = { onChange(macro.copy(prompt = it)) },
                label = { Text("Send this prompt") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = macro.enabled,
                    onCheckedChange = { onChange(macro.copy(enabled = it)) },
                )
                Text(
                    text = if (macro.enabled) "Enabled" else "Disabled",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete shortcut")
                }
            }
        }
    }
}
