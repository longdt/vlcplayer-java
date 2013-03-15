package com.solt.mediaplayer.vlc.swt;

import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

public class MPlayerRendererCanvas extends Canvas implements MPlayerRendererInterface {
	
	MPlayerRendererInterface impl;
	
	public MPlayerRendererCanvas(Composite parent,int style) {
		super(parent,style);
	}
	
	public long getComponentId() {		
		return handle;
	}
	

}
