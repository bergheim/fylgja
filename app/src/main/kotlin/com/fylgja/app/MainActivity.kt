package com.fylgja.app

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {

    private val openLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { pickedUri ->
            contentResolver.takePersistableUriPermission(
                pickedUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            // Save URI to preferences
            val prefs = getSharedPreferences("habits", MODE_PRIVATE)
            prefs.edit().putString("org_uri", pickedUri.toString()).apply()

            // Refresh widgets
            refreshAllWidgets()
        }
    }

    private fun refreshAllWidgets() {
        val mgr = AppWidgetManager.getInstance(this)
        listOf(CountWidgetProvider::class.java, GraphWidgetProvider::class.java).forEach { cls ->
            val ids = mgr.getAppWidgetIds(ComponentName(this, cls))
            mgr.notifyAppWidgetUpdate(ids)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                SettingsScreen()
            }
        }
    }

    @Composable
    fun SettingsScreen() {
        val context = LocalContext.current
        val store = remember { HabitStore(context) }
        val prefs = context.getSharedPreferences("habits", MODE_PRIVATE)
        val orgUri = prefs.getString("org_uri", null)
        val habits by store.habitsFlow().collectAsState(initial = emptyList())

        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "Fylgja",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Org file:", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            orgUri ?: "(none selected)",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { openLauncher.launch(arrayOf("text/*", "application/octet-stream", "*/*")) }) {
                            Text(if (orgUri == null) "Pick habits.org" else "Change file")
                        }
                    }
                }

                if (habits.isNotEmpty()) {
                    Text("Habits found:", style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp))
                    LazyColumn {
                        items(habits) { habit ->
                            HabitRow(habit)
                        }
                    }
                } else if (orgUri != null) {
                    Text("Reading org file...", color = Color.Gray)
                    LaunchedEffect(orgUri) {
                        try {
                            val uri = Uri.parse(orgUri)
                            val text = contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
                            if (text != null) {
                                val parsed = OrgReader.parse(text)
                                store.saveHabits(parsed)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun HabitRow(habit: Habit) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(habit.icon, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(12.dp))
            Text(habit.name, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.weight(1f))
            Text(
                "Today: ${habit.todayCount()}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}
