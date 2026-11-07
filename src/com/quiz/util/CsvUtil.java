package com.quiz.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal RFC-4180 style CSV reader/writer (quoted fields, embedded commas,
 * doubled quotes and line breaks inside quotes). Written by hand to show Java I/O.
 */
public final class CsvUtil {

    private CsvUtil() {
    }

    /** Reads every record from the reader. Blank lines are skipped. */
    public static List<List<String>> readAll(BufferedReader reader) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        boolean fieldStarted = false;
        int ch;
        while ((ch = reader.read()) != -1) {
            char c = (char) ch;
            if (inQuotes) {
                if (c == '"') {
                    reader.mark(1);
                    int next = reader.read();
                    if (next == '"') {
                        field.append('"');
                    } else {
                        inQuotes = false;
                        if (next != -1) reader.reset();
                    }
                } else {
                    field.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
                fieldStarted = true;
            } else if (c == ',') {
                row.add(field.toString());
                field.setLength(0);
                fieldStarted = true;
            } else if (c == '\n' || c == '\r') {
                if (c == '\r') {
                    reader.mark(1);
                    if (reader.read() != '\n') reader.reset();
                }
                if (fieldStarted || field.length() > 0 || !row.isEmpty()) {
                    row.add(field.toString());
                    rows.add(row);
                }
                row = new ArrayList<>();
                field.setLength(0);
                fieldStarted = false;
            } else {
                field.append(c);
                fieldStarted = true;
            }
        }
        if (fieldStarted || field.length() > 0 || !row.isEmpty()) {
            row.add(field.toString());
            rows.add(row);
        }
        // Strip a UTF-8 BOM if Excel added one
        if (!rows.isEmpty() && !rows.get(0).isEmpty() && rows.get(0).get(0).startsWith("﻿")) {
            rows.get(0).set(0, rows.get(0).get(0).substring(1));
        }
        return rows;
    }

    /** Formats one record as a CSV line (without the trailing newline). */
    public static String toLine(Object... values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(escape(values[i] == null ? "" : values[i].toString()));
        }
        return sb.toString();
    }

    /**
     * Neutralises spreadsheet formulas in free text (names, subjects): a cell starting with =, +, - or @
     * would otherwise be run as a formula by Excel / LibreOffice ("CSV injection"). Numbers are left alone.
     */
    public static String safeText(String s) {
        if (s == null || s.isEmpty()) return s;
        char c = s.charAt(0);
        boolean risky = c == '=' || c == '+' || c == '-' || c == '@' || c == '\t' || c == '\r';
        return risky && !s.matches("[-+]?\\d+(\\.\\d+)?") ? "'" + s : s;
    }

    public static String escape(String s) {
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return '"' + s.replace("\"", "\"\"") + '"';
        }
        return s;
    }
}
