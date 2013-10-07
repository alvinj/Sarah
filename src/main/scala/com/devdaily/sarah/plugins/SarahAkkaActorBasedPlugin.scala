package com.devdaily.sarah.plugins

import com.devdaily.sarah.actors.Brain
import akka.actor.Actor

/**
 * This is the main interface for Sarah Akka Actor-based plugins.
 * 
 * If you want to write a plugin for Sarah that uses an Actor, 
 * you'll need to implement this interface.
 */
abstract class SarahAkkaActorBasedPlugin extends Actor with SarahPlugin {

//  override def startPlugin {
//    this.start
//  }
  
}
