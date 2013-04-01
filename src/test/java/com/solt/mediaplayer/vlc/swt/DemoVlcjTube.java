/*
 * This file is part of VLCJ.
 *
 * VLCJ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * VLCJ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with VLCJ.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Copyright 2009, 2010, 2011 Caprica Software Limited.
 */

package com.solt.mediaplayer.vlc.swt;

import java.awt.Canvas;
import java.awt.Frame;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.TitleEvent;
import org.eclipse.swt.browser.TitleListener;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import uk.co.caprica.vlcj.discovery.NativeDiscovery;
import uk.co.caprica.vlcj.player.MediaPlayer;
import uk.co.caprica.vlcj.player.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.embedded.videosurface.CanvasVideoSurface;
import uk.co.caprica.vlcj.runtime.x.LibXUtil;

/**
 * An application that embeds a native Web Browser component and a native
 * media player component.
 * <p>
 * Activating a video hyperlink causes the video to be played by the embedded
 * native media player.
 * <p>
 * Press the F11 key to toggle full-screen mode.
 * <p>
 * Press the ESCAPE key to quit the current video and return to the browser.
 * <p> 
 * WebKit is a better choice than Mozilla for the embedded browser.
 * <pre>
 *   -Dorg.eclipse.swt.browser.UseWebKitGTK=true
 * </pre>
 * With SWT 3.7, WebKit (if available) is the default implementation and the 
 * above property is not required. 
 */
public class DemoVlcjTube {

  /**
   * Initial URL.
   */
  private static final String HOME_URL = "http://www.youtube.com";
  
  /**
   * URLs matching this pattern will be intercepted and played by the embedded
   * media player.
   */
  private static final String WATCH_VIDEO_PATTERN = "http://www.youtube.com/watch\\?v=.*";
  
  /**
   * Pre-compiled regular expression pattern.
   */
  private Pattern watchLinkPattern;

  /**
   * UI components.
   */
  private Display display;
  private Shell shell;
  private StackLayout stackLayout;
  private Composite browserPanel;
  private Browser browser;
  private Composite videoPanel;
  private Composite videoComposite;
  private Frame videoFrame;
  private Canvas videoSurfaceCanvas;
  private CanvasVideoSurface videoSurface;
  private Cursor emptyCursor;
  /**
   * Native media player components.
   */
  private MediaPlayerFactory mediaPlayerFactory;
  private EmbeddedMediaPlayer mediaPlayer;
  
  /**
   * Application entry point.
   * 
   * @param args command-line arguments
   * @throws Exception if an error occurs
   */
  public static void main(String[] args) throws Exception {
	new NativeDiscovery().discover();
    LibXUtil.initialise();
    new DemoVlcjTube().start();
  }
  
  /**
   * Create an application.
   * 
   * @throws Exception if an error occurs
   */
  public DemoVlcjTube() throws Exception {
    watchLinkPattern = Pattern.compile(WATCH_VIDEO_PATTERN);
    
    createUserInterface();
    createEmptyCursor();
    createMediaPlayer();
  }
  
  /**
   * Create the user interface controls.
   */
  private void createUserInterface() {
    display = new Display();
    
    stackLayout = new StackLayout();

    shell = new Shell(display);
    shell.setLayout(stackLayout);
    shell.setSize(1200, 900);
    
    browserPanel = new Composite(shell, SWT.NONE);
    browserPanel.setLayout(new FillLayout());

    browser = new Browser(browserPanel, SWT.NONE);
    browser.setJavascriptEnabled(true);
    
    videoPanel = new Composite(shell, SWT.NONE);
    videoPanel.setLayout(new FillLayout());
    videoPanel.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
    
    videoComposite = new Composite(videoPanel, SWT.EMBEDDED | SWT.NO_BACKGROUND);
    videoComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
    videoFrame = SWT_AWT.new_Frame(videoComposite);
    videoSurfaceCanvas = new Canvas();
    videoSurfaceCanvas.setBackground(java.awt.Color.black);
    videoFrame.add(videoSurfaceCanvas);

    showBrowser();
    
    display.addFilter(SWT.KeyDown, new Listener() {
      public void handleEvent(Event evt) {
        switch(evt.keyCode) {
          case SWT.ESC:
            if(stackLayout.topControl == videoPanel) {
              mediaPlayer.stop();
              showBrowser();
            }
            break;
          
          case SWT.F11:
            shell.setFullScreen(!shell.getFullScreen());
            break;
        }
      }
    });
    
    shell.addShellListener(new ShellAdapter() {
      @Override
      public void shellClosed(ShellEvent evt) {
        mediaPlayer.release();
        mediaPlayerFactory.release();
      }
    });
    
    browser.addTitleListener(new TitleListener() {
      public void changed(TitleEvent e) {
        shell.setText("vlcj - " + e.title);
      }
    });
    
    browser.addLocationListener(new LocationAdapter() {
      @Override
      public void changing(LocationEvent evt) {
        Matcher matcher = watchLinkPattern.matcher(evt.location);
        if(matcher.matches()) {
          evt.doit = false;
          showVideo();
          boolean saveAudio = false;
          if(saveAudio) {
            mediaPlayer.playMedia(evt.location, getRecordAudioMediaOptions("vlcj-tube"));
          }
          else {
            mediaPlayer.playMedia("D:\\Music\\Gockhuyet_kienza.avi");
          }
        }
      }
    });
    
    browser.addKeyListener(new KeyAdapter() {
      @Override
      public void keyReleased(KeyEvent e) {
        switch(e.keyCode) {
        }
      }
    });
    
    videoComposite.addKeyListener(new KeyAdapter() {
      @Override
      public void keyReleased(KeyEvent e) {
        switch(e.keyCode) {
        }
      }
    });
    
    shell.open();
  }
  
  /**
   * Create an empty cursor.
   */
  private void createEmptyCursor() {
//    Color white = display.getSystemColor(SWT.COLOR_WHITE);
//    Color black = display.getSystemColor(SWT.COLOR_BLACK);
//    PaletteData palette = new PaletteData(new RGB[] { white.getRGB(), black.getRGB() });
//    ImageData sourceData = new ImageData(16, 16, 1, palette);
//    sourceData.transparentPixel = 0;
//    emptyCursor = new Cursor(display, sourceData, 0, 0);
  }
  
  /**
   * Create the native media player components.
   */
  private void createMediaPlayer() {
    mediaPlayerFactory = new MediaPlayerFactory("--no-video-title-show");
    mediaPlayer = mediaPlayerFactory.newEmbeddedMediaPlayer();
    mediaPlayer.setPlaySubItems(true);
    videoSurface = mediaPlayerFactory.newVideoSurface(videoSurfaceCanvas);
    mediaPlayer.setVideoSurface(videoSurface);
    
    mediaPlayer.addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
      @Override
      public void opening(MediaPlayer mediaPlayer) {
        System.out.println("opening");
        // Similar to Swing, obey the SWT threading model...
        display.asyncExec(new Runnable() {
          public void run() {
            showVideo();
          }
        });
      }
      
      @Override
    	public void buffering(MediaPlayer mediaPlayer, float newCache) {
    		System.out.println("buffering..." + newCache);
    	}
      
      @Override
    	public void paused(MediaPlayer mediaPlayer) {
    		System.out.println("paused");
    	}
      
      @Override
    	public void mediaStateChanged(MediaPlayer mediaPlayer, int newState) {
    		System.out.println("state change: " + newState);
    	}
      
      @Override
    	public void error(MediaPlayer mediaPlayer) {
    	  System.out.println("error");
      }
      
      @Override
      public void finished(MediaPlayer mediaPlayer) {
        System.out.println("finished");
        // Similar to Swing, obey the SWT threading model...
        display.asyncExec(new Runnable() {
          public void run() {
            showBrowser();
          }
        });
      }
    });
  }
  
  /**
   * Start the application.
   * <p>
   * Execute the SWT message loop.
   */
  private void start() {
    browser.setUrl(HOME_URL);

    while(!shell.isDisposed()) {
      if(!display.readAndDispatch()) {
        display.sleep();
      }
    }
    display.dispose();
  }
  
  private void showBrowser() {
    showView(browserPanel);
  }
  
  private void showVideo() {
    showView(videoPanel);
  }
  
  private void showView(Composite view) {
    stackLayout.topControl = view;
    shell.layout();
  }

  // FIXME with saving audio - either there's a problem with 1.1.x and duplicate display (plays locally but audio is corrupted)
  //       or with 1.2.x there's a problem with duplicate=display generally (doesn't play locally) but at least the audio is not corrupted
  //       don't know yet if 1.1.x works without duplicate=display
  
  private String[] getRecordAudioMediaOptions(String name) {
    File file = getFile(name);
    StringBuilder sb = new StringBuilder(200);
    sb.append("sout=#transcode{acodec=mp3,channels=2,ab=192,samplerate=44100,vcodec=dummy}:standard{mux=raw,access=file,dst=");
//    sb.append("sout=#transcode{acodec=mp3,channels=2,ab=192,samplerate=44100,vcodec=dummy}:duplicate{dst=display,dst=std{access=file,mux=raw,dst=");
    sb.append(file.getPath());
    sb.append("}}");
    return new String[] {sb.toString()};
  }

  private File getFile(String name) {
    StringBuilder sb = new StringBuilder(100);
    sb.append(name);
    sb.append('-');
    sb.append(new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()));
    sb.append(".mp3");
    File userHomeDirectory = new File(System.getProperty("user.home"));
    File saveDirectory = new File(userHomeDirectory, "vlcj-tube");
    if(!saveDirectory.exists()) {
      saveDirectory.mkdirs();
    }
    return new File(saveDirectory, sb.toString());
  }
}
