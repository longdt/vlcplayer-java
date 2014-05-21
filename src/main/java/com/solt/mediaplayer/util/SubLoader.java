package com.solt.mediaplayer.util;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import com.solt.media.util.FileUtils;
import com.solt.media.util.SystemProperties;
import com.solt.mediaplayer.vlc.remote.Language;
import com.solt.mediaplayer.vlc.remote.LanguageSource;
import com.solt.mediaplayer.vlc.swt.MPlayerFrame;

public class SubLoader implements Runnable {
	private Language language;
	private MPlayerFrame player;
	
	public SubLoader(MPlayerFrame player, Language language) {
		this.language = language;
		language.setLoading(true);
		this.player = player;
	}

	@Override
	public void run() {
		File temp = new File(SystemProperties.getMetaDataPath(), language.getName());
		try {
			if (FileUtils.copyFile(new URL(language.getSourceInfo()), temp)) {
				language.setSourceInfo(temp.getAbsolutePath());
				language.setSource(LanguageSource.FILE);
				temp.deleteOnExit();
				//set subtitle
				Language currentSubtitle = player.getActiveSubtitle();
				if(currentSubtitle != null && language.getId().equals(currentSubtitle.getId()) && language.getName().equals(currentSubtitle.getName())) {
					player.setSubtitles(language);		
				}
			} else {
				temp.delete();
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

}
