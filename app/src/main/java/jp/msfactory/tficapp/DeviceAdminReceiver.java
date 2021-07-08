package jp.msfactory.tficapp;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * KIOSKアプリのためのレシーバ
 */
public class DeviceAdminReceiver extends android.app.admin.DeviceAdminReceiver {
    /**
     * ロックダウンされた
     *
     * @param context
     * @param intent
     */
    @Override
    public void onEnabled(Context context, Intent intent) {
        super.onEnabled(context, intent);
        Log.d("TAG_KIOSK", "DeviceAdminReceiver onEnabled");
    }

    /**
     * ロックダウン解除された
     *
     * @param context
     * @param intent
     */
    @Override
    public void onDisabled(Context context, Intent intent) {
        super.onDisabled(context, intent);
        Log.d("TAG_KIOSK", "DeviceAdminReceiver onDisabled");
    }
}