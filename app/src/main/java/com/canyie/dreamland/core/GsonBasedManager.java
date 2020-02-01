package com.canyie.dreamland.core;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.canyie.dreamland.utils.AppGlobals;
import com.canyie.dreamland.utils.DLog;
import com.google.gson.JsonSyntaxException;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * @author canyie
 */
@SuppressWarnings("WeakerAccess")
public abstract class GsonBasedManager<T> extends BaseManager<T> {
    private static final String TAG = "GsonBasedManager";
    private final Type mType;

    protected GsonBasedManager(String filename) {
        super(filename);

        Type genericSuperclass = getClass().getGenericSuperclass();
        if (!(genericSuperclass instanceof ParameterizedType)) {
            throw new RuntimeException("Missing type parameter.");
        }
        Type type = ((ParameterizedType) genericSuperclass).getActualTypeArguments()[0];
        assert type != null;
        mType = type;
    }

    @Nullable @Override protected T deserialize(String str) {
        try {
           return AppGlobals.getGson().fromJson(str, mType);
        } catch (JsonSyntaxException e) {
            DLog.e(TAG, "!!! JsonSyntaxException thrown !!!", e);
            DLog.e(TAG, "json content: %s", str);
        }
        return null;
    }
}
