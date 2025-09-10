// IBydMateServer.aidl
package com.toddmo.bydmate.aidl;

import android.view.InputEvent;
// Declare any non-default types here with import statements
import android.os.ResultReceiver;

interface IBydMateServer {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void injectInputEvent(in InputEvent event, in int displayId);
}