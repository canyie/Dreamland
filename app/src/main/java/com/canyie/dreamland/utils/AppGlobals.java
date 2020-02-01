package com.canyie.dreamland.utils;

import com.google.gson.Gson;

/**
 * @author canyie
 */
public final class AppGlobals {
    private static final Gson GSON = new Gson();

    public static Gson getGson() {
        return GSON;
    }
}
