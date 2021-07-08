package jp.msfactory.tficapp;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public class Util {
	private MainActivity getMainActivity() {
		return MainActivity.getMainActivity();
	}
	/**
	 * メッセージをTOAST表示する
	 *
	 * @param activity
	 * @param message
	 */
	public void info(Activity activity, String message){
		try{
			Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
		}catch(Exception e){

		}
	}

	/**
	 * URLを読み込みUTF-8文字列を返す
	 *
	 * @param urlstr
	 * @return
	 */
	public String urlToStr(String urlstr){
		return urlToStr(urlstr, "UTF-8");
	}

	/**
	 * URLを読み込み文字列を返す
	 *
	 * @param urlstr
	 * @param charset
	 * @return
	 */
	public String urlToStr(String urlstr, String charset){
		try {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			urlToOutputStream(urlstr, os);
			return new String(os.toByteArray());
		} catch (Exception e) {
		    e.printStackTrace();
		}
		return null;
	}

	/**
	 * URLを読み込みファイルに保存する
	 *
 	 * @param urlstr
	 * @param file
	 * @return
	 */
	public static String urlToFile(String urlstr, File file){
		try {
		    FileOutputStream fileOutput = new FileOutputStream(file);
			urlToOutputStream(urlstr, fileOutput);
			return file.getAbsolutePath();
		} catch (Exception e) {
		    e.printStackTrace();
		}
		return null;
	}

	/**
	 * URLを読み込む
	 *
	 * @param urlstr
	 * @param os
	 */
	public static void urlToOutputStream(String urlstr, OutputStream os){

		try {
		    URL url = new URL(urlstr);
		    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
		    urlConnection.setRequestMethod("GET");
		    urlConnection.setDoOutput(true);
		    urlConnection.connect();

		    InputStream is = urlConnection.getInputStream();

		    byte[] buffer = new byte[1024];
		    int bufferLength = 0;

		    while ( (bufferLength = is.read(buffer)) > 0 ) {
		    	os.write(buffer, 0, bufferLength);
		    }
		    os.close();
		    is.close();

		} catch (Exception e) {
		    e.printStackTrace();
		}

	}


	/**
	 * Bitmapをバイト配列に変換
	 *
	 * @param bitmap
	 * @return
	 */
	public byte[] convertBitmap2ByteArray(Bitmap bitmap) {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
		byte[] bitmapdata = stream.toByteArray();

		return bitmapdata;
	}

	/**
	 * 文字列をローカルストレージに世損
	 *
	 * @param path
	 * @param data
	 * @return trueなら成功
	 */
	public boolean putUrlFile(String path, String data) {
		boolean result = false;
		int BUFFER_SIZE = 10240;

		try {
			byte[] buffer = data.getBytes();
			if (buffer != null) {
				BufferedOutputStream out;

				File file = new File(path);
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
	 * ファイルとフォルダをzip圧縮する
	 *
	 * @param files
	 * @param zipFilePath
	 * @param password
	 */
	public void compressFiles(
			List<File> files, String zipFilePath, String password)
	{
		int compressionMethod = Zip4jConstants.COMP_DEFLATE;
		int compressionLevel = Zip4jConstants.DEFLATE_LEVEL_NORMAL;
		int encryptionMethod = Zip4jConstants.ENC_METHOD_STANDARD;
		int aesKeyStrength = Zip4jConstants.AES_STRENGTH_256;

		try{
			ZipFile zipFile = new ZipFile(zipFilePath);

			/** パラメータ */
			ZipParameters params = new ZipParameters();
			params.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
			params.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_NORMAL);

			params.setCompressionMethod(compressionMethod);
			params.setCompressionLevel(compressionLevel);

			if (password != null) {
				params.setEncryptFiles(true);
				params.setEncryptionMethod(encryptionMethod);
				params.setAesKeyStrength(aesKeyStrength);
				params.setPassword(password);
			}

			/** ファイルの圧縮 */
			for(File file : files){
				myLog(APP_NAME, "zip:" + file.getPath());
				if(file.isFile())
					zipFile.addFile(file, params);
				else
					zipFile.addFolder(file, params);
			}
		} catch (ZipException e) {
			e.printStackTrace();
		}
	}

	/**
	 * zipファイルを解凍する
	 *
	 * @param zipFilePath
	 * @param destinationPath
	 * @param password
	 */
	public void uncompressFiles(String zipFilePath, String destinationPath, String password) {
		try {
			File dir = new File(destinationPath);
			if (!dir.exists()) {
				dir.mkdir();
			}
			try {
				ZipFile zipFile = new ZipFile(zipFilePath);
				if (zipFile.isEncrypted()) {
					zipFile.setPassword(password);
				}
				zipFile.extractAll(destinationPath);
			} catch (ZipException e) {
				e.printStackTrace();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * file pathをURLにエンコード
	 *
	 * @param file
	 * @return
	 */
	private String encodeUrl(String file) {
		URL url = null;
		try {
			url = new URL(file);
			Uri.Builder builder = new Uri.Builder();
			builder.scheme(url.getProtocol());
			builder.encodedAuthority(url.getHost());
			builder.path(url.getPath());
			String query = url.getQuery();
			if (query != null) {
				String[] querys = query.split("&");
				for (int i = 0; i < querys.length; i++) {
					String key = querys[i].split("=")[0].trim();
					String value = querys[i].split("=")[1].trim();
					builder.appendQueryParameter(key, value);
				}
			}
			return builder.build().toString();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return null;
	}
	/**
	 * InputStreamをバイト配列に変換する
	 *
	 * @param is
	 * @return バイト配列
	 */
	private byte[] getInputStreamBytes(InputStream is) {
		// byte型の配列を出力先とするクラス。
		// 通常、バイト出力ストリームはファイルやソケットを出力先とするが、
		// ByteArrayOutputStreamクラスはbyte[]変数、つまりメモリを出力先とする。
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		OutputStream os = new BufferedOutputStream(b);
		int c;
		try {
			while ((c = is.read()) != -1) {
				os.write(c);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (os != null) {
				try {
					os.flush();
					os.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		// 書き込み先はByteArrayOutputStreamクラス内部となる。
		// この書き込まれたバイトデータをbyte型配列として取り出す場合には、
		// toByteArray()メソッドを呼び出す。
		return b.toByteArray();
	}

	/**
	 * URLを読み込みバイト配列を返却
	 *
	 * @param path
	 * @return
	 */
	public byte[] getUrlContent(String path) {
		byte[] result = null;
		try {
			java.net.URL url = new URL(encodeUrl(path));
			InputStream is = url.openStream();
			result = getInputStreamBytes(is);
			is.close();
		} catch (FileNotFoundException e) {
			result = null;
		} catch (Exception e) {
			e.printStackTrace();
			result = null;
		}
		return result;
	}

	/**
	 * URLを読み込みUTF-8文字列を返却
	 *
	 * @param path
	 * @return
	 */
	public String getUrlFile(String path) {
		String result = "";
		if (path.length() == 0) {
			return result;
		}
		try {
			byte[] buffer = getUrlContent(path);
			if (buffer != null) {
				result = new String(buffer, "UTF-8").replace("\r", "").trim();
			}
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		return result;
	}
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
	 * デバイス固有ID取得
	 * （IMEI -> wlan0 MACアドレス-> eth0 MACアドレスの順番でデバイスを特定する）
	 *
	 * @return
	 */
	public String getDeviceid() {
		String deviceId = null;
		TelephonyManager telephonyManager = (TelephonyManager) getMainActivity().getSystemService(Context.TELEPHONY_SERVICE);

		int permissionCheck = ContextCompat.checkSelfPermission(getMainActivity(),
				Manifest.permission.READ_PHONE_STATE);
		if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
			//@sz2
			//deviceId = telephonyManager.getDeviceId();
		}
		if (deviceId == null) {
			deviceId = MyMacAddress.myMacAddressString();
		}
		if (deviceId == null) {
			deviceId = "";
		}
		return deviceId;
	}

	public static final String AESKEY = "kujira0123kujira";

	/**
	 * AES文字列復号化
	 *
	 * @param encrypted
	 * @param key
	 * @return
	 * @throws NoSuchPaddingException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeyException
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws UnsupportedEncodingException
	 */
	public String decrypt(String encrypted, String key) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, UnsupportedEncodingException {
		String strResult = "";
		//SecretKeySpec sks = new SecretKeySpec(key.getBytes(), "Blowfish");
		SecretKeySpec sks = new SecretKeySpec(key.getBytes(), "AES");

		try {
			// 復号
			Cipher c = null;
			//c = Cipher.getInstance("Blowfish/ECB/PKCS5Padding");
			c = Cipher.getInstance("AES/ECB/PKCS5Padding");
			c.init(Cipher.DECRYPT_MODE, sks);
			byte decrypted[] = c.doFinal(Base64.decode(encrypted, Base64.DEFAULT));
			strResult = new String(decrypted, "UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
		}
		// 暗号化文字列を返却
		return strResult;
	}

	/**
	 * AES文字列暗号化
	 *
	 * @param text
	 * @param key
	 * @return
	 * @throws NoSuchPaddingException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeyException
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 */
	public String encrypt(String text, String key) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
		String strResult = "";
		//SecretKeySpec sks = new SecretKeySpec(key.getBytes(), "AES");
		//SecretKeySpec sks = new SecretKeySpec(key.getBytes(), "Blowfish");
		SecretKeySpec sks = new SecretKeySpec(key.getBytes(), "AES");

		byte[] input = text.getBytes();

		try {
			// 暗号化
			Cipher c = null;
			//c = Cipher.getInstance("Blowfish/ECB/PKCS5Padding");
			c = Cipher.getInstance("AES/ECB/PKCS5Padding");
			c.init(Cipher.ENCRYPT_MODE, sks);
			byte encrypted[] = c.doFinal(input);
			strResult = Base64.encodeToString(encrypted, Base64.DEFAULT);
		} catch (Exception e) {
			e.printStackTrace();
		}
		// 暗号化文字列を返却
		return strResult;
	}

	public static final String APP_NAME = MainActivity.APP_NAME;
	public static final String DIRNAME = "/sdcard/" + APP_NAME;
	private static final String ERR_FILENAME = "/sdcard/" + APP_NAME + "/error.txt";
	private static final String LOG_DIRNAME = "/sdcard/" + APP_NAME + "/logs/";
	private static final String LOG_FILENAME = "/sdcard/" + APP_NAME + "/log.dat";

	/**
	 * ローカルストレージへのログ出力
	 *
	 * @param tag ログの分類タグ
	 * @param mes ログメッセージ
	 */
	public void myLog(String tag, String mes) {
		mylog(tag + ":" + mes);
	}

	/**
	 * ローカルストレージへのログ出力
	 *
	 * @param mes ログメッセージ
	 */
	public void mylog(String mes)
	{
		SharedPreferences sps = PreferenceManager
				.getDefaultSharedPreferences(getMainActivity());
		String logMode = sps.getString("logMode", "");

		boolean forceEnableLog = false;
		if (forceEnableLog || (logMode.length() > 0 && !logMode.equals("OFF"))) {
			long msec = System.currentTimeMillis();
			Date date = new Date(msec);
			final DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");

			removeLogs(LOG_DIRNAME, -21);   //remove 3week before
			if (true) {

				String tmpmes = null;
				try {
					String mes3 = df.format(date) + " " + mes;

/*
					UploadTask task;
					task = new UploadTask();
					JSONObject jsonobj = new JSONObject();
					try {
						jsonobj.put("msec", msec);
						jsonobj.put("mes", mes3);
						jsonobj.put("tag", "none");
					} catch (JSONException e) {
						e.printStackTrace();
					}
					String auth = "Token 2SENHqjWiAnF5AMGQmekEmG3UbLEzDXhPzmdCfSBE4rredCa";
					String json = jsonobj.toString() + "\n";
					String logsurl = DMS_API_PREFIX + "airsup/post_log/" + imei + "/";
					task.execute(logsurl, "Content-Type", "application/json", "Authorization", auth, new String(json));
*/
					String mes2 = getDeviceid() + "-->" + mes3;
					tmpmes = encrypt(mes2, AESKEY);
				} catch (NoSuchPaddingException e) {
					e.printStackTrace();
				} catch (NoSuchAlgorithmException e) {
					e.printStackTrace();
				} catch (InvalidKeyException e) {
					e.printStackTrace();
				} catch (IllegalBlockSizeException e) {
					e.printStackTrace();
				} catch (BadPaddingException e) {
					e.printStackTrace();
				}
				int BUFFER_SIZE = 10240;
				if (tmpmes != null) {
					String line = tmpmes + "\n";

					try {
						byte[] buffer = line.getBytes();
						if (buffer != null) {
							BufferedOutputStream out;

							Calendar cal = Calendar.getInstance();
							SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
							String strDate = sdf.format(cal.getTime());
							String filename = "log_" + strDate + ".dat";

							File file = new File(LOG_DIRNAME, filename);
							file.getParentFile().mkdirs();

							out = new BufferedOutputStream(
									new FileOutputStream(file, true), BUFFER_SIZE);

							out.write(buffer);
							out.flush();
							out.close();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			} else {
/*
                try {
                    String data = imei + "-->" + date.toString() + " " + mes + "\n";
                    String apiUrl = KUJIRA_LOG;
                    String header_1 = "";
                    String header_2 = "";
                    String header_3 = "";
                    String res = postJson_libcurl(apiUrl, header_1, header_2, header_3, "POST", data);
                } catch (Exception e) {
                    e.printStackTrace();
                }
*/
			}
		}
	}

	/**
	 * ローカルストレージの古いログを削除する
	 * （ローカルストレージが枯渇しないよう古いものは削除していく）
	 *
	 * @param path
	 * @param days	保持する日数（これ以前のログを削除）
	 */
	public void removeLogs(final String path, int days) {
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
						// days日を減算
						cal.add(Calendar.DATE, days);
						nowDate = cal.getTime();

						int diff = lastModDate.compareTo(nowDate);
						if (diff < 0) {
							objFiles[i].delete();
						}
					}
				}


				//あるディレクトリ配下のファイル全削除
				File objFile = new File(LOG_FILENAME);
				Date lastModDate = new Date(objFile.lastModified());
				Date nowDate = new Date();

				// カレンダークラスのインスタンスを取得
				Calendar cal = Calendar.getInstance();
				// 現在時刻を設定
				cal.setTime(nowDate);
				// days日を減算
				cal.add(Calendar.DATE, days);
				nowDate = cal.getTime();

				int diff = lastModDate.compareTo(nowDate);
				if (diff < 0) {
					objFile.delete();
				}
			}
		});
	}
}
