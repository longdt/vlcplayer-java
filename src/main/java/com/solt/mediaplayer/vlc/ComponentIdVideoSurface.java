package com.solt.mediaplayer.vlc;

import java.awt.Canvas;

import uk.co.caprica.vlcj.binding.LibVlc;
import uk.co.caprica.vlcj.logger.Logger;
import uk.co.caprica.vlcj.player.MediaPlayer;
import uk.co.caprica.vlcj.player.embedded.videosurface.CanvasVideoSurface;
import uk.co.caprica.vlcj.player.embedded.videosurface.VideoSurfaceAdapter;
import uk.co.caprica.vlcj.player.embedded.videosurface.linux.LinuxVideoSurfaceAdapter;
import uk.co.caprica.vlcj.player.embedded.videosurface.mac.MacVideoSurfaceAdapter;
import uk.co.caprica.vlcj.player.embedded.videosurface.windows.WindowsVideoSurfaceAdapter;
import uk.co.caprica.vlcj.runtime.RuntimeUtil;

/**
 * Encapsulation of a video surface that wraps the native component id of the video surface
 * component.
 * <p>
 * This is required for example when using remote out-of-process media players but with video
 * rendering in a local application. In these scenarios, it is not possible to serialize the Canvas
 * component to the remote process to get the proper component ID (the copied Canvas component would
 * have a different native ID).
 * <p>
 * It is also not possible to get a native component ID if the component is not displayable.
 */
public class ComponentIdVideoSurface extends CanvasVideoSurface {

    /**
     * Serial version.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Native component identifier for the video surface.
     */
    private final long componentId;

    /**
     * Create a new video surface.
     * 
     * @param componentId native component identifier for the video surface
     * @param videoSurfaceAdapter adapter to attach a video surface to a native media player
     */
    public ComponentIdVideoSurface(long componentId, VideoSurfaceAdapter videoSurfaceAdapter) {
        super(new Canvas(), videoSurfaceAdapter);
        this.componentId = componentId;
    }

    @Override
    public void attach(LibVlc libvlc, MediaPlayer mediaPlayer) {
        Logger.debug("attach()");
        videoSurfaceAdapter.attach(libvlc, mediaPlayer, componentId);
    }
    
    public static ComponentIdVideoSurface create(long componentId) {
        Logger.debug("newVideoSurface(componentId={})", componentId);
        VideoSurfaceAdapter videoSurfaceAdapter;
        if(RuntimeUtil.isNix()) {
            videoSurfaceAdapter = new LinuxVideoSurfaceAdapter();
        }
        else if(RuntimeUtil.isWindows()) {
            videoSurfaceAdapter = new WindowsVideoSurfaceAdapter();
        }
        else if(RuntimeUtil.isMac()) {
            videoSurfaceAdapter = new MacVideoSurfaceAdapter();
        }
        else {
            throw new RuntimeException("Unable to create a media player - failed to detect a supported operating system");
        }
        ComponentIdVideoSurface videoSurface = new ComponentIdVideoSurface(componentId, videoSurfaceAdapter);
        Logger.debug("videoSurface={}", videoSurface);
        return videoSurface;
    }
}