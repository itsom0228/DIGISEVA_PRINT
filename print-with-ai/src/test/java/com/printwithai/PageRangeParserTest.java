package com.printwithai;

import com.printwithai.exception.InvalidFileException;
import com.printwithai.util.PageRangeParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PageRangeParserTest {

    @Test
    @DisplayName("Parse 'ALL' keyword or null expression")
    void testParseAllPages() {
        List<Integer> pages = PageRangeParser.parse("ALL", 5);
        assertEquals(List.of(1, 2, 3, 4, 5), pages);

        List<Integer> pagesNull = PageRangeParser.parse(null, 3);
        assertEquals(List.of(1, 2, 3), pagesNull);
    }

    @Test
    @DisplayName("Parse valid complex range expression: '1-3, 5, 8-10'")
    void testParseComplexRange() {
        List<Integer> pages = PageRangeParser.parse("1-3, 5, 8-10", 12);
        assertEquals(List.of(1, 2, 3, 5, 8, 9, 10), pages);
    }

    @Test
    @DisplayName("Deduplicate and sort overlapping ranges: '3, 1-3, 2'")
    void testDeduplicateAndSort() {
        List<Integer> pages = PageRangeParser.parse("3, 1-3, 2", 5);
        assertEquals(List.of(1, 2, 3), pages);
    }

    @Test
    @DisplayName("Reject invalid range syntax: start > end (e.g. '5-2')")
    void testStartGreaterThanEnd() {
        Exception exception = assertThrows(InvalidFileException.class, () ->
                PageRangeParser.parse("5-2", 10)
        );
        assertTrue(exception.getMessage().contains("start cannot be greater than end"));
    }

    @Test
    @DisplayName("Reject page number exceeding total document pages")
    void testPageNumberExceedsTotal() {
        Exception exception = assertThrows(InvalidFileException.class, () ->
                PageRangeParser.parse("1-6", 5)
        );
        assertTrue(exception.getMessage().contains("exceeds total document pages"));
    }

    @Test
    @DisplayName("Reject non-numeric input: 'abc'")
    void testNonNumericInput() {
        assertThrows(InvalidFileException.class, () ->
                PageRangeParser.parse("1, two, 3", 5)
        );
    }

    @Test
    @DisplayName("Reject 0 or negative page numbers")
    void testZeroOrNegativePageNumbers() {
        assertThrows(InvalidFileException.class, () ->
                PageRangeParser.parse("0-3", 5)
        );
    }
}
