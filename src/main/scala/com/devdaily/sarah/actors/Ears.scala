package com.devdaily.sarah.actors

import akka.actor._
import akka.event.Logging
import edu.cmu.sphinx.frontend.util.Microphone
import edu.cmu.sphinx.recognizer.Recognizer
import com.devdaily.sarah.Sarah
import com.weiglewilczek.slf4s._

case class StartListeningMessage
case class StopListeningMessage
case class InitEarsMessage

/**
 * This class can receive messages from the EarHelper or the Brain,
 * and sends messages to the Brain.
 * 
 * Messages from the EarHelper are strings, presumably of something
 * a person said. We forward those to the Brain if we are in "listening" mode.
 * 
 * The Brain tells us when we should be listening or not listening by
 * sending messages to us through our priority mailbox.
 * 
 * TODO - Put in some limits so only the Brain and EarHelper classes can send
 * us messages.
 * 
 */
class Ears(sarah: Sarah,
           microphone:Microphone, 
           recognizer:Recognizer) 
extends Actor 
with Logging
{

  val log = Logger("Ears")
  var listeningToUser = false
  val brain:ActorRef = context.actorFor("../Brain")
  
  def receive = {
    case InitEarsMessage =>
         startEarHelper

    case StartListeningMessage => 
         log.debug("EARS got StartListeningMessage")
         listeningToUser = true

    case StopListeningMessage => 
         log.debug("EARS got StopListeningMessage")
         listeningToUser = false

    case MessageFromEarHelper(message) => 
         handleSomethingWeHeard(message)

    case _ => log.info("EARS got a wrongful message") 
  }
  
  def handleSomethingWeHeard(whatWeHeard: String) {
    if (!listeningToUser) return
    log.info("EARS HEARD: " + whatWeHeard + " (sending to brain)")
    brain ! MessageFromEars(whatWeHeard)
  }
  
  def startEarHelper {
    log.info("EARS got InitEarsMessage")
    val earHelper = context.actorOf(Props(new EarHelper(microphone, recognizer)), name = "EarHelper")
    earHelper ! InitEarsMessage
  }
  
  
}








