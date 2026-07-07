package com.fylgja.app

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class WidgetTapReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "com.fylgja.app.COUNTER_TAP" -> {
                val habitName = intent.getStringExtra("habit_name") ?: return
                CoroutineScope(Dispatchers.IO).launch {
                    val prefs = context.getSharedPreferences("habits", Context.MODE_PRIVATE)
                    val orgUriStr = prefs.getString("org_uri", null) ?: return@launch
                    val orgUri = Uri.parse(orgUriStr)

                    try {
                        OrgWriter.appendClockEntry(context.contentResolver, orgUri, habitName)
                        HabitStore(context).saveHabits(
                            OrgReader.parse(
                                context.contentResolver.openInputStream(orgUri)!!.bufferedReader().readText()
                            )
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    val mgr = AppWidgetManager.getInstance(context)
                    val ids = mgr.getAppWidgetIds(ComponentName(context, CountWidgetProvider::class.java))
                    mgr.notifyAppWidgetUpdate(ids)
                }
            }

            "com.fylgja.app.GRAPH_REFRESH" -> {
                val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
                if (widgetId >= 0) {
                    val mgr = AppWidgetManager.getInstance(context)
                    GraphWidgetProvider.buildAndUpdate(context, mgr, widgetId,
                        GraphWidgetProvider(context).daysForId(context, widgetId))
                }
            }
        }
    }
}
