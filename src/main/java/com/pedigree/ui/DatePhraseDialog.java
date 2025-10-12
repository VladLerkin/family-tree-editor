package com.pedigree.ui;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Optional;

/**
 * Simple date phrase builder dialog inspired by the provided screenshots.
 * It does not implement calendar conversions; it only composes a textual phrase
 * that can be stored in the model (GEDCOM-like).
 */
public class DatePhraseDialog {

    public enum Calendar {
        GREGORIAN("Gregorian"), JULIAN("Julian"), HEBREW("Hebrew"), FRENCH_REV("French Rev."), UNKNOWN("Unknown");
        final String label; Calendar(String l){this.label=l;}
        @Override public String toString(){return label;}
    }

    private static final String[] MONTHS = {"", "JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"};
    private static final String[] HEBREW_MONTHS = {"", "TSH", "CSH", "KSL", "TVT", "SHV", "ADR", "ADS", "NSN", "IYR", "SVN", "TMZ", "AAV"};
    private static final String[] FRENCH_REV_MONTHS = {"", "VEND", "BRUM", "FRIM", "NIVO", "PLUV", "VENT", "GERM", "FLOR", "PRAI", "MESS", "THER", "FRUC"};

    private static ComboBox<String> createDayCombo() {
        ComboBox<String> cb = new ComboBox<>();
        cb.getItems().add("");
        for (int i = 1; i <= 31; i++) cb.getItems().add(String.valueOf(i));
        cb.getSelectionModel().select(0);
        cb.setPrefWidth(65);
        return cb;
    }

    private static void updateMonthCombo(ComboBox<String> monthCombo, Calendar calendar) {
        String currentSelection = monthCombo.getSelectionModel().getSelectedItem();
        monthCombo.getItems().clear();
        String[] monthArray = switch (calendar) {
            case HEBREW -> HEBREW_MONTHS;
            case FRENCH_REV -> FRENCH_REV_MONTHS;
            default -> MONTHS;
        };
        for (String m : monthArray) {
            monthCombo.getItems().add(m);
        }
        // Try to preserve selection if it exists in the new list
        if (currentSelection != null && monthCombo.getItems().contains(currentSelection)) {
            monthCombo.getSelectionModel().select(currentSelection);
        } else {
            monthCombo.getSelectionModel().select(0);
        }
    }

    private static <T> void standardize(ComboBox<T> cal, ComboBox<String> day, ComboBox<String> month, TextField year) {
        if (cal != null) cal.setPrefWidth(120);
        if (day != null) day.setPrefWidth(65);
        if (month != null) month.setPrefWidth(85);
        if (year != null) year.setPrefColumnCount(5);
    }

    private static void setFixedWidth(javafx.scene.Node node, double width) {
        if (node != null) {
            node.setStyle("-fx-pref-width: " + width + "px; -fx-min-width: " + width + "px; -fx-max-width: " + width + "px;");
        }
    }

    public Optional<String> show(String initial) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Date");

        ToggleGroup group = new ToggleGroup();
        RadioButton rbExact = new RadioButton("Exact date:"); rbExact.setToggleGroup(group);
        RadioButton rbPeriod = new RadioButton("Date period:"); rbPeriod.setToggleGroup(group);
        RadioButton rbRange = new RadioButton("Date range:"); rbRange.setToggleGroup(group);
        RadioButton rbApprox = new RadioButton("Aproximated date:"); rbApprox.setToggleGroup(group);
        RadioButton rbInterpreted = new RadioButton("Interpreted date:"); rbInterpreted.setToggleGroup(group);
        RadioButton rbPhrase = new RadioButton("Date phrase:"); rbPhrase.setToggleGroup(group);

        // Exact
        ComboBox<Calendar> cal1 = new ComboBox<>(); cal1.getItems().addAll(Calendar.values()); cal1.getSelectionModel().select(Calendar.GREGORIAN);
        ComboBox<String> d1 = createDayCombo();
        ComboBox<String> m1 = new ComboBox<>(); for (String m: MONTHS) m1.getItems().add(m); m1.getSelectionModel().select(0);
        TextField y1 = new TextField();
        CheckBox bc1 = new CheckBox("B.C.");
        standardize(cal1, d1, m1, y1);
        // Disable B.C. for HEBREW and FRENCH_REV calendars, and update month names
        cal1.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == Calendar.HEBREW || newVal == Calendar.FRENCH_REV) {
                bc1.setSelected(false);
                bc1.setDisable(true);
            } else {
                bc1.setDisable(false);
            }
            updateMonthCombo(m1, newVal);
        });

        // Period FROM .. TO
        CheckBox fromChecked = new CheckBox("FROM"); fromChecked.setSelected(true);
        ComboBox<Calendar> calFrom = new ComboBox<>(); calFrom.getItems().addAll(Calendar.values()); calFrom.getSelectionModel().select(Calendar.GREGORIAN);
        ComboBox<String> dFrom = createDayCombo();
        ComboBox<String> mFrom = new ComboBox<>(); for (String m: MONTHS) mFrom.getItems().add(m); mFrom.getSelectionModel().select(0);
        TextField yFrom = new TextField();
        CheckBox bcFrom = new CheckBox("B.C.");
        CheckBox toChecked = new CheckBox("TO"); toChecked.setSelected(true);
        ComboBox<Calendar> calTo = new ComboBox<>(); calTo.getItems().addAll(Calendar.values()); calTo.getSelectionModel().select(Calendar.GREGORIAN);
        ComboBox<String> dTo = createDayCombo();
        ComboBox<String> mTo = new ComboBox<>(); for (String m: MONTHS) mTo.getItems().add(m); mTo.getSelectionModel().select(0);
        TextField yTo = new TextField();
        CheckBox bcTo = new CheckBox("B.C.");
        standardize(calFrom, dFrom, mFrom, yFrom);
        standardize(calTo, dTo, mTo, yTo);
        // Disable B.C. for HEBREW and FRENCH_REV calendars, and update month names
        calFrom.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == Calendar.HEBREW || newVal == Calendar.FRENCH_REV) {
                bcFrom.setSelected(false);
                bcFrom.setDisable(true);
            } else {
                bcFrom.setDisable(!fromChecked.isSelected());
            }
            updateMonthCombo(mFrom, newVal);
        });
        calTo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == Calendar.HEBREW || newVal == Calendar.FRENCH_REV) {
                bcTo.setSelected(false);
                bcTo.setDisable(true);
            } else {
                bcTo.setDisable(!toChecked.isSelected());
            }
            updateMonthCombo(mTo, newVal);
        });

        // Enable/disable left-side FROM fields based on checkbox
        fromChecked.selectedProperty().addListener((obs, o, n) -> {
            calFrom.setDisable(!n);
            dFrom.setDisable(!n);
            mFrom.setDisable(!n);
            yFrom.setDisable(!n);
            bcFrom.setDisable(!n);
        });
        // Set initial state for FROM fields
        calFrom.setDisable(!fromChecked.isSelected());
        dFrom.setDisable(!fromChecked.isSelected());
        mFrom.setDisable(!fromChecked.isSelected());
        yFrom.setDisable(!fromChecked.isSelected());
        bcFrom.setDisable(!fromChecked.isSelected());

        // Enable/disable right-side TO fields based on checkbox
        toChecked.selectedProperty().addListener((obs, o, n) -> {
            calTo.setDisable(!n);
            dTo.setDisable(!n);
            mTo.setDisable(!n);
            yTo.setDisable(!n);
            bcTo.setDisable(!n);
        });
        // Set initial state for TO fields
        calTo.setDisable(!toChecked.isSelected());
        dTo.setDisable(!toChecked.isSelected());
        mTo.setDisable(!toChecked.isSelected());
        yTo.setDisable(!toChecked.isSelected());
        bcTo.setDisable(!toChecked.isSelected());

        // Range BET .. AND
        ComboBox<String> rangeType = new ComboBox<>(); rangeType.getItems().addAll("BET", "BEF", "AFT"); rangeType.getSelectionModel().select("BET");
        rangeType.setPrefWidth(70);
        ComboBox<Calendar> calBet = new ComboBox<>(); calBet.getItems().addAll(Calendar.values()); calBet.getSelectionModel().select(Calendar.GREGORIAN);
        ComboBox<String> dBet = createDayCombo();
        ComboBox<String> mBet = new ComboBox<>(); for (String m: MONTHS) mBet.getItems().add(m); mBet.getSelectionModel().select(0);
        TextField yBet = new TextField();
        CheckBox bcBet = new CheckBox("B.C.");
        Label andLbl = new Label("AND");
        ComboBox<Calendar> calAnd = new ComboBox<>(); calAnd.getItems().addAll(Calendar.values()); calAnd.getSelectionModel().select(Calendar.GREGORIAN);
        ComboBox<String> dAnd = createDayCombo();
        ComboBox<String> mAnd = new ComboBox<>(); for (String m: MONTHS) mAnd.getItems().add(m); mAnd.getSelectionModel().select(0);
        TextField yAnd = new TextField();
        CheckBox bcAnd = new CheckBox("B.C.");
        standardize(calBet, dBet, mBet, yBet);
        standardize(calAnd, dAnd, mAnd, yAnd);
        // Disable B.C. for HEBREW and FRENCH_REV calendars, and update month names
        calBet.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == Calendar.HEBREW || newVal == Calendar.FRENCH_REV) {
                bcBet.setSelected(false);
                bcBet.setDisable(true);
            } else {
                bcBet.setDisable(false);
            }
            updateMonthCombo(mBet, newVal);
        });
        calAnd.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == Calendar.HEBREW || newVal == Calendar.FRENCH_REV) {
                bcAnd.setSelected(false);
                bcAnd.setDisable(true);
            } else {
                bcAnd.setDisable(false);
            }
            updateMonthCombo(mAnd, newVal);
        });

        // Approx ABT
        ComboBox<String> approxType = new ComboBox<>(); approxType.getItems().addAll("ABT", "CAL", "EST"); approxType.getSelectionModel().select("ABT");
        approxType.setPrefWidth(70);
        ComboBox<Calendar> calAbt = new ComboBox<>(); calAbt.getItems().addAll(Calendar.values()); calAbt.getSelectionModel().select(Calendar.GREGORIAN);
        ComboBox<String> dAbt = createDayCombo();
        ComboBox<String> mAbt = new ComboBox<>(); for (String m: MONTHS) mAbt.getItems().add(m); mAbt.getSelectionModel().select(0);
        TextField yAbt = new TextField();
        CheckBox bcAbt = new CheckBox("B.C.");
        standardize(calAbt, dAbt, mAbt, yAbt);
        // Disable B.C. for HEBREW and FRENCH_REV calendars, and update month names
        calAbt.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == Calendar.HEBREW || newVal == Calendar.FRENCH_REV) {
                bcAbt.setSelected(false);
                bcAbt.setDisable(true);
            } else {
                bcAbt.setDisable(false);
            }
            updateMonthCombo(mAbt, newVal);
        });

        // Interpreted INT (date (phrase))
        ComboBox<Calendar> calInt = new ComboBox<>(); calInt.getItems().addAll(Calendar.values()); calInt.getSelectionModel().select(Calendar.GREGORIAN);
        ComboBox<String> dInt = createDayCombo();
        ComboBox<String> mInt = new ComboBox<>(); for (String m: MONTHS) mInt.getItems().add(m); mInt.getSelectionModel().select(0);
        TextField yInt = new TextField();
        CheckBox bcInt = new CheckBox("B.C.");
        TextField phraseInt = new TextField(); phraseInt.setPrefColumnCount(18);
        standardize(calInt, dInt, mInt, yInt);
        // Disable B.C. for HEBREW and FRENCH_REV calendars, and update month names
        calInt.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == Calendar.HEBREW || newVal == Calendar.FRENCH_REV) {
                bcInt.setSelected(false);
                bcInt.setDisable(true);
            } else {
                bcInt.setDisable(false);
            }
            updateMonthCombo(mInt, newVal);
        });

        // Free phrase
        TextField phrase = new TextField();

        GridPane grid = new GridPane();
        grid.setHgap(6); grid.setVgap(8); grid.setPadding(new Insets(10));

        // Create fixed-width labels and controls for column 1 to ensure calendar alignment
        Label emptyLbl = new Label("");
        setFixedWidth(emptyLbl, 70);
        setFixedWidth(fromChecked, 70);
        Label intLbl = new Label("INT");
        setFixedWidth(intLbl, 70);

        int r=0;
        // Exact date row: radio button in col 0, empty space in col 1 for alignment, then calendar in col 2
        grid.add(rbExact, 0, r);
        grid.add(emptyLbl, 1, r);
        grid.add(cal1, 2, r);
        grid.add(d1, 3, r);
        grid.add(m1, 4, r);
        grid.add(y1, 5, r);
        grid.add(bc1, 6, r++);
        
        // Period row: FROM in col 1, calendar in col 2, then date fields; TO in col 7, calendar in col 8, date fields
        grid.add(rbPeriod, 0, r);
        grid.add(fromChecked, 1, r);
        grid.add(calFrom, 2, r);
        grid.add(dFrom, 3, r);
        grid.add(mFrom, 4, r);
        grid.add(yFrom, 5, r);
        grid.add(bcFrom, 6, r);
        grid.add(toChecked, 7, r);
        grid.add(calTo, 8, r);
        grid.add(dTo, 9, r);
        grid.add(mTo, 10, r);
        grid.add(yTo, 11, r);
        grid.add(bcTo, 12, r++);
        
        // Range row: BET in col 1, calendar in col 2 (aligned with FROM row calendar)
        grid.add(rbRange, 0, r);
        grid.add(rangeType, 1, r);
        grid.add(calBet, 2, r);
        grid.add(dBet, 3, r);
        grid.add(mBet, 4, r);
        grid.add(yBet, 5, r);
        grid.add(bcBet, 6, r);
        grid.add(andLbl, 7, r);
        grid.add(calAnd, 8, r);
        grid.add(dAnd, 9, r);
        grid.add(mAnd, 10, r);
        grid.add(yAnd, 11, r);
        grid.add(bcAnd, 12, r++);
        
        // Approximated row: ABT type in col 1, calendar in col 2 (aligned)
        grid.add(rbApprox, 0, r);
        grid.add(approxType, 1, r);
        grid.add(calAbt, 2, r);
        grid.add(dAbt, 3, r);
        grid.add(mAbt, 4, r);
        grid.add(yAbt, 5, r);
        grid.add(bcAbt, 6, r++);
        
        // Interpreted row: INT in col 1, calendar in col 2 (aligned)
        grid.add(rbInterpreted, 0, r);
        grid.add(intLbl, 1, r);
        grid.add(calInt, 2, r);
        grid.add(dInt, 3, r);
        grid.add(mInt, 4, r);
        grid.add(yInt, 5, r);
        grid.add(bcInt, 6, r);
        HBox phraseBox = new HBox(4, new Label("("), phraseInt, new Label(")"));
        HBox.setHgrow(phraseInt, Priority.ALWAYS);
        GridPane.setColumnSpan(phraseBox, 6);
        grid.add(phraseBox, 7, r++);
        
        // Phrase row: text field spanning multiple columns
        grid.add(rbPhrase, 0, r);
        HBox phraseBoxLast = new HBox(4, new Label("("), phrase, new Label(")"));
        HBox.setHgrow(phrase, Priority.ALWAYS);
        GridPane.setColumnSpan(phraseBoxLast, 12);
        grid.add(phraseBoxLast, 1, r++);

        Button btnOk = new Button("OK");
        Button btnCancel = new Button("Cancel");
        HBox actions = new HBox(8, btnOk, btnCancel);
        grid.add(actions, 1, r);

        // Pre-fill based on initial date string if provided
        if (initial != null && !initial.isBlank()) {
            parseAndPopulate(initial, rbExact, rbPeriod, rbRange, rbApprox, rbInterpreted, rbPhrase,
                    cal1, d1, m1, y1, bc1,
                    fromChecked, calFrom, dFrom, mFrom, yFrom, bcFrom, toChecked, calTo, dTo, mTo, yTo, bcTo,
                    rangeType, calBet, dBet, mBet, yBet, bcBet, calAnd, dAnd, mAnd, yAnd, bcAnd,
                    approxType, calAbt, dAbt, mAbt, yAbt, bcAbt,
                    calInt, dInt, mInt, yInt, bcInt, phraseInt,
                    phrase);
        } else {
            rbExact.setSelected(true);
        }

        final String[] result = new String[1];
        btnOk.setOnAction(e -> {
            RadioButton sel = (RadioButton) group.getSelectedToggle();
            if (sel == rbExact) {
                result[0] = buildExact(cal1, d1, m1, y1, bc1);
            } else if (sel == rbPeriod) {
                String left = buildExact(calFrom, dFrom, mFrom, yFrom, bcFrom);
                String right = toChecked.isSelected() ? buildExact(calTo, dTo, mTo, yTo, bcTo) : null;
                if (left == null && right == null) { showWarn(); return; }
                String prefix = fromChecked.isSelected() ? "FROM " : "";
                String middle = (toChecked.isSelected() && right != null) ? (" TO " + right) : "";
                result[0] = (prefix + (left != null ? left : "") + middle).trim();
            } else if (sel == rbRange) {
                String left = buildExact(calBet, dBet, mBet, yBet, bcBet);
                String right = buildExact(calAnd, dAnd, mAnd, yAnd, bcAnd);
                if (left == null || right == null) { showWarn(); return; }
                result[0] = rangeType.getValue() + " " + left + " AND " + right;
            } else if (sel == rbApprox) {
                String base = buildExact(calAbt, dAbt, mAbt, yAbt, bcAbt);
                if (base == null) { showWarn(); return; }
                result[0] = approxType.getValue() + ' ' + base;
            } else if (sel == rbInterpreted) {
                String base = buildExact(calInt, dInt, mInt, yInt, bcInt);
                if (base == null) { showWarn(); return; }
                String phr = phraseInt.getText() != null ? phraseInt.getText().trim() : "";
                if (phr.isEmpty()) { showWarn(); return; }
                result[0] = "INT " + base + " (" + phr + ")";
            } else {
                String phr = phrase.getText() != null ? phrase.getText().trim() : "";
                if (phr.isEmpty()) { showWarn(); return; }
                result[0] = "(" + phr + ")";
            }
            stage.close();
        });
        btnCancel.setOnAction(e -> stage.close());

        stage.setScene(new Scene(grid));
        stage.showAndWait();
        return Optional.ofNullable(result[0]);
    }

    private static void showWarn() {
        Dialogs.showError("Date", "Please provide enough date details.");
    }

    private static String buildExact(ComboBox<Calendar> cal, ComboBox<String> d, ComboBox<String> m, TextField y, CheckBox bc) {
        String year = y.getText() != null ? y.getText().trim() : "";
        String month = m.getSelectionModel().getSelectedItem();
        String day = d.getSelectionModel().getSelectedItem();
        if (day == null) day = "";
        StringBuilder sb = new StringBuilder();
        if (cal.getValue() != null && cal.getValue() != Calendar.GREGORIAN) {
            String escapeCode = switch (cal.getValue()) {
                case HEBREW -> "@#DHEBREW@";
                case FRENCH_REV -> "@#DFRENCH R@";
                case JULIAN -> "@#DJULIAN@";
                case UNKNOWN -> "@#DUNKNOWN@";
                default -> cal.getValue().label;
            };
            sb.append(escapeCode).append(' ');
        }
        if (!day.isEmpty()) sb.append(day).append(' ');
        if (month != null && !month.isEmpty()) sb.append(month).append(' ');
        if (year.isEmpty() && sb.length()==0) return null;
        if (!year.isEmpty()) sb.append(year);
        if (bc.isSelected()) sb.append(" B.C.");
        return sb.toString().trim();
    }

    private static void parseAndPopulate(String initial,
                                         RadioButton rbExact, RadioButton rbPeriod, RadioButton rbRange, RadioButton rbApprox, RadioButton rbInterpreted, RadioButton rbPhrase,
                                         ComboBox<Calendar> cal1, ComboBox<String> d1, ComboBox<String> m1, TextField y1, CheckBox bc1,
                                         CheckBox fromChecked, ComboBox<Calendar> calFrom, ComboBox<String> dFrom, ComboBox<String> mFrom, TextField yFrom, CheckBox bcFrom,
                                         CheckBox toChecked, ComboBox<Calendar> calTo, ComboBox<String> dTo, ComboBox<String> mTo, TextField yTo, CheckBox bcTo,
                                         ComboBox<String> rangeType, ComboBox<Calendar> calBet, ComboBox<String> dBet, ComboBox<String> mBet, TextField yBet, CheckBox bcBet,
                                         ComboBox<Calendar> calAnd, ComboBox<String> dAnd, ComboBox<String> mAnd, TextField yAnd, CheckBox bcAnd,
                                         ComboBox<String> approxType, ComboBox<Calendar> calAbt, ComboBox<String> dAbt, ComboBox<String> mAbt, TextField yAbt, CheckBox bcAbt,
                                         ComboBox<Calendar> calInt, ComboBox<String> dInt, ComboBox<String> mInt, TextField yInt, CheckBox bcInt, TextField phraseInt,
                                         TextField phrase) {
        String s = initial.trim();
        
        // Check for Interpreted date: INT date (phrase)
        if (s.startsWith("INT ")) {
            rbInterpreted.setSelected(true);
            String rest = s.substring(4).trim();
            int parenIndex = rest.indexOf('(');
            if (parenIndex > 0) {
                String datePart = rest.substring(0, parenIndex).trim();
                String phrasePart = rest.substring(parenIndex + 1).trim();
                if (phrasePart.endsWith(")")) {
                    phrasePart = phrasePart.substring(0, phrasePart.length() - 1).trim();
                }
                parseDateIntoControls(datePart, calInt, dInt, mInt, yInt, bcInt);
                phraseInt.setText(phrasePart);
            } else {
                parseDateIntoControls(rest, calInt, dInt, mInt, yInt, bcInt);
            }
            return;
        }
        
        // Check for Approximated date: ABT/CAL/EST date
        if (s.startsWith("ABT ") || s.startsWith("CAL ") || s.startsWith("EST ")) {
            rbApprox.setSelected(true);
            String typeStr = s.substring(0, 3);
            approxType.getSelectionModel().select(typeStr);
            String rest = s.substring(4).trim();
            parseDateIntoControls(rest, calAbt, dAbt, mAbt, yAbt, bcAbt);
            return;
        }
        
        // Check for Range: BET/BEF/AFT date AND date
        if (s.startsWith("BET ") || s.startsWith("BEF ") || s.startsWith("AFT ")) {
            int andIndex = s.indexOf(" AND ");
            if (andIndex > 0) {
                rbRange.setSelected(true);
                String typeStr = s.substring(0, 3);
                rangeType.getSelectionModel().select(typeStr);
                String leftPart = s.substring(4, andIndex).trim();
                String rightPart = s.substring(andIndex + 5).trim();
                parseDateIntoControls(leftPart, calBet, dBet, mBet, yBet, bcBet);
                parseDateIntoControls(rightPart, calAnd, dAnd, mAnd, yAnd, bcAnd);
                return;
            }
        }
        
        // Check for Period: FROM date TO date
        boolean hasFrom = s.startsWith("FROM ");
        int toIndex = s.indexOf(" TO ");
        if (hasFrom || toIndex > 0) {
            rbPeriod.setSelected(true);
            if (hasFrom) {
                fromChecked.setSelected(true);
                String fromPart;
                if (toIndex > 0) {
                    fromPart = s.substring(5, toIndex).trim();
                    String toPart = s.substring(toIndex + 4).trim();
                    toChecked.setSelected(true);
                    parseDateIntoControls(fromPart, calFrom, dFrom, mFrom, yFrom, bcFrom);
                    parseDateIntoControls(toPart, calTo, dTo, mTo, yTo, bcTo);
                } else {
                    fromPart = s.substring(5).trim();
                    toChecked.setSelected(false);
                    parseDateIntoControls(fromPart, calFrom, dFrom, mFrom, yFrom, bcFrom);
                }
            } else {
                // Only TO part
                fromChecked.setSelected(false);
                toChecked.setSelected(true);
                String toPart = s.substring(toIndex + 4).trim();
                parseDateIntoControls(toPart, calTo, dTo, mTo, yTo, bcTo);
            }
            return;
        }
        
        // Check if it looks like a simple date (contains year or month codes)
        if (s.matches(".*\\d+.*") || containsMonthCode(s)) {
            rbExact.setSelected(true);
            parseDateIntoControls(s, cal1, d1, m1, y1, bc1);
            return;
        }
        
        // Default to phrase
        rbPhrase.setSelected(true);
        // Strip parentheses if present
        if (s.startsWith("(") && s.endsWith(")")) {
            s = s.substring(1, s.length() - 1).trim();
        }
        phrase.setText(s);
    }
    
    private static boolean containsMonthCode(String s) {
        for (String m : MONTHS) {
            if (!m.isEmpty() && s.contains(m)) return true;
        }
        for (String m : HEBREW_MONTHS) {
            if (!m.isEmpty() && s.contains(m)) return true;
        }
        for (String m : FRENCH_REV_MONTHS) {
            if (!m.isEmpty() && s.contains(m)) return true;
        }
        return false;
    }
    
    private static void parseDateIntoControls(String datePart, ComboBox<Calendar> cal, ComboBox<String> d, ComboBox<String> m, TextField y, CheckBox bc) {
        if (datePart == null || datePart.isEmpty()) return;
        
        String s = datePart.trim();
        Calendar calendar = Calendar.GREGORIAN;
        
        // Check for calendar escape codes
        if (s.startsWith("@#DHEBREW@")) {
            calendar = Calendar.HEBREW;
            s = s.substring(10).trim();
        } else if (s.startsWith("@#DFRENCH R@")) {
            calendar = Calendar.FRENCH_REV;
            s = s.substring(12).trim();
        } else if (s.startsWith("@#DJULIAN@")) {
            calendar = Calendar.JULIAN;
            s = s.substring(10).trim();
        } else if (s.startsWith("@#DUNKNOWN@")) {
            calendar = Calendar.UNKNOWN;
            s = s.substring(11).trim();
        }
        
        cal.getSelectionModel().select(calendar);
        
        // Check for B.C.
        boolean isBc = s.endsWith(" B.C.");
        if (isBc) {
            s = s.substring(0, s.length() - 5).trim();
            bc.setSelected(true);
        } else {
            bc.setSelected(false);
        }
        
        // Parse date components: day month year or month year or just year
        String[] parts = s.split("\\s+");
        String dayStr = "", monthStr = "", yearStr = "";
        
        if (parts.length == 3) {
            // day month year
            dayStr = parts[0];
            monthStr = parts[1];
            yearStr = parts[2];
        } else if (parts.length == 2) {
            // Could be day month, month year, or day year
            if (isMonthCode(parts[0])) {
                monthStr = parts[0];
                yearStr = parts[1];
            } else if (isMonthCode(parts[1])) {
                dayStr = parts[0];
                monthStr = parts[1];
            } else {
                // Assume month year
                monthStr = parts[0];
                yearStr = parts[1];
            }
        } else if (parts.length == 1) {
            // Just year or month
            if (isMonthCode(parts[0])) {
                monthStr = parts[0];
            } else {
                yearStr = parts[0];
            }
        }
        
        // Set day
        if (!dayStr.isEmpty() && dayStr.matches("\\d+")) {
            d.getSelectionModel().select(dayStr);
        } else {
            d.getSelectionModel().select(0);
        }
        
        // Set month
        if (!monthStr.isEmpty()) {
            m.getSelectionModel().select(monthStr);
        } else {
            m.getSelectionModel().select(0);
        }
        
        // Set year
        y.setText(yearStr);
    }
    
    private static boolean isMonthCode(String s) {
        for (String m : MONTHS) {
            if (!m.isEmpty() && m.equals(s)) return true;
        }
        for (String m : HEBREW_MONTHS) {
            if (!m.isEmpty() && m.equals(s)) return true;
        }
        for (String m : FRENCH_REV_MONTHS) {
            if (!m.isEmpty() && m.equals(s)) return true;
        }
        return false;
    }

}
