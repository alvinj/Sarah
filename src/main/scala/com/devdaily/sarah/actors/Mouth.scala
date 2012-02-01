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
        case message: SpeakMessageFromBrain =>  
             val s = format("(MOUTH) GOT PLEASE-SAY REQUEST (%s) AT (%d)", message.message, System.currentTimeMillis)
             println(s)
             speak(message.message)
        case unknown => 
             log.info(format("got an unknown request(%s), ignoring it", unknown.toString))
      }
    }
  }

  def timestamp = System.currentTimeMillis
  
  /**
   * Speak whatever needs to be spoken, then wait the given time
   * before returning.
   */
  def speak(textToSpeak: String) {
    val s = format("(MOUTH) ENTERED SPEAK (%s) AT (%d)", textToSpeak, System.currentTimeMillis)
    println(s)
    // TODO sarah may want to speak, even in a light sleep or deep sleep; account for that
    log.info(format("(%d) about to say (%s)", timestamp, textToSpeak))
    sarah.setMouthState(Sarah.MOUTH_STATE_SPEAKING)
    val t1 = timestamp

    val s1 = format("(MOUTH) CALLING COMPUTERVOICE.SPEAK (%s) AT (%d)", textToSpeak, System.currentTimeMillis)
    println(s1)

    log.info(format("just before ComputerVoice.speak (%d)", t1))
    ComputerVoice.speak(textToSpeak)

    val s2 = format("(MOUTH) RETURNED FROM COMPUTERVOICE.SPEAK (%s) AT (%d)", textToSpeak, System.currentTimeMillis)
    println(s2)

    val t2 = timestamp
    log.info(format("just after ComputerVoice.speak (%d)", t2))
    log.info(format("time it took to speak (%d)", t2-t1))
    sarah.clearMicrophone
    brain.markThisAsTheLastTimeSarahSpoke
    PluginUtils.sleep(Brain.SHORT_DELAY)
    sarah.setMouthState(Sarah.MOUTH_STATE_NOT_SPEAKING)

    val s3 = format("(MOUTH) LEAVING SPEAK (%s) AT (%d)", textToSpeak, System.currentTimeMillis)
    println(s3)

  }
  

}











