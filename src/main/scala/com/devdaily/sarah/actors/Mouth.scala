package com.devdaily.sarah.actors

import akka.actor._
import com.weiglewilczek.slf4s.Logging
import com.weiglewilczek.slf4s.Logger
import com.devdaily.sarah.Sarah
import com.devdaily.sarah.ComputerVoice
import com.devdaily.sarah.plugins.PluginUtils
import com.devdaily.sarah.plugins.PlaySoundFileRequest

/**
 * Mouth has the responsibility of speaking whatever it is told to speak,
 * in the order the requests are given. It also sends priority messages to
 * the Brain so the Brain will always know the current Mouth state.
 */
class Mouth(sarah: Sarah) extends akka.actor.Actor with Logging {
  
  val log = Logger("Mouth")
  
  def receive = {
    case message: SpeakMessageFromBrain =>  
         speak(message.message)
    case playSoundFileRequest: PlaySoundFileRequest =>
         handlePlaySoundFileRequest(playSoundFileRequest)
    case unknown => 
         log.info(format("got an unknown request(%s), ignoring it", unknown.toString))
  }

  def timestamp = System.currentTimeMillis

  def handlePlaySoundFileRequest(playSoundFileRequest: PlaySoundFileRequest) {
    playSound(playSoundFileRequest.soundFile)
  }
  
  /**
   * Speak whatever needs to be spoken, then wait the given time
   * before returning.
   */
  def speak(textToSpeak: String) {
    sarah.setMouthState(Sarah.MOUTH_STATE_SPEAKING)
    ComputerVoice.speak(textToSpeak)
    sarah.clearMicrophone
    // TODO add this back in
    // brain.markThisAsTheLastTimeSarahSpoke
    PluginUtils.sleep(Brain.SHORT_DELAY)
    sarah.setMouthState(Sarah.MOUTH_STATE_NOT_SPEAKING)
  }
  
  /**
   * Play the given sound file.
   * @see http://www.devdaily.com/java/java-play-sound-file-from-command-line-wav-headless-mode
   */
  def playSound(soundFile: String) {
    import java.io.FileInputStream
    import sun.audio.AudioStream
    import sun.audio.AudioPlayer
    try {
      var in = new FileInputStream(soundFile)
      val audioStream = new AudioStream(in)
      AudioPlayer.player.start(audioStream)
    } catch {
      case e: Exception => e.printStackTrace
    }
  }
  

}











