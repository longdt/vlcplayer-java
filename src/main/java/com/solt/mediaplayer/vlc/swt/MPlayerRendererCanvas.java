package com.solt.mediaplayer.vlc.swt;

import java.awt.Canvas;
import java.awt.Frame;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import com.sun.jna.Native;

public class MPlayerRendererCanvas extends Composite implements MPlayerRendererInterface {
	private Canvas videoSurfaceCanvas;
	private Frame videoFrame;
	
	public MPlayerRendererCanvas(Composite parent) {
		super(parent, SWT.EMBEDDED);
		videoSurfaceCanvas = new Canvas();
		setLayoutData(new GridData(GridData.FILL_BOTH));
	    videoFrame = SWT_AWT.new_Frame(this);
	    videoSurfaceCanvas.setBackground(java.awt.Color.black);
	    videoFrame.add(videoSurfaceCanvas);
	}
	
	public void release() {
		videoFrame.dispose();
		dispose();
	}
	
	public long getComponentId() {		
		return Native.getComponentID(videoSurfaceCanvas);
	}
	
	@Override
	public void addListener(final int eventType, final Listener listener) {
		if (eventType == SWT.MouseMove) {
			videoSurfaceCanvas.addMouseMotionListener(new MouseAdapter() {
				@Override
				public void mouseMoved(MouseEvent e) {
					Display.getDefault().asyncExec(new Runnable() {
					    public void run() {
							Event evt = new Event();
							listener.handleEvent(evt);
					    }
					});
				}
			});
		} else if (eventType == SWT.MouseEnter) {
			videoSurfaceCanvas.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseEntered(MouseEvent e) {
					Display.getDefault().asyncExec(new Runnable() {
					    public void run() {
							Event evt = new Event();
							listener.handleEvent(evt);
					    }
					});
				}
			});
		} else if (eventType == SWT.MouseExit) {
			videoSurfaceCanvas.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseExited(MouseEvent e) {
					Display.getDefault().asyncExec(new Runnable() {
					    public void run() {
							Event evt = new Event();
							listener.handleEvent(evt);
					    }
					});
				}
			});
		} else if (eventType == SWT.KeyDown) {
			videoSurfaceCanvas.addKeyListener(new KeyAdapter() {
				@Override
				public void keyPressed(final KeyEvent e) {
					Display.getDefault().asyncExec(new Runnable() {
					    public void run() {
							Event evt = new Event();
							evt.character = e.getKeyChar();
							listener.handleEvent(evt);
					    }
					});
				}
			});
		} else if (eventType == SWT.MouseDoubleClick){
			videoSurfaceCanvas.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					 if (e.getClickCount() == 2) { //double click
						 Display.getDefault().asyncExec(new Runnable() {
							 public void run() {
								 Event evt = new Event();
								 listener.handleEvent(evt);
							 }
						 });
					 }
				}
			});
		} else if (eventType == SWT.MouseUp || eventType == SWT.MouseDown) {
			videoSurfaceCanvas.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					 if (e.getClickCount() == 1) {
						 Display.getDefault().asyncExec(new Runnable() {
						    public void run() {
								Event evt = new Event();
								listener.handleEvent(evt);
						    }
						});
					 }
				}
			});
		}
		
	}
}
