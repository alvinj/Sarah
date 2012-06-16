package com.devdaily.sarah.actors

import akka.actor._
import edu.cmu.sphinx.frontend.util.Microphone
import edu.cmu.sphinx.recognizer.Recognizer
import java.util.Date
import akka.event.Logging
import com.weiglewilczek.slf4s.Logging
import com.weiglewilczek.slf4s.Logger

case class MessageFromEarHelper(message: String)

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
class EarHelper(microphone:Microphone, recognizer:Recognizer) 
extends Actor
with Logging
{
  
  val log = Logger("EarHelper")
  //val log = Logging(context.system, this)
  var ears:ActorRef = _
  
  def receive = {
    case InitEarsMessage =>
      log.info("EarHelper got init message")
      ears = context.parent
      listen
  }
  
  def listen {
    while (true) {
      // the loop will pause here while it waits for something else from the user, so
      // it's not a "tight loop"
      val whatIThinkThePersonSaid = recognizer.recognize.getBestFinalResultNoFiller
      log.info("EarHelper: User said: " + whatIThinkThePersonSaid)
      if (whatIThinkThePersonSaid.trim != "") {
        ears ! MessageFromEarHelper(whatIThinkThePersonSaid)
      } else {
        //log.info("heard a noise, but couldn't interpret it")
      }
    }
  }

  
  
  
}








