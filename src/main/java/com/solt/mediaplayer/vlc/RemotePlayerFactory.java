package com.solt.mediaplayer.vlc;

import com.sun.jna.Native;
import java.awt.Canvas;
import java.util.ArrayList;
import java.util.List;

import uk.co.caprica.vlcj.runtime.RuntimeUtil;
 
public class RemotePlayerFactory {
 
    public static RemotePlayer getRemotePlayer(Canvas canvas) {
        try {
            long drawable = Native.getComponentID(canvas);
            StreamWrapper wrapper = startSecondJVM(drawable);
            final RemotePlayer player = new RemotePlayer(wrapper);
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
 
    private static StreamWrapper startSecondJVM(long drawable) throws Exception {
        String separator = System.getProperty("file.separator");
        String classpath = System.getProperty("java.class.path");
        String path = System.getProperty("java.home")
                + separator + "bin" + separator + "java";
        if (RuntimeUtil.isWindows()) {
        	path = path + ".exe";
        }
        List<String> cmdList = new ArrayList<String>();
        cmdList.add(path);
        boolean debugMode = false;
        if (debugMode) {
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
        }
        cmdList.add(OutOfProcessPlayer.class.getName());
        cmdList.add(Long.toString(drawable));
        ProcessBuilder processBuilder = new ProcessBuilder(cmdList);
        Process process = processBuilder.start();
        return new StreamWrapper(process.getInputStream(), process.getOutputStream());
    }
}