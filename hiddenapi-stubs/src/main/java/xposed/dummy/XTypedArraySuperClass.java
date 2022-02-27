package xposed.dummy;

import android.content.res.TypedArrayHidden;

/**
 * This class is used as super class of XResources.XTypedArray.
 */
public class XTypedArraySuperClass extends TypedArrayHidden {
    protected XTypedArraySuperClass() {
        super(null);
        throw new UnsupportedOperationException("Stub!");
    }
}
