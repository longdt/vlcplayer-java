package com.solt.mediaplayer.vlc.swt;

import java.awt.Canvas;
import java.awt.Frame;

import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.solt.mediaplayer.vlc.remote.RemotePlayer;
import com.solt.mediaplayer.vlc.remote.RemotePlayerFactory;

import uk.co.caprica.vlcj.component.EmbeddedMediaPlayerComponent;
import uk.co.caprica.vlcj.discovery.NativeDiscovery;

public class DemoPlayer {
	protected Shell shell;

	static {
		 new NativeDiscovery().discover();
	}
	/**
	 * Launch the application.
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			DemoPlayer window = new DemoPlayer();
			window.open();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Open the window.
	 */
	public void open() {
		Display display = Display.getDefault();
		createContents();
		shell.open();
		shell.layout();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	/**
	 * Create contents of the window.
	 */
	protected void createContents() {
		shell = new Shell();
		shell.setSize(880, 480);
		shell.setText("SWT Application");
		Composite videoComposite = new Composite(shell, SWT.EMBEDDED | SWT.NO_BACKGROUND);
		Frame videoFrame = SWT_AWT.new_Frame(videoComposite);
		Canvas videoSurface = new Canvas();
		videoSurface.setBackground(java.awt.Color.black);
		videoFrame.add(videoSurface);
		videoComposite.setBounds(0, 0, 864, 442);
		videoComposite.setVisible(true);
		RemotePlayer player = RemotePlayerFactory.getRemotePlayer(videoSurface);
		player.load("D:\\Music\\Gockhuyet_kienza.avi");
		player.play();
	}

}
