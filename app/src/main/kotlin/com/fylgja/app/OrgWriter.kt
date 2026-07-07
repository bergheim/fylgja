package com.fylgja.app

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object OrgWriter {

    suspend fun appendClockEntry(
        resolver: ContentResolver,
        orgUri: Uri,
        habitName: String
    ) = withContext(Dispatchers.IO) {
        val lines = resolver.openInputStream(orgUri)?.bufferedReader()?.readLines()
            ?: throw IllegalStateException("Cannot read org file")

        val now = LocalDateTime.now()
        val dayAbbr = now.format(DateTimeFormatter.ofPattern("EEE", Locale.US))
        val clockLine = "CLOCK: [${now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))} $dayAbbr ${now.format(DateTimeFormatter.ofPattern("HH:mm"))}]"

        val output = mutableListOf<String>()
        var foundHabits = false
        var atTarget = false
        var inLogbook = false
        var inserted = false

        for (line in lines) {
            if (line.trim() == "* Habits") { foundHabits = true }

            if (foundHabits && line.trim() == "** $habitName") {
                atTarget = true
            } else if (atTarget && line.trim().startsWith("* ")) {
                atTarget = false
            }

            if (atTarget && line.trim() == ":LOGBOOK:") {
                inLogbook = true
                output.add(line)
                continue
            }

            if (inLogbook && line.trim() == ":END:") {
                output.add(clockLine)
                inserted = true
                inLogbook = false
                atTarget = false
                output.add(line)
                continue
            }

            output.add(line)
        }

        if (!inserted) throw IllegalStateException("LOGBOOK drawer not found for $habitName")

        resolver.openOutputStream(orgUri, "wt")?.use { os ->
            os.write(output.joinToString("\n").toByteArray())
        } ?: throw IllegalStateException("Cannot write org file")
    }
}
