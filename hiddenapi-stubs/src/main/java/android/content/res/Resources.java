package android.content.res;

import android.graphics.Movie;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import java.io.InputStream;

public class Resources {
	@SuppressWarnings("serial")
	public static class NotFoundException extends RuntimeException {
		public NotFoundException() {
		}

		public NotFoundException(String name) {
			throw new UnsupportedOperationException("Stub!");
		}
	}

	public final class Theme {
	}

	public Resources(ClassLoader classLoader) {
		throw new UnsupportedOperationException("Stub!");
	}

	public Resources(AssetManager assets, DisplayMetrics metrics, Configuration config) {
		throw new UnsupportedOperationException("Stub!");
	}

	public static Resources getSystem() {
		throw new UnsupportedOperationException("Stub!");
	}

	public XmlResourceParser getAnimation(int id) throws NotFoundException {
		throw new UnsupportedOperationException("Stub!");
	}

	public final AssetManager getAssets() {
		throw new UnsupportedOperationException("Stub!");
	}

	public boolean getBoolean(int id) throws NotFoundException {
		throw new UnsupportedOperationException("Stub!");
	}

	public int getColor(int id) throws NotFoundException {
		throw new UnsupportedOperationException("Stub!");
	}

	public ColorStateList getColorStateList(int id) throws NotFoundException {
		throw new UnsupportedOperationException("Stub!");
	}

	public Configuration getConfiguration() {
		throw new UnsupportedOperationException("Stub!");
	}

	public float getDimension(int id) throws NotFoundException {
		throw new UnsupportedOperationException("Stub!");
	}

	public int getDimensionPixelOffset(int id) throws NotFoundException {
		throw new UnsupportedOperationException("Stub!");
	}

	public int getDimensionPixelSize(int id) throws NotFoundException {
		throw new UnsupportedOperationException("Stub!");
	}

	public DisplayMetrics getDisplayMetrics() {
		throw new UnsupportedOperationException("Stub!");
	}

	public Drawable getDrawable(int id) throws NotFoundException {
		throw new UnsupportedOperationException("Stub!");
	}

	/** Since SDK21 */
	public Drawable getDrawable(int id, Theme theme) throws NotFoundException {
		throw new UnsupportedOperationException("Stub!");
	}

	// Dreamland changed: remove support for CM12 (it based on android 5 and not supported)
//	/** Since SDK21, CM12 */
//	public Drawable getDrawable(int id, Theme theme, boolean supportComposedIcons) throws NotFoundException {
//		throw new UnsupportedOperationException("Stub!");
//	}

	public Drawable getDrawableForDensity(int id, int density) throws NotFoundException {
		throw new UnsupportedOperationException("Stub!");
	}

	/** Since SDK21 */
	public Drawable getDrawableForDensity(int id, int density, Theme theme) {
		throw new UnsupportedOperationException("Stub!");
	}

	// Dreamland changed: remove support for CM12 (it based on android 5 and not supported)
//	/** Since SDK21, CM12 */
//	public Drawable getDrawableForDensity(int id, int density, Theme theme, boolean supportComposedIcons) {
//		throw new UnsupportedOperationException("Stub!");
//	}

	/** Since SDK21 */
	public float getFloat(int id) {
		throw new UnsupportedOperationException("Stub!");
	}

	public float getFraction(int id, int base, int pbase) {
		throw new UnsupportedOperationException("Stub!");
	}

	public int getIdentifier(String name, String defType, String defPackage) {
		throw new UnsupportedOperationException("Stub!");
	}

	public int[] getIntArray(int id) throws NotFoundException {
		throw new UnsupportedOperationException("Stub!");
	}

	public int getInteger(int id) throws NotFoundException {
		throw new UnsupportedOperationException("Stub!");
	}

	public XmlResourceParser getLayout(int id) throws NotFoundException {
		throw new UnsupportedOperationException("Stub!");
	}

	public Movie getMovie(int id) throws NotFoundException {
		throw new UnsupportedOperationException("Stub!");
	}

	public String getQuantityString(int id, int quantity) throws NotFoundException {
		throw new UnsupportedOperationException("Stub!");
	}

	public String getQuantityString(int id, int quantity, Object... formatArgs) {
		throw new UnsupportedOperationException("Stub!");
	}

	public CharSequence getQuantityText(int id, int quantity) throws NotFoundException {
		throw new UnsupportedOperationException("Stub!");
	}

	public String getResourceEntryName(int resid) throws NotFoundException {
		throw new UnsupportedOperationException("Stub!");
	}

	public String getResourceName(int resid) throws NotFoundException {
		throw new UnsupportedOperationException("Stub!");
	}

	public String getResourcePackageName(int resid) throws NotFoundException {
		throw new UnsupportedOperationException("Stub!");
	}

	public String getResourceTypeName(int resid) throws NotFoundException {
		throw new UnsupportedOperationException("Stub!");
	}

	public String getString(int id) throws NotFoundException {
		throw new UnsupportedOperationException("Stub!");
	}

	public String getString(int id, Object... formatArgs) throws NotFoundException {
		throw new UnsupportedOperationException("Stub!");
	}

	public String[] getStringArray(int id) throws NotFoundException {
		throw new UnsupportedOperationException("Stub!");
	}

	public CharSequence getText(int id) throws NotFoundException {
		throw new UnsupportedOperationException("Stub!");
	}

	public CharSequence getText(int id, CharSequence def) {
		throw new UnsupportedOperationException("Stub!");
	}

	public CharSequence[] getTextArray(int id) throws NotFoundException {
		throw new UnsupportedOperationException("Stub!");
	}

	public void getValue(int id, TypedValue outValue, boolean resolveRefs) {
		throw new UnsupportedOperationException("Stub!");
	}

	public XmlResourceParser getXml(int id) throws NotFoundException {
		throw new UnsupportedOperationException("Stub!");
	}

	public InputStream openRawResource(int id) throws NotFoundException {
		throw new UnsupportedOperationException("Stub!");
	}

	public TypedArray obtainTypedArray(int id) {
		throw new UnsupportedOperationException("Stub!");
	}

	public ClassLoader getClassLoader() {
		throw new UnsupportedOperationException("Stub!");
	}

	public ResourcesImpl getImpl() {
		throw new UnsupportedOperationException("Stub!");
	}

	public void setImpl(ResourcesImpl impl) {
		throw new UnsupportedOperationException("Stub!");
	}
}
