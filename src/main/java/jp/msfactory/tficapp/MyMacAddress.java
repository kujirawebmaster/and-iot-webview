package jp.msfactory.tficapp;

import android.util.Log;

import java.net.NetworkInterface;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Macアドレス取得クラス
 *
 */
public class MyMacAddress {

    private static final String TAG = MyMacAddress.class.getSimpleName();
    private static StringBuilder mStringBuilder = new StringBuilder();

    /**
     * WiFiが存在すれば wlan0、なければ eth0のMACアドレスを返却する
     *
     * @return  MACアドレス
     */
    public static String myMacAddressString() {
        String result = null;
        Map<String, String> macs = getMacAddress();

        mStringBuilder.setLength(0);
        for (Entry<String, String> e : macs.entrySet()) {

            if (e.getValue() != null && e.getValue().length() > 0) {
                if (result == null && e.getKey().equals("eth0")) {
                    result = e.getValue();
                } if (e.getKey().equals("wlan0")) {
                    result = e.getValue();
                }
            }
            mStringBuilder.append("intf name:" + e.getKey());
            mStringBuilder.append(", mac:" + e.getValue());
            mStringBuilder.append("\n");
        }
        return result;
    }

    public static String getMacAddressString() {

        Map<String, String> macs = getMacAddress();

        mStringBuilder.setLength(0);
        for (Entry<String, String> e : macs.entrySet()) {

            mStringBuilder.append("intf name:" + e.getKey());
            mStringBuilder.append(", mac:" + e.getValue());
            mStringBuilder.append("\n");
        }
        return mStringBuilder.toString();
    }

    public static Map<String, String> getMacAddress() {
        HashMap<String, String> macs = new HashMap<String, String>();
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());

            Log.d(TAG,"intf num:" + interfaces.size());
            for (NetworkInterface intf : interfaces) {
                String name = intf.getName();
                if (null != name) {
                    byte[] raw = intf.getHardwareAddress();
                    if (null != raw) {
                        String mac = getMacString(raw);
                        macs.put(name, mac);
                        Log.d(TAG,"intf name:" + name + ", mac:" + mac);
                    } else {
                        macs.put(name, null);
                        Log.d(TAG,"intf name:" + name + ", mac: null");
                    }
                }
            }
        } catch(Exception e) {
            Log.d(TAG, "exception occured:", e);
            return null;
        }
        return macs;
    }

    private static String getMacString(byte[] raw) {
        mStringBuilder.setLength(0);
        for (int i = 0; i < raw.length; i++) {
            mStringBuilder.append(String.format("%02x", raw[i]));
        }
        return mStringBuilder.toString();
    }
}