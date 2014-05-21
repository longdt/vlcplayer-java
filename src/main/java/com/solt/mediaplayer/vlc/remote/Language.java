package com.solt.mediaplayer.vlc.remote;

import java.security.InvalidParameterException;
import java.util.Locale;

public class Language {
	
	String id;
	String name;
	LanguageSource source;
	String sourceInfo;
	private boolean loading;


	Locale language;
	
	public Language(LanguageSource source, String id) {
		if (id == null || source == null) {
			throw new InvalidParameterException("source and id must not null");
		}
		this.source = source;
		this.id = id;
	}
	
	

	public void setName(String name) {
		this.name = name;
	}
	
	public void setLanguage(String isoCode) {
		language = ISO639.getLocaleFromISO639_2(isoCode);
	}
	
	public void setLanguage(Locale locale) {
		language = locale;
	}
	
	public Locale getLanguage() {
		return language;
	}
	
	public String getName() {
		return name;
	}
	
	public String getId() {
		return id;
	}
	
	public LanguageSource getSource() {
		return source;
	}
	
	public void setSource(LanguageSource source) {
		this.source = source;
	}
	
	public String getSourceInfo() {
		return sourceInfo;
	}

	public void setSourceInfo(String sourceInfo) {
		this.sourceInfo = sourceInfo;
	}

	public boolean isLoading() {
		return loading;
	}

	public void setLoading(boolean loading) {
		this.loading = loading;
	}
}
