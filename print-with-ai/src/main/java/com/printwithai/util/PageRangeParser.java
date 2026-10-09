package com.printwithai.util;

import com.printwithai.exception.InvalidFileException;

import java.util.*;

public class PageRangeParser {

    /**
     * Parses page range expression like "1-3, 5, 8-10" or "ALL"
     * against total document pages.
     *
     * @param expression User entered range or "ALL"
     * @param totalPages Actual total pages in the document
     * @return Sorted list of unique 1-based page numbers
     */
    public static List<Integer> parse(String expression, int totalPages) {
        if (totalPages <= 0) {
            throw new InvalidFileException("Total document pages must be greater than zero.");
        }

        if (expression == null || expression.trim().isEmpty() || expression.equalsIgnoreCase("ALL")) {
            List<Integer> allPages = new ArrayList<>(totalPages);
            for (int i = 1; i <= totalPages; i++) {
                allPages.add(i);
            }
            return allPages;
        }

        Set<Integer> selectedPages = new TreeSet<>();
        String[] parts = expression.split(",");

        for (String rawPart : parts) {
            String part = rawPart.trim();
            if (part.isEmpty()) {
                continue;
            }

            if (part.contains("-")) {
                String[] rangeBounds = part.split("-");
                if (rangeBounds.length != 2) {
                    throw new InvalidFileException("Invalid page range syntax: '" + part + "'. Expected format: start-end (e.g., 1-5).");
                }
                int start;
                int end;
                try {
                    start = Integer.parseInt(rangeBounds[0].trim());
                    end = Integer.parseInt(rangeBounds[1].trim());
                } catch (NumberFormatException e) {
                    throw new InvalidFileException("Page numbers must be integers in range: '" + part + "'.");
                }

                if (start <= 0 || end <= 0) {
                    throw new InvalidFileException("Page numbers must be greater than zero: '" + part + "'.");
                }
                if (start > end) {
                    throw new InvalidFileException("Range start cannot be greater than end: '" + part + "'.");
                }
                if (end > totalPages) {
                    throw new InvalidFileException("Selected page " + end + " exceeds total document pages (" + totalPages + ").");
                }

                for (int page = start; page <= end; page++) {
                    selectedPages.add(page);
                }
            } else {
                int singlePage;
                try {
                    singlePage = Integer.parseInt(part);
                } catch (NumberFormatException e) {
                    throw new InvalidFileException("Invalid page number: '" + part + "'.");
                }

                if (singlePage <= 0) {
                    throw new InvalidFileException("Page number must be greater than zero: " + singlePage);
                }
                if (singlePage > totalPages) {
                    throw new InvalidFileException("Selected page " + singlePage + " exceeds total document pages (" + totalPages + ").");
                }
                selectedPages.add(singlePage);
            }
        }

        if (selectedPages.isEmpty()) {
            throw new InvalidFileException("No valid pages selected from range: '" + expression + "'.");
        }

        return new ArrayList<>(selectedPages);
    }

    public static String toCanonicalString(List<Integer> pages, int totalPages) {
        if (pages == null || pages.isEmpty()) {
            return "ALL";
        }
        if (pages.size() == totalPages) {
            return "ALL";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pages.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(pages.get(i));
        }
        return sb.toString();
    }
}
