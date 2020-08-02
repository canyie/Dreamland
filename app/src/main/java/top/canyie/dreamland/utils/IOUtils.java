package top.canyie.dreamland.utils;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * Created by canyie on 2019/11/24.
 */
public final class IOUtils {
    private static final String TAG = "IOUtils";
    private IOUtils() {}

    public static String readAllString(@NonNull File file) throws IOException {
        return readAllString(new FileReader(file));
    }

    public static String readAllString(@NonNull InputStream input) throws IOException {
        return readAllString(new InputStreamReader(input));
    }

    public static String readAllString(@NonNull Reader input) throws IOException {
        BufferedReader br = new BufferedReader(input);
        try {
            StringBuilder sb = new StringBuilder();
            String line;
            while((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        } finally {
            closeQuietly(br);
        }
    }

    public static void writeToFile(File file, String content) throws IOException {
        FileWriter fw = new FileWriter(file);
        try {
            fw.write(content);
        } finally {
            closeQuietly(fw);
        }
    }

    public static void closeQuietly(@Nullable Closeable closeable) {
        if(closeable != null) {
            try {
                closeable.close();
            } catch(IOException e) {
                Log.e(TAG, "Error while closing Closeable " + closeable, e);
            }
        }
    }

    public static File ensureDirectoryExisting(@NonNull File directory) {
        if (!(directory.exists() || directory.mkdirs() || directory.exists())) {
            throw new IllegalStateException("Can't create directory: " + directory.getAbsolutePath());
        }
        return directory;
    }
}
