package com.fpt.swp.util;

public final class AppConstants {

    private AppConstants() {}

    // ─── Regex Patterns ────────────────────────────────────────────────────────
    public static final String PHONE_PATTERN = "^0[35789][0-9]{8}$";
    public static final String PASSWORD_PATTERN =
            "^(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{9,}$";

    // ─── Validation Messages ───────────────────────────────────────────────────
    public static final String MSG_PHONE_INVALID =
            "Phone must start with 03, 05, 07, 08, 09 and have exactly 10 digits";
    public static final String MSG_PASSWORD_INVALID =
            "Password must be at least 9 characters, contain 1 uppercase, 1 number, 1 special character";
    public static final String MSG_DOB_INVALID =
            "Date of birth must be a past date and year must be greater than 1920";

    // ─── Security ──────────────────────────────────────────────────────────────
    public static final long DEFAULT_JWT_EXPIRATION_MS = 86_400_000L; // 1 day
    public static final String JWT_SECRET_PROPERTY = "${app.jwt-secret}";
    public static final int LOGIN_MAX_ATTEMPTS = 5;
    public static final int LOGIN_LOCKOUT_MINUTES = 15;

    // ─── Forgot Password ───────────────────────────────────────────────────────
    public static final String FORGOT_PASSWORD_PREFIX = "Trend@";
    public static final int FORGOT_PASSWORD_SUFFIX_LENGTH = 6;
    public static final int FORGOT_PASSWORD_DIGIT_COUNT = 1;

    // ─── API ───────────────────────────────────────────────────────────────────
    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_LIMIT = 10;
    public static final int MAX_LIMIT = 100;

    // ─── CORS ──────────────────────────────────────────────────────────────────
    public static final String[] CORS_ALLOWED_ORIGINS = {
            "http://localhost:5173",
            "http://localhost:3000",
            "https://trend-searchor-fe.vercel.app",
            "https://trend-searchor-fe-*.vercel.app"
    };
}
