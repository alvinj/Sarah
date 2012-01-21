package com.devdaily.sarah.actors

import scala.actors._
import com.weiglewilczek.slf4s.Logging
import com.weiglewilczek.slf4s.Logger

/**
 * Mostly because of the Sphinx framework and microphone handling,
 * I need an intermediary that works between the Ear and the Brain.
 */
class EarBrainIntermediary(brain: Brain, ears: Ears) 
extends Actor 
with Logging
{

  // TODO is this approach needed any more? if not, delete.
  var listeningToUser = true

  val log = Logger("EarBrainIntermediary")

  def act() {
    loop {
      log.info("(Intermediary) in loop")
      receive {
        case message: MessageFromEars =>
          log.info("(Intermediary) message received from ears, thinking about it. msg = " + message)
          if (message.textFromUser == null || message.textFromUser.trim == "") {
            log.info("(Intermediary) Got an empty message from the ears, not going to send it to the brain.")
            listeningToUser = true
          }
          else if (message.textFromUser == "please listen") {
            // TODO this approach may be dead now
            log.info("(Intermediary) I think user said 'please listen'.")
            listeningToUser = true
          }
          else if (listeningToUser) {
            listeningToUser = false
            log.info("(Intermediary) sending message to brain")
            log.info("(Intermediary) no longer listening to user")
            brain ! message.textFromUser
          }
          else {
            log.info("(Intermediary) *IGNORING USER*, NOT sending this text to brain: " + message.textFromUser)
          }
        //
        // TODO replace these strings with constants
        //
        case messageFromBrain: MessageFromBrain =>
          if (messageFromBrain.message == "START LISTENING") {
            log.info("(Intermediary) Brain said to START listening to user.")
            listeningToUser = true
          } else {
            log.info("(Intermediary) Brain said to STOP listening to user.")
            listeningToUser = false
          }
        case Die => 
          log.info("EarBrainIntermediary got Die message")
          exit
        case unknown => 
          log.info("(Intermediary) Got an unknown message, doing nothing.")
      }
    }
  }

  
}

