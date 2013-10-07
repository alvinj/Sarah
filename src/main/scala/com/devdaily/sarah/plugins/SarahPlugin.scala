package com.devdaily.sarah.plugins

import akka.actor.ActorRef

/**
 * TODO This API is TBD
 */
trait SarahPlugin {
  
  var brain: ActorRef = null

  def connectToBrain(brain: ActorRef) {
    this.brain = brain
  }
  
  def startPlugin 

  // phrases the plugin can handle
  def textPhrasesICanHandle: List[String]

  // callback to tell the plugin to handle the given phrase.
  // returns true if the phrase was handled.
  def handlePhrase(phrase: String): Boolean

  def setPluginDirectory(dir: String) {
    // do nothing by default
  }
  
  
  
}