package com.solt.mediaplayer.vlc.remote;

import com.solt.mediaplayer.vlc.VLCPlayer;
import com.sun.jna.Native;
import java.awt.Canvas;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import uk.co.caprica.vlcj.runtime.RuntimeUtil;
 
public class RemotePlayerFactory {
	private static final boolean DEBUG_MODE = false;
 
    public static RemotePlayer getRemotePlayer(long componentId) {
        try {
            Process process = startSecondJVM(componentId);
            final RemotePlayer player = new RemotePlayer(process.getInputStream(), process.getOutputStream());
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    player.close();
                }
            });
            return player;
        }
        catch (Exception ex) {
            throw new RuntimeException("Couldn't create remote player", ex);
        }
    }
    
    public static RemotePlayer getRemotePlayer(Canvas canvas) {
    	  return getRemotePlayer(Native.getComponentID(canvas));
    }
    
    public static Process startSecondJVM(long componentId, String fileOrUrl) throws Exception {
        String separator = System.getProperty("file.separator");
        String classpath = System.getProperty("java.class.path");
        String path = System.getProperty("java.home")
                + separator + "bin" + separator + "java";
        if (RuntimeUtil.isWindows()) {
        	path = path + ".exe";
        }
        List<String> cmdList = new ArrayList<String>();
        cmdList.add(path);
        if (DEBUG_MODE) {
        	cmdList.add("-Xdebug");
        	cmdList.add("-Xnoagent");
        	cmdList.add("-Djava.compiler=NONE");
        	cmdList.add("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000");
        }
        cmdList.add("-cp");
        cmdList.add(classpath);
        String jnaLibPath = System.getProperty("jna.library.path");
        if (jnaLibPath != null) {
        	cmdList.add("-Djna.library.path=" + jnaLibPath);
        } else {
        	File vlc = new File("VLC");
        	if (vlc.isDirectory()) {
        		cmdList.add("-Djna.library.path=" + vlc.getAbsolutePath());
        	}
        }
        cmdList.add(VLCPlayer.class.getName());
        cmdList.add(Long.toString(componentId));
        if (fileOrUrl != null) {
        	cmdList.add(fileOrUrl);
        }
        ProcessBuilder processBuilder = new ProcessBuilder(cmdList);
        return processBuilder.start();
    }

	public static Process startSecondJVM(long componentId) throws Exception {
		return startSecondJVM(componentId, null);
	}
}