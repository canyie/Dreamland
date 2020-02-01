package com.canyie.dreamland.core;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.canyie.dreamland.utils.DLog;
import com.canyie.dreamland.utils.IOUtils;

import java.io.File;
import java.io.IOException;

/**
 * @author canyie
 */
@SuppressWarnings("WeakerAccess")
public abstract class BaseManager<T> {
    private static final String TAG = "BaseManager";
    private final File mFile;
    private final File mBackupFile;
    private final Object mLock = new Object();
    private T mObject;
    private volatile boolean mLoaded;

    protected BaseManager(String filename) {
        mFile = new File(Dreamland.BASE_DIR, filename);
        mBackupFile = new File(Dreamland.BASE_DIR,filename + ".bak");
    }

    public File getFile() {
        return mFile;
    }

    public File getBackupFile() {
        return mBackupFile;
    }

    protected void startLoad() {
        new Thread(this::loadFromDisk, getClass().getName()  + "-load").start();
    }

    @NonNull protected T getRawObject() {
        ensureDataLoaded();
        return mObject;
    }

    @Nullable protected abstract T deserialize(String str);
    @NonNull protected abstract T createEmptyObject();

    private void loadFromDisk() {
        T obj = readObjectFromDisk();
        synchronized (mLock) {
            this.mObject = obj;
            mLoaded = true;
            mLock.notifyAll();
        }
    }

    @NonNull protected T readObjectFromDisk() {
        T obj = null;
        try {
            obj = readFromDisk();
        } catch (IOException e) {
            DLog.e(TAG, "!!! Failed to read " + mFile.getAbsolutePath() + "  !!!", e);
        }
        if (obj == null) {
            obj = createEmptyObject();
        }
        return obj;
    }

    @Nullable private T readFromDisk() throws IOException {
        File file = mBackupFile.exists() ? mBackupFile : mFile;
        if (!file.exists()) {
            return null;
        }
        String content = IOUtils.readAllString(file);
        return deserialize(content);
    }

    public void ensureDataLoaded() {
        if (mLoaded) return;
        synchronized (mLock) {
            if (mLoaded) return;
            boolean interrupted = false;
            try {
                while (!mLoaded) {
                    try {
                        mLock.wait();
                    } catch (InterruptedException e) {
                        interrupted = true;
                    }
                }
            } finally {
                if (interrupted)
                    Thread.currentThread().interrupt();
            }
        }
    }
}