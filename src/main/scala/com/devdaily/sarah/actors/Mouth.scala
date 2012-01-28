package com.devdaily.sarah.actors

import scala.actors.Actor
import com.weiglewilczek.slf4s.Logging
import com.weiglewilczek.slf4s.Logger
import com.devdaily.sarah.Sarah
import com.devdaily.sarah.ComputerVoice
import com.devdaily.sarah.plugins.Utils

class Mouth(sarah: Sarah, brain: Brain) extends Actor with Logging {
  
  val log = Logger("Mouth")

  def act() {
    loop {
      react {
        case message: SpeakMessageFromBrain => speak(message.message)
        case unknown => log.info(format("got an unknown request(%s), ignoring it", unknown.toString))
      }
    }
  }

  def timestamp = System.currentTimeMillis
  
  /**
   * Speak whatever needs to be spoken, then wait the given time
   * before returning.
   */
  def speak(textToSpeak: String) {
    // TODO may want to check the time sarah last spoke. think about this
    //      when i'm not so tired.
    log.info(format("(%d) about to say (%s)", timestamp, textToSpeak))
    var previousState = sarah.getState
    sarah.setCurrentState(Sarah.SARAH_IS_SPEAKING)
    ComputerVoice.speak(textToSpeak)
    sarah.clearMicrophone
    brain.markThisAsTheLastTimeSarahSpoke
    
    // TODO at some point i should be able to get rid of this artificial
    //      wait time
    Utils.sleep(brain.delayAfterSpeaking)

    // TODO i'm not convinced this is the right thing to do, but it is
    //      correct to say that sarah is not speaking any more.
    sarah.setCurrentState(previousState)
  }
  

}





