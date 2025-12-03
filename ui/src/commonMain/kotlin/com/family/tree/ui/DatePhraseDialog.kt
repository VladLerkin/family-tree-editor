@file:OptIn(ExperimentalMaterial3Api::class)

package com.family.tree.ui

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

// Full KMP equivalent of JavaFX DatePhraseDialog with formalized modes and GEDCOM formats

private enum class CalKind(val title: String, val prefix: String) {
    GREGORIAN("Gregorian", ""),
    JULIAN("Julian", "@#DJULIAN@ "),
    HEBREW("Hebrew", "@#DHEBREW@ "),
    FRENCH_REV("French Rev.", "@#DFRENCH R@ "),
    UNKNOWN("Unknown", "@#DUNKNOWN@ ");
}

private val MONTHS = listOf("", "JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")
private val HEBREW_MONTHS = listOf("", "TSH", "CSH", "KSL", "TVT", "SHV", "ADR", "ADS", "NSN", "IYR", "SVN", "TMZ", "AAV")
private val FRENCH_REV_MONTHS = listOf("", "VEND", "BRUM", "FRIM", "NIVO", "PLUV", "VENT", "GERM", "FLOR", "PRAI", "MESS", "THER", "FRUC")

// Top-level helper so it is visible everywhere and avoids forward-reference issues inside local scopes
private fun isMonthCodeStrict(s: String): Boolean {
    if (MONTHS.any { it.isNotEmpty() && it == s }) return true
    if (HEBREW_MONTHS.any { it.isNotEmpty() && it == s }) return true
    if (FRENCH_REV_MONTHS.any { it.isNotEmpty() && it == s }) return true
    return false
}

// IMPORTANT: fields must be observable for Compose, otherwise TextField/Checkbox won't be able to update values.
// Therefore we use mutableStateOf for each property.
private class ExactState(
    cal: CalKind = CalKind.GREGORIAN,
    day: String = "",
    month: String = "",
    year: String = "",
    bc: Boolean = false
) {
    var cal by mutableStateOf(cal)
    var day by mutableStateOf(day)
    var month by mutableStateOf(month)
    var year by mutableStateOf(year)
    var bc by mutableStateOf(bc)
}

private enum class Mode { EXACT, PERIOD, RANGE, APPROX, INTERPRETED, PHRASE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePhraseDialog(
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // Main dialog state
    var mode by remember { mutableStateOf(Mode.EXACT) }

    // Exact
    val exact = remember { ExactState() }

    // Period (FROM/TO are optional)
    var useFrom by remember { mutableStateOf(false) }
    var useTo by remember { mutableStateOf(false) }
    val fromState = remember { ExactState() }
    val toState = remember { ExactState() }

    // Range (BET/BEF/AFT)
    var rangeType by remember { mutableStateOf("BET") } // BET|BEF|AFT
    val betLeft = remember { ExactState() }
    val andRight = remember { ExactState() }

    // Approx (ABT/CAL/EST)
    var approxType by remember { mutableStateOf("ABT") }
    val approxExact = remember { ExactState() }

    // Interpreted: date + phrase
    val interpretedExact = remember { ExactState() }
    var interpretedPhrase by remember { mutableStateOf("") }

    // Phrase: free text
    var phrase by remember { mutableStateOf("") }

    // Parse initial → state
    LaunchedEffect(initial) {
        parseInitialToState(initial, onMode = { mode = it }) { target, value ->
            when (target) {
                "exact" -> copyInto(exact, value)
                "from" -> copyInto(fromState, value)
                "to" -> copyInto(toState, value)
                "bet" -> copyInto(betLeft, value)
                "and" -> copyInto(andRight, value)
                "approx" -> copyInto(approxExact, value)
                "int" -> copyInto(interpretedExact, value)
            }
        }.also { details ->
            details.rangeType?.let { rangeType = it }
            details.useFrom?.let { useFrom = it }
            details.useTo?.let { useTo = it }
            details.approxType?.let { approxType = it }
            details.interpretedPhrase?.let { interpretedPhrase = it }
            details.phrase?.let { phrase = it }
        }
    }

    // Use BasicAlertDialog to control container width (on Android standard AlertDialog has fixed narrow width)
    // IMPORTANT: properties.usePlatformDefaultWidth = false — removes platform width restriction on Android
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(1.0f) // maximum width on screen
                .widthIn(min = 1100.dp, max = 1920.dp) // even wider (~+20%)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Title
                Text("Event Date", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))

                // Content + Actions (scrollable together so buttons are visible on all tabs)
                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 540.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Mode selector
                    ModeSelector(mode = mode, onMode = { mode = it })
                    Spacer(Modifier.height(8.dp))

                    when (mode) {
                        Mode.EXACT -> ExactBlock(exact)
                        Mode.PERIOD -> PeriodBlock(useFrom, { useFrom = it }, fromState, useTo, { useTo = it }, toState)
                        Mode.RANGE -> RangeBlock(rangeType, { rangeType = it }, betLeft, andRight)
                        Mode.APPROX -> ApproxBlock(approxType, { approxType = it }, approxExact)
                        Mode.INTERPRETED -> InterpretedBlock(interpretedExact, interpretedPhrase, { interpretedPhrase = it })
                        Mode.PHRASE -> PhraseBlock(phrase) { phrase = it }
                    }

                    Spacer(Modifier.height(16.dp))
                    // Actions
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismiss) { Text("Cancel") }
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = {
                            val result = buildGedcomDate(
                                mode, exact, useFrom, fromState, useTo, toState,
                                rangeType, betLeft, andRight, approxType, approxExact,
                                interpretedExact, interpretedPhrase, phrase
                            )
                            if (result == null) return@TextButton
                            onConfirm(result)
                        }) { Text("OK") }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeSelector(mode: Mode, onMode: (Mode) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        listOf(
            Mode.EXACT to "Exact",
            Mode.PERIOD to "Period",
            Mode.RANGE to "Range",
            Mode.APPROX to "Approx",
            Mode.INTERPRETED to "Interpreted",
            Mode.PHRASE to "Phrase"
        ).forEach { (m, label) ->
            FilterChip(
                selected = mode == m,
                onClick = { onMode(m) },
                label = { Text(label) },
                modifier = Modifier.padding(end = 6.dp)
            )
        }
    }
}

@Composable
private fun ExactBlock(state: ExactState) {
    CalendarRow(state)
    Spacer(Modifier.height(6.dp))
    DayMonthYearRow(state)
}

@Composable
private fun PeriodBlock(useFrom: Boolean, setUseFrom: (Boolean) -> Unit, from: ExactState,
                        useTo: Boolean, setUseTo: (Boolean) -> Unit, to: ExactState) {
    SectionCard(title = "FROM", trailing = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = useFrom, onCheckedChange = setUseFrom)
            Text("Enabled")
        }
    }) {
        if (useFrom) ExactBlock(from)
    }
    Spacer(Modifier.height(8.dp))
    SectionCard(title = "TO", trailing = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = useTo, onCheckedChange = setUseTo)
            Text("Enabled")
        }
    }) {
        if (useTo) ExactBlock(to)
    }
}

@Composable
private fun RangeBlock(rangeType: String, setType: (String) -> Unit, left: ExactState, right: ExactState) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Type:")
        Spacer(Modifier.width(8.dp))
        var menu by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = menu, onExpandedChange = { menu = it }) {
            OutlinedTextField(
                value = rangeType,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.menuAnchor().width(160.dp),
                label = { Text("Range type") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menu) }
            )
            ExposedDropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                listOf("BET", "BEF", "AFT").forEach { t ->
                    DropdownMenuItem(text = { Text(t) }, onClick = { setType(t); menu = false })
                }
            }
        }
    }
    Spacer(Modifier.height(6.dp))
    SectionCard(title = if (rangeType == "BET") "BET (left)" else "Date") {
        ExactBlock(left)
    }
    if (rangeType == "BET") {
        Spacer(Modifier.height(6.dp))
        SectionCard(title = "AND (right)") {
            ExactBlock(right)
        }
    }
}

@Composable
private fun ApproxBlock(approxType: String, setType: (String) -> Unit, exact: ExactState) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Type:")
        Spacer(Modifier.width(8.dp))
        var menu by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = menu, onExpandedChange = { menu = it }) {
            OutlinedTextField(
                value = approxType,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.menuAnchor().width(160.dp),
                label = { Text("Approx type") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menu) }
            )
            ExposedDropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                listOf("ABT", "CAL", "EST").forEach { t ->
                    DropdownMenuItem(text = { Text(t) }, onClick = { setType(t); menu = false })
                }
            }
        }
    }
    Spacer(Modifier.height(6.dp))
    SectionCard(title = "Approx date") {
        ExactBlock(exact)
    }
}

@Composable
private fun InterpretedBlock(exact: ExactState, phrase: String, setPhrase: (String) -> Unit) {
    SectionCard(title = "Interpreted date") {
        ExactBlock(exact)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = phrase,
            onValueChange = setPhrase,
            label = { Text("Interpretation (in parentheses)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}

@Composable
private fun PhraseBlock(value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text("Phrase") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarRow(state: ExactState) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        var menu by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = menu, onExpandedChange = { menu = it }) {
            OutlinedTextField(
                value = state.cal.title,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.menuAnchor().width(220.dp),
                label = { Text("Calendar") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menu) }
            )
            ExposedDropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                CalKind.values().forEach { k ->
                    DropdownMenuItem(text = { Text(k.title) }, onClick = { state.cal = k; menu = false })
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.weight(1f))
                trailing?.invoke()
            }
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

private const val ZWSP = "\u200B" // zero-width space to force floating label when value is empty

private fun displayValue(v: String): String = if (v.isEmpty()) ZWSP else v

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayMonthYearRow(state: ExactState) {
    val months = when (state.cal) {
        CalKind.HEBREW -> HEBREW_MONTHS
        CalKind.FRENCH_REV -> FRENCH_REV_MONTHS
        else -> MONTHS
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        // Day
        var dayMenu by remember(state.cal) { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = dayMenu, onExpandedChange = { dayMenu = it }) {
            OutlinedTextField(
                value = displayValue(state.day),
                onValueChange = {},
                readOnly = true,
                label = { Text("Day") },
                modifier = Modifier.menuAnchor().width(90.dp),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayMenu) }
            )
            ExposedDropdownMenu(expanded = dayMenu, onDismissRequest = { dayMenu = false }) {
                listOf("") + (1..31).map { it.toString() } .forEach { d ->
                    DropdownMenuItem(text = { Text(d.ifEmpty { "(none)" }) }, onClick = { state.day = d; dayMenu = false })
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        // Month
        var monthMenu by remember(state.cal) { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = monthMenu, onExpandedChange = { monthMenu = it }) {
            OutlinedTextField(
                value = displayValue(state.month),
                onValueChange = {},
                readOnly = true,
                label = { Text("Month") },
                modifier = Modifier.menuAnchor().width(120.dp),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthMenu) }
            )
            ExposedDropdownMenu(expanded = monthMenu, onDismissRequest = { monthMenu = false }) {
                months.forEach { m ->
                    val label = if (m.isEmpty()) "(none)" else m
                    DropdownMenuItem(text = { Text(label) }, onClick = { state.month = m; monthMenu = false })
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        OutlinedTextField(
            value = displayValue(state.year),
            onValueChange = { state.year = it.filter { ch -> ch.isDigit() } },
            label = { Text("Year") },
            modifier = Modifier.width(120.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(Modifier.width(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = state.bc, onCheckedChange = { state.bc = it })
            Text("B.C.")
        }
    }
}

// ----------------- PARSING/BUILDING LOGIC -----------------

private data class ParseDetails(
    val rangeType: String? = null,
    val useFrom: Boolean? = null,
    val useTo: Boolean? = null,
    val approxType: String? = null,
    val interpretedPhrase: String? = null,
    val phrase: String? = null
)

private fun copyInto(dst: ExactState, src: ExactState) {
    dst.cal = src.cal
    dst.day = src.day
    dst.month = src.month
    dst.year = src.year
    dst.bc = src.bc
}

private fun parseInitialToState(
    initial: String,
    onMode: (Mode) -> Unit,
    put: (target: String, value: ExactState) -> Unit
): ParseDetails {
    var s = initial.trim()
    if (s.isEmpty()) return ParseDetails()

    // Helpers
    fun parseCalendarPrefix(text: String): Pair<CalKind, String> {
        var t = text
        val kind = when {
            t.startsWith("@#DHEBREW@") -> { t = t.removePrefix("@#DHEBREW@").trim(); CalKind.HEBREW }
            t.startsWith("@#DFRENCH R@") -> { t = t.removePrefix("@#DFRENCH R@").trim(); CalKind.FRENCH_REV }
            t.startsWith("@#DJULIAN@") -> { t = t.removePrefix("@#DJULIAN@").trim(); CalKind.JULIAN }
            t.startsWith("@#DUNKNOWN@") -> { t = t.removePrefix("@#DUNKNOWN@").trim(); CalKind.UNKNOWN }
            else -> CalKind.GREGORIAN
        }
        return kind to t
    }
    fun parseExact(text: String): ExactState {
        var t = text.trim()
        val (cal, t1) = parseCalendarPrefix(t)
        t = t1
        val bc = t.endsWith(" B.C.")
        if (bc) t = t.removeSuffix(" B.C.").trim()
        val parts = t.split(Regex("\\s+")).filter { it.isNotEmpty() }
        var day = ""
        var month = ""
        var year = ""
        if (parts.size == 3) {
            day = parts[0]; month = parts[1]; year = parts[2]
        } else if (parts.size == 2) {
            // if one of two is month, the other is year or day
            val isMonth0 = isMonthCodeStrict(parts[0])
            val isMonth1 = isMonthCodeStrict(parts[1])
            if (isMonth0 && !isMonth1) { month = parts[0]; year = parts[1] }
            else if (!isMonth0 && isMonth1) { day = parts[0]; month = parts[1] }
            else { month = parts[0]; year = parts[1] }
        } else if (parts.size == 1) {
            if (isMonthCodeStrict(parts[0])) month = parts[0] else year = parts[0]
        }
        return ExactState(cal = cal, day = day, month = month, year = year, bc = bc)
    }

    // INT ... ( ... )
    if (s.startsWith("INT ")) {
        onMode(Mode.INTERPRETED)
        val rest = s.removePrefix("INT ").trim()
        val parenIndex = rest.indexOf('(')
        if (parenIndex > 0) {
            val datePart = rest.substring(0, parenIndex).trim()
            var phrasePart = rest.substring(parenIndex + 1).trim()
            if (phrasePart.endsWith(")")) phrasePart = phrasePart.dropLast(1).trim()
            put("int", parseExact(datePart))
            return ParseDetails(interpretedPhrase = phrasePart)
        }
        put("int", parseExact(rest))
        return ParseDetails()
    }

    // ABT/CAL/EST
    if (s.startsWith("ABT ") || s.startsWith("CAL ") || s.startsWith("EST ")) {
        onMode(Mode.APPROX)
        val type = s.substring(0, 3)
        val rest = s.substring(4).trim()
        put("approx", parseExact(rest))
        return ParseDetails(approxType = type)
    }

    // Range: BET … AND … | BEF … | AFT …
    if (s.startsWith("BET ") || s.startsWith("BEF ") || s.startsWith("AFT ")) {
        val type = s.substring(0, 3)
        if (type == "BET") {
            val andIdx = s.indexOf(" AND ")
            if (andIdx > 0) {
                onMode(Mode.RANGE)
                val left = s.substring(4, andIdx).trim()
                val right = s.substring(andIdx + 5).trim()
                put("bet", parseExact(left))
                put("and", parseExact(right))
                return ParseDetails(rangeType = "BET")
            }
        } else {
            onMode(Mode.RANGE)
            val rest = s.substring(4).trim()
            put("bet", parseExact(rest))
            return ParseDetails(rangeType = type)
        }
    }

    // Period: FROM … [TO …] | [FROM …] TO …
    val hasFrom = s.startsWith("FROM ")
    val toIdx = s.indexOf(" TO ")
    if (hasFrom || toIdx > 0) {
        onMode(Mode.PERIOD)
        if (hasFrom) {
            if (toIdx > 0) {
                val fromPart = s.substring(5, toIdx).trim()
                val toPart = s.substring(toIdx + 4).trim()
                put("from", parseExact(fromPart))
                put("to", parseExact(toPart))
                return ParseDetails(useFrom = true, useTo = true)
            } else {
                val fromPart = s.substring(5).trim()
                put("from", parseExact(fromPart))
                return ParseDetails(useFrom = true, useTo = false)
            }
        } else {
            val toPart = s.substring(toIdx + 4).trim()
            put("to", parseExact(toPart))
            return ParseDetails(useFrom = false, useTo = true)
        }
    }

    // Exact vs Phrase
    val hasDigits = s.any { it.isDigit() }
    val hasMonth = MONTHS.any { it.isNotEmpty() && s.contains(it) } ||
            HEBREW_MONTHS.any { it.isNotEmpty() && s.contains(it) } ||
            FRENCH_REV_MONTHS.any { it.isNotEmpty() && s.contains(it) }
    return if (hasDigits || hasMonth) {
        onMode(Mode.EXACT)
        put("exact", parseExact(s))
        ParseDetails()
    } else {
        onMode(Mode.PHRASE)
        ParseDetails(phrase = s)
    }
}

private fun buildExact(s: ExactState): String? {
    val sb = StringBuilder()
    // calendar prefix
    if (s.cal != CalKind.GREGORIAN) sb.append(s.cal.prefix)
    if (s.day.isNotEmpty()) sb.append(s.day).append(' ')
    if (s.month.isNotEmpty()) sb.append(s.month).append(' ')
    val coreEmpty = sb.isEmpty()
    if (s.year.isEmpty() && coreEmpty) return null
    if (s.year.isNotEmpty()) sb.append(s.year)
    if (s.bc) sb.append(" B.C.")
    return sb.toString().trim()
}

private fun buildGedcomDate(
    mode: Mode,
    exact: ExactState,
    useFrom: Boolean,
    fromState: ExactState,
    useTo: Boolean,
    toState: ExactState,
    rangeType: String,
    betLeft: ExactState,
    andRight: ExactState,
    approxType: String,
    approxExact: ExactState,
    interpretedExact: ExactState,
    interpretedPhrase: String,
    phrase: String
): String? {
    return when (mode) {
        Mode.EXACT -> buildExact(exact)
        Mode.PERIOD -> {
            val left = if (useFrom) buildExact(fromState) else null
            val right = if (useTo) buildExact(toState) else null
            if (left == null && right == null) null
            else if (left != null && right != null) "FROM $left TO $right"
            else if (left != null) "FROM $left"
            else "TO $right"
        }
        Mode.RANGE -> {
            when (rangeType) {
                "BET" -> {
                    val l = buildExact(betLeft)
                    val r = buildExact(andRight)
                    if (l == null || r == null) null else "BET $l AND $r"
                }
                "BEF" -> buildExact(betLeft)?.let { "BEF $it" }
                "AFT" -> buildExact(betLeft)?.let { "AFT $it" }
                else -> null
            }
        }
        Mode.APPROX -> buildExact(approxExact)?.let { "$approxType $it" }
        Mode.INTERPRETED -> buildExact(interpretedExact)?.let { base ->
            if (interpretedPhrase.isNotBlank()) "INT $base (${interpretedPhrase.trim()})" else "INT $base"
        }
        Mode.PHRASE -> phrase.ifBlank { null }
    }
}
