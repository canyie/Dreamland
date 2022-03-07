package top.canyie.dreamland.utils;

import android.os.Build;

import java.util.Locale;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.S;
import static android.os.Build.VERSION_CODES.S_V2;

/**
 * @author canyie
 */
public final class BuildUtils {
    private BuildUtils() {}

    public static boolean isAtLeastT() {
        return SDK_INT > S_V2 || (SDK_INT == S_V2 && isAtLeastPreReleaseCodename("Tiramisu"));
    }

    public static boolean isAlLeastSv2() {
        return SDK_INT >= S_V2 || (SDK_INT == S && isAtLeastPreReleaseCodename("Sv2"));
    }

    // https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:core/core/src/main/java/androidx/core/os/BuildCompat.java;l=49;drc=f8ab4c3030c3fbadca32a9593c522c89a9f2cadf
    private static boolean isAtLeastPreReleaseCodename(String codename) {
        final String buildCodename = Build.VERSION.CODENAME.toUpperCase(Locale.ROOT);

        // Special case "REL", which means the build is not a pre-release build.
        if ("REL".equals(buildCodename)) {
            return false;
        }

        return buildCodename.compareTo(codename.toUpperCase(Locale.ROOT)) >= 0;
    }
}
