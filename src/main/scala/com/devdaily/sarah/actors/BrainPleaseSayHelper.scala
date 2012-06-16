package com.devdaily.sarah.actors

import akka.actor._
import com.weiglewilczek.slf4s.Logging
import com.weiglewilczek.slf4s.Logger
import com.devdaily.sarah.plugins.PleaseSay

/**
 * A class to offload "PleaseSay" requests from the Brain.
 */
class BrainPleaseSayHelper(mouth: Mouth)
extends Actor
with Logging
{

  val log = Logger("BrainPleaseSayHelper")

  def receive = {
    case pleaseSay: PleaseSay =>
         val s = format("GOT PLEASE-SAY REQUEST (%s) AT (%d)", pleaseSay.textToSay, System.currentTimeMillis)
         log.info(s)
         handlePleaseSayRequest(pleaseSay)
    case unknown => 
         log.info(format("got an unknown request(%s), ignoring it", unknown.toString))
  }
  
  private def handlePleaseSayRequest(pleaseSay: PleaseSay) {
    val s = format("SENDING MSG (%s) TO MOUTH AT (%d)", pleaseSay.textToSay, System.currentTimeMillis)
    log.info(s)
    // TODO add this back in
    //mouth ! SpeakMessageFromBrain(pleaseSay.textToSay)
  }  
  
}

