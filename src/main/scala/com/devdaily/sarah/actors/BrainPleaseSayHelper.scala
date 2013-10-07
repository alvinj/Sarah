package com.devdaily.sarah.actors

import akka.actor._
import com.weiglewilczek.slf4s.Logging
import com.weiglewilczek.slf4s.Logger
import com.devdaily.sarah.plugins.PleaseSay

/**
 * A class to offload "PleaseSay" requests from the Brain.
 */
class BrainPleaseSayHelper(mouth: ActorRef)
extends Actor
with Logging
{

  val log = Logger("BrainPleaseSayHelper")

  def receive = {
//    case pleaseSay: PleaseSay =>
//         log.info(format("got a please-say request (%s) at (%d)", pleaseSay.textToSay, System.currentTimeMillis))
//         handlePleaseSayRequest(pleaseSay)

    case unknown => 
         log.info(format("got an unknown request(%s), ignoring it", unknown.toString))
  }
  
}

