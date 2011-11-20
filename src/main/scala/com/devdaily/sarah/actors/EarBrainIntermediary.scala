package com.devdaily.sarah.actors

import scala.actors._

/**
 * Mostly because of the Sphinx framework and microphone handling,
 * I need an intermediary that works between the Ear and the Brain.
 */
class EarBrainIntermediary(brain: Brain, ears: Ears) extends Actor {

  // TODO is this approach needed any more? if not, delete.
  var listeningToUser = true
  
  def act() {
    loop {
      println("(Intermediary) in loop")
      receive {
        case message: MessageFromEars =>
          println("(Intermediary) message received from ears, thinking about it. msg = " + message)
          if (message.textFromUser == null || message.textFromUser.trim == "") {
            println("(Intermediary) Got an empty message from the ears, not going to send it to the brain.")
            listeningToUser = true
          }
          else if (message.textFromUser == "please listen") {
            // TODO this approach may be dead now
            println("(Intermediary) I think user said 'please listen'.")
            listeningToUser = true
          }
          else if (listeningToUser) {
            listeningToUser = false
            println("(Intermediary) sending message to brain")
            println("(Intermediary) no longer listening to user")
            brain ! message.textFromUser
          }
          else {
            println("(Intermediary) NOT listening to user, not sending this text to brain: " + message.textFromUser)
          }
        //
        // TODO replace these strings with constants
        //
        case messageFromBrain: MessageFromBrain =>
          if (messageFromBrain.message == "START LISTENING") {
            println("(Intermediary) Brain said to START listening to user.")
            listeningToUser = true
          } else {
            println("(Intermediary) Brain said to STOP listening to user.")
            listeningToUser = false
          }
        case Die => 
          println("EarBrainIntermediary got Die message")
          exit
        case unknown => 
          println("(Intermediary) Got an unknown message, doing nothing.")
      }
    }
  }

  
}

