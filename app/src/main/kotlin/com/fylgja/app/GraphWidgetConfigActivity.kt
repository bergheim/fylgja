package com.fylgja.app

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class GraphWidgetConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val widgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        setContent {
            var days by remember { mutableStateOf(7) }

            MaterialTheme(colorScheme = lightColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                    Column {
                        Text("Graph Widget — days shown:")
                        Spacer(Modifier.height(8.dp))
                        Row {
                            Button(onClick = { if (days > 1) days-- }) { Text("-") }
                            Text("$days days", modifier = Modifier.padding(horizontal = 16.dp))
                            Button(onClick = { if (days < 90) days++ }) { Text("+") }
                        }
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = {
                            val prefs = getSharedPreferences("widgets", MODE_PRIVATE)
                            prefs.edit().putInt("graph_days_$widgetId", days).apply()
                            val mgr = AppWidgetManager.getInstance(this@GraphWidgetConfigActivity)
                            GraphWidgetProvider.buildAndUpdate(
                                this@GraphWidgetConfigActivity, mgr, widgetId, days
                            )
                            setResult(Activity.RESULT_OK, Intent().putExtra(
                                AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
                            finish()
                        }) { Text("Done") }
                    }
                }
            }
        }
    }
}
