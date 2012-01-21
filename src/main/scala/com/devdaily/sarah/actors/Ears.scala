package com.devdaily.sarah.actors

import scala.actors._
import edu.cmu.sphinx.frontend.util.Microphone
import edu.cmu.sphinx.recognizer.Recognizer;
import java.util.Date

/**
 * This actor has the responsibility of listening to whatever the human says,
 * and then transmitting that information to the brain.
 * 
 * In this design, the ears have the responsibility of deciphering what the human
 * said, and then passing that text to the brain.
 */
class Ears(microphone:Microphone, recognizer:Recognizer, var earBrainIntermediary: EarBrainIntermediary) extends Actor {

  def act() {
    println("(Ears) Entered Ears::act")
    while (true) {
      val whatIThinkThePersonSaid = recognizer.recognize().getBestFinalResultNoFiller()
      if (whatIThinkThePersonSaid.trim() != "") {
        println("(Ears) ears heard this: " + whatIThinkThePersonSaid)
        earBrainIntermediary ! MessageFromEars(whatIThinkThePersonSaid)
      } else {
        println("(Ears) ears heard a noise, but couldn't interpret it")
      }
    }
  
  }  
  
}

