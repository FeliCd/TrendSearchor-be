package com.fpt.swp.util;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public final class TextUtils {

    private TextUtils() {}

    private static final Pattern PERCENT_ENCODED = Pattern.compile(".*%[0-9a-fA-F]{2}.*");

    /**
     * Một số record từ nguồn ngoài (OpenAlex/Crossref) có title bị URL-encode sẵn
     * (vd "Gen%c3%a7lik%20Merkezleri"). Nếu chuỗi KHÔNG có dấu cách nhưng CÓ dấu '%',
     * gần như chắc chắn bị encode → decode UTF-8. Title thường (có dấu cách, kể cả
     * "50% off") không bị đụng.
     */
    public static String decodeIfPercentEncoded(String s) {
        if (s == null) return null;
        if (!s.contains(" ") && s.indexOf('%') >= 0) {
            try {
                return URLDecoder.decode(s, StandardCharsets.UTF_8);
            } catch (Exception ignored) {
                // decode lỗi → giữ nguyên
            }
        }
        return s;
    }

    /**
     * true nếu title không dùng được: null/rỗng, quá ngắn, hoặc vẫn còn dạng
     * URL-encode sau khi đã cố decode (record rác).
     */
    public static boolean isUnusableTitle(String title) {
        if (title == null) return true;
        String t = title.trim();
        if (t.length() < 3) return true;
        return !t.contains(" ") && PERCENT_ENCODED.matcher(t).matches();
    }
}
