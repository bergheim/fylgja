package com.fylgja.app

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CounterService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification()
        startForeground(1, notification)

        CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                delay(60_000)
                val mgr = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                mgr.notify(1, buildNotification())
            }
        }
        return START_STICKY
    }

    private fun buildNotification(): Notification {
        val store = HabitStore(this)
        val habits = kotlinx.coroutines.runBlocking { store.habitsFlow().first() }
        val summary = if (habits.isEmpty()) "No habits" else habits.joinToString("  ") {
            "${it.icon} ${it.todayCount()}"
        }

        return Notification.Builder(this, "habit_counters")
            .setContentTitle("Habit Summary")
            .setContentText(summary)
            .setSmallIcon(android.R.drawable.btn_star)
            .setOngoing(true)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel("habit_counters", "Habit Counters",
            NotificationManager.IMPORTANCE_LOW).apply {
            setShowBadge(false)
        }
        val mgr = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        mgr.createNotificationChannel(channel)
    }
}
