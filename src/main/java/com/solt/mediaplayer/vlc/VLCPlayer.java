package com.solt.mediaplayer.vlc;

import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.List;

import org.json.simple.JSONObject;

import com.solt.mediaplayer.vlc.remote.ComponentIdVideoSurface;

import uk.co.caprica.vlcj.binding.LibVlc;
import uk.co.caprica.vlcj.binding.LibVlcFactory;
import uk.co.caprica.vlcj.discovery.NativeDiscovery;
import uk.co.caprica.vlcj.player.MediaPlayer;
import uk.co.caprica.vlcj.player.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.TrackDescription;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;

 
/**
 * Sits out of process so as not to crash the primary VM.
 * @author Michael
 */
public class VLCPlayer {
	private EmbeddedMediaPlayer mediaPlayer;
	private MediaPlayerFactory playerFactory;
	private volatile boolean loadSubFile; //hack fast check loadsub from file
	
    public VLCPlayer(int canvasId) throws Exception {
    	this(canvasId, null);
	}
	
    public VLCPlayer(final long canvasId, String media) throws Exception {
 
        //Lifted pretty much out of the VLCJ code
    	LibVlc libvlc = LibVlcFactory.factory().synchronise().log().create();
    	playerFactory = new MediaPlayerFactory(libvlc, "--no-video-title", "--freetype-font=Tahoma Bold");
    	mediaPlayer = playerFactory.newEmbeddedMediaPlayer();
    	mediaPlayer.setEnableKeyInputHandling(false);
    	mediaPlayer.setEnableMouseInputHandling(false);
        mediaPlayer.setVideoSurface(ComponentIdVideoSurface.create(canvasId));
        if (media != null) {
        	mediaPlayer.playMedia(media);
        }
        mediaPlayer.addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
        	private boolean playing;
        	
        	@SuppressWarnings("unchecked")
			@Override
        	public void playing(MediaPlayer mediaPlayer) {
        		System.out.println(VLCCommand.STATUS_PLAYING);
        		if (playing) {
        			return;
        		}
        		playing = true;
        		List<TrackDescription> spus = mediaPlayer.getSpuDescriptions();
        		JSONObject subs = new JSONObject();
        		for (TrackDescription td : spus) {
        			if (td.id() == -1) {
        				continue;
        			}
        			subs.put(td.id(), td.description());
        		}
        		if (!subs.isEmpty()) {
        			if (!loadSubFile) { //hack fast check loadsub from file. If call loadSub then doesn't set sub state
        				subs.put(VLCCommand.SUB_STATE, String.valueOf(mediaPlayer.getSpu()));
        			}
        			System.out.println(VLCCommand.STATUS_SUBS + " " + subs.toString());
        		}
        	}
        	
        	@Override
        	public void timeChanged(MediaPlayer mediaPlayer, long newTime) {
        		System.out.println(VLCCommand.STATUS_TIME + " " + (newTime / 1000f));
        	}
        	
        	@Override
        	public void lengthChanged(MediaPlayer mediaPlayer, long newLength) {
        		System.out.println(VLCCommand.STATUS_LENGTH + " " + (newLength / 1000f));
        	}
        	
        	@Override
        	public void finished(MediaPlayer mediaPlayer) {
        		System.out.println(VLCCommand.STATUS_FINISHED);
        	}
        });
    }
    
    public void handleRequest() throws NumberFormatException, IOException {
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
                System.out.println(VLCCommand.ANS_TIME + " " + time / 1000f);
            } else if (inputLine.equalsIgnoreCase(VLCCommand.GET_VOLUME)) {
            	int volume = mediaPlayer.getVolume();
            	System.out.println(VLCCommand.ANS_VOLUME + " " + volume);
            } else if (inputLine.equalsIgnoreCase(VLCCommand.GET_DIMENSION)) {
            	Dimension dim = mediaPlayer.getVideoDimension();
            	if (dim != null) {
            		System.out.println(VLCCommand.ANS_DIMENSION + " " + dim.width + "x" + dim.height);
            	}
            } else if (inputLine.startsWith(VLCCommand.LOAD_SUB)) {
            	inputLine = inputLine.substring(VLCCommand.LOAD_SUB.length() + 1);
            	mediaPlayer.setSubTitleFile(new File(inputLine));
            	loadSubFile = true;
            	System.out.println(VLCCommand.ANS_SUB_FILE + " "+ inputLine);
            } else if (inputLine.startsWith(VLCCommand.SET_SUB)) {
            	inputLine = inputLine.substring(VLCCommand.SET_SUB.length() + 1);
            	try {
            		int subId = Integer.parseInt(inputLine);
            		mediaPlayer.setSpu(subId);
            	} catch (NumberFormatException e) {
            		mediaPlayer.setSubTitleFile(new File(inputLine));
            	}
            } else if (inputLine.equalsIgnoreCase(VLCCommand.CLOSE)) {
            	return;
            }
            else {
                System.err.println("unknown command: ." + inputLine + ".");
            }
        }
    }
    
    public void shudown() {
    	playerFactory.release();
    	mediaPlayer.release();
    }
    
	public void setVideoSurface(long componentId) {
    	mediaPlayer.setVideoSurface(ComponentIdVideoSurface.create(componentId));
    }
 
    public static void main(String[] args) {
    	new NativeDiscovery().discover();
        PrintStream stream = null;
        String media = null;
        VLCPlayer player = null;
        try {
            stream = new PrintStream(new File("vlcErr.txt"));
            System.setErr(stream); //This is important, need to direct error stream somewhere
            if (args.length == 2) {
            	media = args[1];
            }
            player = new VLCPlayer(Long.parseLong(args[0]), media);
            player.handleRequest();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
        	if (stream != null) {
        		stream.close();
        	}
        	if (player != null) {
        		player.shudown();
        	}
        }
    }
}
