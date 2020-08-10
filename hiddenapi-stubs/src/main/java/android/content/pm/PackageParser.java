package android.content.pm;

import java.io.File;

public class PackageParser {
	public static class PackageLite {
		public final String packageName = null;
	}

	// Dreamland changed: remove parsePackageLite(String, int) for before sdk 21
//	/** Before SDK21 */
//	public static PackageLite parsePackageLite(String packageFile, int flags) {
//		throw new UnsupportedOperationException("Stub!");
//	}

	/** Since SDK21 */
	public static PackageLite parsePackageLite(File packageFile, int flags) throws PackageParserException {
		throw new UnsupportedOperationException("Stub!");
	}

	/** Since SDK21 */
	@SuppressWarnings("serial")
	public static class PackageParserException extends Exception {
	}
}
