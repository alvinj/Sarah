package com.devdaily.sarah.actors

import akka.actor._
import com.weiglewilczek.slf4s.Logging
import com.weiglewilczek.slf4s.Logger
import com.devdaily.sarah.Sarah
import com.devdaily.sarah.ComputerVoice
import com.devdaily.sarah.plugins.PluginUtils
import com.devdaily.sarah.plugins.PlaySoundFileRequest

case class InitMouthMessage

/**
 * Mouth has the responsibility of speaking whatever it is told to speak,
 * in the order the requests are given. It also sends priority messages to
 * the Brain so the Brain will always know the current Mouth state.
 */
class Mouth(sarah: Sarah) extends akka.actor.Actor with Logging {
  
  val log = Logger("Mouth")
  val brain:ActorRef = context.actorFor("../Brain")
  var mouthHelper:ActorRef = _

  def receive = {

    case InitMouthMessage =>
         startMouthHelper
    
    case message: SpeakMessageFromBrain =>
         mouthHelper ! message

    case playSoundFileRequest: PlaySoundFileRequest =>
         mouthHelper ! playSoundFileRequest

    case MouthIsSpeaking =>
         // get this from our helper, pass it on
         brain ! MouthIsSpeaking
         
    case MouthIsFinishedSpeaking =>
         // get this from our helper, pass it on
         brain ! MouthIsFinishedSpeaking

    case unknown => 
         log.info(format("got an unknown request(%s), ignoring it", unknown.toString))
  }

  def startMouthHelper {
    mouthHelper = context.actorOf(Props(new MouthHelper(sarah)), name = "MouthHelper")
  }

}











