package jp.msfactory.tficapp;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


/**
 * soft updaterのクラス
 */
public class AutoUpdateEventListenerList {
	private Set<AutoUpdateEventListener> listeners = new HashSet<AutoUpdateEventListener>();

	public void addEventListener(AutoUpdateEventListener l) {
		if(!listeners.contains(l)){
			listeners.add(l);
		}
	}

	public void removeEventListener(AutoUpdateEventListener l) {
		listeners.remove(l);
	}

	public void downloadCompleteNotify(){
		for(Iterator<AutoUpdateEventListener> i = listeners.iterator(); i.hasNext();){
			i.next().onUpdateApkDownloadComplete();
		}
	}

	public void downloadErrorNotify(){
		for(Iterator<AutoUpdateEventListener> i = listeners.iterator(); i.hasNext();){
			i.next().onUpdateApkDownloadError();
		}
	}

	public void updateAvailableNotify(){
		for(Iterator<AutoUpdateEventListener> i = listeners.iterator(); i.hasNext();){
			i.next().onUpdateAvailable();
		}
	}
}
