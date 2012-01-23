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

  override def startPlugin {
    this.start
  }
  
}

