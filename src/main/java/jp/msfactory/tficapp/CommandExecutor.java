package jp.msfactory.tficapp;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * コマンドライン実行クラス
 */
public class CommandExecutor {

    private static boolean isStopProcess = false;

    /**
     * コマンドラインを実行して結果を返す
     *
     * @param command
     * @return
     * @throws IOException
     */
    public static String execCommand(String command) throws IOException {
        isStopProcess = false;

        if (command == null || command.equals("")) {
            return null;
        }
        Runtime runtime = Runtime.getRuntime();
        Process process = runtime.exec(command);
        String result = readInputStream(process.getInputStream());
        if (result == null || result.equals("")) {
            result = readInputStream(process.getErrorStream());
        }

        if (process != null) {
            process.destroy();
        }

        return result;
    }

    private static String readInputStream(InputStream in) throws IOException {
        InputStreamReader inReader;
        BufferedReader bufReader = null;
        StringBuffer output = new StringBuffer();
        try {
            inReader = new InputStreamReader(in);
            bufReader = new BufferedReader(inReader);

            String line;
            while (((line = bufReader.readLine()) != null) && !isStopProcess) {
                output.append(line + "\n");
                Log.d("CommandExe", "output:" + output);
            }

        } finally {
            if (bufReader != null) {
                bufReader.close();
            }

        }

        return output.toString();
    }

    public synchronized static void stopProcess() {
        isStopProcess = true;
    }
}
