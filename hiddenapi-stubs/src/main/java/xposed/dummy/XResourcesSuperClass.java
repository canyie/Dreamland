package xposed.dummy;

import android.content.res.Resources;

/**
 * This class is used as super class of XResources.
 */
public class XResourcesSuperClass extends Resources {
    protected XResourcesSuperClass(ClassLoader classLoader) {
        super(null, null, null);
        throw new UnsupportedOperationException("Stub!");
    }
}
