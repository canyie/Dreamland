package com.canyie.dreamland;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends Activity {

    // Used to load the 'native-lib' library on application startup.

    static {
        System.loadLibrary("dreamland");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onButtonClick(View view) {
        testNativeHook();
    }

    private native void testNativeHook();
}
