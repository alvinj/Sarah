package com.devdaily.sarah.actors

import akka.actor._
import akka.event.Logging
import edu.cmu.sphinx.frontend.util.Microphone
import edu.cmu.sphinx.recognizer.Recognizer

import com.devdaily.sarah.Sarah
import com.weiglewilczek.slf4s.Logging
import com.weiglewilczek.slf4s.Logger

case class StartListeningMessage
case class StopListeningMessage
case class InitEarsMessage

class Ears(sarah: Sarah,
           microphone:Microphone, 
           recognizer:Recognizer) 
extends Actor 
with Logging
{

  val log = Logger("Ears")
  //val log = Logging(context.system, this)
  var listeningToUser = false
  
  def sendMessageToTheBrain(message: String) {
    log.info(format("heard this (%s); sending msg to brain", message))
    
    // TODO add this back in
    //brain ! MessageFromEars(message)
  }
  
  def receive = {
    case InitEarsMessage => 
         startEarHelper

    case StartListeningMessage => 
         log.debug("EARS got StartListeningMessage")
         listeningToUser = true

    case StopListeningMessage => 
         listeningToUser = false

    case MessageFromEarHelper(message) => 
         handleSomethingWeHeard(message)

    case _ => log.info("EARS got a wrongful message") 
  }
  
  def handleSomethingWeHeard(whatWeHeard: String) {
    if (!listeningToUser) return
    // TODO send the message to the Brain
    log.info("EARS HEARD: " + whatWeHeard)
  }
  
  def startEarHelper {
    log.info("EARS got InitEarsMessage")
    val earHelper = context.actorOf(Props(new EarHelper(microphone, recognizer)), name = "EarHelper")
    earHelper ! InitEarsMessage
  }
  
  
}








