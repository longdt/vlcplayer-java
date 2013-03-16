package com.solt.mediaplayer.vlc;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

import com.solt.mediaplayer.vlc.remote.ComponentIdVideoSurface;

import uk.co.caprica.vlcj.binding.LibVlc;
import uk.co.caprica.vlcj.binding.LibVlcFactory;
import uk.co.caprica.vlcj.discovery.NativeDiscovery;
import uk.co.caprica.vlcj.player.MediaPlayer;
import uk.co.caprica.vlcj.player.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;

 
/**
 * Sits out of process so as not to crash the primary VM.
 * @author Michael
 */
public class VLCPlayer {
	private EmbeddedMediaPlayer mediaPlayer;
	
    public VLCPlayer(int canvasId) throws Exception {
    	this(canvasId, null);
	}
	
    public VLCPlayer(final long canvasId, String media) throws Exception {
 
        //Lifted pretty much out of the VLCJ code
    	LibVlc libvlc = LibVlcFactory.factory().synchronise().log().create();
    	MediaPlayerFactory playerFactory = new MediaPlayerFactory(libvlc, "--no-video-title");
    	mediaPlayer = playerFactory.newEmbeddedMediaPlayer();

        mediaPlayer.setVideoSurface(ComponentIdVideoSurface.create(canvasId));
        if (media != null) {
        	mediaPlayer.playMedia(media);
        }
        mediaPlayer.addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
        	
        	@Override
        	public void playing(MediaPlayer mediaPlayer) {
        		System.out.println(VLCCommand.STATUS_PLAYING);
        	}
        	
        	@Override
        	public void timeChanged(MediaPlayer mediaPlayer, long newTime) {
        		System.out.println(VLCCommand.STATUS_TIME + " " + (newTime / 1000f));
        	}
        	
        	@Override
        	public void lengthChanged(MediaPlayer mediaPlayer, long newLength) {
        		System.out.println(VLCCommand.STATUS_LENGTH + " " + (newLength / 1000f));
        	}
        });
        handleRequest();
    }
    
    private void handleRequest() throws NumberFormatException, IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String inputLine;
 
        //Process the input - I know this isn't very OO but it works for now...
        while ((inputLine = in.readLine()) != null) {
            if (inputLine.startsWith(VLCCommand.OPEN)) {
                inputLine = inputLine.substring(VLCCommand.OPEN.length() + 1);
                mediaPlayer.prepareMedia(inputLine);
            }
            else if (inputLine.equalsIgnoreCase(VLCCommand.PLAY)) {
                mediaPlayer.play();
            }
            else if (inputLine.equalsIgnoreCase(VLCCommand.PAUSE)) {
                mediaPlayer.pause();
            }
            else if (inputLine.equalsIgnoreCase(VLCCommand.STOP)) {
                mediaPlayer.stop();
            }
            else if (inputLine.equalsIgnoreCase(VLCCommand.GET_PLAYABLE)) {
                System.out.println(VLCCommand.ANS_PLAYABLE + " " + mediaPlayer.isPlayable());
            }
            else if (inputLine.startsWith(VLCCommand.SET_TIME)) {
                inputLine = inputLine.substring(VLCCommand.SET_TIME.length() + 1);
                mediaPlayer.setTime(Long.parseLong(inputLine) * 1000);
            }
            else if (inputLine.startsWith(VLCCommand.SET_MUTE)) {
                inputLine = inputLine.substring(VLCCommand.SET_MUTE.length() + 1);
                mediaPlayer.mute(Boolean.parseBoolean(inputLine));
            } else if (inputLine.startsWith(VLCCommand.SET_VOLUME)) {
            	inputLine = inputLine.substring(VLCCommand.SET_VOLUME.length() + 1);
            	mediaPlayer.setVolume(Integer.parseInt(inputLine));
            }
            else if (inputLine.equalsIgnoreCase(VLCCommand.GET_MUTE)) {
                boolean mute = mediaPlayer.isMute();
                System.out.println(VLCCommand.ANS_MUTE + " " + mute);
            }
            else if (inputLine.equalsIgnoreCase(VLCCommand.GET_LENGTH)) {
                long length = mediaPlayer.getLength();
                System.out.println(VLCCommand.ANS_LENGTH + " " + length);
            }
            else if (inputLine.equalsIgnoreCase(VLCCommand.GET_TIME)) {
                long time = mediaPlayer.getTime();
                System.out.println(VLCCommand.ANS_TIME + " " + time);
            } else if (inputLine.equalsIgnoreCase(VLCCommand.GET_VOLUME)) {
            	int volume = mediaPlayer.getVolume();
            	System.out.println(VLCCommand.ANS_VOLUME + " " + volume);
            }
             
            else if (inputLine.equalsIgnoreCase(VLCCommand.CLOSE)) {
                System.exit(0);
            }
            else {
                System.err.println("unknown command: ." + inputLine + ".");
            }
        }
    }
    
	public void setVideoSurface(long componentId) {
    	mediaPlayer.setVideoSurface(ComponentIdVideoSurface.create(componentId));
    }
 
    public static void main(String[] args) {
    	new NativeDiscovery().discover();
        PrintStream stream = null;
        String media = null;
        try {
            stream = new PrintStream(new File("vlcErr.txt"));
            System.setErr(stream); //This is important, need to direct error stream somewhere
            if (args.length == 2) {
            	media = args[1];
            }
            new VLCPlayer(Integer.parseInt(args[0]), media);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        finally {
            stream.close();
        }
    }
}
