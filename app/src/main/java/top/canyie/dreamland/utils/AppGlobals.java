package top.canyie.dreamland.utils;

import com.google.gson.Gson;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author canyie
 */
public final class AppGlobals {
    private static final Gson GSON = new Gson();
    private static final ExecutorService DEFAULT_EXECUTOR = Executors.newFixedThreadPool(4);

    public static Gson getGson() {
        return GSON;
    }

    public static ExecutorService getDefaultExecutor() {
        return DEFAULT_EXECUTOR;
    }
}
