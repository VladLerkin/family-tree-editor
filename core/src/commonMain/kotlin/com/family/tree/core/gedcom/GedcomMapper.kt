package com.family.tree.core.gedcom

import com.family.tree.core.model.Gender
import com.family.tree.core.model.Individual
import com.family.tree.core.model.Family

/**
 * GEDCOM mapping utilities for KMP.
 * Handles name parsing, gender mapping, date formatting/parsing, and xref generation.
 */
object GedcomMapper {
    
    fun buildIndividualXrefs(individuals: List<Individual>): Map<String, String> {
        val map = mutableMapOf<String, String>()
        for (i in individuals.indices) {
            map[individuals[i].id.value] = "@I${i + 1}@"
        }
        return map
    }

    fun buildFamilyXrefs(families: List<Family>): Map<String, String> {
        val map = mutableMapOf<String, String>()
        for (i in families.indices) {
            map[families[i].id.value] = "@F${i + 1}@"
        }
        return map
    }

    fun buildName(ind: Individual): String {
        val first = safe(ind.firstName)
        val last = safe(ind.lastName)
        // GEDCOM NAME format: Given /Surname/
        return "$first /$last/".trim()
    }

    fun sexCode(gender: Gender?): String {
        if (gender == null) return "U"
        return when (gender) {
            Gender.MALE -> "M"
            Gender.FEMALE -> "F"
            else -> "U"
        }
    }

    fun parseSex(code: String?): Gender {
        if (code == null) return Gender.UNKNOWN
        return when (code.trim().uppercase()) {
            "M" -> Gender.MALE
            "F" -> Gender.FEMALE
            else -> Gender.UNKNOWN
        }
    }

    fun formatDate(year: Int?, month: Int? = null, day: Int? = null): String? {
        if (year == null) return null
        val parts = mutableListOf<String>()
        
        if (day != null && month != null) {
            parts.add(day.toString())
        }
        
        if (month != null) {
            val monthName = when (month) {
                1 -> "JAN"
                2 -> "FEB"
                3 -> "MAR"
                4 -> "APR"
                5 -> "MAY"
                6 -> "JUN"
                7 -> "JUL"
                8 -> "AUG"
                9 -> "SEP"
                10 -> "OCT"
                11 -> "NOV"
                12 -> "DEC"
                else -> null
            }
            if (monthName != null) {
                parts.add(monthName)
            }
        }
        
        parts.add(year.toString())
        return parts.joinToString(" ")
    }

    fun parseDate(text: String?): Triple<Int?, Int?, Int?>? {
        if (text == null) return null
        val s = text.trim().uppercase()
        // Expect formats like: 5 JAN 1980, 01 JAN 1980, JAN 1980, 1980
        val parts = s.split(Regex("\\s+")).filter { it.isNotEmpty() }
        
        return try {
            when (parts.size) {
                3 -> {
                    val day = parts[0].toIntOrNull()
                    val month = parseMonth(parts[1])
                    val year = parts[2].toIntOrNull()
                    Triple(year, month, day)
                }
                2 -> {
                    val month = parseMonth(parts[0])
                    val year = parts[1].toIntOrNull()
                    Triple(year, month, null)
                }
                1 -> {
                    val year = parts[0].toIntOrNull()
                    Triple(year, null, null)
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseMonth(mon: String?): Int? {
        if (mon == null) return null
        val m = mon.trim().uppercase()
        return when (m) {
            "JAN" -> 1
            "FEB" -> 2
            "MAR" -> 3
            "APR" -> 4
            "MAY" -> 5
            "JUN" -> 6
            "JUL" -> 7
            "AUG" -> 8
            "SEP", "SEPT" -> 9
            "OCT" -> 10
            "NOV" -> 11
            "DEC" -> 12
            else -> null
        }
    }

    fun safe(s: String?): String = s ?: ""

    fun parseName(nameLine: String?): Pair<String, String> {
        // Returns (given, surname)
        if (nameLine == null) return Pair("", "")
        val s = nameLine.trim()
        val firstSlash = s.indexOf('/')
        val secondSlash = if (firstSlash >= 0) s.indexOf('/', firstSlash + 1) else -1
        var given = s
        var surname = ""
        if (firstSlash >= 0 && secondSlash > firstSlash) {
            given = s.substring(0, firstSlash).trim()
            surname = s.substring(firstSlash + 1, secondSlash).trim()
        }
        return Pair(given, surname)
    }

    fun extractYearFromDate(date: String?): Int? {
        if (date == null) return null
        val parsed = parseDate(date)
        return parsed?.first
    }
}
