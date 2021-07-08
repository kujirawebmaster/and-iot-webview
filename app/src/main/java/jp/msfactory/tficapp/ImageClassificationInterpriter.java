package jp.msfactory.tficapp;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tensor Flow lite の画像分類インタープリタ
 *
 */
public class ImageClassificationInterpriter {
    // パラメータ定数
    private static final int BATCH_SIZE = 1; //バッチサイズ
    private static final int INPUT_PIXELS = 3; //入力ピクセル
    private final static int INPUT_SIZE = 224; // 入力サイズ
    private boolean IS_QUANTIZED = true; //量子化
    private static final float IMAGE_MEAN = 127.5f;
    private static final float IMAGE_STD = 127.5f;

    // システム
    private Context context;
    private Interpreter interpreter;
    private List<String> labels;
    private int[] imageBuffer = new int[INPUT_SIZE * INPUT_SIZE];

    // 入力
    private Bitmap inBitmap;
    private Canvas inCanvas;
    private Rect inBitmapSrc = new Rect();
    private Rect inBitmapDst = new Rect(0, 0, INPUT_SIZE, INPUT_SIZE);
    private ByteBuffer inBuffer;

    // 出力
    private byte[][] outByteProbs;
    private float[][] outFloatProbs;

    // コンストラクタ
    public ImageClassificationInterpriter(Context context) {
        this.context = context;

        // モデルの読み込み
        MappedByteBuffer model = loadModel("model.tflite");

        // ラベルの読み込み
        this.labels = loadLabel("labels.txt");

        // インタプリタの生成
        Interpreter.Options options = new Interpreter.Options();
        //options.setUseNNAPI(true); //NNAPI
        //options.addDelegate(new GpuDelegate()); //GPU
        options.setNumThreads(1); // スレッド数
        this.interpreter = new Interpreter(model, options);

        // 入力の初期化
        this.inBitmap = Bitmap.createBitmap(
                INPUT_SIZE, INPUT_SIZE, Bitmap.Config.ARGB_8888);
        this.inCanvas = new Canvas(inBitmap);
        int numBytesPerChannel = IS_QUANTIZED ? 1 : 4;
        this.inBuffer = ByteBuffer.allocateDirect(
                BATCH_SIZE * INPUT_SIZE * INPUT_SIZE * INPUT_PIXELS * numBytesPerChannel);
        inBuffer.order(ByteOrder.nativeOrder());

        // 出力の初期化
        if (IS_QUANTIZED) {
            this.outByteProbs = new byte[1][labels.size()];
        } else {
            this.outFloatProbs = new float[1][labels.size()];
        }
    }
    public void close() {
        this.interpreter.close();
        context = null;
    }

    // モデルの読み込み
    private MappedByteBuffer loadModel(String modelPath) {
        try {
            AssetFileDescriptor fd = this.context.getAssets().openFd(modelPath);
            FileInputStream in = new FileInputStream(fd.getFileDescriptor());
            FileChannel fileChannel = in.getChannel();
            return fileChannel.map(FileChannel.MapMode.READ_ONLY,
                    fd.getStartOffset(), fd.getDeclaredLength());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ラベルの読み込み
    private List<String> loadLabel(String labelPath) {
        try {
            List<String> labels = new ArrayList<>();
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    this.context.getAssets().open(labelPath)));
            String line;
            while ((line = reader.readLine()) != null) {
                labels.add(line);
            }
            reader.close();
            return labels;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // 推論
    public List<Map.Entry<String, Float>> predict(Bitmap bitmap) {
        // 入力画像の生成
        int minSize = Math.min(bitmap.getWidth(), bitmap.getHeight());
        int dx = (bitmap.getWidth()-minSize)/2;
        int dy = (bitmap.getHeight()-minSize)/2;
        this.inBitmapSrc.set(dx, dy, dx+minSize, dy+minSize);
        inCanvas.drawBitmap(bitmap, this.inBitmapSrc, this.inBitmapDst, null);

        // 入力バッファの生成
        bmpToInBuffer(inBitmap);

        // 推論
        if (IS_QUANTIZED) {
            this.interpreter.run(this.inBuffer, this.outByteProbs);
        } else {
            this.interpreter.run(this.inBuffer, this.outFloatProbs);
        }

        // 結果の取得
        Map<String, Float> map = new HashMap();
        for (int i = 0; i < this.labels.size(); i++) {
            String title = this.labels.size() > i ? this.labels.get(i) : "unknown";
            float prob = getProb(i);
            map.put(title, prob);
        }

        // ソート
        List<Map.Entry<String, Float>> results = new ArrayList<>(map.entrySet());
        Collections.sort(results, new Comparator<Map.Entry<String, Float>>() {
            @Override
            public int compare(Map.Entry<String, Float> obj1, Map.Entry<String, Float> obj2) {
                return obj2.getValue().compareTo(obj1.getValue());
            }
        });
        return results;
    }

    // Bitmap → 入力バッファ
    private void bmpToInBuffer(Bitmap bitmap) {
        this.inBuffer.rewind();
        bitmap.getPixels(this.imageBuffer, 0, bitmap.getWidth(),
                0, 0, bitmap.getWidth(), bitmap.getHeight());
        int pixel = 0;
        for (int i = 0; i < INPUT_SIZE; ++i) {
            for (int j = 0; j < INPUT_SIZE; ++j) {
                int pixelValue = imageBuffer[pixel++];
                if (IS_QUANTIZED) {
                    inBuffer.put((byte)((pixelValue >> 16) & 0xFF));
                    inBuffer.put((byte)((pixelValue >> 8) & 0xFF));
                    inBuffer.put((byte)(pixelValue & 0xFF));
                } else {
                    inBuffer.putFloat((((pixelValue >> 16) & 0xFF)-IMAGE_MEAN)/IMAGE_STD);
                    inBuffer.putFloat((((pixelValue >> 8) & 0xFF)-IMAGE_MEAN)/IMAGE_STD);
                    inBuffer.putFloat(((pixelValue & 0xFF)-IMAGE_MEAN)/IMAGE_STD);
                }
            }
        }
    }

    //確率の取得
    private float getProb(int index) {
        if (IS_QUANTIZED) {
            return (this.outByteProbs[0][index] & 0xff)/255.0f;
        } else {
            return this.outFloatProbs[0][index];
        }
    }
}
