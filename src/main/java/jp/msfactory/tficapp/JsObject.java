package jp.msfactory.tficapp;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * javascriptインターフェースクラス
 */
public class JsObject {
    private static final int REQUEST_CODE_EXECAPP = 100; // For call execApp methods

    private MainActivity getMainActivity() {
        return MainActivity.getMainActivity();
    }
    /**
     * OSの設定アプリ起動
     *
     */
    @JavascriptInterface
    public void settings() {
        getMainActivity().stopLockDown();
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_SETTINGS);
        getMainActivity().startActivity(intent);
    }

    float screenBrightness;

    /**
     * バックライトの明るさ取得
     *
     * @return
     */
    @JavascriptInterface
    public float getBacklight() {
        return getScreenBrightness();
    }
    /**
     * バックライトの明るさ取得
     *
     * @return
     */
    @JavascriptInterface
    public float getScreenBrightness() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                WindowManager.LayoutParams lp = getMainActivity().getWindow().getAttributes();
                screenBrightness = lp.screenBrightness;
            }
        });
        thread.start();

        while (thread.isAlive()) {
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                // TODO 自動生成された catch ブロック
                e.printStackTrace();
            }
            // nop
        }

        return screenBrightness;
    }

    /**
     * バックライトの明るさ設定
     *
     * @param val
     */
    @JavascriptInterface
    public void setBacklight(float val) {
        setScreenBrightness(val);
    }
    /**
     * バックライトの明るさ設定
     *
     * @param val
     */
    @JavascriptInterface
    public void setScreenBrightness(float val) {
        getMainActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
                //getMainActivity().getWindow().getAttributes();
                if (true) {
                    lp = getMainActivity().getWindow().getAttributes();
                    screenBrightness = lp.screenBrightness;
                }
                lp.screenBrightness = val;;
                getMainActivity().getWindow().setAttributes(lp);
            }
        });
    }

    /**
     * スリープ許可状態変更
     *
     * @param flag trueならスリープ許可
     */
    @JavascriptInterface
    public void setKeepScreenBacklight(boolean flag) {
        getMainActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (flag) {
                    getMainActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                } else {
                    getMainActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
            }
        });
    }

    private ProgressDialog mProgressDialog;

    /**
     * ローディングダイアログ表示
     *
     */
    @JavascriptInterface
    public void showLoading() {
        getMainActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mProgressDialog == null) {
                    mProgressDialog = new ProgressDialog(getMainActivity());;
                }
                mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                //mProgressDialog.setTitle("処理中");
                mProgressDialog.setMessage("Please Wait!!");
                mProgressDialog.setCancelable(false);
                mProgressDialog.setCanceledOnTouchOutside(false);
                Window window = mProgressDialog.getWindow();
                window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
                mProgressDialog.show();
            }
        });
    }

    /**
     * ローディングダイアログ停止
     *
     */
    @JavascriptInterface
    public void hideLoading() {
        getMainActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mProgressDialog != null) {
                    mProgressDialog.dismiss();
                }
            }
        });
    }

    /**
     * TOASTメッセージ表示
     *
     * @param mes
     */
    @JavascriptInterface
    public void showToast(String mes) {
        Toast.makeText(getMainActivity(), mes, Toast.LENGTH_SHORT).show();
    }

    /**
     * webviewの履歴クリア
     * （うまく動作しない）
     *
     */
    @JavascriptInterface
    public void clearHistory() {
        getMainActivity().webviewClearHistory();
    }

    /**
     * webviewトップページへ遷移
     *
     */
    @JavascriptInterface
    public void goHome() {
        loadMyHome();
    }
    /**
     * webviewトップページへ遷移
     *
     */
    @JavascriptInterface
    public void loadMyHome() {
        getMainActivity().webviewLoadUrl2Data(getMainActivity().URL_MYHOME);
        hideNavigationBar();
    }

    /**
     * webviewのページ遷移
     * （クロスドメインでもOK）
     *
     * @param url
     */
    @JavascriptInterface
    public void loadUrl(String url) {
        getMainActivity().webviewLoadUrl2Data(url);
    }

    /**
     * ハンドサイン結果を通知するコールバック関数名を設定
     * （コールバック解除する場合はから文字列を指定する）
     *
     * @param callback
     */
    @JavascriptInterface
    public void setHandSignCallback(String callback)
    {
        getMainActivity().setTfliteCallback(callback);
    }

    /**
     * ハンドサインサブウィンドウの表示
     *
     */
    @JavascriptInterface
    public void openHandsignWindow()
    {
        MainActivity.getMainActivity().openSubWindow();
    }
    /**
     * ハンドサインサブウィンドウの表示
     *
     */
    @JavascriptInterface
    public void openSubWindow()
    {
        MainActivity.getMainActivity().openSubWindow();
    }
    /**
     * ハンドサインサブウィンドウの非表示
     *
     */
    @JavascriptInterface
    public void closeHandsignWindow() {
        MainActivity.getMainActivity().closeSubWindow();
    }
    /**
     * ハンドサインサブウィンドウの非表示
     *
     */
    @JavascriptInterface
    public void closeSubWindow() {
        MainActivity.getMainActivity().closeSubWindow();
    }

    /**
     * variables
     */
    private HashMap<String,String> mapVar = new HashMap<String,String>();

    /**
     * ローカルストレージへjavascript変数保存
     *
     * @param key
     * @param value
     */
    @JavascriptInterface
    public void setVar(String key, String value) {
        if (true) {
            setPreferenceString(null, key, value);
        } else {
            mapVar.put(key, value);
        }
    }

    /**
     * ローカルストレージへjavascript変数保存
     *
     * @param type プリファレンスタイプ（空なら共通プリファレンス）
     * @param key
     * @param value
     */
    @JavascriptInterface
    public void setPreferenceString(String type, String key, String value) {
        SharedPreferences sp;
        if (type == null || type.length() == 0) {
            sp = PreferenceManager
                    .getDefaultSharedPreferences(getMainActivity());
        } else {
            sp = getMainActivity().getSharedPreferences(type, Context.MODE_PRIVATE);
        }

        sp.edit().putString(key, value).commit();
    }

    /**
     * ローカルストレージからjavascript変数取得
     *
     * @param key
     * @return
     */
    @JavascriptInterface
    public String getVar(String key) {
        String value;
        if (true) {
            value = getPreferenceString(null, key);
        } else {
            value = mapVar.get(key);
        }
        return value;
    }

    /**
     * ローカルストレージからjavascript変数取得
     *
     * @param type プリファレンスタイプ（空なら共通プリファレンス）
     * @param key
     * @return
     */
    @JavascriptInterface
    public String getPreferenceString(String type, String key) {
        SharedPreferences sp;
        if (type == null || type.length() == 0) {
            sp = PreferenceManager
                    .getDefaultSharedPreferences(getMainActivity());
        } else {
            sp = getMainActivity().getSharedPreferences(type, Context.MODE_PRIVATE);
        }

        String result = null;
        result = sp.getString(key, "");
        return result;
    }

    /**
     * ローカルストレージの変数削除
     *
     * @param key
     */
    @JavascriptInterface
    public void removeVar(String key) {
        mapVar.remove(key);
    }

    /**
     * ローカルストレージの変数全初期化
     *
     */
    @JavascriptInterface
    public void initPreference() {
        return;
    }


    /**
     * javascript関数のeval()
     * （video autoplayで使用していた）
     *
     * @param func
     */
    @JavascriptInterface
    public void nativeToJavascript(String func) {
        getMainActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getMainActivity().execJavaScript(func);
            }
        });
    }

    /**
     * 音声認識開始
     *
     * @param sttCallback
     * @param interval
     */
    @JavascriptInterface
    public void startSTT(String sttCallback, int interval) {
        getMainActivity().runOnUiThread(new Runnable() {
            public void run() {
                if (getMainActivity().mRecognizer != null) {
                    getMainActivity().mRecognizer.stopListening();

                    getMainActivity().mSttCallback = sttCallback;
                    getMainActivity().mSttInterval = interval;

                    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                    intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getMainActivity().getPackageName());
                    intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

                    intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3600000);
                    intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 3600000);
                    intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3600000);

                    getMainActivity().mRecognizer.startListening(intent);
                }
            }
        });
    }

    /**
     * 音声認識停止
     */
    @JavascriptInterface
    public void stopSTT() {
        getMainActivity().runOnUiThread(new Runnable() {
            public void run() {
                getMainActivity().mSttCallback = "";
                if (getMainActivity().mRecognizer != null) {
                    getMainActivity().mRecognizer.stopListening();
                }
            }
        });
    }

    /**
     * 音声合成開始
     *
     * @param talktext
     */
    @JavascriptInterface
    public void startTTS(String talktext) {
        try {
            if (getMainActivity().mTts != null) {
                //mTts.setLanguage(Locale.JAPAN);
                getMainActivity().mTts.speak(talktext, TextToSpeech.QUEUE_ADD, null);
            } else {
                getMainActivity().mTts = new TextToSpeech(getMainActivity(), new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(int status) {
                        //mTts.setLanguage(Locale.JAPAN);
                        getMainActivity().mTts.speak(talktext, TextToSpeech.QUEUE_ADD, null);
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 音声合成停止
     */
    @JavascriptInterface
    public void stopTTS() {
        if (getMainActivity().mTts != null) {
            getMainActivity().mTts.stop();
        }
    }

    /**
     * アプリロックダウン（KIOSK）開始
     */
    @JavascriptInterface
    public void startLockTask() {
        getMainActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getMainActivity().startLockDown();
            }
        });
    }
    /**
     * アプリロックダウン（KIOSK）解除
     */
    @JavascriptInterface
    public void stopLockTask() {
        getMainActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getMainActivity().stopLockDown();
            }
        });
    }

    /**
     * アプリアップデータチェック
     * （許可された場合はインストールまで行われる：アプリは強制終了する）
     */
    @JavascriptInterface
    public void updateCheck() {
        getMainActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getMainActivity().updater.updateCheck();
            }
        });
    }

    /**
     * 他アプリインテント実行
     *
     * @param packageName
     * @param className
     * @param action
     * @param uri
     * @param extra
     * @return
     */
    public boolean execApp(String packageName, String className, String action,
                           String uri, String extra) {
        return execApp(packageName, className, action,uri, extra, false);
    }

    /**
     * 他アプリインテント実行
     *
     * @param packageName
     * @param className
     * @param action
     * @param uri
     * @param extra
     * @param newtask
     * @return
     */
    public boolean execApp(String packageName, String className, String action,
                           String uri, String extra, boolean newtask) {

        boolean result = false;

        try {
            Intent intent = null;
            if (action.length() > 0 && uri.length() > 0) {
                // URIとアクションででインテント起動する
                intent = new Intent(action, Uri.parse(uri));
                String j = Intent.ACTION_DIAL;
            }
            if (packageName.length() > 0 && className.length() > 0) {
                if (intent == null) {
                    intent = new Intent();
                }
                intent.setClassName(packageName, className);
            }
            if (packageName.length() > 0) {
                // パッケージ名のみでアプリ起動する
                if (intent == null) {
                    intent = new Intent(Intent.ACTION_MAIN, null);
                }
                intent.addCategory(Intent.CATEGORY_LAUNCHER);

                PackageManager pManager = getMainActivity().getPackageManager();
                List<ResolveInfo> appInfo = pManager.queryIntentActivities(
                        intent, 0);

                for (ResolveInfo info : appInfo) {
                    if (info.activityInfo.applicationInfo.packageName
                            .equals(packageName)) {
                        ComponentName componentName;
                        componentName = new ComponentName(
                                info.activityInfo.applicationInfo.packageName,
                                info.activityInfo.name);
                        intent.setComponent(componentName);
                        int launchFlag = Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED;
                        intent.setFlags(launchFlag);
                    }
                }
            }
            if (intent != null) {
                if (newtask) {
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                }
                getMainActivity().startActivityForResult(intent, REQUEST_CODE_EXECAPP);
                result = true;
            }
        } catch (Exception e) {
            Toast.makeText(getMainActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return result;
    }

    /**
     * ナビゲーションバー非表示
     */
    @JavascriptInterface
    public void hideNavigationBar() {
        MainActivity.getMainActivity().hideNavigationBar();
    }

    /**
     * 自アプリ終了
     */
    @JavascriptInterface
    public void exit() {
        finish();
        System.exit(0);  //Destroy whole Application
    }
    /**
     * 自アプリ終了
     */
    @JavascriptInterface
    public void finish() {
        getMainActivity().runOnUiThread(new Runnable() {
            public void run() {
                getMainActivity().finish();
            }
        });
    }
    /**
     * 他アプリパッケージ実行
     *
     * @param packageName
     * @param className
     * @param action
     * @param uri
     * @param extra
     * @return
     */
    @JavascriptInterface
    public int execPackage(String packageName, String className,
                           String action, String uri, String extra) {
        getMainActivity().runOnUiThread(new Runnable() {
            public void run() {
                execApp(packageName,
                        className, action,
                        uri, extra);
            }
        });
        return 0;
    }

    /**
     * 他アプリパッケージ実行
     *
     * @param packageName
     * @param className
     * @param action
     * @param uri
     * @param extra
     * @param newtask
     * @return
     */
    @JavascriptInterface
    public int execPackage(String packageName, String className,
                           String action, String uri, String extra, boolean newtask) {
        getMainActivity().runOnUiThread(new Runnable() {
            public void run() {
                execApp(packageName,
                        className, action,
                        uri, extra, newtask);
            }
        });
        return 0;
    }
    /**
     * コマンドライン実行
     *
     * @param cmd
     * @return
     */
    @JavascriptInterface
    public String execBin(String cmd) {
        CommandExecutor commandExecutor = new CommandExecutor();
        String result = "";
        try {
            result = commandExecutor.execCommand(cmd);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }


    /**
     * USBプリンタ処理（このアプリでは使用しない）
     */
    int cnt = 0;
    int jsobjResult;
    @JavascriptInterface
    public int usbDriverCheck() {
        if ((cnt & 1) == 0) {
            openSubWindow();
        } else {
            closeSubWindow();
        }
        cnt++;
        jsobjResult = -999999;
        return jsobjResult;
    }
    @JavascriptInterface
    public int printPngUrl(String url) {
        Bitmap bmpTmp = null;
        //bmpTmp = downloadImage(url);
        jsobjResult = -999999;
        if (bmpTmp != null) {
            bmpTmp.recycle();
        }
        return jsobjResult;
    }

    /**
     * URLの画像ダウンロード
     *
     * @param address
     * @return
     */
    public Bitmap downloadImage(String address) {
        Bitmap bmp = null;

        HttpURLConnection urlConnection = null;

        try {
            java.net.URL url = new URL( address );

            // HttpURLConnection インスタンス生成
            urlConnection = (HttpURLConnection) url.openConnection();

            // タイムアウト設定
            urlConnection.setReadTimeout(10000);
            urlConnection.setConnectTimeout(20000);

            // リクエストメソッド
            urlConnection.setRequestMethod("GET");

            // リダイレクトを自動で許可しない設定
            urlConnection.setInstanceFollowRedirects(false);

            // ヘッダーの設定(複数設定可能)
            urlConnection.setRequestProperty("Accept-Language", "jp");

            // 接続
            urlConnection.connect();

            int resp = urlConnection.getResponseCode();

            switch (resp){
                case HttpURLConnection.HTTP_OK:
                    try(InputStream is = urlConnection.getInputStream()){
                        bmp = BitmapFactory.decodeStream(is);
                        is.close();
                    } catch(IOException e){
                        e.printStackTrace();
                    }
                    break;
                case HttpURLConnection.HTTP_UNAUTHORIZED:
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            Log.d("debug", "downloadImage error");
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        return bmp;
    }

    /**
     * Bitmapを回転して返却
     *
     * @param beforeBmp
     * @param degrees
     * @return
     */
    Bitmap rotate90(Bitmap beforeBmp, int degrees) {
        int w = beforeBmp.getWidth();
        int h = beforeBmp.getHeight();

        Matrix m = new Matrix();
        m.setRotate(degrees);

        Bitmap afterBmp = Bitmap.createBitmap(beforeBmp, 0, 0, w, h, m, false);
        return afterBmp;
    }

    /**
     * Bitmapをファイル保存
     *
     * @param bmp
     * @param path
     * @return
     */
    public boolean saveBitmap(Bitmap bmp, String path) {
        boolean result = false;
        try {
            // 保存処理開始
            FileOutputStream fos = null;
            fos = new FileOutputStream(new File(path));

            // jpegで保存
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);

            // 保存処理終了
            fos.close();

            result = true;
        } catch (Exception e) {
            Log.e("Error", "" + e.toString());
            result = false;
        }
        return result;
    }


    /**
     * 文字列をAES暗号化
     *
     * @param text
     * @return
     */
    @JavascriptInterface
    public String encrypt(String text) {
        String result = null;
        try {
            Util util = new Util();
            result = util.encrypt( text, MainActivity.AESKEY );
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 文字列をAES復号化
     *
     * @param text
     * @return
     */
    @JavascriptInterface
    public String decrypt(String text) {
        String result = null;
        try {
            Util util = new Util();
            result = util.decrypt( text, MainActivity.AESKEY );
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return result;
    }



    /**
     * アプリバージョン取得
     *
     * @return
     */
    @JavascriptInterface
    public String getAppVersion() {
        String result = "";

        PackageManager pm = getMainActivity().getPackageManager();
        String packageName = getMainActivity().getPackageName();
        PackageInfo info = null;
        try {
            info = pm.getPackageInfo(packageName, PackageManager.GET_META_DATA);
            int currentVersionCode = info.versionCode;
            String currentVersionName = info.versionName;
            if (MainActivity.DEBUG_VER.length() > 0 && MainActivity.DEBUG_VER.startsWith("debug")) {
                result = currentVersionName + "(DEBUG)," + currentVersionCode;
            } else {
                result = currentVersionName + "(RELEASE)," + currentVersionCode;
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * ローカルストレージフォルダの削除
     * （２日以内に生成されたファイルは削除しない：メール送信など非同期でファイルアクセスされることがあるため）
     *
     * @param path
     */
    @JavascriptInterface
    public void removeFolder2Day(String path) {
        getMainActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //あるディレクトリ配下のファイル全削除
                File[] objFiles = (new File(path)).listFiles();
                if (objFiles != null) {
                    for (int i = 0; i < objFiles.length; i++) {
                        Date lastModDate = new Date(objFiles[i].lastModified());
                        Date nowDate = new Date();

                        // カレンダークラスのインスタンスを取得
                        Calendar cal = Calendar.getInstance();
                        // 現在時刻を設定
                        cal.setTime(nowDate);
                        // 2日を減算
                        cal.add(Calendar.DATE, -2);
                        nowDate = cal.getTime();

                        int diff = lastModDate.compareTo(nowDate);
                        if (diff < 0) {
                            objFiles[i].delete();
                        }
                    }
                }
            }
        });
    }

    /**
     * ローカルストレージフォルダの削除
     *
     * @param path
     */
    @JavascriptInterface
    public void removeFolder(String path) {
        getMainActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //あるディレクトリ配下のファイル全削除
                File[] objFiles = (new File(path)).listFiles();
                if (objFiles != null) {
                    for (int i = 0; i < objFiles.length; i++) {
                        objFiles[i].delete();
                    }
                }
            }
        });
    }

    /**
     * アプリローカルフォルダ名の主t六
     *
     * @return
     */
    @JavascriptInterface
    public String getFilesDir() {
        String result = Util.DIRNAME;
        File file = getMainActivity().getCacheDir();
        if (true) {
            result = file.getPath();
        }
        return result;
    }

    /**
     * base64エンコードされたjpegをファイル保存
     *
     * @param filepath
     * @param base64
     */
    @JavascriptInterface
    public void saveJpeg(String filepath, String base64) {
        File file = new File(filepath);
        file.delete();
        file.getParentFile().mkdirs();

        if (base64.length() > 0) {
            byte[] decodedString = Base64.decode(base64, Base64.DEFAULT);
            Bitmap bmp = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            if (bmp != null) {
                //make file
                try {
                    FileOutputStream out = new FileOutputStream(file);
                    boolean result = bmp.compress(Bitmap.CompressFormat.JPEG, 100, out);
                    out.flush();
                    out.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * bitmapをファイル保存
     *
     * @param path
     * @param bitmap
     */
    public void saveBitmapToSd(String path, Bitmap bitmap) {
        try {
            File file = new File(path);
            file.getParentFile().mkdirs();

            // 保存処理開始
            FileOutputStream fos = null;
            fos = new FileOutputStream(file);

            // jpegで保存
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);

            // 保存処理終了
            fos.close();
        } catch (Exception e) {
            Util util = new Util();
            util.myLog("Error", "" + e.toString());
        }
    }

    /**
     * bitmapファイルを縮小する
     * （カメラ撮影した画像を小さくする）
     *
     * @param path
     * @param width
     * @param height
     * @param reverseUpdown
     * @param reverseLeftRight
     */
    @JavascriptInterface
    public void shrinkPhoto(String path, int width, int height, boolean reverseUpdown, boolean reverseLeftRight) {
        Bitmap bitmap = BitmapUtil.createBitmap(path, width, height);
        if (bitmap.getWidth() < bitmap.getHeight()) {
            int tmp = width;
            width = height;
            height = tmp;
            bitmap.recycle();
            bitmap = BitmapUtil.createBitmap(path, width, height);
        }
        if (bitmap.getHeight() > height || bitmap.getWidth() > width) {
            Bitmap bitmap2 = BitmapUtil.resize(bitmap, width, height);
            bitmap.recycle();
            bitmap = bitmap2;
        }
        width = bitmap.getWidth();
        height = bitmap.getHeight();
        if (reverseUpdown) {
            // 上下反転
            Matrix matrix = new Matrix();
            matrix.preScale(1, -1);
            // 回転したビットマップを作成  
            Bitmap bitmap2 = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
            bitmap.recycle();
            bitmap = bitmap2;
        }
        if (reverseLeftRight) {
            // 上下反転
            Matrix matrix = new Matrix();
            matrix.preScale(-1, 1);
            // 回転したビットマップを作成  
            Bitmap bitmap2 = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
            bitmap.recycle();
            bitmap = bitmap2;
        }
        saveBitmapToSd(path, bitmap);
        bitmap.recycle();
    }

    /**
     * 画像ファイルを読み込みpngのbase64エンコードして取得
     *
     * @param path
     * @return
     */
    @JavascriptInterface
    public String getDataBase64(String path) {
        String result = "";
        File file = new File(path);
        if (file.exists()) {
            BitmapFactory.Options opt = new BitmapFactory.Options();
            Bitmap bitmap = BitmapFactory.decodeFile(file.getPath(), opt);
            Util util = new Util();
            String bmpString = Base64.encodeToString(util.convertBitmap2ByteArray(bitmap), Base64.DEFAULT);
            result = "data:image/png;base64," + bmpString;
        }
        return result;
    }

    /**
     * 文字列をファイル保存
     *
     * @param path
     * @param data
     * @return
     */
    @JavascriptInterface
    public boolean writeFile(String path, String data) {
        Util util = new Util();
        return util.putUrlFile(path, data);
    }

    /**
     * 文字列をCSV保存（BOMを付加する）
     *
     * @param path
     * @param data
     * @return
     */
    @JavascriptInterface
    public boolean writeFileCSV(String path, String data) {
        byte [] bom = { (byte)0xef, (byte)0xbb, (byte)0xbf };

        String strBom = new String(bom);
        Util util = new Util();
        return util.putUrlFile(path, strBom + data);
    }

    /**
     * zipファイルの解凍
     *
     * @param zipFilePath
     * @param destinationPath
     * @param password
     */
    @JavascriptInterface
    public void unzipfile(String zipFilePath, String destinationPath, String password) {
        Util util = new Util();
        util.uncompressFiles(zipFilePath, destinationPath, password);
    }

    /**
     * ファイル読み込み
     *
     * @param path
     * @return
     */
    @JavascriptInterface
    public String getFile(String path) {
        Util util = new Util();
        String result = util.getUrlFile(path);
        return result;
    }

    /**
     * URLファイルをローカルストレージファイルに保存
     *
     * @param path
     * @param url
     * @return
     */
    @JavascriptInterface
    public boolean downloadPath(String path, String url) {
        boolean result = false;
        int BUFFER_SIZE = 10240;
        File file = new File(path);
        file.getParentFile().mkdirs();
        file.delete();

        try {
            Util util = new Util();
            byte[] buffer = util.getUrlContent(url);
            if (buffer != null) {
                BufferedOutputStream out;

                file.getParentFile().mkdirs();

                out = new BufferedOutputStream(
                        new FileOutputStream(file, false), BUFFER_SIZE);

                out.write(buffer);
                out.flush();
                out.close();
                result = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * デバイスIMEI取得
     * （IMEIが取得できない場合はMACアドレスとなる）
     *
     * @return
     */
    @JavascriptInterface
    public String getIMEI() {
        return getDeviceId();
    }
    /**
     * デバイス固有ID取得
     *
     * @return
     */
    @JavascriptInterface
    public String getDeviceId() {
        Util util = new Util();
        String result = util.getDeviceid();
        return result;
    }

    /**
     * ローカルストレージへログ保存
     *
     * @param mes
     */
    @JavascriptInterface
    public void log(String mes)
    {
        Util util = new Util();
        util.myLog(Util.APP_NAME, mes);
    }

}
