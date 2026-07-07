package com.fylgja.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.widget.RemoteViews
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first

class GraphWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        CoroutineScope(Dispatchers.IO).launch {
            val prefs = context.getSharedPreferences("habits", Context.MODE_PRIVATE)
            val orgUri = prefs.getString("org_uri", null) ?: return@launch
            val uri = Uri.parse(orgUri)
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: return@launch
            val habits = OrgReader.parse(text)
            HabitStore(context).saveHabits(habits)
            ids.forEach { id ->
                buildAndUpdate(context, mgr, id, daysForId(context, id))
            }
        }
    }

    fun daysForId(context: Context, id: Int): Int {
        val prefs = context.getSharedPreferences("widgets", Context.MODE_PRIVATE)
        return prefs.getInt("graph_days_$id", 7)
    }

    companion object {
        fun buildAndUpdate(context: Context, mgr: AppWidgetManager, id: Int, days: Int) {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                val habits = HabitStore(context).habitsFlow().first()
                if (habits.isEmpty()) return@launch
                val habit = habits[0] // First habit by default
                val data = habit.lastNDaysCounts(days)
                val maxVal = maxOf(data.map { it.second }.maxOrNull() ?: 1, 1)

                val rv = RemoteViews(context.packageName, R.layout.widget_layout)

                // Draw bar chart as Bitmap via Canvas
                val width = 300
                val height = 150
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)

                val barColor = try { Color.parseColor(habit.color) } catch (e: Exception) { Color.parseColor("#3498db") }
                val paint = Paint().apply {
                    color = barColor
                    style = Paint.Style.FILL
                }

                val barWidth = (width.toFloat() / (days + 1)).toFloat()
                data.forEachIndexed { i, (_, count) ->
                    val barHeight = (count.toFloat() / maxVal) * (height - 20)
                    val left = (i + 0.5f) * barWidth
                    canvas.drawRect(left, height - barHeight, left + barWidth * 0.7f, height.toFloat(), paint)
                }

                // Draw icon text
                val textPaint = android.text.TextPaint().apply {
                    color = Color.BLACK
                    textSize = 10f
                    isAntiAlias = true
                }
                canvas.drawText("${habit.icon} ${habit.name}", 8f, 20f, textPaint)

                rv.setImageViewBitmap(android.R.id.icon, bitmap)

                // Tap to refresh
                val refreshIntent = Intent(context, WidgetTapReceiver::class.java).apply {
                    action = "com.fylgja.app.GRAPH_REFRESH"
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                }
                val pi = PendingIntent.getBroadcast(context, id * 2 + 1, refreshIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                rv.setOnClickPendingIntent(android.R.id.icon, pi)

                mgr.updateAppWidget(id, rv)
            }
        }
    }
}
