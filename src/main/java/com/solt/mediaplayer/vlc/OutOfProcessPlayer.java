package com.solt.mediaplayer.vlc;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintStream;

import uk.co.caprica.vlcj.binding.LibVlc;
import uk.co.caprica.vlcj.binding.LibVlcFactory;
import uk.co.caprica.vlcj.discovery.NativeDiscovery;
import uk.co.caprica.vlcj.player.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;

import com.sun.jna.NativeLibrary;
 
/**
 * Sits out of process so as not to crash the primary VM.
 * @author Michael
 */
public class OutOfProcessPlayer {
 
    public OutOfProcessPlayer(final long canvasId) throws Exception {
 
        //Lifted pretty much out of the VLCJ code
    	LibVlc libvlc = LibVlcFactory.factory().synchronise().log().create();
    	MediaPlayerFactory playerFactory = new MediaPlayerFactory(libvlc, "--no-video-title");
        EmbeddedMediaPlayer mediaPlayer = playerFactory.newEmbeddedMediaPlayer();

        mediaPlayer.setVideoSurface(ComponentIdVideoSurface.create(canvasId));
 
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String inputLine;
 
        //Process the input - I know this isn't very OO but it works for now...
        while ((inputLine = in.readLine()) != null) {
            if (inputLine.startsWith("open ")) {
                inputLine = inputLine.substring("open ".length());
                mediaPlayer.prepareMedia(inputLine);
            }
            else if (inputLine.equalsIgnoreCase("play")) {
                mediaPlayer.play();
            }
            else if (inputLine.equalsIgnoreCase("pause")) {
                mediaPlayer.pause();
            }
            else if (inputLine.equalsIgnoreCase("stop")) {
                mediaPlayer.stop();
            }
            else if (inputLine.equalsIgnoreCase("playable?")) {
                System.out.println(mediaPlayer.isPlayable());
            }
            else if (inputLine.startsWith("setTime ")) {
                inputLine = inputLine.substring("setTime ".length());
                mediaPlayer.setTime(Long.parseLong(inputLine));
            }
            else if (inputLine.startsWith("setMute ")) {
                inputLine = inputLine.substring("setMute ".length());
                mediaPlayer.mute(Boolean.parseBoolean(inputLine));
            }
            else if (inputLine.equalsIgnoreCase("mute?")) {
                boolean mute = mediaPlayer.isMute();
                System.out.println(mute);
            }
            else if (inputLine.equalsIgnoreCase("length?")) {
                long length = mediaPlayer.getLength();
                System.out.println(length);
            }
            else if (inputLine.equalsIgnoreCase("time?")) {
                long time = mediaPlayer.getTime();
                System.out.println(time);
            }
            else if (inputLine.equalsIgnoreCase("close")) {
                System.exit(0);
            }
            else {
                System.out.println("unknown command: ." + inputLine + ".");
            }
        }
    }
 
    public static void main(String[] args) {
    	new NativeDiscovery().discover();
        PrintStream stream = null;
        try {
            stream = new PrintStream(new File("ooplog.txt"));
            System.setErr(stream); //This is important, need to direct error stream somewhere
            new OutOfProcessPlayer(Integer.parseInt(args[0]));
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        finally {
            stream.close();
        }
    }
}
