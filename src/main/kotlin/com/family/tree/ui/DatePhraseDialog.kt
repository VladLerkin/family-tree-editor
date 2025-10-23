package com.family.tree.ui

import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.stage.Modality
import javafx.stage.Stage
import java.util.*

/**
 * Faithful Kotlin port of the original Java DatePhraseDialog.
 * Preserves all fields, logic, and formatting of the dialog.
 */
class DatePhraseDialog {

    enum class Calendar(private val label: String) { GREGORIAN("Gregorian"), JULIAN("Julian"), HEBREW("Hebrew"), FRENCH_REV("French Rev."), UNKNOWN("Unknown"); override fun toString(): String = label }

    companion object {
        private val MONTHS = arrayOf("", "JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")
        private val HEBREW_MONTHS = arrayOf("", "TSH", "CSH", "KSL", "TVT", "SHV", "ADR", "ADS", "NSN", "IYR", "SVN", "TMZ", "AAV")
        private val FRENCH_REV_MONTHS = arrayOf("", "VEND", "BRUM", "FRIM", "NIVO", "PLUV", "VENT", "GERM", "FLOR", "PRAI", "MESS", "THER", "FRUC")

        private fun createDayCombo(): ComboBox<String> {
            val cb = ComboBox<String>()
            cb.items.add("")
            for (i in 1..31) cb.items.add(i.toString())
            cb.selectionModel.select(0)
            cb.prefWidth = 65.0
            return cb
        }

        private fun updateMonthCombo(monthCombo: ComboBox<String>, calendar: Calendar) {
            val current = monthCombo.selectionModel.selectedItem
            monthCombo.items.clear()
            val arr = when (calendar) {
                Calendar.HEBREW -> HEBREW_MONTHS
                Calendar.FRENCH_REV -> FRENCH_REV_MONTHS
                else -> MONTHS
            }
            for (m in arr) monthCombo.items.add(m)
            if (current != null && monthCombo.items.contains(current)) {
                monthCombo.selectionModel.select(current)
            } else monthCombo.selectionModel.select(0)
        }

        private fun <T> standardize(cal: ComboBox<T>?, day: ComboBox<String>?, month: ComboBox<String>?, year: TextField?) {
            if (cal != null) cal.prefWidth = 120.0
            if (day != null) day.prefWidth = 65.0
            if (month != null) month.prefWidth = 85.0
            if (year != null) year.prefColumnCount = 5
        }

        private fun setFixedWidth(node: javafx.scene.Node?, width: Double) {
            node?.style = "-fx-pref-width: ${width}px; -fx-min-width: ${width}px; -fx-max-width: ${width}px;"
        }

        private fun showWarn() { Dialogs.showError("Date", "Please provide enough date details.") }

        private fun buildExact(cal: ComboBox<Calendar>, d: ComboBox<String>, m: ComboBox<String>, y: TextField, bc: CheckBox): String? {
            val year = y.text?.trim() ?: ""
            var month = m.selectionModel.selectedItem
            var day = d.selectionModel.selectedItem
            if (day == null) day = ""
            val sb = StringBuilder()
            val cv = cal.value
            if (cv != null && cv != Calendar.GREGORIAN) {
                val escape = when (cv) {
                    Calendar.HEBREW -> "@#DHEBREW@"
                    Calendar.FRENCH_REV -> "@#DFRENCH R@"
                    Calendar.JULIAN -> "@#DJULIAN@"
                    Calendar.UNKNOWN -> "@#DUNKNOWN@"
                    else -> cv.toString()
                }
                sb.append(escape).append(' ')
            }
            if (!day.isNullOrEmpty()) sb.append(day).append(' ')
            if (!month.isNullOrEmpty()) sb.append(month).append(' ')
            if (year.isEmpty() && sb.isEmpty()) return null
            if (year.isNotEmpty()) sb.append(year)
            if (bc.isSelected) sb.append(" B.C.")
            return sb.toString().trim()
        }

        private fun containsMonthCode(s: String): Boolean {
            for (m in MONTHS) if (m.isNotEmpty() && s.contains(m)) return true
            for (m in HEBREW_MONTHS) if (m.isNotEmpty() && s.contains(m)) return true
            for (m in FRENCH_REV_MONTHS) if (m.isNotEmpty() && s.contains(m)) return true
            return false
        }

        private fun isMonthCode(s: String): Boolean {
            for (m in MONTHS) if (m.isNotEmpty() && m == s) return true
            for (m in HEBREW_MONTHS) if (m.isNotEmpty() && m == s) return true
            for (m in FRENCH_REV_MONTHS) if (m.isNotEmpty() && m == s) return true
            return false
        }

        private fun parseDateIntoControls(datePart: String?, cal: ComboBox<Calendar>, d: ComboBox<String>, m: ComboBox<String>, y: TextField, bc: CheckBox) {
            if (datePart.isNullOrEmpty()) return
            var s = datePart.trim()
            var calendar = Calendar.GREGORIAN
            if (s.startsWith("@#DHEBREW@")) { calendar = Calendar.HEBREW; s = s.substring(10).trim() }
            else if (s.startsWith("@#DFRENCH R@")) { calendar = Calendar.FRENCH_REV; s = s.substring(12).trim() }
            else if (s.startsWith("@#DJULIAN@")) { calendar = Calendar.JULIAN; s = s.substring(10).trim() }
            else if (s.startsWith("@#DUNKNOWN@")) { calendar = Calendar.UNKNOWN; s = s.substring(11).trim() }
            cal.selectionModel.select(calendar)
            val isBc = s.endsWith(" B.C.")
            if (isBc) { s = s.substring(0, s.length - 5).trim(); bc.isSelected = true } else bc.isSelected = false
            val parts = s.split(Regex("\\s+"))
            var dayStr = ""
            var monthStr = ""
            var yearStr = ""
            if (parts.size == 3) {
                dayStr = parts[0]; monthStr = parts[1]; yearStr = parts[2]
            } else if (parts.size == 2) {
                if (isMonthCode(parts[0])) { monthStr = parts[0]; yearStr = parts[1] }
                else if (isMonthCode(parts[1])) { dayStr = parts[0]; monthStr = parts[1] }
                else { monthStr = parts[0]; yearStr = parts[1] }
            } else if (parts.size == 1) {
                if (isMonthCode(parts[0])) monthStr = parts[0] else yearStr = parts[0]
            }
            if (dayStr.isNotEmpty() && dayStr.matches(Regex("\\d+"))) d.selectionModel.select(dayStr) else d.selectionModel.select(0)
            if (monthStr.isNotEmpty()) m.selectionModel.select(monthStr) else m.selectionModel.select(0)
            y.text = yearStr
        }

        private fun parseAndPopulate(initial: String,
                                     rbExact: RadioButton, rbPeriod: RadioButton, rbRange: RadioButton, rbApprox: RadioButton, rbInterpreted: RadioButton, rbPhrase: RadioButton,
                                     cal1: ComboBox<Calendar>, d1: ComboBox<String>, m1: ComboBox<String>, y1: TextField, bc1: CheckBox,
                                     fromChecked: CheckBox, calFrom: ComboBox<Calendar>, dFrom: ComboBox<String>, mFrom: ComboBox<String>, yFrom: TextField, bcFrom: CheckBox,
                                     toChecked: CheckBox, calTo: ComboBox<Calendar>, dTo: ComboBox<String>, mTo: ComboBox<String>, yTo: TextField, bcTo: CheckBox,
                                     rangeType: ComboBox<String>, calBet: ComboBox<Calendar>, dBet: ComboBox<String>, mBet: ComboBox<String>, yBet: TextField, bcBet: CheckBox,
                                     calAnd: ComboBox<Calendar>, dAnd: ComboBox<String>, mAnd: ComboBox<String>, yAnd: TextField, bcAnd: CheckBox,
                                     approxType: ComboBox<String>, calAbt: ComboBox<Calendar>, dAbt: ComboBox<String>, mAbt: ComboBox<String>, yAbt: TextField, bcAbt: CheckBox,
                                     calInt: ComboBox<Calendar>, dInt: ComboBox<String>, mInt: ComboBox<String>, yInt: TextField, bcInt: CheckBox, phraseInt: TextField,
                                     phrase: TextField) {
            var s = initial.trim()
            if (s.startsWith("INT ")) {
                rbInterpreted.isSelected = true
                val rest = s.substring(4).trim()
                val parenIndex = rest.indexOf('(')
                if (parenIndex > 0) {
                    val datePart = rest.substring(0, parenIndex).trim()
                    var phrasePart = rest.substring(parenIndex + 1).trim()
                    if (phrasePart.endsWith(")")) phrasePart = phrasePart.substring(0, phrasePart.length - 1).trim()
                    parseDateIntoControls(datePart, calInt, dInt, mInt, yInt, bcInt)
                    phraseInt.text = phrasePart
                } else {
                    parseDateIntoControls(rest, calInt, dInt, mInt, yInt, bcInt)
                }
                return
            }
            if (s.startsWith("ABT ") || s.startsWith("CAL ") || s.startsWith("EST ")) {
                rbApprox.isSelected = true
                val typeStr = s.substring(0, 3)
                approxType.selectionModel.select(typeStr)
                val rest = s.substring(4).trim()
                parseDateIntoControls(rest, calAbt, dAbt, mAbt, yAbt, bcAbt)
                return
            }
            if (s.startsWith("BET ") || s.startsWith("BEF ") || s.startsWith("AFT ")) {
                val andIndex = s.indexOf(" AND ")
                if (andIndex > 0) {
                    rbRange.isSelected = true
                    val typeStr = s.substring(0, 3)
                    rangeType.selectionModel.select(typeStr)
                    val leftPart = s.substring(4, andIndex).trim()
                    val rightPart = s.substring(andIndex + 5).trim()
                    parseDateIntoControls(leftPart, calBet, dBet, mBet, yBet, bcBet)
                    parseDateIntoControls(rightPart, calAnd, dAnd, mAnd, yAnd, bcAnd)
                    return
                }
            }
            val hasFrom = s.startsWith("FROM ")
            val toIndex = s.indexOf(" TO ")
            if (hasFrom || toIndex > 0) {
                rbPeriod.isSelected = true
                if (hasFrom) {
                    fromChecked.isSelected = true
                    if (toIndex > 0) {
                        val fromPart = s.substring(5, toIndex).trim()
                        val toPart = s.substring(toIndex + 4).trim()
                        toChecked.isSelected = true
                        parseDateIntoControls(fromPart, calFrom, dFrom, mFrom, yFrom, bcFrom)
                        parseDateIntoControls(toPart, calTo, dTo, mTo, yTo, bcTo)
                    } else {
                        val fromPart = s.substring(5).trim()
                        toChecked.isSelected = false
                        parseDateIntoControls(fromPart, calFrom, dFrom, mFrom, yFrom, bcFrom)
                    }
                } else {
                    fromChecked.isSelected = false
                    toChecked.isSelected = true
                    val toPart = s.substring(toIndex + 4).trim()
                    parseDateIntoControls(toPart, calTo, dTo, mTo, yTo, bcTo)
                }
                return
            }
            if (s.matches(Regex(".*\\d+.*")) || containsMonthCode(s)) {
                rbExact.isSelected = true
                parseDateIntoControls(s, cal1, d1, m1, y1, bc1)
                return
            }
            rbPhrase.isSelected = true
            if (s.startsWith("(") && s.endsWith(")")) s = s.substring(1, s.length - 1).trim()
            phrase.text = s
        }
    }

    fun show(initial: String?): Optional<String> {
        val stage = Stage()
        stage.initModality(Modality.APPLICATION_MODAL)
        stage.title = "Date"

        val group = ToggleGroup()
        val rbExact = RadioButton("Exact date:"); rbExact.toggleGroup = group
        val rbPeriod = RadioButton("Date period:"); rbPeriod.toggleGroup = group
        val rbRange = RadioButton("Date range:"); rbRange.toggleGroup = group
        val rbApprox = RadioButton("Aproximated date:"); rbApprox.toggleGroup = group
        val rbInterpreted = RadioButton("Interpreted date:"); rbInterpreted.toggleGroup = group
        val rbPhrase = RadioButton("Date phrase:"); rbPhrase.toggleGroup = group

        // Exact
        val cal1 = ComboBox<Calendar>(); cal1.items.addAll(*Calendar.values()); cal1.selectionModel.select(Calendar.GREGORIAN)
        val d1 = createDayCombo()
        val m1 = ComboBox<String>(); for (m in MONTHS) m1.items.add(m); m1.selectionModel.select(0)
        val y1 = TextField()
        val bc1 = CheckBox("B.C.")
        standardize(cal1, d1, m1, y1)
        cal1.selectionModel.selectedItemProperty().addListener { _, _, newVal ->
            if (newVal == Calendar.HEBREW || newVal == Calendar.FRENCH_REV) { bc1.isSelected = false; bc1.isDisable = true } else { bc1.isDisable = false }
            updateMonthCombo(m1, newVal)
        }

        // Period FROM .. TO
        val fromChecked = CheckBox("FROM"); fromChecked.isSelected = true
        val calFrom = ComboBox<Calendar>(); calFrom.items.addAll(*Calendar.values()); calFrom.selectionModel.select(Calendar.GREGORIAN)
        val dFrom = createDayCombo()
        val mFrom = ComboBox<String>(); for (m in MONTHS) mFrom.items.add(m); mFrom.selectionModel.select(0)
        val yFrom = TextField()
        val bcFrom = CheckBox("B.C.")
        val toChecked = CheckBox("TO"); toChecked.isSelected = true
        val calTo = ComboBox<Calendar>(); calTo.items.addAll(*Calendar.values()); calTo.selectionModel.select(Calendar.GREGORIAN)
        val dTo = createDayCombo()
        val mTo = ComboBox<String>(); for (m in MONTHS) mTo.items.add(m); mTo.selectionModel.select(0)
        val yTo = TextField()
        val bcTo = CheckBox("B.C.")
        standardize(calFrom, dFrom, mFrom, yFrom)
        standardize(calTo, dTo, mTo, yTo)
        calFrom.selectionModel.selectedItemProperty().addListener { _, _, newVal ->
            if (newVal == Calendar.HEBREW || newVal == Calendar.FRENCH_REV) { bcFrom.isSelected = false; bcFrom.isDisable = true } else { bcFrom.isDisable = !fromChecked.isSelected }
            updateMonthCombo(mFrom, newVal)
        }
        calTo.selectionModel.selectedItemProperty().addListener { _, _, newVal ->
            if (newVal == Calendar.HEBREW || newVal == Calendar.FRENCH_REV) { bcTo.isSelected = false; bcTo.isDisable = true } else { bcTo.isDisable = !toChecked.isSelected }
            updateMonthCombo(mTo, newVal)
        }
        fromChecked.selectedProperty().addListener { _, _, n ->
            calFrom.isDisable = !n; dFrom.isDisable = !n; mFrom.isDisable = !n; yFrom.isDisable = !n; bcFrom.isDisable = !n
        }
        calFrom.isDisable = !fromChecked.isSelected; dFrom.isDisable = !fromChecked.isSelected; mFrom.isDisable = !fromChecked.isSelected; yFrom.isDisable = !fromChecked.isSelected; bcFrom.isDisable = !fromChecked.isSelected
        toChecked.selectedProperty().addListener { _, _, n ->
            calTo.isDisable = !n; dTo.isDisable = !n; mTo.isDisable = !n; yTo.isDisable = !n; bcTo.isDisable = !n
        }
        calTo.isDisable = !toChecked.isSelected; dTo.isDisable = !toChecked.isSelected; mTo.isDisable = !toChecked.isSelected; yTo.isDisable = !toChecked.isSelected; bcTo.isDisable = !toChecked.isSelected

        // Range BET .. AND
        val rangeType = ComboBox<String>(); rangeType.items.addAll("BET", "BEF", "AFT"); rangeType.selectionModel.select("BET"); rangeType.prefWidth = 70.0
        val calBet = ComboBox<Calendar>(); calBet.items.addAll(*Calendar.values()); calBet.selectionModel.select(Calendar.GREGORIAN)
        val dBet = createDayCombo()
        val mBet = ComboBox<String>(); for (m in MONTHS) mBet.items.add(m); mBet.selectionModel.select(0)
        val yBet = TextField()
        val bcBet = CheckBox("B.C.")
        val andLbl = Label("AND")
        val calAnd = ComboBox<Calendar>(); calAnd.items.addAll(*Calendar.values()); calAnd.selectionModel.select(Calendar.GREGORIAN)
        val dAnd = createDayCombo()
        val mAnd = ComboBox<String>(); for (m in MONTHS) mAnd.items.add(m); mAnd.selectionModel.select(0)
        val yAnd = TextField()
        val bcAnd = CheckBox("B.C.")
        standardize(calBet, dBet, mBet, yBet)
        standardize(calAnd, dAnd, mAnd, yAnd)
        calBet.selectionModel.selectedItemProperty().addListener { _, _, newVal ->
            if (newVal == Calendar.HEBREW || newVal == Calendar.FRENCH_REV) { bcBet.isSelected = false; bcBet.isDisable = true } else { bcBet.isDisable = false }
            updateMonthCombo(mBet, newVal)
        }
        calAnd.selectionModel.selectedItemProperty().addListener { _, _, newVal ->
            if (newVal == Calendar.HEBREW || newVal == Calendar.FRENCH_REV) { bcAnd.isSelected = false; bcAnd.isDisable = true } else { bcAnd.isDisable = false }
            updateMonthCombo(mAnd, newVal)
        }

        // Approx ABT/CAL/EST
        val approxType = ComboBox<String>(); approxType.items.addAll("ABT", "CAL", "EST"); approxType.selectionModel.select("ABT"); approxType.prefWidth = 70.0
        val calAbt = ComboBox<Calendar>(); calAbt.items.addAll(*Calendar.values()); calAbt.selectionModel.select(Calendar.GREGORIAN)
        val dAbt = createDayCombo()
        val mAbt = ComboBox<String>(); for (m in MONTHS) mAbt.items.add(m); mAbt.selectionModel.select(0)
        val yAbt = TextField()
        val bcAbt = CheckBox("B.C.")
        standardize(calAbt, dAbt, mAbt, yAbt)
        calAbt.selectionModel.selectedItemProperty().addListener { _, _, newVal ->
            if (newVal == Calendar.HEBREW || newVal == Calendar.FRENCH_REV) { bcAbt.isSelected = false; bcAbt.isDisable = true } else { bcAbt.isDisable = false }
            updateMonthCombo(mAbt, newVal)
        }

        // Interpreted INT (date (phrase))
        val calInt = ComboBox<Calendar>(); calInt.items.addAll(*Calendar.values()); calInt.selectionModel.select(Calendar.GREGORIAN)
        val dInt = createDayCombo()
        val mInt = ComboBox<String>(); for (m in MONTHS) mInt.items.add(m); mInt.selectionModel.select(0)
        val yInt = TextField()
        val bcInt = CheckBox("B.C.")
        val phraseInt = TextField(); phraseInt.prefColumnCount = 18
        standardize(calInt, dInt, mInt, yInt)
        calInt.selectionModel.selectedItemProperty().addListener { _, _, newVal ->
            if (newVal == Calendar.HEBREW || newVal == Calendar.FRENCH_REV) { bcInt.isSelected = false; bcInt.isDisable = true } else { bcInt.isDisable = false }
            updateMonthCombo(mInt, newVal)
        }

        // Free phrase
        val phrase = TextField()

        val grid = GridPane()
        grid.hgap = 6.0; grid.vgap = 8.0; grid.padding = Insets(10.0)

        val emptyLbl = Label("")
        setFixedWidth(emptyLbl, 70.0)
        setFixedWidth(fromChecked, 70.0)
        val intLbl = Label("INT")
        setFixedWidth(intLbl, 70.0)

        var r = 0
        grid.add(rbExact, 0, r)
        grid.add(emptyLbl, 1, r)
        grid.add(cal1, 2, r)
        grid.add(d1, 3, r)
        grid.add(m1, 4, r)
        grid.add(y1, 5, r)
        grid.add(bc1, 6, r++)

        grid.add(rbPeriod, 0, r)
        grid.add(fromChecked, 1, r)
        grid.add(calFrom, 2, r)
        grid.add(dFrom, 3, r)
        grid.add(mFrom, 4, r)
        grid.add(yFrom, 5, r)
        grid.add(bcFrom, 6, r)
        grid.add(toChecked, 7, r)
        grid.add(calTo, 8, r)
        grid.add(dTo, 9, r)
        grid.add(mTo, 10, r)
        grid.add(yTo, 11, r)
        grid.add(bcTo, 12, r++)

        grid.add(rbRange, 0, r)
        grid.add(rangeType, 1, r)
        grid.add(calBet, 2, r)
        grid.add(dBet, 3, r)
        grid.add(mBet, 4, r)
        grid.add(yBet, 5, r)
        grid.add(bcBet, 6, r)
        grid.add(Label("AND"), 7, r)
        grid.add(calAnd, 8, r)
        grid.add(dAnd, 9, r)
        grid.add(mAnd, 10, r)
        grid.add(yAnd, 11, r)
        grid.add(bcAnd, 12, r++)

        grid.add(rbApprox, 0, r)
        grid.add(approxType, 1, r)
        grid.add(calAbt, 2, r)
        grid.add(dAbt, 3, r)
        grid.add(mAbt, 4, r)
        grid.add(yAbt, 5, r)
        grid.add(bcAbt, 6, r++)

        grid.add(rbInterpreted, 0, r)
        grid.add(intLbl, 1, r)
        grid.add(calInt, 2, r)
        grid.add(dInt, 3, r)
        grid.add(mInt, 4, r)
        grid.add(yInt, 5, r)
        grid.add(bcInt, 6, r)
        val phraseBox = HBox(4.0, Label("("), phraseInt, Label(")"))
        HBox.setHgrow(phraseInt, Priority.ALWAYS)
        GridPane.setColumnSpan(phraseBox, 6)
        grid.add(phraseBox, 7, r++)

        grid.add(rbPhrase, 0, r)
        val phraseBoxLast = HBox(4.0, Label("("), phrase, Label(")"))
        HBox.setHgrow(phrase, Priority.ALWAYS)
        GridPane.setColumnSpan(phraseBoxLast, 12)
        grid.add(phraseBoxLast, 1, r++)

        val btnOk = Button("OK")
        val btnCancel = Button("Cancel")
        val actions = HBox(8.0, btnOk, btnCancel)
        grid.add(actions, 1, r)

        if (!initial.isNullOrBlank()) {
            parseAndPopulate(initial,
                rbExact, rbPeriod, rbRange, rbApprox, rbInterpreted, rbPhrase,
                cal1, d1, m1, y1, bc1,
                fromChecked, calFrom, dFrom, mFrom, yFrom, bcFrom, toChecked, calTo, dTo, mTo, yTo, bcTo,
                rangeType, calBet, dBet, mBet, yBet, bcBet, calAnd, dAnd, mAnd, yAnd, bcAnd,
                approxType, calAbt, dAbt, mAbt, yAbt, bcAbt,
                calInt, dInt, mInt, yInt, bcInt, phraseInt,
                phrase)
        } else {
            rbExact.isSelected = true
        }

        val result = arrayOfNulls<String>(1)
        btnOk.setOnAction {
            val sel = group.selectedToggle as RadioButton
            when (sel) {
                rbExact -> result[0] = buildExact(cal1, d1, m1, y1, bc1)
                rbPeriod -> {
                    val left = buildExact(calFrom, dFrom, mFrom, yFrom, bcFrom)
                    val right = if (toChecked.isSelected) buildExact(calTo, dTo, mTo, yTo, bcTo) else null
                    if (left == null && right == null) { showWarn(); return@setOnAction }
                    val prefix = if (fromChecked.isSelected) "FROM " else ""
                    val middle = if (toChecked.isSelected && right != null) " TO $right" else ""
                    result[0] = (prefix + (left ?: "") + middle).trim()
                }
                rbRange -> {
                    val left = buildExact(calBet, dBet, mBet, yBet, bcBet)
                    val right = buildExact(calAnd, dAnd, mAnd, yAnd, bcAnd)
                    if (left == null || right == null) { showWarn(); return@setOnAction }
                    result[0] = rangeType.value + " " + left + " AND " + right
                }
                rbApprox -> {
                    val base = buildExact(calAbt, dAbt, mAbt, yAbt, bcAbt)
                    if (base == null) { showWarn(); return@setOnAction }
                    result[0] = approxType.value + ' ' + base
                }
                rbInterpreted -> {
                    val base = buildExact(calInt, dInt, mInt, yInt, bcInt)
                    if (base == null) { showWarn(); return@setOnAction }
                    val phr = phraseInt.text?.trim() ?: ""
                    if (phr.isEmpty()) { showWarn(); return@setOnAction }
                    result[0] = "INT $base ($phr)"
                }
                else -> {
                    val phr = phrase.text?.trim() ?: ""
                    if (phr.isEmpty()) { showWarn(); return@setOnAction }
                    result[0] = "($phr)"
                }
            }
            stage.close()
        }
        btnCancel.setOnAction { stage.close() }

        stage.scene = Scene(grid)
        stage.showAndWait()
        return Optional.ofNullable(result[0])
    }
}
