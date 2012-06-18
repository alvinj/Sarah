package com.devdaily.sarah.actors

import akka.actor._
import edu.cmu.sphinx.frontend.util.Microphone
import edu.cmu.sphinx.recognizer.Recognizer
import java.util.Date
import akka.event.Logging
import com.weiglewilczek.slf4s._
import com.devdaily.sarah.Sarah
import com.devdaily.sarah.plugins.PlaySoundFileRequest
import com.devdaily.sarah.ComputerVoice
import com.devdaily.sarah.plugins.PluginUtils

case class MouthIsSpeaking
case class MouthIsFinishedSpeaking

/**
 * This class is a child of the Mouth class, and does the actual
 * speaking (so the Mouth class can be free to receive other messages).
 */
class MouthHelper(sarah: Sarah) 
extends Actor
with Logging
{
  
  val log = Logger("MouthHelper")
  val mouth:ActorRef = context.parent
  
  def receive = {

    case message: SpeakMessageFromBrain =>  
         speak(message.message)

    case playSoundFileRequest: PlaySoundFileRequest =>
         playSoundFile(playSoundFileRequest)

    case unknown => 
         log.info(format("got an unknown request(%s), ignoring it", unknown.toString))

  }

  /**
   * Speak whatever needs to be spoken, then wait the given time
   * before returning.
   */
  def speak(textToSpeak: String) {
    mouth ! MouthIsSpeaking
    ComputerVoice.speak(textToSpeak)
    sarah.clearMicrophone
    PluginUtils.sleep(Brain.SHORT_DELAY)
    mouth ! MouthIsFinishedSpeaking
  }
  
  def playSoundFile(playSoundFileRequest: PlaySoundFileRequest) {
    mouth ! MouthIsSpeaking
    playSound(playSoundFileRequest.soundFile)
    PluginUtils.sleep(Brain.SHORT_DELAY)
    mouth ! MouthIsFinishedSpeaking
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








