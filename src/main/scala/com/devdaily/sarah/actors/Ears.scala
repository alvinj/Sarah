package com.devdaily.sarah.actors

import scala.actors._
import edu.cmu.sphinx.frontend.util.Microphone
import edu.cmu.sphinx.recognizer.Recognizer;
import java.util.Date
import com.weiglewilczek.slf4s.Logging
import com.weiglewilczek.slf4s.Logger

/**
 * This actor has the responsibility of listening to whatever the human says,
 * and then transmitting that information to the brain.
 * 
 * In this design, the ears have the responsibility of deciphering what the human
 * said, and then passing that text to the brain.
 */
class Ears(microphone:Microphone, recognizer:Recognizer, var earBrainIntermediary: EarBrainIntermediary) 
extends Actor 
with Logging
{

  val log = Logger("Brain")

  def act() {
    log.info("(Ears) Entered Ears::act")
    while (true) {
      val whatIThinkThePersonSaid = recognizer.recognize().getBestFinalResultNoFiller()
      if (whatIThinkThePersonSaid.trim() != "") {
        log.info("(Ears) ears heard this: " + whatIThinkThePersonSaid)
        earBrainIntermediary ! MessageFromEars(whatIThinkThePersonSaid)
      } else {
        log.info("(Ears) ears heard a noise, but couldn't interpret it")
      }
    }
  
  }  
  
}

