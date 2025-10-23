package com.family.tree.gedcom

import com.family.tree.model.Family
import com.family.tree.model.Gender
import com.family.tree.model.Individual
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

/**
 * Kotlin port of GedcomMapper (1:1 with Java implementation).
 */
object GedcomMapper {
    @JvmStatic
    fun buildIndividualXrefs(individuals: List<Individual>): Map<String, String> {
        val map = HashMap<String, String>()
        for (i in individuals.indices) {
            map[individuals[i].id] = "@I${i + 1}@"
        }
        return map
    }

    @JvmStatic
    fun buildFamilyXrefs(families: List<Family>): Map<String, String> {
        val map = HashMap<String, String>()
        for (i in families.indices) {
            map[families[i].id] = "@F${i + 1}@"
        }
        return map
    }

    @JvmStatic
    fun buildName(ind: Individual): String {
        val first = safe(ind.firstName)
        val last = safe(ind.lastName)
        // GEDCOM NAME format: Given /Surname/
        return "$first /$last/".trim()
    }

    @JvmStatic
    fun sexCode(gender: Gender?): String {
        if (gender == null) return "U"
        return when (gender) {
            Gender.MALE -> "M"
            Gender.FEMALE -> "F"
            else -> "U"
        }
    }

    @JvmStatic
    fun parseSex(code: String?): Gender {
        if (code == null) return Gender.UNKNOWN
        return when (code.trim().uppercase(Locale.ROOT)) {
            "M" -> Gender.MALE
            "F" -> Gender.FEMALE
            else -> Gender.UNKNOWN
        }
    }

    @JvmStatic
    fun formatDate(date: LocalDate?): String? {
        if (date == null) return null
        // Use day month-abbrev year, e.g., 5 JAN 1980 per GEDCOM 5.5
        var mon = date.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).uppercase(Locale.ROOT)
        // Ensure 3-letter month abbreviations
        if (mon.length > 3) mon = mon.substring(0, 3)
        return "${date.dayOfMonth} $mon ${date.year}"
    }

    @JvmStatic
    fun parseDate(text: String?): LocalDate? {
        if (text == null) return null
        val s = text.trim().uppercase(Locale.ROOT)
        // Expect formats like: 5 JAN 1980, 01 JAN 1980, JAN 1980, 1980
        val parts = s.split("\u0020", "\t", "\n", "\r").filter { it.isNotEmpty() }
        return try {
            when (parts.size) {
                3 -> {
                    val day = parts[0].toInt()
                    val m = parseMonth(parts[1])
                    val year = parts[2].toInt()
                    if (m != null) LocalDate.of(year, m, kotlin.math.min(kotlin.math.max(1, day), 28)) else null
                }
                2 -> {
                    val m = parseMonth(parts[0])
                    val year = parts[1].toInt()
                    if (m != null) LocalDate.of(year, m, 1) else null
                }
                1 -> {
                    val year = parts[0].toInt()
                    LocalDate.of(year, 1, 1)
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseMonth(mon: String?): Month? {
        if (mon == null) return null
        val m = mon.trim().uppercase(Locale.ROOT)
        return when (m) {
            "JAN" -> Month.JANUARY
            "FEB" -> Month.FEBRUARY
            "MAR" -> Month.MARCH
            "APR" -> Month.APRIL
            "MAY" -> Month.MAY
            "JUN" -> Month.JUNE
            "JUL" -> Month.JULY
            "AUG" -> Month.AUGUST
            "SEP", "SEPT" -> Month.SEPTEMBER
            "OCT" -> Month.OCTOBER
            "NOV" -> Month.NOVEMBER
            "DEC" -> Month.DECEMBER
            else -> null
        }
    }

    @JvmStatic
    fun safe(s: String?): String = s ?: ""

    @JvmStatic
    fun parseName(nameLine: String?): Array<String> {
        // Returns [given, surname]
        if (nameLine == null) return arrayOf("", "")
        val s = nameLine.trim()
        val firstSlash = s.indexOf('/')
        val secondSlash = if (firstSlash >= 0) s.indexOf('/', firstSlash + 1) else -1
        var given = s
        var surname = ""
        if (firstSlash >= 0 && secondSlash > firstSlash) {
            given = s.substring(0, firstSlash).trim()
            surname = s.substring(firstSlash + 1, secondSlash).trim()
        }
        return arrayOf(given, surname)
    }
}