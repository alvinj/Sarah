package com.devdaily.sarah.plugins

import com.devdaily.sarah.actors.Brain

/**
 * TODO This API is TBD
 */
trait SarahPlugin {
  
  var brain: Brain = null

//  var pluginName: String = null
//  var mainClass: String = null
  
  /**
   * Make connections in both directions, to and from Sarah's brain.
   */
  def connectToBrain(brain: Brain) {
    this.brain = brain
    // tell the brain to add us to its list of plugins
    brain ! this
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