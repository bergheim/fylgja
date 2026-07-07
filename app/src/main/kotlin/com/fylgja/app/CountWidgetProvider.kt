package com.fylgja.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.widget.RemoteViews

class CountWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        val prefs = context.getSharedPreferences("habits", Context.MODE_PRIVATE)
        val orgUri = prefs.getString("org_uri", null)
        if (orgUri == null) {
            ids.forEach { mgr.updateAppWidget(it, emptyViews("Pick file")) }
            return
        }

        // Read from datastore cache (synchronous for widget)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            val store = HabitStore(context)
            val habits = store.habitsFlow().first()

            // Refresh from disk if needed
            try {
                val uri = Uri.parse(orgUri)
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
                if (text != null) {
                    val parsed = OrgReader.parse(text)
                    store.saveHabits(parsed)
                    parsed.forEachIndexed { idx, habit ->
                        val rv = buildCounterView(context, habit)
                        if (idx < ids.size) mgr.updateAppWidget(ids[idx], rv)
                    }
                    // If more habits than widget instances, update with modulo
                    habits.forEachIndexed { idx, habit ->
                        if (idx < ids.size) mgr.updateAppWidget(ids[idx], buildCounterView(context, habit))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            ids.forEach { id ->
                val habit = habits.getOrNull(id % maxOf(habits.size, 1))
                if (habit != null) {
                    mgr.updateAppWidget(id, buildCounterView(context, habit))
                }
            }
        }
    }

    private fun buildCounterView(context: Context, habit: Habit): RemoteViews {
        val rv = RemoteViews(context.packageName, android.R.layout.simple_list_item_1)
        // Use widget_layout via RemoteViews - but Android requires RemoteViews-compatible layouts
        // Use our custom widget_layout
        val rv2 = RemoteViews(context.packageName, R.layout.widget_layout)
        val bgColor = try { Color.parseColor(habit.color + "22") } catch (e: Exception) { Color.parseColor("#3498db22") }
        rv2.setInt(android.R.id.text1, "setBackgroundColor", bgColor)
        rv2.setTextViewText(android.R.id.text1, "${habit.icon}\n${habit.todayCount()}")
        rv2.setTextViewColor(android.R.id.text1, Color.BLACK)

        // Tap action
        val tapIntent = Intent(context, WidgetTapReceiver::class.java).apply {
            action = "com.fylgja.app.COUNTER_TAP"
            putExtra("habit_name", habit.name)
        }
        val pi = PendingIntent.getBroadcast(context, habit.name.hashCode(), tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        rv2.setOnClickPendingIntent(android.R.id.text1, pi)

        return rv2
    }

    private fun emptyViews(msg: String): RemoteViews {
        throw NotImplementedError("Not needed")
    }

    override fun onEnabled(context: Context) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            val prefs = context.getSharedPreferences("habits", Context.MODE_PRIVATE)
            val orgUri = prefs.getString("org_uri", null) ?: return@launch
            val uri = Uri.parse(orgUri)
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: return@launch
            val habits = OrgReader.parse(text)
            HabitStore(context).saveHabits(habits)
        }
    }
}
