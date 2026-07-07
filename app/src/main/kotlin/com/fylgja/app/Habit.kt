package com.fylgja.app

data class Habit(
    val name: String,
    val icon: String = "📝",
    val color: String = "#3498db",
    val type: String = "counter",
    val clockEntries: List<ClockEntry> = emptyList()
) {
    fun todayCount(): Int {
        val today = java.time.LocalDate.now().toString()
        return when (type) {
            "binary" -> if (clockEntries.any { it.date == today }) 1 else 0
            else -> clockEntries.count { it.date == today }
        }
    }

    fun lastNDaysCounts(n: Int): List<Pair<String, Int>> {
        val result = mutableListOf<Pair<String, Int>>()
        val today = java.time.LocalDate.now()
        for (i in (n - 1) downTo 0) {
            val date = today.minusDays(i.toLong())
            val count = clockEntries.count { it.date == date.toString() }
            result.add(date.toString() to count)
        }
        return result
    }
}

data class ClockEntry(val date: String, val time: String, val full: String)
