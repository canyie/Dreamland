package android.os;

/**
 * @author canyie
 */
public final class UserHandle {
    public static final int USER_ALL = -1;
    public static final UserHandle ALL = new UserHandle(USER_ALL);

    private UserHandle(int h) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static int getUserId(int uid) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static int getCallingUserId() {
        throw new UnsupportedOperationException("Stub!");
    }
}
