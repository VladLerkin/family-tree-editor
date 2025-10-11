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

    private static ComboBox<String> createDayCombo() {
        ComboBox<String> cb = new ComboBox<>();
        cb.getItems().add("");
        for (int i = 1; i <= 31; i++) cb.getItems().add(String.valueOf(i));
        cb.getSelectionModel().select(0);
        cb.setPrefWidth(65);
        return cb;
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

        // Approx ABT
        ComboBox<String> approxType = new ComboBox<>(); approxType.getItems().addAll("ABT", "EST", "CAL"); approxType.getSelectionModel().select("ABT");
        approxType.setPrefWidth(70);
        ComboBox<Calendar> calAbt = new ComboBox<>(); calAbt.getItems().addAll(Calendar.values()); calAbt.getSelectionModel().select(Calendar.GREGORIAN);
        ComboBox<String> dAbt = createDayCombo();
        ComboBox<String> mAbt = new ComboBox<>(); for (String m: MONTHS) mAbt.getItems().add(m); mAbt.getSelectionModel().select(0);
        TextField yAbt = new TextField();
        CheckBox bcAbt = new CheckBox("B.C.");
        standardize(calAbt, dAbt, mAbt, yAbt);

        // Interpreted INT (date (phrase))
        ComboBox<Calendar> calInt = new ComboBox<>(); calInt.getItems().addAll(Calendar.values()); calInt.getSelectionModel().select(Calendar.GREGORIAN);
        ComboBox<String> dInt = createDayCombo();
        ComboBox<String> mInt = new ComboBox<>(); for (String m: MONTHS) mInt.getItems().add(m); mInt.getSelectionModel().select(0);
        TextField yInt = new TextField();
        CheckBox bcInt = new CheckBox("B.C.");
        TextField phraseInt = new TextField(); phraseInt.setPrefColumnCount(18);
        standardize(calInt, dInt, mInt, yInt);

        // Free phrase
        TextField phrase = new TextField();

        GridPane grid = new GridPane();
        grid.setHgap(6); grid.setVgap(8); grid.setPadding(new Insets(10));

        // Create fixed-width labels and controls for column 1 to ensure calendar alignment
        Label emptyLbl = new Label("");
        setFixedWidth(emptyLbl, 70);
        setFixedWidth(fromChecked, 70);
        Label betLbl = new Label("BET");
        setFixedWidth(betLbl, 70);
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
        grid.add(betLbl, 1, r);
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

        // Pre-fill simple initial phrase if provided
        if (initial != null && !initial.isBlank()) {
            phrase.setText(initial);
            rbPhrase.setSelected(true);
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
                result[0] = "BET " + left + " AND " + right;
            } else if (sel == rbApprox) {
                String base = buildExact(calAbt, dAbt, mAbt, yAbt, bcAbt);
                if (base == null) { showWarn(); return; }
                result[0] = approxType.getValue() + ' ' + base;
            } else if (sel == rbInterpreted) {
                String base = buildExact(calInt, dInt, mInt, yInt, bcInt);
                if (base == null) { showWarn(); return; }
                String phr = phraseInt.getText() != null ? phraseInt.getText().trim() : "";
                result[0] = "INT " + base + (phr.isEmpty() ? "" : " (" + phr + ")");
            } else {
                String phr = phrase.getText() != null ? phrase.getText().trim() : "";
                if (phr.isEmpty()) { showWarn(); return; }
                result[0] = phr;
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
            sb.append(cal.getValue().label).append(' ');
        }
        if (!day.isEmpty()) sb.append(day).append(' ');
        if (month != null && !month.isEmpty()) sb.append(month).append(' ');
        if (year.isEmpty() && sb.length()==0) return null;
        if (!year.isEmpty()) sb.append(year);
        if (bc.isSelected()) sb.append(" B.C.");
        return sb.toString().trim();
    }

}
