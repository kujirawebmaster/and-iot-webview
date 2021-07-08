package jp.msfactory.tficapp;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import androidx.core.content.FileProvider;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * soft updaterクラス
 */
public class Updater {
	private static final String TAG = "myUpdater";

	private int currentVersionCode;
	private int latestVersionCode;
	private String currentVersionName;
	private String latestVersionName;
	private String latestDescription;

	private String updateXmlUrl;
	private String updateApkFileUrl;
	private File tempApkFile;

	private AutoUpdateEventListenerList updateListeners;
	private Activity activity;

	public Updater(Activity activity, String updateXmlUrl) {
		this.updateXmlUrl = updateXmlUrl;
		this.activity = activity;

		updateListeners = new AutoUpdateEventListenerList();
		updateListeners.addEventListener((AutoUpdateEventListener) getActivity());
	}
	public Activity getActivity() {
		return activity;
	}

	private boolean updateAvailableCheck(){
		try {
			getCurrentVersionInfo();
			getLatestVersionInfo();

			if(currentVersionCode<latestVersionCode){
				return true;
			}
		} catch (FileNotFoundException e) {
			Log.i(TAG, e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}

	private void getCurrentVersionInfo() throws Exception {

		PackageManager pm = getActivity().getPackageManager();
		String packageName = getActivity().getPackageName();
		PackageInfo info = pm.getPackageInfo(packageName, PackageManager.GET_META_DATA);

		currentVersionCode = info.versionCode;
		currentVersionName = info.versionName;
	}

	private void getLatestVersionInfo() throws Exception {

        HashMap<String, String> map = parseUpdateXml(updateXmlUrl);

        latestVersionCode = Integer.parseInt(map.get("versionCode"));
        latestVersionName = map.get("versionName");
        updateApkFileUrl = map.get("url");
        latestDescription = map.get("description");
	}

    public void updateCheck(){
        new updateCheckTask().execute();
    }

    private class updateCheckTask extends AsyncTask<Object, Object, Object> {

		@Override
		protected Object doInBackground(Object... arg0) {
			boolean updateAvailable = updateAvailableCheck();
			if(updateAvailable){
				return new Object();
			}else{
				return null;
			}
		}

		@Override
		protected void onPostExecute(Object result) {
			if(result!=null){
				updateListeners.updateAvailableNotify();
			}
		}

    }

    public void downloadApk(){
    	new downloadTask().execute();
    }

    private class downloadTask extends AsyncTask<Object, Object, Object> {

		@Override
		protected Object doInBackground(Object... params) {
			try {
				File cacheDir = getActivity().getExternalCacheDir();
				cacheDir = new File(getActivity().getExternalCacheDir(), MainActivity.APP_NAME);
				cacheDir.mkdirs();
				tempApkFile = File.createTempFile("apk", ".apk", cacheDir);
				Util.urlToFile(updateApkFileUrl, tempApkFile);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Object result) {
	    	if(tempApkFile!=null && tempApkFile.exists()){
	    		updateListeners.downloadCompleteNotify();
	    	}else{
	    		updateListeners.downloadErrorNotify();
	    	}
		}

    }

	public void installApk(){
		if(tempApkFile==null || !tempApkFile.exists()){
			return;
		}

		Intent intent = new Intent(Intent.ACTION_VIEW);
		Uri dataUri = Uri.fromFile(tempApkFile);
		if(Build.VERSION.SDK_INT >= 23) {
			dataUri = FileProvider.getUriForFile(getActivity(), MainActivity.fileprovider, tempApkFile);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
		} else {
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		}
		intent.setDataAndType(dataUri, "application/vnd.android.package-archive");
		getActivity().startActivity(intent);
	}

    public void deleteTempApkFile(){
    	try{
	    	File cacheDir = getActivity().getExternalCacheDir();
	    	File[] cacheFiles = cacheDir.listFiles();
	    	for (int i = 0; i < cacheFiles.length; i++) {
				if(cacheFiles[i].getName().endsWith(".apk")){
					cacheFiles[i].delete();
				}
			}
    	}catch(Exception e){

    	}
    }

    private HashMap<String, String> parseUpdateXml(String url) throws Exception {

        HashMap<String, String> map = new HashMap<String, String>();

        InputStream is = new URL(url).openConnection().getInputStream();
        BufferedInputStream bis = new BufferedInputStream(is);

        DocumentBuilderFactory document_builder_factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder document_builder = document_builder_factory.newDocumentBuilder();
        Document document = document_builder.parse(bis);
        Element root = document.getDocumentElement();

        if(root.getTagName().equals("update")){
	        NodeList nodelist = root.getChildNodes();
	        for (int j=0;j<nodelist.getLength();j++){
	        	Node node = nodelist.item(j);
	        	if(node.getNodeType()== Node.ELEMENT_NODE){
	        		Element element = (Element) node;
		        	String name = element.getTagName();
		        	String value = element.getTextContent().trim();
			        map.put(name, value);
	        	}
	        }
        }

		return map;
    }
}
