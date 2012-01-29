package com.devdaily.sarah.actors

import scala.actors.Actor
import com.weiglewilczek.slf4s.Logging
import com.weiglewilczek.slf4s.Logger
import com.devdaily.sarah.Sarah
import com.devdaily.sarah.ComputerVoice
import com.devdaily.sarah.plugins.PluginUtils

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
    val t1 = timestamp
    log.info(format("just before ComputerVoice.speak (%d)", t1))
    ComputerVoice.speak(textToSpeak)
    val t2 = timestamp
    log.info(format("just after ComputerVoice.speak (%d)", t2))
    log.info(format("time it took to speak (%d)", t2-t1))
    sarah.clearMicrophone
    brain.markThisAsTheLastTimeSarahSpoke
    
    // TODO at some point i should be able to get rid of this artificial
    //      wait time
    PluginUtils.sleep(Brain.SHORT_DELAY)

    // TODO i'm not convinced this is the right thing to do, but it is
    //      correct to say that sarah is not speaking any more.
    sarah.setCurrentState(previousState)
  }
  

}





