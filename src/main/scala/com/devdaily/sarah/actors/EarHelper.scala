package com.devdaily.sarah.actors

import akka.actor._
import edu.cmu.sphinx.frontend.util.Microphone
import edu.cmu.sphinx.recognizer.Recognizer
import java.util.Date
import akka.event.Logging
import com.weiglewilczek.slf4s._

case class MessageFromEarHelper(message: String)

/**
 * This class does the actual listening (presumably to a person)
 * to anything that comes through the microphone, using the Sphinx
 * project recognizer class. When something is heard, we send whatever
 * text we get from Sphinx to the Ears class.
 */
class EarHelper(microphone:Microphone, recognizer:Recognizer) 
extends Actor
with Logging
{
  
  val log = Logger("EarHelper")
  var ears:ActorRef = _
  
  def receive = {
    case InitEarsMessage =>
      ears = context.parent
      listen
  }
  
  def listen {
    while (true) {
      // the loop will pause here while it waits for something else from the user, so
      // it's not a "tight loop"
      val whatIThinkThePersonSaid = recognizer.recognize.getBestFinalResultNoFiller
      if (whatIThinkThePersonSaid.trim != "") {
        ears ! MessageFromEarHelper(whatIThinkThePersonSaid)
      } else {
        //log.info("heard a noise, but couldn't interpret it")
      }
    }
  }

  
  
  
}








