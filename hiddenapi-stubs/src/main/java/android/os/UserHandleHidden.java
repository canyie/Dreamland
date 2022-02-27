package android.os;

import dev.rikka.tools.refine.RefineAs;

/**
 * @author canyie
 */
@RefineAs(UserHandle.class)
public final class UserHandleHidden {
    public static final int USER_ALL = -1;
    public static final UserHandleHidden ALL = new UserHandleHidden(USER_ALL);

    private UserHandleHidden(int h) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static int getUserId(int uid) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static int getCallingUserId() {
        throw new UnsupportedOperationException("Stub!");
    }
}
