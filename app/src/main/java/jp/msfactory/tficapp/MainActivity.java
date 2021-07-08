package jp.msfactory.tficapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements AutoUpdateEventListener, OnKeyboardVisibilityListener {
    private final String TAG = MainActivity.class.getName();
    private Bundle savedInstanceState;
    private static MainActivity mActivity;

    //ナビゲーションバー非表示フラグ
    boolean useFullscreen = true;
    //音声認識
    public TextToSpeech mTts;
    public SpeechRecognizer mRecognizer;
    public RecognitionListener mRecognitionListener;
    public String mSttCallback = "";
    public int mSttInterval = 1000;

    // updater
    public Updater updater;
    private ProgressDialog loadingDialog = null;

    public static final String AESKEY = "msfactoryTficapp";
    public static final String APP_NAME = "and-iot";
    public static final String DEBUG_VER = "";
    private static final String updateXmlUrl = "http://kujira-rs01.com/mics/" + APP_NAME + "/" + DEBUG_VER + "update.xml";
    public static final String fileprovider = "jp.msfactory." + APP_NAME + ".fileprovider";

    private ImageClassificationInterpriter _interpriter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        this.mActivity = this;
        this.savedInstanceState = savedInstanceState;
        //soft update
        updater = new Updater(this, updateXmlUrl);
        //updater.updateCheck();

        //指定自国でアプリ終了する
        setStopApp();

        // 全パーミッションのチェック
        if (allPermissionsGranted()) {
            //パーミッション許可なのでメイン処理開始
            startMain(this.savedInstanceState);
        } else {
            //パーミッションリクエスト
            ActivityCompat.requestPermissions(this,
                    REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        if (useFullscreen) {
            if (false) {
                findViewById(R.id.container).setOnSystemUiVisibilityChangeListener(
                        new View.OnSystemUiVisibilityChangeListener() {
                            @Override
                            public void onSystemUiVisibilityChange(int visibility) {
                                if (useFullscreen || visibility == View.SYSTEM_UI_FLAG_VISIBLE) {
                                    new Handler().postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            // ナビゲーションバーを消す
                                            findViewById(R.id.container)
                                                    .setSystemUiVisibility(
                                                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                                                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                                                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                                                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                                                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                                                    | View.SYSTEM_UI_FLAG_IMMERSIVE
                                                                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                                                    | View.SYSTEM_UI_FLAG_FULLSCREEN);
                                        }
                                    }, 2000);
                                }
                            }
                        });
            }
            // ナビゲーションバーを消す
            hideNavigationBar();
        }

        initSpeech2Text();

        // Other stuff...
        setKeyboardVisibilityListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (useFullscreen) {
            // ナビゲーションバーを消す
            hideNavigationBar();
        }

        // アプリロックダウン(KIOSK)処理
        DevicePolicyManager myDevicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName mDPM = new ComponentName(this, DeviceAdminReceiver.class);
        if (myDevicePolicyManager.isDeviceOwnerApp(this.getPackageName())) {
            String[] packages = {this.getPackageName()};
            myDevicePolicyManager.setLockTaskPackages(mDPM, packages);
        } else {
            boolean useDeviceOwnerMes = false;       //@@@2021/05/18 検眼はありませんのメッセージが分かりにくいので削除するかも
            if (useDeviceOwnerMes) {
                Toast.makeText(getMainActivity(),"簡易キオスクモードです。", Toast.LENGTH_LONG).show();
            }
        }
        if (true) {
            Handler handler = new Handler(getMainLooper());
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    startLockDown();
                }
            }, 500);
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        closeSubWindow();

        if (mTts != null) {
            mTts.stop();
            mTts.shutdown();
            mTts = null;
        }
        if (mRecognizer != null) {
            mRecognizer.stopListening();
            mRecognizer.cancel();
            mRecognizer.destroy();
            mRecognizer.setRecognitionListener(null);
            mRecognizer = null;
            mRecognitionListener = null;
        }

        _interpriter.close();
        _interpriter = null;

        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
            mReceiver = null;
        }

        webview.removeJavascriptInterface("sticktv");
        webview.stopLoading();
        webview.setWebChromeClient(null);
        webview.setWebViewClient(null);
        webview.destroy();
        jsobj = null;
        webview = null;
        mActivity = null;

        mSttCallback = null;
        mUIsMyHomeHook = null;
        urlMyHomeHook = null;
        delayFunc = null;
        mHandler.removeCallbacks(stopApp);
        stopApp = null;
        mHandler = null;
    }
    /**
     * KIOSアプリの状態
     * trueならロックダウン中（KIOSK状態）
     */
    boolean isLockDown = false;

    /**
     * アクティビティでアプリをロックダウン開始する（KIOSK状態）
     */
    public void startLockDown() {
        if (!isLockDown) {
            try {
                startLockTask();
            } catch (Exception e) {
                e.printStackTrace();
            }
            //本当にロックされたかはレシーバーでないとわからない
            isLockDown = true;
        }
    }
    /**
     * アクティビティでアプリをロックダウン解除する（KIOSK解除）
     */
    public void stopLockDown() {
        if (isLockDown) {
            try {
                //@@@ロックされていない状態で呼び出すとアプリクラッシュ
                stopLockTask();
            } catch (Exception e) {
                e.printStackTrace();
            }
            //本当にアンロックされたかはレシーバーでないとわからない
            isLockDown = false;
        }
    }

    // スレッドUI操作用ハンドラ
    private Handler mHandler = new Handler();
    // テキストオブジェクト
    private Runnable stopApp;
    //reboot時刻
    private int stopHour = -1;
    private void setStopApp() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);

        String key = "reboothour";
        String hour = null;
        hour = sp.getString(key, "").trim();
        if (hour.length() > 0) {
            try {
                stopHour = Integer.parseInt(hour.trim());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            stopHour = -1;
        }

        stopApp = new Runnable() {
            public void run() {
                Calendar now = Calendar.getInstance();
                int _hour = now.get(Calendar.HOUR_OF_DAY);
                int _minute = now.get(Calendar.MINUTE);
                if (_hour == stopHour && _minute == 0) {
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            finish();
                            System.exit(1);  //Destroy whole Application
                        }
                    }, 60000);
                    mHandler.removeCallbacks(stopApp);
                } else {
                    mHandler.removeCallbacks(stopApp);
                    mHandler.postDelayed(stopApp, 60000);
                }
            }
        };
        mHandler.postDelayed(stopApp, 60000);
    }



/*
    public static void closeMain() {
        getMainActivity().moveTaskToBack(true);
    }
*/
    /**
     * メインアクティビティ開始処理
     * 全パーミッション許可後に開始する
     *
     * @param savedInstanceState
     */
    public void startMain(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            _interpriter = new ImageClassificationInterpriter(this);

            if (Build.VERSION.SDK_INT >= 23) {
                // Marshmallow 以上用の処理
                if (Settings.canDrawOverlays(this)) {
                } else {
                    // サブウィンドウ生成の権限がなかった場合は、下記で権限を取得する
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse(PermmisionPackageName));
                    startActivityForResult(intent, ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE);
                }
            }

            View view = this.findViewById(R.id.container);
            util = new Util();
            util.myLog(TAG, "stgartMain()");
            _deviceId = util.getDeviceid();

            if (true) {
                webview = new MyWebView(this);
                webview.setWebChromeClient(new myWebChromeClient());
                webview.setWebViewClient(new WebViewClient());
                // Configure the webview
                WebSettings s = webview.getSettings();
                s.setMediaPlaybackRequiresUserGesture(false);

                webview.setInitialScale(100);
                //webview.setVerticalScrollBarEnabled(false);
                //webview.requestFocusFromTouch();

                boolean useZoom = false;
                if (useZoom) {
                    s.setBuiltInZoomControls(true);
                    s.setSupportZoom(true);
                    s.setDisplayZoomControls(false);
                } else {
                    s.setBuiltInZoomControls(false);
                }
                s.setUseWideViewPort(true);
                s.setLoadWithOverviewMode(true);

                s.setJavaScriptEnabled(true);
                s.setJavaScriptCanOpenWindowsAutomatically(true);
                s.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
                // old s.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
    /*
                    s.setSaveFormData(false);
                    s.setSavePassword(false);
    */
                // enable navigator.geolocation
                s.setGeolocationEnabled(true);
                s.setGeolocationDatabasePath(this.getDir("geolocation", Context.MODE_PRIVATE).getPath());

                // enable Web Storage: localStorage, sessionStorage
                s.setDomStorageEnabled(true);
                s.setAppCacheMaxSize(5 * 1048576);
                String pathToCache = this.getDir("database", Context.MODE_PRIVATE).getPath();
                s.setAppCachePath(pathToCache);
                s.setAppCacheEnabled(true);

                s.setAllowFileAccess(true);
                s.setAllowContentAccess(true);
                s.setDatabaseEnabled(true);
                s.setLoadsImagesAutomatically(true);
                s.setJavaScriptCanOpenWindowsAutomatically(true);
                s.setSupportMultipleWindows(true);

                //noinspection deprecation
                s.setAllowFileAccessFromFileURLs(true);
                //noinspection deprecation
                s.setAllowUniversalAccessFromFileURLs(true);

                jsobj = new JsObject();
                //webview.clearFormData();
                webview.clearCache(true);
                webview.addJavascriptInterface(jsobj, "sticktv");

                ViewGroup wholder = (ViewGroup) view.findViewById(R.id.container);
                wholder.addView(webview, new RelativeLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT));

            }
            boolean useNetConnectRecv = true;
            if (useNetConnectRecv) {
                mReceiver = new mBroadcastReceiver();
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(ACTION_CONNECT);
                intentFilter.addAction(ACTION_TFLITE);
                registerReceiver(mReceiver, intentFilter);
            }

            webviewLoadurl(URL_SPLASH);

            boolean checkLocal = true;
            if (checkLocal) {
                String myHomeHook = util.getUrlFile(urlMyHomeHook).split("\n")[0].trim();
                if (myHomeHook != null && myHomeHook.length() > 0 && myHomeHook.startsWith("file://")) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            networkConnect();
                        }
                    }, 10000);
                }
            }
        }
    }

    private static final int REQUEST_CODE_PERMISSIONS  = 101;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.INTERNET,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,

            Manifest.permission.CALL_PHONE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.ACCESS_COARSE_LOCATION

    };


    /**
     * アプリパーミッションのリクエスト
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startMain(this.savedInstanceState);
            } else {
                Toast.makeText(this, "ユーザーから権限が許可されていません。",
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    /**
     * 全パーミッションの許可状態取得
     *
     * @return  trueなら全パーミッション取得済み
     */
    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * jabvascriptのeval()実行
     * @param func
     */
    public void execJavaScript(String func) {
        webview.loadUrl("javascript:" + func);
    }

    /**
     * 音声認識初期化処理
     */
    private void initSpeech2Text() {
        Boolean useStt = true;
        if (useStt) {
            mRecognitionListener = new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {

                }

                @Override
                public void onBeginningOfSpeech() {

                }

                @Override
                public void onRmsChanged(float rmsdB) {

                }

                @Override
                public void onBufferReceived(byte[] buffer) {

                }

                @Override
                public void onEndOfSpeech() {

                }

                /**
                 * 音声認識エラー処理
                 * javascriptにエラーメッセージの文字列を通知
                 * mSttCallbackがコールバック関数名
                 *
                 * @param error
                 */
                @Override
                public void onError(int error) {
                    String errorStr = "unknown";
                    switch (error) {
                        case SpeechRecognizer.ERROR_AUDIO:
                            errorStr = "Audio recording error";
                            break;
                        case SpeechRecognizer.ERROR_CLIENT:
                            errorStr = "Other client side errors";
                            break;
                        case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                            errorStr = "Insufficient permissions";
                            break;
                        case SpeechRecognizer.ERROR_NETWORK:
                            errorStr = "Network related errors";
                            break;
                        case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                            errorStr = "Network operation timed out";
                            break;
                        case SpeechRecognizer.ERROR_NO_MATCH:
                            errorStr = "No recognition result matched";
                            break;
                        case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                            errorStr = "RecognitionService busy";
                            break;
                        case SpeechRecognizer.ERROR_SERVER:
                            errorStr = "Server sends error status";
                            break;
                        case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                            errorStr = "No speech input";
                            break;
                        default:
                            errorStr = "unknown";
                            break;
                    }
                    String errorMes = "音声認識エラー:" + error + "," + errorStr;

                    //javascriptのコールバック通知
                    //パラメータとしてエラーメッセージを通知
                    if (mSttCallback != null && mSttCallback.length() > 0) {
                        String exec = "if(typeof "+ mSttCallback + " == 'function') { " + mSttCallback + "('" + errorMes + "'); }";
                        execJavaScript(exec);
                    }
                    if (true) {
                        //showUiToast(errorMes);
                        if (mSttCallback != null && mSttCallback.length() > 0) {
                            try {
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        mRecognizer.cancel();
                                    }
                                });
                                Thread.sleep(mSttInterval);
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                                        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                                        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getMainActivity().getPackageName());
                                        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

                                        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3600000);
                                        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 3600000);
                                        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3600000);

                                        mRecognizer.startListening(intent);
                                    }
                                });
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                        }
                    }
                }

                /**
                 * 音声認識結果処理
                 * javascriptに音声認識結果の文字列を通知
                 * mSttCallbackがコールバック関数名
                 * コールバックのパラメータは'音声認識:' + 認識結果
                 *
                 * @param results
                 */
                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> values = results.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION);
                    for (String val : values) {
                        //webview notify
                        if (mSttCallback != null && mSttCallback.length() > 0) {
                            String exec = "if(typeof "+ mSttCallback + " == 'function') { " + mSttCallback + "('音声認識:" + val + "'); }";
                            execJavaScript(exec);
                        }
                        //showUiToast("ボイス：" + val);
                    }
                    if (true) {
                        if (mSttCallback != null && mSttCallback.length() > 0) {
                            try {
                                Thread.sleep(mSttInterval);
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                                        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                                        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getMainActivity().getPackageName());
                                        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

                                        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3600000);
                                        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 3600000);
                                        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3600000);

                                        mRecognizer.startListening(intent);
                                    }
                                });
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }

                /**
                 * 音声認識途中結果処理
                 * javascriptに音声認識結果の文字列を通知
                 * mSttCallbackがコールバック関数名
                 * コールバックのパラメータは'音声認識（パーシャル）:' + 認識結果
                 *
                 * @param partialResults
                 */
                @Override
                public void onPartialResults(Bundle partialResults) {
                    ArrayList<String> values = partialResults.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION);
                    for (String val : values) {
                        //webview notify
                        boolean usePers = false;
                        if (usePers && mSttCallback != null && mSttCallback.length() > 0) {
                            String exec = "if(typeof "+ mSttCallback + " == 'function') { " + mSttCallback + "('音声認識（パーシャル）:" + val + "'); }";
                            execJavaScript(exec);
                        }
                        //showUiToast("ボイス：" + val);
                    }
                }

                @Override
                public void onEvent(int eventType, Bundle params) {

                }
            };

            //startSpeechRecognition()
            mRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            mRecognizer.setRecognitionListener(mRecognitionListener);
            if (false) {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getMainActivity().getPackageName());
                intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

                intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3600000);
                intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 3600000);
                intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3600000);

                mRecognizer.startListening(intent);
            }

        }
    }

    /**
     * アプリアップデーター更新ありの処理
     *
     */
    @Override
    public void onUpdateAvailable() {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                showUpdateDialog();
            }
        });
    }

    /**
     * アプリアップデーターダウンロード鑑賞処理
     * 自動的にインストールする
     *
     */
    @Override
    public void onUpdateApkDownloadComplete() {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                cancelLoadingDialog();
                stopLockDown();
                updater.installApk();
            }
        });
    }

    /**
     * アプリアップデーターダウンロード失敗処理
     *
     */
    @Override
    public void onUpdateApkDownloadError() {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                cancelLoadingDialog();
            }
        });
    }

    /**
     * アプリアップデーターダイアログ表示
     *
     */
    private void showUpdateDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Update")
                //.setIcon(R.drawable.ic_launcher)
                .setMessage("Update available")
                .setPositiveButton("Update",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                showLoadingDialog();
                                updater.downloadApk();
                            }
                        }).setNegativeButton("Cancel", null).show();
    }
    /**
     * アプリアップデーターダイアログ表示
     * アップデート中なのでクルクルする
     *
     */
    private void showLoadingDialog() {
        loadingDialog = ProgressDialog.show(this, "", "downloading ...", true);
    }
    /**
     * アプリアップデーターダイアログ非表示
     * クルクルを停止する
     *
     */
    private void cancelLoadingDialog() {
        if (loadingDialog != null)
            loadingDialog.cancel();
    }



    private Util util;

    class MyWebView extends WebView {
        public MyWebView(Context context) {
            super(context);
        }

        private boolean useStopLockDown = true;
        private int cntbackpress = 0;
        private long lastTime = 0;
        private long settingTime = 0;

        @Override
        public boolean onKeyDown(int keyCode, KeyEvent event) {
            boolean useBackkey = false;  //@@@sz3
            if (useBackkey) {
                return super.onKeyDown(keyCode, event);
            }
            boolean result = false;
            if (result == false) {
                if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) {
                    boolean useBackWebview = false;
                    if (useBackWebview) {
                        //最初のページは起動ロゴなので戻れないようにする
                        if (canGoBackOrForward(-2)) {
                            long now = System.currentTimeMillis();
                            if (now >= (settingTime + 5000)) {
                                settingTime = 0;
                                goBack();
                            }
                        }
                    }

                    //5秒間に20回押されたらロックダウン解除
                    if (useStopLockDown) {
                        long now = System.currentTimeMillis();
                        if (now >= (lastTime + 5000)) {
                            lastTime = now + 5000;
                            cntbackpress = 0;
                        } else {
                            cntbackpress++;
                            if (cntbackpress >= 20) {
                                lastTime = now + 5000;
                                settingTime = now + 5000;
                                cntbackpress = 0;
                                webview.loadUrl(URL_SETTING);
                            }
                        }
                    }

                    result = true;
                } else {
                    result = super.onKeyDown(keyCode, event);
                }
            }
            return result;
        }
    }
    public void hideNavigationBar() {
        MainActivity.getMainActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (useFullscreen) {
                    new Handler().postDelayed(new Runnable() {
                          @Override
                          public void run() {
                              InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                              if (getCurrentFocus() != null) {
                                  inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                              }
                          }
                    }, 1000);
                    // ナビゲーションバーを消す
                    final View parentView = ((ViewGroup) findViewById(android.R.id.content)).getChildAt(0);
                    parentView.setOnSystemUiVisibilityChangeListener(
                            new View.OnSystemUiVisibilityChangeListener() {
                                @Override
                                public void onSystemUiVisibilityChange(int visibility) {
                                    if (useFullscreen || visibility == View.SYSTEM_UI_FLAG_VISIBLE) {
                                        new Handler().postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                // ナビゲーションバーを消す
                                                final View parentView = ((ViewGroup) findViewById(android.R.id.content)).getChildAt(0);
                                                parentView.setSystemUiVisibility(
                                                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                                        | View.SYSTEM_UI_FLAG_IMMERSIVE
                                                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
                                            }
                                        }, 2000);
                                    }
                                }
                            });

                    parentView.setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_IMMERSIVE
                                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_FULLSCREEN);
                }
            }
        });
    }
    private class myWebChromeClient extends WebChromeClient {
        @Override
        public void onPermissionRequest(PermissionRequest request) {
            getMainActivity().runOnUiThread(new Runnable() {
                @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void run() {
                    request.grant(request.getResources());
                }
            });
        }

        @Override
        public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, final JsPromptResult result) {
            final EditText editView = new EditText(getMainActivity());
            editView.setText(defaultValue);

            //これで切り分けるのは本当はいけないな
            if (false) {
                if (message.contains("パスワード")) {
                    editView.setInputType(InputType.TYPE_CLASS_TEXT |
                            InputType.TYPE_TEXT_VARIATION_PASSWORD);
                }
            }

            AlertDialog.Builder dlg = new AlertDialog.Builder(mActivity);
            dlg.setMessage(message);
            //dlg.setTitle("入力");
            dlg.setView(editView);
            dlg.setCancelable(true);
            dlg.setPositiveButton(android.R.string.ok,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            String value = editView.getText().toString();
                            //hideNavigationBar();
                            result.confirm(value);
                        }
                    });
            dlg.setNegativeButton(android.R.string.cancel,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            //hideNavigationBar();
                            result.cancel();
                        }
                    });
            dlg.setOnCancelListener(
                    new DialogInterface.OnCancelListener() {
                        public void onCancel(DialogInterface dialog) {
                            //hideNavigationBar();
                            result.cancel();
                        }
                    });
/*
			dlg.setOnKeyListener(new DialogInterface.OnKeyListener() {
				//DO NOTHING
				public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
					if (keyCode == KeyEvent.KEYCODE_BACK)
					{
                        //hideNavigationBar();
						result.cancel();
						return false;
					}
					else
						return true;
				}
			});
*/
            dlg.create();
            AlertDialog tmpDlg = dlg.show();
            Window window = tmpDlg.getWindow();
            //window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);

            return true;
        };

        @Override
        public boolean onJsAlert(WebView view, String url,	final String message, final JsResult result) {
            AlertDialog.Builder dlg = new AlertDialog.Builder(mActivity);
            dlg.setMessage(message);
            //dlg.setTitle("アラート");
            //Don't let alerts break the back button
            dlg.setCancelable(true);
            dlg.setPositiveButton(android.R.string.ok,
                    new AlertDialog.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            //hideNavigationBar();
                            result.confirm();
                        }
                    });
            dlg.setOnCancelListener(
                    new DialogInterface.OnCancelListener() {
                        public void onCancel(DialogInterface dialog) {
                            //hideNavigationBar();
                            result.cancel();
                        }
                    });
            dlg.setOnKeyListener(new DialogInterface.OnKeyListener() {
                //DO NOTHING
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                    if (keyCode == KeyEvent.KEYCODE_BACK)
                    {
                        //hideNavigationBar();
                        result.confirm();
                        return false;
                    }
                    else
                        return true;
                }
            });
            dlg.create();
            AlertDialog tmpDlg = dlg.show();
            Window window = tmpDlg.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);

            return true;
        }
        @Override
        public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
            AlertDialog.Builder dlg = new AlertDialog.Builder(mActivity);
            dlg.setMessage(message);
            //dlg.setTitle("確認");
            dlg.setCancelable(true);
            dlg.setPositiveButton(android.R.string.ok,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            //hideNavigationBar();
                            result.confirm();
                        }
                    });
            dlg.setNegativeButton(android.R.string.cancel,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            //hideNavigationBar();
                            result.cancel();
                        }
                    });
            dlg.setOnCancelListener(
                    new DialogInterface.OnCancelListener() {
                        public void onCancel(DialogInterface dialog) {
                            //hideNavigationBar();
                            result.cancel();
                        }
                    });
            dlg.setOnKeyListener(new DialogInterface.OnKeyListener() {
                //DO NOTHING
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                    if (keyCode == KeyEvent.KEYCODE_BACK)
                    {
                        //hideNavigationBar();
                        result.cancel();
                        return false;
                    }
                    else
                        return true;
                }
            });
            dlg.create();
            AlertDialog tmpDlg = dlg.show();
            Window window = tmpDlg.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);

            return true;
        }
    };


    mBroadcastReceiver mReceiver;


    public static final String URL_SPLASH = "file:///android_asset/splash/splash.html";
    //public static final String URL_MYHOME = "https://thermoin-login.and-iot.jp/front/";
    //public static final String URL_MYHOME = "https://front.and-iot.jp/";
    public static final String URL_MYHOME = "https://device-agency.co.jp/mics/" + APP_NAME + "/Tflite.html";
    public static final String URL_SETTING = "https://device-agency.co.jp/mics/" + APP_NAME + "/setting.html";
    public static final String ACTIVATION_SCHEME = "https";
    public static final String ACTIVATION_AUTHORITY = "device-agency.co.jp";
    public static final String ACTIVATION_PATH = "/mics/" + APP_NAME + "/check.php";

    private JsObject jsobj;
    private WebView webview;
    private String _deviceId;

    private boolean useActivate = false;


    //
    // load url
    //
    public void loadUrl2Data(String url) {
        getMainActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (webview != null) {
                    webview.loadUrl(url);
                }
            }
        });
    }
    //HomeUrl hook
    private boolean mUIbExistyHomeHook;
    private String mUIsMyHomeHook = "";
    private String urlMyHomeHook = "file:///sdcard/" + Util.APP_NAME + "/myHomeHook.txt";

    private void loadMyHome() {
        mUIbExistyHomeHook = false;
        boolean useLoadBefore = false;
        if (useLoadBefore) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    mUIsMyHomeHook = util.getUrlFile(urlMyHomeHook).split("\n")[0].trim();
                    String text = util.getUrlFile(mUIsMyHomeHook);
                    mUIbExistyHomeHook = (text.length() > 0);
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
        } else {
            mUIsMyHomeHook = util.getUrlFile(urlMyHomeHook).split("\n")[0].trim();
            mUIbExistyHomeHook = (mUIsMyHomeHook.length() > 0);
        }

        webview.clearCache(true);

        //default url
        SharedPreferences sps = PreferenceManager
                .getDefaultSharedPreferences(this);
        String urltoken = sps.getString("urlToken", "");

        String url = URL_MYHOME;
        if (mUIbExistyHomeHook) {{
            url = mUIsMyHomeHook;
        }}
        if (urltoken.length() > 0) {
            url = urltoken;
        }
        if (_deviceId != null) {
            if (url.contains("?")) {
                url += "&";
            } else {
                url += "?";
            }
            url += "mac=" + _deviceId;
        }
        loadUrl2Data(url);
    }


    /*
     * ネットワーク接続処理
     */
    public void networkConnect()
    {
        if (useActivate) {
            new Handler().postDelayed(delayFunc, 1000);
        } else {
            loadMyHome();
        }
    }
    private Runnable delayFunc = new Runnable() {
        @Override
        public void run() {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    activate();
                }
            });
            thread.start();
        }
    };

    public void activate() {

        // 遅延実行したい処理
        String scheme = ACTIVATION_SCHEME;
        String authority = ACTIVATION_AUTHORITY;
        String path = ACTIVATION_PATH;

        SharedPreferences sps = PreferenceManager
                .getDefaultSharedPreferences(this);
        String serialno = "";
        try {
            serialno = util.decrypt(sps.getString("serialno", ""), MainActivity.AESKEY);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Uri.Builder uriBuilder = new Uri.Builder();

        uriBuilder.scheme(scheme);
        uriBuilder.authority(authority);
        uriBuilder.path(path);
        uriBuilder.appendQueryParameter("deviceid", _deviceId);
        uriBuilder.appendQueryParameter("serialno", serialno);
        String url = uriBuilder.toString();

        String result = util.getUrlFile(url);
        String[] results = result.split(",");

        if (results != null && results.length > 0 && results[0].trim().equals("OK")) {
            getMainActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    loadMyHome();
                }
            });
        } else {
            getMainActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //テキスト入力を受け付けるビューを作成します。
                    EditText editView = new EditText(getMainActivity());
                    new AlertDialog.Builder(getMainActivity())
                            .setIcon(android.R.drawable.ic_dialog_info)
                            .setTitle("認証エラー")
                            .setMessage("シリアル番号を入力してください")
                            //setViewにてビューを設定します。
                            .setView(editView)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    //入力した文字をトースト出力する
                                    String serialno = editView.getText().toString();
                                    try {
                                        serialno = util.encrypt(serialno, AESKEY);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    SharedPreferences sps = PreferenceManager
                                            .getDefaultSharedPreferences(getMainActivity());
                                    sps.edit().putString("serialno", serialno).commit();
                                    new Handler().postDelayed(delayFunc, 1000);
                                }
                            })
                            .setNegativeButton("キャンセル", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    new Handler().postDelayed(delayFunc, 1000);
                                }
                            })
                            .setCancelable(false)
                            .show();
                }
            });
        }
    }

    /**
     * 接続状態の変化を受け取るレシーバークラス
     */
    public static final String ACTION_TFLITE = "jp.msfactory.ACTION_TF";
    public static final String ACTION_CONNECT= "android.net.conn.CONNECTIVITY_CHANGE";
    public String tflite_callback = null;
    public void setTfliteCallback(String csallback) {
        tflite_callback = csallback;
    }

    public void webviewClearHistory() { webview.clearHistory(); }
    public void webviewLoadurl(String url) {
        webview.loadUrl(url);
    }
    public void webviewLoadUrl2Data(String url) {
        loadUrl2Data(url);
    }


    public class mBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_TFLITE)) {
                String text = intent.getStringExtra("text").replaceAll("\n", "<br>");
                boolean isCallbackJson = true;
                if (isCallbackJson) {
                    text = "[";
                    String [] matchs = intent.getStringExtra("text").split("\n");
                    int cnt = 0;
                    for (int n = 0; n < matchs.length; n++) {
                        if (matchs[n].length() == 0) {
                            continue;
                        }
                        if (cnt > 0) {
                            text += ", ";
                        }

                        String label = matchs[n].split(":")[0].trim().toLowerCase();
                        int st = label.indexOf(' ');
                        st = -1;
                        if (st >= 0) {
                            label = label.substring(st+1);
                        }
                        label.replaceAll("\"", "\\\"");
                        String pers = matchs[n].split(":")[1].trim().split("%")[0].trim();
                        int val = Integer.valueOf(pers);
                        text += " {\"name\":\"" + label + "\", \"value\":" + val + "}";
                        cnt++;
                    }
                    text += " ]";
                }
                if (tflite_callback != null && tflite_callback.length() > 0) {
                    webview.loadUrl("javascript:if(typeof " + tflite_callback + " == 'function') { " + tflite_callback + "('" + text + "'); }");
                }
            }
            if (intent.getAction().equals(ACTION_CONNECT)) {
                ConnectivityManager cm = (ConnectivityManager) getMainActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = cm.getActiveNetworkInfo();

                boolean isConnected = (networkInfo != null && networkInfo.isConnected());
                if (isConnected) {
                    // 接続状態
                    networkConnect();
                } else {
                    // 切断状態
                }
            }
        }
    }



    private SubWindowFlagment _windowFragment;
    /*
     * サブウィンドウの生成
     */
    public void openSubWindow() {
        closeSubWindow();

        int LAYOUT_FLAG;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        }

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                LAYOUT_FLAG,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | // 下の画面を操作できるようにする
                        WindowManager.LayoutParams.FLAG_FULLSCREEN |
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.RIGHT; // 右上に表示

        getMainActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (_windowFragment == null) {
                    _windowFragment = new SubWindowFlagment();
                    View view = _windowFragment.loadView(_interpriter);
                    if (view != null) {
try {
    WindowManager _windowManager = (WindowManager) getMainActivity().getSystemService(Context.WINDOW_SERVICE);
    _windowManager.addView(view, params);
} catch (Exception e) {
    e.printStackTrace();
}
                    }
                } else {
                    Log.e(APP_NAME, "openSubWindow() is not null");
                }
            }
        });
    }


    /*
     * サブウィンドウを閉じる
     */
    public void closeSubWindow() {
        if (_windowFragment != null && _windowFragment.view != null)  {
            final SubWindowFlagment t_windowFragment = _windowFragment;
            _windowFragment = null;
            getMainActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (t_windowFragment != null) {
                        t_windowFragment.destroyView();
                        if (t_windowFragment.view != null) {
try {
                            WindowManager _windowManager = (WindowManager) getMainActivity().getSystemService(Context.WINDOW_SERVICE);
                            _windowManager.removeView(t_windowFragment.view);
} catch (Exception e) {
    e.printStackTrace();
}
                        }
                        t_windowFragment.view = null;
                    } else {
                        Log.e(APP_NAME, "closeSubWindow() is null");
                    }
                }
            });
        }
    }
    public static final int ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE = 1234; // 適当な数字でOK？
    public static final String PermmisionPackageName = "package:jp.msfactory.tficapp";

    public static MainActivity getMainActivity() {
        return MainActivity.mActivity;
    }




    private void setKeyboardVisibilityListener(final OnKeyboardVisibilityListener onKeyboardVisibilityListener) {
        final View parentView = ((ViewGroup) findViewById(android.R.id.content)).getChildAt(0);
        parentView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            private boolean alreadyOpen;
            private final int defaultKeyboardHeightDP = 100;
            private final int EstimatedKeyboardDP = defaultKeyboardHeightDP + (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? 48 : 0);
            private final Rect rect = new Rect();

            @Override
            public void onGlobalLayout() {
                int estimatedKeyboardHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, EstimatedKeyboardDP, parentView.getResources().getDisplayMetrics());
                parentView.getWindowVisibleDisplayFrame(rect);
                int heightDiff = parentView.getRootView().getHeight() - (rect.bottom - rect.top);
                boolean isShown = heightDiff >= estimatedKeyboardHeight;

                if (isShown == alreadyOpen) {
                    Log.i("Keyboard state", "Ignoring global layout change...");
                    return;
                }
                alreadyOpen = isShown;
                onKeyboardVisibilityListener.onVisibilityChanged(isShown);
            }
        });
    }


    @Override
    public void onVisibilityChanged(boolean visible) {
        //Toast.makeText(MainActivity.this, visible ? "Keyboard is active" : "Keyboard is Inactive", Toast.LENGTH_SHORT).show();
        if (visible == false) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    // ナビゲーションバーを消す
                    final View parentView = ((ViewGroup) findViewById(android.R.id.content)).getChildAt(0);
                    parentView.setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_IMMERSIVE
                                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_FULLSCREEN);
                }
            }, 1000);
        }
    }
}