package com.fylgja.app

object OrgReader {

    fun parse(orgText: String): List<Habit> {
        val lines = orgText.lines()
        val habits = mutableListOf<Habit>()
        var inHabitsSection = false

        var currentName: String? = null
        var icon = "📝"
        var color = "#3498db"
        var type = "counter"
        var clocks = mutableListOf<ClockEntry>()
        var inLogbook = false

        fun commit() {
            currentName?.let {
                habits.add(Habit(it, icon, color, type, clocks.toList()))
            }
            currentName = null
            icon = "📝"
            color = "#3498db"
            type = "counter"
            clocks = mutableListOf()
            inLogbook = false
        }

        for (line in lines) {
            val trimmed = line.trim()

            if (trimmed == "* Habits") {
                inHabitsSection = true
                continue
            }
            if (trimmed.startsWith("* ") && !trimmed.startsWith("** ") && inHabitsSection) {
                inHabitsSection = false
                commit()
                break
            }

            if (!inHabitsSection) continue

            if (trimmed.startsWith("** ")) {
                if (currentName != null) commit()
                currentName = trimmed.removePrefix("** ")
                continue
            }

            if (trimmed.startsWith(":PROPERTIES:")) continue
            if (trimmed == ":END:") { inLogbook = false; continue }
            if (trimmed.startsWith(":LOGBOOK:")) { inLogbook = true; continue }

            if (trimmed.startsWith(":ICON:")) {
                icon = trimmed.removePrefix(":ICON:").trim()
            } else if (trimmed.startsWith(":COLOR:")) {
                color = trimmed.removePrefix(":COLOR:").trim()
            } else if (trimmed.startsWith(":TYPE:")) {
                type = trimmed.removePrefix(":TYPE:").trim()
            } else if (inLogbook && trimmed.startsWith("CLOCK:")) {
                val ts = trimmed.removePrefix("CLOCK:").trim().trim('[', ']')
                val parts = ts.split(" ")
                if (parts.size >= 3) {
                    clocks.add(ClockEntry(parts[0], parts[2], ts))
                }
            }
        }
        commit()
        return habits
    }
}
