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
    brain.addPluginModule(this)
  }
  
  def startPlugin 

}