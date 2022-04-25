package android.app;

import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.res.CompatibilityInfo;
import android.content.res.Resources;
import android.os.Build;
import android.view.Display;

import androidx.annotation.Keep;

import java.lang.ref.WeakReference;
import java.util.Map;

import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;

import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findFieldIfExists;
import static de.robv.android.xposed.XposedHelpers.findMethodExactIfExists;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.newInstance;
import static de.robv.android.xposed.XposedHelpers.setFloatField;

/**
 * Contains various methods for information about the current app.
 *
 * <p>For historical reasons, this class is in the {@code android.app} package. It can't be moved
 * without breaking compatibility with existing modules.
 */
@Keep public final class AndroidAppHelper {
	private AndroidAppHelper() {}

	private static final Class<?> CLASS_RESOURCES_KEY;

	private static final boolean HAS_IS_THEMEABLE;
	private static final boolean HAS_THEME_CONFIG_PARAMETER;

	static {
		// Dreamland changed: remove unreachable condition
//		CLASS_RESOURCES_KEY = (Build.VERSION.SDK_INT < 19) ?
//			  findClass("android.app.ActivityThread$ResourcesKey", null)
//			: findClass("android.content.res.ResourcesKey", null);

		CLASS_RESOURCES_KEY = findClass("android.content.res.ResourcesKey", null);

		HAS_IS_THEMEABLE = findFieldIfExists(CLASS_RESOURCES_KEY, "mIsThemeable") != null;
		HAS_THEME_CONFIG_PARAMETER = HAS_IS_THEMEABLE
				&& findMethodExactIfExists("android.app.ResourcesManager", null, "getThemeConfig") != null;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Map<Object, WeakReference> getResourcesMap(ActivityThread activityThread) {
		// Dreamland changed: remove unreachable condition
		Object resourcesManager = getObjectField(activityThread, "mResourcesManager");
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			return (Map) getObjectField(resourcesManager, "mResourceImpls");
		} else {
//		} else if (Build.VERSION.SDK_INT >= 19) {
			return (Map) getObjectField(resourcesManager, "mActiveResources");
//		} else {
//			return (Map) getObjectField(activityThread, "mActiveResources");
		}
	}

	// Dreamland changed: remove unreachable methods
//	/* For SDK 15 & 16 */
//	private static Object createResourcesKey(String resDir, float scale) {
//		try {
//			if (HAS_IS_THEMEABLE)
//				return newInstance(CLASS_RESOURCES_KEY, resDir, scale, false);
//			else
//				return newInstance(CLASS_RESOURCES_KEY, resDir, scale);
//		} catch (Throwable t) {
//			XposedBridge.log(t);
//			return null;
//		}
//	}
//
	/* For SDK 17 & 18 & 23 */
	// Dreamland changed: inline arguments
	private static Object createResourcesKeyM(String resDir, float scale) {
		try {
			if (HAS_THEME_CONFIG_PARAMETER)
				return newInstance(CLASS_RESOURCES_KEY, resDir, Display.DEFAULT_DISPLAY, null, scale, false, null);
			else if (HAS_IS_THEMEABLE)
				return newInstance(CLASS_RESOURCES_KEY, resDir, Display.DEFAULT_DISPLAY, null, scale, false);
			else
				return newInstance(CLASS_RESOURCES_KEY, resDir, Display.DEFAULT_DISPLAY, null, scale);
		} catch (Throwable t) {
			XposedBridge.log(t);
			return null;
		}
	}

	/* For SDK 19 - 22 */
	// Dreamland changed: inline arguments
	private static Object createResourcesKeyL(String resDir, float scale) {
		try {
			if (HAS_THEME_CONFIG_PARAMETER)
				return newInstance(CLASS_RESOURCES_KEY, resDir, Display.DEFAULT_DISPLAY, null, scale, false, null, null);
			else if (HAS_IS_THEMEABLE)
				return newInstance(CLASS_RESOURCES_KEY, resDir, Display.DEFAULT_DISPLAY, null, scale, false, null);
			else
				return newInstance(CLASS_RESOURCES_KEY, resDir, Display.DEFAULT_DISPLAY, null, scale, null);
		} catch (Throwable t) {
			XposedBridge.log(t);
			return null;
		}
	}

	// Dreamland changed: createResourcesKey is inlined for API 24+
//	/* For SDK 24+ */
//	private static Object createResourcesKey(String resDir, String[] splitResDirs, String[] overlayDirs, String[] libDirs, int displayId, Configuration overrideConfiguration, CompatibilityInfo compatInfo) {
//		try {
//			return newInstance(CLASS_RESOURCES_KEY, resDir, splitResDirs, overlayDirs, libDirs, displayId, overrideConfiguration, compatInfo);
//		} catch (Throwable t) {
//			XposedBridge.log(t);
//			return null;
//		}
//	}

	/** @hide */
	public static void addActiveResource(String resDir, float scale, boolean isThemeable, Resources resources) {
		addActiveResource(resDir, resources);
	}

	/** @hide */
	public static void addActiveResource(String resDir, Resources resources) {
		ActivityThread thread = ActivityThread.currentActivityThread();
		if (thread == null) {
			return;
		}

		// Dreamland changed: remove unreachable conditions
		Object resourcesKey;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			CompatibilityInfo compatInfo = (CompatibilityInfo) newInstance(CompatibilityInfo.class);
			setFloatField(compatInfo, "applicationScale", resources.hashCode());
			// Dreamland changed: inline createResourcesKey for API 24+
			resourcesKey = newInstance(CLASS_RESOURCES_KEY, resDir, null, null, null, Display.DEFAULT_DISPLAY, null, compatInfo);
		} else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.M) {
			resourcesKey = createResourcesKeyM(resDir, resources.hashCode());
		} else {
//		} else if (Build.VERSION.SDK_INT >= 19) {
			resourcesKey = createResourcesKeyL(resDir, resources.hashCode());
//		} else if (Build.VERSION.SDK_INT >= 17) {
//			resourcesKey = createResourcesKey(resDir, Display.DEFAULT_DISPLAY, null, resources.hashCode());
//		} else {
//			resourcesKey = createResourcesKey(resDir, resources.hashCode());
		}

		// Dreamland changed: remove useless judgments
//		if (resourcesKey != null) {
//			if (Build.VERSION.SDK_INT >= 24) {
//				Object resImpl = getObjectField(resources, "mResourcesImpl");
//				getResourcesMap(thread).put(resourcesKey, new WeakReference<>(resImpl));
//			} else {
//				getResourcesMap(thread).put(resourcesKey, new WeakReference<>(resources));
//			}

		Object resImpl = getObjectField(resources, "mResourcesImpl");
		getResourcesMap(thread).put(resourcesKey, new WeakReference<>(resImpl));
//		}
	}

	/**
	 * Returns the name of the current process. It's usually the same as the main package name.
	 */
	public static String currentProcessName() {
		String processName = ActivityThread.currentPackageName();
		if (processName == null)
			return "android";
		return processName;
	}

	/**
	 * Returns information about the main application in the current process.
	 *
	 * <p>In a few cases, multiple apps might run in the same process, e.g. the SystemUI and the
	 * Keyguard which both have {@code android:process="com.android.systemui"} set in their
	 * manifest. In those cases, the first application that was initialized will be returned.
	 */
	public static ApplicationInfo currentApplicationInfo() {
		ActivityThread am = ActivityThread.currentActivityThread();
		if (am == null)
			return null;

		Object boundApplication = getObjectField(am, "mBoundApplication");
		if (boundApplication == null)
			return null;

		return (ApplicationInfo) getObjectField(boundApplication, "appInfo");
	}

	/**
	 * Returns the Android package name of the main application in the current process.
	 *
	 * <p>In a few cases, multiple apps might run in the same process, e.g. the SystemUI and the
	 * Keyguard which both have {@code android:process="com.android.systemui"} set in their
	 * manifest. In those cases, the first application that was initialized will be returned.
	 */
	public static String currentPackageName() {
		ApplicationInfo ai = currentApplicationInfo();
		return (ai != null) ? ai.packageName : "android";
	}

	/**
	 * Returns the main {@link Application} object in the current process.
	 *
	 * <p>In a few cases, multiple apps might run in the same process, e.g. the SystemUI and the
	 * Keyguard which both have {@code android:process="com.android.systemui"} set in their
	 * manifest. In those cases, the first application that was initialized will be returned.
	 */
	public static Application currentApplication() {
		return ActivityThread.currentApplication();
	}

	/** @deprecated Use {@link XSharedPreferences} instead. */
	@SuppressWarnings("UnusedParameters")
	@Deprecated
	public static SharedPreferences getSharedPreferencesForPackage(String packageName, String prefFileName, int mode) {
		return new XSharedPreferences(packageName, prefFileName);
	}

	/** @deprecated Use {@link XSharedPreferences} instead. */
	@Deprecated
	public static SharedPreferences getDefaultSharedPreferencesForPackage(String packageName) {
		return new XSharedPreferences(packageName);
	}

	/** @deprecated Use {@link XSharedPreferences#reload} instead. */
	@Deprecated
	public static void reloadSharedPreferencesIfNeeded(SharedPreferences pref) {
		if (pref instanceof XSharedPreferences) {
			((XSharedPreferences) pref).reload();
		}
	}
}
