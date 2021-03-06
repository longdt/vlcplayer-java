package com.solt.mediaplayer.vlc.remote;


import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.solt.mediaplayer.vlc.VLCCommand;


public abstract class MPlayer extends BaseMediaPlayer {
	private static final int DEFAULT_TIME_WAIT_BUFFER = 1200;
	private List<String> output;
	
	private volatile boolean disposed = false;
	
	private Thread outputParser;
		
	private volatile int timeWait = DEFAULT_TIME_WAIT_BUFFER;
	private boolean firstLengthReceived = false;
	private boolean firstVolumeReceived = false;
	
	public MPlayer() {
		output = new LinkedList<String>();
		outputParser = new Thread("MPlayer output parser") {
			public void run() {
				try {
					boolean buffering = false;
					while(!disposed) {
						String line = null;
						synchronized (output) {
							if(!output.isEmpty()) {
								
								line = output.remove(0);	
																
							} else {
								output.wait(timeWait);
								if (output.isEmpty()) {
									line = null;
								} else {
									line = output.remove(0);
								}
							}
						}
						
						if ( line != null ){
							
							//System.out.println(line);
							if (buffering) {
								stateChanged(MediaPlaybackState.Continue);
								buffering = false;
							}
							try{
								parseOutput(line);
							}catch( Throwable e ){
								
								e.printStackTrace();
							}
						} else if (timeWait != 0 && getCurrentState() != MediaPlaybackState.Stopped) {
							stateChanged(MediaPlaybackState.Buffering);
							buffering = true;
						}
					}
				} catch (Throwable e) {
					
					e.printStackTrace();
				}
			};
		};
		outputParser.setDaemon(true);
		outputParser.start();
		
		
	}
	
	private static final String ANS_ASPECT = "ANS_ASPECT=";
	
	private static final String ID_VIDEO_ASPECT = "ID_VIDEO_ASPECT=";
	
	private static final String ID_EXIT = "ID_EXIT=";
	
	private MPlayerInstance	current_instance;
	
	private boolean parsingLanguage;
	private boolean isAudioTrack;
	private Language language;
	private volatile boolean firstTime = true;
	private int width;
	private float aspect;
	
	private void parseOutput(String line) {
		boolean stillParsing = false;
			
		if(line.startsWith(VLCCommand.STATUS_TIME)) {
			float time = Float.parseFloat(line.substring(VLCCommand.STATUS_TIME.length()));
			MPlayerInstance instance = getCurrentInstance();
			
			if ( instance != null ){
				instance.positioned(time);
				if (firstTime) {
					instance.doGetDimension();
					firstTime = false;
				}
			}
			reportPosition(time);
		} else
		if(line.startsWith(VLCCommand.STATUS_PLAYING)) {
			//Ok, so the file is initialized, let's gather information
			stateListener.stateChanged(MediaPlaybackState.Playing);
			
			MPlayerInstance instance = getCurrentInstance();
			
			if ( instance != null ){
			
				instance.initialised();
			}
			
			reportNewState(MediaPlaybackState.Playing);
		} else
		if(line.startsWith(VLCCommand.ANS_TIME)) {
			try {
				MPlayerInstance instance = getCurrentInstance();
				
				if ( instance != null ){
					instance.positioned();
				}
				
				float position = Float.parseFloat(line.substring(VLCCommand.ANS_TIME.length()));
				reportPosition(position);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else
		if(line.startsWith(VLCCommand.STATUS_LENGTH)) {
			try {
				float duration = Float.parseFloat(line.substring(VLCCommand.STATUS_LENGTH.length()));
				if(!firstLengthReceived) {
					firstLengthReceived = true;
				}
				reportDuration(duration);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else
		if(line.startsWith(VLCCommand.ANS_VOLUME)) {
			try {
				int volume = (int) Float.parseFloat(line.substring(VLCCommand.ANS_VOLUME.length()));
				reportVolume(volume);
				if(!firstVolumeReceived) {
					firstVolumeReceived = true;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else
		if(line.startsWith(ID_VIDEO_ASPECT)) {
			try {
				float aspect = Float.parseFloat(line.substring(ID_VIDEO_ASPECT.length()));
				if(aspect > 0) {
					setAspectRatio(aspect);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else
		if(line.startsWith(VLCCommand.ANS_DIMENSION)) {
			try {
				Pattern p = Pattern.compile(".*?([0-9]+)x([0-9]+).*?");
				Matcher m = p.matcher(line);
				if(m.matches()) {
					width = Integer.parseInt(m.group(1));
					int height = Integer.parseInt(m.group(2));
					
					if(metaDataListener != null) {
						setAspectRatio((float)width / (float)height);
					}

					int videoWidth = width;
					int videoHeight = height;
					
					int displayWidth = videoWidth;
					int displayHeight = videoHeight;
					
					if(aspect > 0 && abs(aspect-(float)videoWidth / (float)videoHeight) > 0.1) {
						displayWidth = (int) (displayHeight * aspect);
					}
					displayHeight += 150;
					if(metaDataListener != null) {
						metaDataListener.receivedVideoResolution(videoWidth, videoHeight);
						metaDataListener.receivedDisplayResolution(displayWidth, displayHeight);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else
		if(line.startsWith(ANS_ASPECT)) {
			try {
				aspect = Float.parseFloat(line.substring(ANS_ASPECT.length()));
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (line.startsWith(VLCCommand.ANS_SUB_FILE)) {
			String fileName = line.substring(VLCCommand.ANS_SUB_FILE.length() + 1);
			language = new Language(LanguageSource.FILE, String.valueOf(fileName.hashCode()));
			try {
				File f = new File(fileName);
				language.setSourceInfo(f.getAbsolutePath());
				fileName = f.getName();
			} catch (Exception e) {
				e.printStackTrace();
			}
			language.setName(fileName);
			Language parsed = language;
			reportParsingDone();
			MPlayerInstance instance = getCurrentInstance();
			
			if ( instance != null ){

				if(instance.activateNextSubtitleLoaded) {
					instance.activateNextSubtitleLoaded = false;
					reportSubtitleChanged(parsed.getId(), parsed.getSource());
				}
			}
		} else if (line.startsWith(VLCCommand.STATUS_SUBS)) {
			JSONObject subStats = (JSONObject) JSONValue.parse(line.substring(VLCCommand.STATUS_SUBS.length() + 1));
			String state = (String) subStats.remove(VLCCommand.SUB_STATE);
			Map<String, String> subs = new TreeMap<String, String>(subStats);
			for (Entry<String, String> entry : subs.entrySet()) {
				language = new Language(LanguageSource.STREAM, entry.getKey());
				language.setName(entry.getValue());
				Language parsed = language;
				reportParsingDone();
				if (entry.getKey().equals(state)) {
					reportSubtitleChanged(parsed.getId(), parsed.getSource());
				}
			}
			
		} else if (line.equals(VLCCommand.STATUS_FINISHED)) {
			reportNewState(MediaPlaybackState.Stopped);
		} else if(line.startsWith("VDecoder init failed")) {
			
			MediaPlaybackState.Failed.setDetails(  "azemp.failed.nocodec"  );
			reportNewState(MediaPlaybackState.Failed);
		}else if(line.startsWith("<vo_direct3d>Reading display capabilities failed")) {
			
			MediaPlaybackState.Failed.setDetails( "azemp.failed.d3dbad" );
			reportNewState(MediaPlaybackState.Failed);
		}else if(line.startsWith(ID_EXIT)) {
			
			reportNewState(MediaPlaybackState.Closed);
		}

		//else System.out.println(line);
		
			
		if(parsingLanguage && ! stillParsing) {
			Language parsed = language;
			reportParsingDone();
			MPlayerInstance instance = getCurrentInstance();
			
			if ( instance != null ){

				if(instance.activateNextSubtitleLoaded) {
					instance.activateNextSubtitleLoaded = false;
					reportSubtitleChanged(parsed.getId(), parsed.getSource());
				}
			}
		}
	}
	
	private double abs(float f) {
		return f > 0 ? f : -f;
	}

	public void doLoadSubtitlesFile(String file,boolean autoPlay) {		
		MPlayerInstance instance = getCurrentInstance();
		
		if ( instance != null ){

			instance.doLoadSubtitlesFile( file, autoPlay );
		}
	}
	
	public void showMessage(String message, int duration) {
		MPlayerInstance instance = getCurrentInstance();
		
		if ( instance != null ){

			instance.sendCommand("osd_show_text \"" + message + "\" " + duration + " " + 0);
		}
		
	}
	
	private void reportSubtitleChanged(String subtitleId,LanguageSource source) {
		if(metaDataListener != null) {
			metaDataListener.activeSubtitleChanged(subtitleId,source);
		}		
	}

	private void reportAudioTrackChanged(String audioId) {
		if(metaDataListener != null) {
			metaDataListener.activeAudioTrackChanged(audioId);
		}		
	}

	private void reportParsingDone() {
		if(isAudioTrack) {
			reportFoundAudioTrack(language);
		} else {
			reportFoundSubtitle(language);
		}
		language = null;
		parsingLanguage = false;
		isAudioTrack = false;
	}
	
	public abstract long getComponentId();
	public abstract void setAspectRatio(float aspectRatio);

	
	public void 
	doOpen(
		String fileOrUrl ) 
	{
		
		MPlayerInstance instance;
		
		synchronized( this ){
			
			doStop( false );
			
			instance = current_instance = new MPlayerInstance();
			synchronized (output) {
				timeWait = DEFAULT_TIME_WAIT_BUFFER;
				output.notifyAll();
			}
		}
		
		reportNewState(MediaPlaybackState.Opening);
		
		firstLengthReceived = false;
		firstVolumeReceived = false;
		
		instance.doOpen( 
			fileOrUrl, 
			getComponentId(),
			new MPlayerInstance.OutputConsumer()
			{
				public void
				consume(
					String line) 
				{
					synchronized (output) {
						output.add(line);
						output.notifyAll();
					}				
				}
			});
	}

	protected MPlayerInstance
	getCurrentInstance()
	{
		synchronized( this ){
			
			return( current_instance );
		}
	}
	
	public void 
	doPause() 
	{
		synchronized (output) {
			timeWait = 0;
			output.notifyAll();
		}
		MPlayerInstance instance = getCurrentInstance();
		
		if ( instance != null ){
		
			instance.doPause();
		}
		
		reportNewState(MediaPlaybackState.Paused);
	}

	
	public void 
	doResume() 
	{
		synchronized (output) {
			timeWait = DEFAULT_TIME_WAIT_BUFFER;
			output.notifyAll();
		}
		MPlayerInstance instance = getCurrentInstance();
		
		if ( instance != null ){

			instance.doResume();
		}
		
		reportNewState(MediaPlaybackState.Playing);
	}

	
	public void 
	doSeek(
		float timeInSecs) 
	{
		MPlayerInstance instance = getCurrentInstance();
		
		if ( instance != null ){

			instance.doSeek( timeInSecs );
		}
	}

	
	public void 
	doSetVolume(
		int volume) 
	{
		MPlayerInstance instance = getCurrentInstance();
		
		if ( instance != null ){
		
			instance.doSetVolume(volume);
		}
		
		reportVolume(volume);
	}

	public void
	mute(
		boolean		on )
	{
		MPlayerInstance instance = getCurrentInstance();
		
		if ( instance != null ){
		
			instance.doMute( on );
		}
	}
	
	public void
	doRedraw()
	{
		MPlayerInstance instance = getCurrentInstance();
		
		if ( instance != null ){
		
			instance.doRedraw();
		}
	}
	
	public void 
	setAudioTrack(
		Language language) 
	{
		MPlayerInstance instance = getCurrentInstance();
		
		if ( instance != null ){
		
			instance.setAudioTrack(language);
		}
	}
	
	public void 
	setSubtitles(
		Language language) 
	{
		MPlayerInstance instance = getCurrentInstance();
		
		if ( instance != null ){
		
			reportSubtitleChanged( instance.setSubtitles(language),language != null ? language.getSource() : null);
		}
	}
	
	public void 
	doStop() 
	{
		doStop( true );
	}
	
	protected void 
	doStop(
		boolean	report_state ) 
	{		
		synchronized( this ){
			if ( current_instance != null ){
				current_instance.doStop();
				current_instance = null;
			}
			synchronized (output) {
				timeWait = 0;
				output.clear();
				output.notifyAll();
			}	
		}
		
		if ( report_state ){
			reportNewState(MediaPlaybackState.Stopped);
		}
	}

	private MetaDataListener metaDataListener;
	private StateListener stateListener;
	private VolumeListener volumeListener;
	private PositionListener positionListener;
	
	
	public void setMetaDataListener(MetaDataListener listener) {
		this.metaDataListener = listener;
	}

	
	public void setStateListener(StateListener listener) {
		this.stateListener = listener;
		
	}

	
	public void setVolumeListener(VolumeListener listener) {
		this.volumeListener = listener;
	}
	
	
	public void setPositionListener(PositionListener listener) {
		this.positionListener = listener;		
	}
	
	private void reportPosition(float position) {
		if(positionListener != null) {
			positionListener.positionChanged(position);
		}
	}
	
	private void reportVolume(int volume) {
		if(volumeListener != null) {
			volumeListener.volumeChanged(volume);
		}
	}
	
	private void reportDuration(float duration) {
		if(metaDataListener != null) {
			metaDataListener.receivedDuration(duration);
		}
	}
	
	private void reportFoundAudioTrack(Language audioTrack) {
		if(metaDataListener != null) {
			metaDataListener.foundAudioTrack(audioTrack);
		}
	}
	
	private void reportFoundSubtitle(Language subtitle) {
		if(metaDataListener != null) {
			metaDataListener.foundSubtitle(subtitle);
		}
	}
	
	private void reportNewState(MediaPlaybackState state) {
		if(stateListener != null) {
			stateListener.stateChanged(state);
		}
	}
	
	public void 
	dispose() 
	{
		disposed = true;
		
		doStop();
	}
}
