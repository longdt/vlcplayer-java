package com.solt.mediaplayer.vlc.remote;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseMediaPlayer implements MediaPlayer,MetaDataListener,StateListener,VolumeListener,PositionListener,TaskListener {

	private List<MetaDataListener> metaDataListeners;
	private List<StateListener> stateListeners;
	private List<VolumeListener> volumeListeners;
	private List<PositionListener> positionListeners;
	private List<TaskListener> taskListeners;
	
	private MediaPlaybackState 	currentState;	
	private int					currentVolume;
	private int					videoWidth;
	private int					videoHeight;
	private int					displayWidth;
	private int					displayHeight;
	private float				currentPositionInSecs;
	private float				durationInSecs;
	private boolean				durationInSecsSet;
	
	private List<Language>		audioTracks;
	private List<Language>		subtitles;
	
	private String				activeAudioTrackId = "0";
	private LanguageSource		activeSubtitleSource = null;
	private String				activeSubtitleId = null;
	
	private String 				openedFile;
	
	
	
	public BaseMediaPlayer() {
		metaDataListeners = new ArrayList<MetaDataListener>(1);
		stateListeners = new ArrayList<StateListener>(1);
		volumeListeners = new ArrayList<VolumeListener>(1);
		positionListeners = new ArrayList<PositionListener>(1);
		taskListeners = new ArrayList<TaskListener>(1);
		
		initialize();
		
		setMetaDataListener(this);
		setStateListener(this);
		setVolumeListener(this);
		setPositionListener(this);
		
	}

	private void initialize() {
		openedFile = null;
		
		audioTracks = new ArrayList<Language>();
		subtitles = new ArrayList<Language>();
		
		activeAudioTrackId = "0";
		activeSubtitleId = null;
		activeSubtitleSource = null;
				
		durationInSecs 			= 0;
		durationInSecsSet		= false;
		currentPositionInSecs	= 0;
		
		currentState = MediaPlaybackState.Uninitialized;
	}
	
	
	
	
	public void addMetaDataListener(MetaDataListener listener) {
		synchronized (metaDataListeners) {
			metaDataListeners.add(listener);
		}
		
	}

	
	public void addStateListener(StateListener listener) {
		synchronized (stateListeners) {
			stateListeners.add(listener);
		}
		
	}

	
	public void addVolumeListener(VolumeListener listener) {
		synchronized (volumeListeners) {
			volumeListeners.add(listener);
		}
		
	}
	
	
	public void addPositionListener(PositionListener listener) {
		synchronized (positionListeners) {
			positionListeners.add(listener);
		}
		
	}
	
	public abstract void setStateListener(StateListener listener);
	public abstract void setVolumeListener(VolumeListener listener);
	public abstract void setMetaDataListener(MetaDataListener listener);
	public abstract void setPositionListener(PositionListener listener);
	public abstract void doOpen(String fileOrUrl);
	public abstract void doPause();
	public abstract void doResume();
	public abstract void doStop();
	public abstract void doSeek(float timeInSecs);
	public abstract void doSetVolume(int volume);	
	public abstract void doLoadSubtitlesFile(String file,boolean autoPlay);

	public void doLoadSubtitlesFile(String file) {
		doLoadSubtitlesFile(file, true);
	}
	
	public void open(String fileOrUrl) {
		if(currentState == MediaPlaybackState.Uninitialized || currentState == MediaPlaybackState.Stopped) {
			openedFile = fileOrUrl;
			doOpen(fileOrUrl);
		} else {
			doStop();
			initialize();
			openedFile = fileOrUrl;
			doOpen(fileOrUrl);
		}
		
		//Finds subtitles
	}

	public String getOpenedFile() {
		return openedFile;
	}
	
	public void loadSubtitlesFile(String file) {
		doLoadSubtitlesFile(file);
	}
	
	public void pause() {
		if(currentState == MediaPlaybackState.Playing) {
			doPause();
		}		
	}

	public void play() {
		if(currentState == MediaPlaybackState.Paused) {
			doResume();
		}
		
	}
	
	public void togglePause() {
		if(currentState == MediaPlaybackState.Paused) {
			doResume();
		} else
		if(currentState == MediaPlaybackState.Playing) {
			doPause();
		}
	}

	public void removeMetaDataListener(MetaDataListener listener) {
		synchronized (metaDataListeners) {
			while(metaDataListeners.contains(listener)) {
				metaDataListeners.remove(listener);
			}
		}
		
	}

	public void removeStateListener(StateListener listener) {
		synchronized (stateListeners) {
			while(stateListeners.contains(listener)) {
				stateListeners.remove(listener);
			}
		}
		
	}

	
	public void removeVolumeListener(VolumeListener listener) {
		synchronized (volumeListeners) {
			while(volumeListeners.contains(listener)) {
				volumeListeners.remove(listener);
			}
		}
		
	}
	
	
	public void removePositionListener(PositionListener listener) {
		synchronized (positionListeners) {
			while(positionListeners.contains(listener)) {
				positionListeners.remove(listener);
			}
		}
		
	}
	
	public synchronized void seek(float timeInSecs) {
		if(timeInSecs < 0) timeInSecs = 0;
		if(timeInSecs > durationInSecs) timeInSecs = durationInSecs;
		
		if(currentState == MediaPlaybackState.Playing || currentState == MediaPlaybackState.Paused) {			
			doSeek(timeInSecs);
		}
		
	}

	
	public void setVolume(int volume) {
		if(currentState == MediaPlaybackState.Playing || currentState == MediaPlaybackState.Paused) {
			doSetVolume(volume);
		}
	}
	
	
	public void receivedDisplayResolution(int width, int height) {
		displayWidth = width;
		displayHeight = height;
		synchronized (metaDataListeners) {
			for(MetaDataListener listener : metaDataListeners) {
				listener.receivedDisplayResolution(width,height);
			}
		}
	}
	
	
	public void receivedDuration(float durationInSecs) {
		durationInSecsSet = true;
		this.durationInSecs = durationInSecs;
		synchronized (metaDataListeners) {
			for(MetaDataListener listener : metaDataListeners) {
				listener.receivedDuration(durationInSecs);
			}
		}
	}
	
	
	public void receivedVideoResolution(int width, int height) {
		videoWidth = width;
		videoHeight = height;
		synchronized (metaDataListeners) {
			for(MetaDataListener listener : metaDataListeners) {
				listener.receivedVideoResolution(width, height);
			}
		}
	}
	
	
	public void foundAudioTrack(Language language) {
		synchronized (audioTracks) {
			audioTracks.add(language);
		}		
		synchronized (metaDataListeners) {
			for(MetaDataListener listener : metaDataListeners) {
				listener.foundAudioTrack(language);
			}
		}
	}
	
	
	public void foundSubtitle(Language language) {
		synchronized (subtitles) {
			subtitles.add(language);
		}
		synchronized (metaDataListeners) {
			for(MetaDataListener listener : metaDataListeners) {
				listener.foundSubtitle(language);
			}
		}
	}
	
	
	public void activeAudioTrackChanged(String audioTrackId) {	
		activeAudioTrackId = audioTrackId;
		synchronized (metaDataListeners) {
			for(MetaDataListener listener : metaDataListeners) {
				listener.activeAudioTrackChanged(audioTrackId);
			}
		}
	}
	
	
	public void activeSubtitleChanged(String subtitleId,LanguageSource source) {
		//System.out.println(subtitleId + " " + source);
		activeSubtitleId = subtitleId;
		activeSubtitleSource = source;
		synchronized (metaDataListeners) {
			for(MetaDataListener listener : metaDataListeners) {
				listener.activeSubtitleChanged(subtitleId,source);
			}
		}
	}
	
	
	public Language getSubtitleByIdAndSource(String subtitleId,LanguageSource source) {
		if(subtitleId == null) return null;
		synchronized (subtitles) {
			for(Language l : subtitles) {
				if(l.id != null && l.id.equals(subtitleId) && l.source == source) {
					return l;
				}
			}
			return null;
		}
		
	}




	public void stateChanged(MediaPlaybackState newState) {
		currentState = newState;
		synchronized (stateListeners) {
			for(StateListener listener : stateListeners) {
				listener.stateChanged( newState);
			}
		}
		
		
	}
	
	
	public void volumeChanged(int newVolume) {
		currentVolume = newVolume;
		synchronized (volumeListeners) {
			for(VolumeListener listener : volumeListeners) {
				listener.volumeChanged(newVolume);
			}			
		}		
	}
	
	
	public void positionChanged(float currentTimeInSecs) {
		if(currentPositionInSecs != currentTimeInSecs) {
			currentPositionInSecs = currentTimeInSecs;
			synchronized (positionListeners) {
				for(PositionListener listener : positionListeners) {
					listener.positionChanged(currentTimeInSecs);
				}			
			}
		}
	}
	
	
	public void addTaskListener(TaskListener listener) {
		synchronized (taskListeners) {
			taskListeners.add(listener);
		}		
	}
	
	
	public void removeTaskListener(TaskListener listener) {
		synchronized (taskListeners) {
			while(taskListeners.contains(listener)) {
				taskListeners.remove(listener);
			}
		}
		
	}
	
	
	public void taskStarted(String taskName) {
		synchronized (taskListeners) {
			for(TaskListener listener : taskListeners) {
				listener.taskStarted(taskName);
			}
		}
		
	}
	
	
	public void taskProgress(String taskName, int percent) {
		synchronized (taskListeners) {
			for(TaskListener listener : taskListeners) {
				listener.taskProgress(taskName,percent);
			}
		}
		
	}
	
	
	public void taskEnded(String taskName) {
		synchronized (taskListeners) {
			for(TaskListener listener : taskListeners) {
				listener.taskEnded(taskName);
			}
		}
	}
	
	
	
	
	public void stop() {
		if(currentState == MediaPlaybackState.Opening || currentState == MediaPlaybackState.Playing || currentState == MediaPlaybackState.Paused || currentState == MediaPlaybackState.Buffering) {
			doStop();
		}		
	}



	public MediaPlaybackState getCurrentState() {
		return currentState;
	}



	public int getVolume() {
		return currentVolume;
	}



	public int getVideoWidth() {
		return videoWidth;
	}



	public int getVideoHeight() {
		return videoHeight;
	}



	public int getDisplayWidth() {
		return displayWidth;
	}



	public int getDisplayHeight() {
		return displayHeight;
	}


	public float getPositionInSecs() {
		return currentPositionInSecs;
	}

	public void
	clearDurationInSecs()
	{
		durationInSecs 		= 0;
		durationInSecsSet	= false;
	}
	
	public void
	setDurationInSecs(
		float		secs )
	{
		// this is a hint in case we don't receive a real value, which sometimes happens during streaming :(
		
		if ( !durationInSecsSet ){
			
			durationInSecs = secs;
		}
	}
	
	public float getDurationInSecs() {
		return durationInSecs;
	}
	
	
	public Language[] getAudioTracks() {
		List<Language> result = new ArrayList<Language>();
		synchronized (audioTracks) {			
			for(Language language : audioTracks) {
				result.add(language);			
			}			
		}
		return result.toArray(new Language[result.size()]);
	}
	
	
	public Language[] getSubtitles() {
		List<Language> result = new ArrayList<Language>();		
		synchronized (subtitles) {			
			for(Language language : subtitles) {
				result.add(language);			
			}			
		}
		return result.toArray(new Language[result.size()]);
	}
	
	
	public String getActiveAudioTrackId() {
		return activeAudioTrackId;
	}
	
	
	public String getActiveSubtitleId() {
		return activeSubtitleId;
	}
	
	public Language getActiveSubtitle() {
		return getSubtitleByIdAndSource(activeSubtitleId,activeSubtitleSource);
	}

}
