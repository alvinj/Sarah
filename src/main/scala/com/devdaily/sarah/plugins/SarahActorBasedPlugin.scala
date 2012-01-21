package com.devdaily.sarah.plugins

import scala.actors.Actor
import com.devdaily.sarah.actors.Brain

/**
 * This is the main interface for Sarah Actor-based plugins.
 * 
 * If you want to write a plugin for Sarah that uses an Actor, 
 * you'll need to implement this interface.
 */
trait SarahActorBasedPlugin extends Actor with SarahPlugin {

  var brain: Brain = null
  var moduleName: String = null
  
  /**
   * Make connections in both directions, to and from Sarah's brain.
   */
  def connectToBrain(brain: Brain) {
    this.brain = brain
    brain.addPluginModule(this)
  }
  
}

