package com.fpt.swp.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextUtilsTest {

    @Test
    void decodesRealGarbledTitlesFromScreenshot() {
        assertEquals("Gençlik Merkezleri Faaliyetlerine",
                TextUtils.decodeIfPercentEncoded("Gen%c3%a7lik%20Merkezleri%20Faaliyetlerine"));
        assertEquals("Türkiye de Yaşayan Do",
                TextUtils.decodeIfPercentEncoded("T%c3%bcrkiye%20de%20Ya%c5%9fayan%20Do"));
    }

    @Test
    void leavesNormalTitlesUntouched() {
        String normal = "Figure 3—figure supplement 2. Results of the protein analyses.";
        assertEquals(normal, TextUtils.decodeIfPercentEncoded(normal));
        // title hợp lệ có '%' và dấu cách → không đụng
        String pct = "50% efficiency improvement in solar cells";
        assertEquals(pct, TextUtils.decodeIfPercentEncoded(pct));
        assertNull(TextUtils.decodeIfPercentEncoded(null));
    }

    @Test
    void flagsUnusableTitles() {
        assertTrue(TextUtils.isUnusableTitle(null));
        assertTrue(TextUtils.isUnusableTitle("  "));
        assertTrue(TextUtils.isUnusableTitle("ab"));                         // quá ngắn
        assertTrue(TextUtils.isUnusableTitle("Foo%c3%a7Bar%20"));            // còn URL-encode
        assertFalse(TextUtils.isUnusableTitle("Gençlik Merkezleri"));        // đã sạch
        assertFalse(TextUtils.isUnusableTitle("Attention Is All You Need"));
    }
}
