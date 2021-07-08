package jp.msfactory.tficapp;

import android.app.Fragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Handler;

import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;


public class SubWindowFlagment extends Fragment {
    public View view;

    // UI
    private TextView textView;
    private ProgressBar barOkView;
    private ProgressBar barOkMatchView;
    private TextView textview_percOk;
    private ProgressBar barNgView;
    private ProgressBar barNgMatchView;
    private TextView textview_percNg;

    private boolean isCheck = true;     //check detect
    private long nextheckTime = 0;      //detect interval last time
    private long lastMatchTime = 0;     //detect match last time

    private static final long MATCH_CHECK_TIME = 2*1000;   //2.0sec
    private static final long STOP_CHECK_TIME = 1*1000;    //1.0sec

    private static final int MATCH_NONE = 0;
    private static final int MATCH_OK = 1;
    private static final int MATCH_NG = 2;
    private int oldMatch = MATCH_NONE;


    private CameraPreview mCamPreview = null;
    private int cameraId = 0;
    private String filePath = null;

    //max 4 people
    private static final int NUM_PERMIT = 1;
    private static Semaphore semaphore = new Semaphore(NUM_PERMIT);

    ImageClassificationInterpriter _interpriter;
    private static final String MODEL_PATH = "mobilenet_v1_1.0_224_quant.tflite";
    private static final boolean QUANT = true;
    private static final String LABEL_PATH = "labels.txt";
    private static final int INPUT_SIZE = 224;

    private int mCameraId = 0;
    Camera mCamera;

    public void destroyView() {
        mCamPreview.clearPreviewCallback();
        while (semaphore.availablePermits() < NUM_PERMIT) {
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        FrameLayout preview = (FrameLayout)view.findViewById(R.id.cameraPreview);
        preview.removeView(mCamPreview);

        mCamPreview.setCamera(null);
        mCamera.release();
        mCamera = null;
        mCamPreview = null;
        //view = null;
    }
    /*
     * 画面呼び出し
     */
    public View loadView(ImageClassificationInterpriter interpriter) {
        LayoutInflater inflater = LayoutInflater.from(MainActivity.getMainActivity());
        view =  inflater.inflate(R.layout.sub_window_fragment, null);

        // メインウィンドウを閉じるボタン
        Button windowButton = (Button)view.findViewById(R.id.closeMainButton);
        windowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeMainWindow();
            }
        });
        windowButton.setVisibility(View.VISIBLE);

        boolean useAutoCloseMain = false;
        if (useAutoCloseMain) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    // TODO: ここで処理を実行する
                    closeMainWindow();
                }
            }, 1000);
        }

        // 常駐アプリを閉じるボタン
        Button closeButton = (Button)view.findViewById(R.id.closeSubButton);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleSubWindow();
            }
        });
        closeButton.setVisibility(View.VISIBLE);

        // 常駐アプリを閉じるボタン
        TextView allButton = (TextView)view.findViewById(R.id.btn_all);
        allButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleSubWindow();
            }
        });
        closeButton.setVisibility(View.VISIBLE);

        // UI
        this.textView = view.findViewById(R.id.text_view);
        this.barOkView = view.findViewById(R.id.ProgressBarOk);
        this.barOkMatchView = view.findViewById(R.id.ProgressBarOkMatch);
        this.textview_percOk = view.findViewById(R.id.text_percOk);
        this.barNgView = view.findViewById(R.id.ProgressBarNg);
        this.barNgMatchView = view.findViewById(R.id.ProgressBarNgMatch);
        this.textview_percNg = view.findViewById(R.id.text_percNg);

        // 推論
        _interpriter = interpriter;

        cameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
        filePath = "/sdcard/Pictures";
        // 利用可能なカメラの個数を取得
        int numberOfCameras = Camera.getNumberOfCameras();
        if (cameraId >= numberOfCameras) {
            cameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
        }

        // FrameLayout に CameraPreview クラスを設定
        FrameLayout preview = (FrameLayout)view.findViewById(R.id.cameraPreview);
        Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
            public void onPreviewFrame(byte[] data, Camera camera) {
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (semaphore.availablePermits() > 0) {
                            try {
                                semaphore.acquire();

                                Camera.Parameters parameters = camera.getParameters();
                                final int width = parameters.getPreviewSize().width;
                                final int height = parameters.getPreviewSize().height;
                                int[] rgb = new int[(width * height)]; // ARGB8888の画素の配列
                                Bitmap bmp = null;
                                try {
                                    bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888); //ARGB8888で空のビットマップ作成
                                    decodeYUV420SP(rgb, data, width, height); // 変換
                                    bmp.setPixels(rgb, 0, width, 0, 0, width, height); // 変換した画素からビットマップにセット
                                    // ★
                                    List<Map.Entry<String, Float>> results = _interpriter.predict(bmp);
                                    MainActivity.getMainActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            viewAnalyze(results);
                                        }
                                    });
/*★DEBUG
                                    final Bitmap tBmp = Bitmap.createBitmap( bmp, 0, 0, bmp.getWidth(), bmp.getHeight());
                                    ImageView imageView = (ImageView) view.findViewById(R.id.imageViewResult);
                                    imageView.setImageBitmap(tBmp);
*/
                                } catch (Exception e) {
                                    // エラー
                                    e.printStackTrace();
                                }
                                if (bmp != null) {
                                    bmp.recycle();
                                    bmp = null;
                                }
                                rgb = null;
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            } finally {
                                semaphore.release();
                            }
                        }
                    }
                });
                thread.start();
            }
        };
        mCamera = Camera.open(cameraId);
        mCamPreview = new CameraPreview(MainActivity.getMainActivity(), previewCallback, cameraId);
        mCamPreview.setCamera(mCamera);
        preview.addView(mCamPreview);

        return view;
    }
    static public void decodeYUV420SP(int[] rgb, byte[] yuv420sp, int width, int height) {
        final int frameSize = width * height;

        for (int j = 0, yp = 0; j < height; j++) {
            int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
            for (int i = 0; i < width; i++, yp++) {
                int y = (0xff & ((int) yuv420sp[yp])) - 16;
                if (y < 0) y = 0;
                if ((i & 1) == 0) {
                    v = (0xff & yuv420sp[uvp++]) - 128;
                    u = (0xff & yuv420sp[uvp++]) - 128;
                }

                int y1192 = 1192 * y;
                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);

                if (r < 0) r = 0; else if (r > 262143) r = 262143;
                if (g < 0) g = 0; else if (g > 262143) g = 262143;
                if (b < 0) b = 0; else if (b > 262143) b = 262143;

                rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
            }
        }
    }

    private void viewAnalyze(List<Map.Entry<String, Float>> results) {
        isCheck = false;
        long nowTime = System.currentTimeMillis();
        if (nextheckTime == 0 || nowTime >= nextheckTime) {
            isCheck = true;
        }
        if (isCheck) {
            nextheckTime = nowTime + 1;
            int thre = 75;
            int colorSelected = Color.CYAN;
            int colorSelecting = Color.WHITE;
            String text = "";
            int count = 0;
            int matchCnt = 8;
            boolean isMatch = false;
            int nowMatch = MATCH_NONE;
            textview_percOk.setTextColor(colorSelecting);
            textview_percNg.setTextColor(colorSelecting);

            for (Map.Entry<String, Float> entry : results) {
                int percent = (int) (entry.getValue() * 100);
                text += entry.getKey() + " : " + percent + "%";

                if (entry.getKey().endsWith("パー")) {
                    barOkView.setProgress(percent);
                    barOkMatchView.setProgress(percent);
                    textview_percOk.setText("" + percent + "%");
                    if (percent >= thre) {
                        isMatch = true;
                        textview_percOk.setTextColor(colorSelected);
                        barOkView.setVisibility(View.GONE);
                        barOkMatchView.setVisibility(View.VISIBLE);
                        nowMatch = MATCH_OK;
                    } else {
                        barOkView.setVisibility(View.VISIBLE);
                        barOkMatchView.setVisibility(View.GONE);
                    }
                }
                if (entry.getKey().endsWith("グー")) {
                    barNgView.setProgress(percent);
                    barNgMatchView.setProgress(percent);
                    textview_percNg.setText("" + percent + "%");
                    if (percent >= thre) {
                        isMatch = true;
                        textview_percNg.setTextColor(colorSelected);
                        barNgView.setVisibility(View.GONE);
                        barNgMatchView.setVisibility(View.VISIBLE);
                        nowMatch = MATCH_NG;
                    } else {
                        barNgView.setVisibility(View.VISIBLE);
                        barNgMatchView.setVisibility(View.GONE);
                    }
                }

                count++;
                if (count >= matchCnt) break;
                text += "\n";
            }
            //textView.setText(text);

            boolean isSend = false;
            if (isMatch) {
                if (oldMatch != nowMatch) {
                    lastMatchTime = nowTime;
                } else {
                    if (nowTime >= (lastMatchTime + MATCH_CHECK_TIME)) {
                        isSend = true;
                    }
                }
            }
            oldMatch = nowMatch;

            //isSend = true;    //send to all
            if (isSend) {
                barOkView.setProgress(0);
                barOkMatchView.setProgress(0);
                textview_percOk.setText("");
                barNgView.setProgress(0);
                barNgMatchView.setProgress(0);
                textview_percNg.setText("");

                switch (nowMatch) {
                    case MATCH_OK:
                        textview_percOk.setText("パーを選択しました");
                        break;
                    case MATCH_NG:
                        textview_percNg.setText("グーを選択しました");
                        break;
                }

                Intent broadcastIntent = new Intent();
                broadcastIntent.setAction(MainActivity.ACTION_TFLITE);
                broadcastIntent.putExtra("text", new String(text));
                MainActivity.getMainActivity().sendBroadcast(broadcastIntent);
                nextheckTime = nowTime + STOP_CHECK_TIME;
                lastMatchTime = nowTime;
                oldMatch = MATCH_NONE;      //@@@無くすとリピートが早くなる
            } else {
                nextheckTime = nowTime + 1;
            }
        }
    }


    /*
     * メインウィンドウを閉じる
     */
    private void closeMainWindow() {
        MainActivity.getMainActivity().moveTaskToBack(true);
    }

    /*
     * サブウィンドウをトグル
     */
    public void toggleSubWindow() {
        LinearLayout rootView = (LinearLayout)view.findViewById(R.id.rootView);
        RelativeLayout tfView = (RelativeLayout)view.findViewById(R.id.tfView);
        FrameLayout preview = (FrameLayout)view.findViewById(R.id.cameraPreview);
        int visible = rootView.getVisibility();
        switch (visible) {
            case View.VISIBLE:
                visible = View.GONE;
                break;
            default:
                visible = View.VISIBLE;
                break;
        }
        if (true && mCamPreview != null) {
            mCamPreview.setVisibility(visible);
        }
        preview.setVisibility(visible);
        tfView.setVisibility(visible);
        rootView.setVisibility(visible);

    }
}
