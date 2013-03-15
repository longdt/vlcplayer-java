package com.solt.mediaplayer.vlc.remote;

public enum MediaPlaybackState {
	Uninitialized,Opening,Playing,Paused,Stopped,Closed,Failed;
	
	private String details;
	
	public void
	setDetails(
		String		_details )
	{
		details = _details;
	}
	
	public String
	getDetails()
	{
		return( details );
	}
}
