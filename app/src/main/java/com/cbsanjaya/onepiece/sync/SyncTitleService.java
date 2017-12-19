package com.cbsanjaya.onepiece.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class SyncTitleService extends Service {
    private static final String TAG = "SyncTitleService";

    private static final Object sSyncAdapterLock = new Object();
    private static SyncTitleAdapter sSyncAdapter = null;

    /**
     * Thread-safe constructor, creates static {@link SyncTitleAdapter} instance.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Service created");
        synchronized (sSyncAdapterLock) {
            if (sSyncAdapter == null) {
                sSyncAdapter = new SyncTitleAdapter(getApplicationContext(), true);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Service destroyed");
    }

    /**
     * Return Binder handle for IPC communication with {@link SyncTitleAdapter}.
     *
     * <p>New sync requests will be sent directly to the SyncAdapter using this channel.
     *
     * @param intent Calling intent
     * @return Binder handle for {@link SyncTitleAdapter}
     */
    @Override
    public IBinder onBind(Intent intent) {
        return sSyncAdapter.getSyncAdapterBinder();
    }
}
