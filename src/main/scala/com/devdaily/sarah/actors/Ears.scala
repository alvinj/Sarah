package com.devdaily.sarah.actors

import scala.actors._
import edu.cmu.sphinx.frontend.util.Microphone
import edu.cmu.sphinx.recognizer.Recognizer
import java.util.Date
import com.weiglewilczek.slf4s.Logging
import com.weiglewilczek.slf4s.Logger
import com.devdaily.sarah.Sarah

/**
 * This actor has the responsibility of listening to whatever the human says,
 * and then transmitting that information to the brain.
 * 
 * In this design, the ears have the responsibility of deciphering what the human
 * said, and then passing that text to the brain.
 * 
 * The ears don't care if Sarah is listening, that's not our problem,
 * the brain can deal with that.
 */
class Ears(sarah: Sarah,
           microphone:Microphone, 
           recognizer:Recognizer, 
           brain: Brain) 
extends Actor 
with Logging
{

  val log = Logger("Ears")

  def act() {
    while (true) {
      val whatIThinkThePersonSaid = recognizer.recognize.getBestFinalResultNoFiller
      if (whatIThinkThePersonSaid.trim != "") {
        log.info(format("heard this (%s); sending msg to brain", whatIThinkThePersonSaid))
        brain ! MessageFromEars(whatIThinkThePersonSaid)
      } else {
        log.info("heard a noise, but couldn't interpret it")
      }
    }
  
  }  
  
}

