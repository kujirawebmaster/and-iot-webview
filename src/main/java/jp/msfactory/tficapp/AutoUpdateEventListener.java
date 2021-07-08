package jp.msfactory.tficapp;

import java.util.EventListener;

/**
 * soft updaterのインターフェイス
 */
public interface AutoUpdateEventListener extends EventListener {
	void onUpdateApkDownloadComplete();
	void onUpdateApkDownloadError();
	void onUpdateAvailable();
}
