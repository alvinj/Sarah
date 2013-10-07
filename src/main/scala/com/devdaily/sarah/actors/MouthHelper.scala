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
import com.devdaily.sarah.SoundFilePlayer

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
  
  /**
   * TODO there is a bug here, where the playSound method returns immediately,
   * even if it plays a 10-second clip. As a result, Sarah doesn't know when
   * it last spoke.
   */
  def playSoundFile(playSoundFileRequest: PlaySoundFileRequest) {
    mouth ! MouthIsSpeaking
    playSound(playSoundFileRequest.soundFile)
    PluginUtils.sleep(Brain.SHORT_DELAY)
    mouth ! MouthIsFinishedSpeaking
  }
  
  def playSound(soundFile: String) {
    try {
      val p = new SoundFilePlayer(soundFile)
      p.play
    } catch {
      case e:Exception => log.error(e.getMessage)
    }
    // TODO need to close the file, but to do so i need a way of knowing
    // when the file is finished playing; this method doesn't seem to block,
    // so you can't just call close() here.
  }
  
  
}















