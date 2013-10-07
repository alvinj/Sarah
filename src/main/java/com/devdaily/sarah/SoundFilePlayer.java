package com.devdaily.sarah;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import javax.sound.sampled.*;
import javazoom.jlgui.basicplayer.BasicController;
import javazoom.jlgui.basicplayer.BasicPlayer;
import javazoom.jlgui.basicplayer.BasicPlayerEvent;
import javazoom.jlgui.basicplayer.BasicPlayerException;
import javazoom.jlgui.basicplayer.BasicPlayerListener;

/**
 * Sample use:
 * 
 * JLayerSoundPlayer mp3 = new JLayerSoundPlayer(filename); mp3.play();
 * I *think* this class requires the following jar files:
 *   basicplayer3.0.jar
 *   jl1.0.1-orig.jar
 *   kj_dsp1.1.jar
 *   mp3spi1.9.4.jar
 *   tritonus_share.jar
 */
public class SoundFilePlayer implements BasicPlayerListener {

  private String soundFileName;

  private BasicPlayer mp3Player = new BasicPlayer();
  private BasicController mp3PlayerController = (BasicController) mp3Player;
  private static final double DEFAULT_GAIN = 0.5;

  // java audio
  Clip clip;

  public SoundFilePlayer(String soundFileName) {
    this.soundFileName = soundFileName;
  }

  /**
   * Call this method to play the file.
   * @throws Exception
   */
  public void play() throws Exception {
    if (soundFileName.toLowerCase().endsWith(".mp3")) {
      playMp3WithJLayer();
    } else {
      playSoundFileWithJavaAudio();
    }
  }

  /**
   * Call this method to properly close everything.
   */
  public void close() {
    if (soundFileName.endsWith(".mp3")) {
      if (mp3Player != null)
        try {
          mp3PlayerController.stop();
        } catch (BasicPlayerException e) {
          // TODO whatev
        }
    } else {
      if (clip != null)
        clip.close();
    }
  }

  // from
  // http://www.java2s.com/Code/Java/Development-Class/AnexampleofloadingandplayingasoundusingaClip.htm
  private void playSoundFileWithJavaAudio() throws UnsupportedAudioFileException, IOException,
      LineUnavailableException {
    AudioInputStream sound = AudioSystem.getAudioInputStream(new File(soundFileName));

    // load the sound into memory (a Clip)
    DataLine.Info info = new DataLine.Info(Clip.class, sound.getFormat());
    Clip clip = (Clip) AudioSystem.getLine(info);
    clip.open(sound);

    // due to bug in Java Sound, explicitly exit the VM when
    // the sound has stopped.
    clip.addLineListener(new LineListener() {
      public void update(LineEvent event) {
        if (event.getType() == LineEvent.Type.STOP) {
          event.getLine().close();
        }
      }
    });

    // play the sound clip
    clip.start();
  }

  //---------------------- JLayer Experiments ------------------------
  
  private void playMp3WithJLayer() throws BasicPlayerException {
    mp3Player.addBasicPlayerListener(this);
    mp3PlayerController.open(new File(soundFileName));
    mp3PlayerController.play();
    mp3PlayerController.setGain(DEFAULT_GAIN);
//     // Set Pan (-1.0 to 1.0).
//     control.setPan(0.0);
  }

  /**
   * only works for jlayer mp3 player
   */
  public void pause() throws BasicPlayerException {
    mp3PlayerController.pause();
  }
  
  /**
   * only works for jlayer mp3 player
   */
  public void stop() throws BasicPlayerException {
    mp3PlayerController.stop();
  }

  /**
   * only works for jlayer mp3 player
   */
  public void resume() throws BasicPlayerException {
    mp3PlayerController.resume();
  }

  /**
   * @param volume
   * A number between 0.0 and 1.0 (loudest)
   * @throws BasicPlayerException
   */
  public void setGain(double volume) throws BasicPlayerException {
    // TODO i'm now ignoring the java code that plays wav files, and
    // just implementing code for the mp3 player
    mp3PlayerController.setGain(volume);
  }

  /**
   * Open callback, stream is ready to play.
   *
   * properties map includes audio format dependant features such as
   * bitrate, duration, frequency, channels, number of frames, vbr flag, ... 
   *
   * @param stream could be File, URL or InputStream
   * @param properties audio stream properties.
   */
  public void opened(Object stream, Map properties)
  {
    // Pay attention to properties. It's useful to get duration, 
    // bitrate, channels, even tag such as ID3v2.
  }

  /**
   * Progress callback while playing.
   * 
   * This method is called severals time per seconds while playing.
   * properties map includes audio format features such as
   * instant bitrate, microseconds position, current frame number, ... 
   * 
   * @param bytesread from encoded stream.
   * @param microseconds elapsed (<b>reseted after a seek !</b>).
   * @param pcmdata PCM samples.
   * @param properties audio stream parameters.
  */
  public void progress(int bytesread, long microseconds, byte[] pcmdata, Map properties)
  {
    // Pay attention to properties. It depends on underlying JavaSound SPI
    // MP3SPI provides mp3.equalizer.
  }

  /**
   * Notification callback for basicplayer events such as opened, eom ...
   * 
   * @param event
   */
  public void stateUpdated(BasicPlayerEvent event)
  {
  }

  /**
   * A handle to the BasicPlayer, plugins may control the player through
   * the controller (play, stop, ...)
   * @param controller : a handle to the player
   */ 
  public void setController(BasicController controller)
  {
  }

}
















