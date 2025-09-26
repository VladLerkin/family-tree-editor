package com.pedigree.gedcom;

import com.pedigree.model.Family;
import com.pedigree.model.Gender;
import com.pedigree.model.Individual;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GedcomMapper {
    public static Map<String, String> buildIndividualXrefs(List<Individual> individuals) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < individuals.size(); i++) {
            map.put(individuals.get(i).getId(), "@I" + (i + 1) + "@");
        }
        return map;
    }

    public static Map<String, String> buildFamilyXrefs(List<Family> families) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < families.size(); i++) {
            map.put(families.get(i).getId(), "@F" + (i + 1) + "@");
        }
        return map;
    }

    public static String buildName(Individual ind) {
        String first = safe(ind.getFirstName());
        String last = safe(ind.getLastName());
        // GEDCOM NAME format: Given /Surname/
        return (first + " /" + last + "/").trim();
    }

    public static String sexCode(Gender gender) {
        if (gender == null) return "U";
        return switch (gender) {
            case MALE -> "M";
            case FEMALE -> "F";
            default -> "U";
        };
    }

    public static Gender parseSex(String code) {
        if (code == null) return Gender.UNKNOWN;
        return switch (code.trim().toUpperCase(Locale.ROOT)) {
            case "M" -> Gender.MALE;
            case "F" -> Gender.FEMALE;
            default -> Gender.UNKNOWN;
        };
    }

    public static String formatDate(LocalDate date) {
        if (date == null) return null;
        // Use day month-abbrev year, e.g., 5 JAN 1980 per GEDCOM 5.5
        String mon = date.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toUpperCase(Locale.ROOT);
        // Ensure 3-letter month abbreviations
        if (mon.length() > 3) mon = mon.substring(0, 3);
        return date.getDayOfMonth() + " " + mon + " " + date.getYear();
    }

    public static LocalDate parseDate(String text) {
        if (text == null) return null;
        String s = text.trim().toUpperCase(Locale.ROOT);
        // Expect formats like: 5 JAN 1980, 01 JAN 1980, JAN 1980, 1980
        String[] parts = s.split("\\s+");
        try {
            if (parts.length == 3) {
                int day = Integer.parseInt(parts[0]);
                Month m = parseMonth(parts[1]);
                int year = Integer.parseInt(parts[2]);
                if (m != null) return LocalDate.of(year, m, Math.min(Math.max(1, day), 28));
            } else if (parts.length == 2) {
                Month m = parseMonth(parts[0]);
                int year = Integer.parseInt(parts[1]);
                if (m != null) return LocalDate.of(year, m, 1);
            } else if (parts.length == 1) {
                int year = Integer.parseInt(parts[0]);
                return LocalDate.of(year, 1, 1);
            }
        } catch (Exception ignore) {
            return null;
        }
        return null;
    }

    private static Month parseMonth(String mon) {
        if (mon == null) return null;
        String m = mon.trim().toUpperCase(Locale.ROOT);
        // Accept 3-letter English abbreviations
        return switch (m) {
            case "JAN" -> Month.JANUARY;
            case "FEB" -> Month.FEBRUARY;
            case "MAR" -> Month.MARCH;
            case "APR" -> Month.APRIL;
            case "MAY" -> Month.MAY;
            case "JUN" -> Month.JUNE;
            case "JUL" -> Month.JULY;
            case "AUG" -> Month.AUGUST;
            case "SEP", "SEPT" -> Month.SEPTEMBER;
            case "OCT" -> Month.OCTOBER;
            case "NOV" -> Month.NOVEMBER;
            case "DEC" -> Month.DECEMBER;
            default -> null;
        };
    }

    public static String safe(String s) {
        return s == null ? "" : s;
    }

    public static String[] parseName(String nameLine) {
        // Returns [given, surname]
        if (nameLine == null) return new String[]{"", ""};
        String s = nameLine.trim();
        int firstSlash = s.indexOf('/');
        int secondSlash = (firstSlash >= 0) ? s.indexOf('/', firstSlash + 1) : -1;
        String given = s;
        String surname = "";
        if (firstSlash >= 0 && secondSlash > firstSlash) {
            given = s.substring(0, firstSlash).trim();
            surname = s.substring(firstSlash + 1, secondSlash).trim();
        }
        return new String[]{given, surname};
    }
}


