package android.os;

/**
 * @author canyie
 */
public final class SELinux {
    private SELinux() {}

    /**
     * Determine whether SELinux is disabled or enabled.
     * @return a boolean indicating whether SELinux is enabled.
     */
    public static boolean isSELinuxEnabled() {
        throw new UnsupportedOperationException("Stub!");
    }

    /**
     * Determine whether SELinux is permissive or enforcing.
     * @return a boolean indicating whether SELinux is enforcing.
     */
    public static boolean isSELinuxEnforced() {
        throw new UnsupportedOperationException("Stub!");
    }

    /**
     * Gets the security context of the current process.
     * @return a String representing the security context of the current process.
     */
    public static String getContext() {
        throw new UnsupportedOperationException("Stub!");
    }

    /**
     * Gets the security context of a given process id.
     * @param pid an int representing the process id to check.
     * @return a String representing the security context of the given pid.
     */
    public static String getPidContext(int pid) {
        throw new UnsupportedOperationException("Stub!");
    }

    /**
     * Check permissions between two security contexts.
     * @param scon The source or subject security context.
     * @param tcon The target or object security context.
     * @param tclass The object security class name.
     * @param perm The permission name.
     * @return a boolean indicating whether permission was granted.
     */
    public static boolean checkSELinuxAccess(String scon, String tcon, String tclass, String perm) {
        throw new UnsupportedOperationException("Stub!");
    }
}
