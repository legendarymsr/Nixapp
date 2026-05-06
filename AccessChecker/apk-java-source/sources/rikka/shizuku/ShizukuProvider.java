package rikka.shizuku;

import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import moe.shizuku.api.BinderContainer;
import rikka.sui.Sui;

/* loaded from: classes.dex */
public class ShizukuProvider extends ContentProvider {
    public static final String ACTION_BINDER_RECEIVED = "moe.shizuku.api.action.BINDER_RECEIVED";
    private static final String EXTRA_BINDER = "moe.shizuku.privileged.api.intent.extra.BINDER";
    public static final String MANAGER_APPLICATION_ID = "moe.shizuku.privileged.api";
    public static final String METHOD_GET_BINDER = "getBinder";
    public static final String METHOD_SEND_BINDER = "sendBinder";
    public static final String PERMISSION = "moe.shizuku.manager.permission.API_V23";
    private static final String TAG = "ShizukuProvider";
    private static boolean enableMultiProcess = false;
    private static boolean isProviderProcess = false;
    private static boolean enableSuiInitialization = true;

    public static void setIsProviderProcess(boolean isProviderProcess2) {
        isProviderProcess = isProviderProcess2;
    }

    public static void enableMultiProcessSupport(boolean isProviderProcess2) {
        Log.d(TAG, "Enable built-in multi-process support (from " + (isProviderProcess2 ? "provider process" : "non-provider process") + ")");
        isProviderProcess = isProviderProcess2;
        enableMultiProcess = true;
    }

    public static void disableAutomaticSuiInitialization() {
        enableSuiInitialization = false;
    }

    public static void requestBinderForNonProviderProcess(Context context) {
        Bundle reply;
        if (isProviderProcess) {
            return;
        }
        Log.d(TAG, "request binder in non-provider process");
        BroadcastReceiver receiver = new BroadcastReceiver() { // from class: rikka.shizuku.ShizukuProvider.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                BinderContainer container = (BinderContainer) intent.getParcelableExtra(ShizukuProvider.EXTRA_BINDER);
                if (container != null && container.binder != null) {
                    Log.i(ShizukuProvider.TAG, "binder received from broadcast");
                    Shizuku.onBinderReceived(container.binder, context2.getPackageName());
                }
            }
        };
        if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(receiver, new IntentFilter(ACTION_BINDER_RECEIVED), 4);
        } else {
            context.registerReceiver(receiver, new IntentFilter(ACTION_BINDER_RECEIVED));
        }
        try {
            reply = context.getContentResolver().call(Uri.parse("content://" + context.getPackageName() + ".shizuku"), METHOD_GET_BINDER, (String) null, new Bundle());
        } catch (Throwable th) {
            reply = null;
        }
        if (reply != null) {
            reply.setClassLoader(BinderContainer.class.getClassLoader());
            BinderContainer container = (BinderContainer) reply.getParcelable(EXTRA_BINDER);
            if (container != null && container.binder != null) {
                Log.i(TAG, "Binder received from other process");
                Shizuku.onBinderReceived(container.binder, context.getPackageName());
            }
        }
    }

    @Override // android.content.ContentProvider
    public void attachInfo(Context context, ProviderInfo info) {
        super.attachInfo(context, info);
        if (info.multiprocess) {
            throw new IllegalStateException("android:multiprocess must be false");
        }
        if (!info.exported) {
            throw new IllegalStateException("android:exported must be true");
        }
        isProviderProcess = true;
    }

    @Override // android.content.ContentProvider
    public boolean onCreate() {
        if (enableSuiInitialization && !Sui.isSui()) {
            boolean result = Sui.init(getContext().getPackageName());
            Log.d(TAG, "Initialize Sui: " + result);
            return true;
        }
        return true;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Removed duplicated region for block: B:19:0x0051 A[RETURN] */
    @Override // android.content.ContentProvider
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public android.os.Bundle call(java.lang.String r4, java.lang.String r5, android.os.Bundle r6) {
        /*
            r3 = this;
            boolean r0 = rikka.sui.Sui.isSui()
            if (r0 == 0) goto L13
            java.lang.String r0 = "ShizukuProvider"
            java.lang.String r1 = "Provider called when Sui is available. Are you using Shizuku and Sui at the same time?"
            android.util.Log.w(r0, r1)
            android.os.Bundle r0 = new android.os.Bundle
            r0.<init>()
            return r0
        L13:
            r0 = 0
            if (r6 != 0) goto L17
            return r0
        L17:
            java.lang.Class<moe.shizuku.api.BinderContainer> r1 = moe.shizuku.api.BinderContainer.class
            java.lang.ClassLoader r1 = r1.getClassLoader()
            r6.setClassLoader(r1)
            android.os.Bundle r1 = new android.os.Bundle
            r1.<init>()
            int r2 = r4.hashCode()
            switch(r2) {
                case -11990190: goto L37;
                case 307050656: goto L2d;
                default: goto L2c;
            }
        L2c:
            goto L41
        L2d:
            java.lang.String r2 = "getBinder"
            boolean r2 = r4.equals(r2)
            if (r2 == 0) goto L2c
            r2 = 1
            goto L42
        L37:
            java.lang.String r2 = "sendBinder"
            boolean r2 = r4.equals(r2)
            if (r2 == 0) goto L2c
            r2 = 0
            goto L42
        L41:
            r2 = -1
        L42:
            switch(r2) {
                case 0: goto L4d;
                case 1: goto L46;
                default: goto L45;
            }
        L45:
            goto L51
        L46:
            boolean r2 = r3.handleGetBinder(r1)
            if (r2 != 0) goto L51
            return r0
        L4d:
            r3.handleSendBinder(r6)
        L51:
            return r1
        */
        throw new UnsupportedOperationException("Method not decompiled: rikka.shizuku.ShizukuProvider.call(java.lang.String, java.lang.String, android.os.Bundle):android.os.Bundle");
    }

    private void handleSendBinder(Bundle extras) {
        if (Shizuku.pingBinder()) {
            Log.d(TAG, "sendBinder is called when already a living binder");
            return;
        }
        BinderContainer container = (BinderContainer) extras.getParcelable(EXTRA_BINDER);
        if (container != null && container.binder != null) {
            Log.d(TAG, "binder received");
            Shizuku.onBinderReceived(container.binder, getContext().getPackageName());
            if (enableMultiProcess) {
                Log.d(TAG, "broadcast binder");
                Intent intent = new Intent(ACTION_BINDER_RECEIVED).putExtra(EXTRA_BINDER, container).setPackage(getContext().getPackageName());
                getContext().sendBroadcast(intent);
            }
        }
    }

    private boolean handleGetBinder(Bundle reply) {
        IBinder binder = Shizuku.getBinder();
        if (binder == null || !binder.pingBinder()) {
            return false;
        }
        reply.putParcelable(EXTRA_BINDER, new BinderContainer(binder));
        return true;
    }

    @Override // android.content.ContentProvider
    public final Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override // android.content.ContentProvider
    public final String getType(Uri uri) {
        return null;
    }

    @Override // android.content.ContentProvider
    public final Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override // android.content.ContentProvider
    public final int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override // android.content.ContentProvider
    public final int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
