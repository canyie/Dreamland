package com.canyie.dreamland.utils;

import android.annotation.SuppressLint;
import android.util.Log;

import com.swift.sandhook.SandHookConfig;

/**
 * @author canyie
 */
@SuppressLint("LogTagMismatch")
@SuppressWarnings("WeakerAccess")
public final class DLog {
    /** Whether to allow log output. Fatal levels are not limited by this. */
    private static final boolean LOG_ENABLED = true;

    public static final int VERBOSE = Log.VERBOSE;
    public static final int DEBUG = Log.DEBUG;
    public static final int INFO = Log.INFO;
    public static final int WARN = Log.WARN;
    public static final int ERROR = Log.ERROR;
    public static final int FATAL = Log.ASSERT;

    public static void v(String tag, String msg, Object... format) {
        if (isLoggable(VERBOSE)) {
            Log.v(tag, String.valueOf(msg));
        }
    }

    public static void d(String tag, String msg, Object... format) {
        if (isLoggable(DEBUG)) {
            Log.d(tag, String.format(msg, format));
        }
    }

    public static void i(String tag, String msg, Object... format) {
        if (isLoggable(INFO)) {
            Log.i(tag, String.format(msg, format));
        }
    }

    public static void w(String tag, String msg, Object... format) {
        if (isLoggable(WARN)) {
            Log.w(tag, String.format(msg, format));
        }
    }

    public static void w(String tag, String msg, Throwable e) {
        if (isLoggable(WARN)) {
            Log.w(tag, msg, e);
        }
    }

    public static void w(String tag, Throwable e) {
        if (isLoggable(WARN)) {
            Log.w(tag, getStackTraceString(e));
        }
    }

    public static void e(String tag, String msg, Object... format) {
        if (isLoggable(ERROR)) {
            Log.e(tag, String.format(msg, format));
        }
    }

    public static void e(String tag, Throwable e) {
        if (isLoggable(ERROR)) {
            Log.e(tag, getStackTraceString(e));
        }
    }

    public static void e(String tag, String msg, Throwable e) {
        if (isLoggable(ERROR)) {
            Log.e(tag, msg, e);
        }
    }

    /**
     * Print a fatal level log. It may cause the application process to terminate unexpectedly.
     */
    public static void f(String tag, String msg, Object... format) {
        Log.wtf(tag, String.format(msg, format));
    }

    /**
     * Print a fatal level log. It may cause the application process to terminate unexpectedly.
     */
    public static void f(String tag, String msg, Throwable e) {
        Log.wtf(tag, msg, e);
    }

    /**
     * Print a fatal level log. It may cause the application process to terminate unexpectedly.
     */
    public static void f(String tag, Throwable e) {
        Log.wtf(tag, e);
    }

    public static String getStackTraceString(Throwable e) {
        return Log.getStackTraceString(e);
    }

    public static void printStackTrace(String tag) {
        Log.e(tag, "here", new Throwable("here"));
    }

    public static boolean isLoggable(int level) {
        if (level == FATAL) {
            return true;
        }
        if (!LOG_ENABLED) {
            return false;
        }
        switch (level) {
            case VERBOSE:
            case DEBUG:
                return SandHookConfig.DEBUG;
        }
        return true;
    }
}
