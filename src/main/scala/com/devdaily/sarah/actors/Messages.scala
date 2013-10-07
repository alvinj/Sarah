package com.devdaily.sarah.actors
import akka.actor.ActorRef
import com.devdaily.sarah.plugins.SarahAkkaActorBasedPlugin

// brain messages

case class ConnectToSiblings
case class SetMinimumWaitTimeAfterSpeaking(waitTime: Int)
case class SetAwarenessState(state: Int)
case class SetEarsState(state: Int)
case class SetMouthState(state: Int)
case class SetBrainStates(awareness: Int, ears: Int, mouth: Int) 

abstract class StateRequestMessage
case object GetAwarenessState extends StateRequestMessage
case object GetEarsState extends StateRequestMessage
case object GetMouthState extends StateRequestMessage
case class GetInSleepMode

// brain helper

case class SomethingWasHeard(whatWeHeard: String,
                             inSleepMode: Boolean,
                             awarenessState: Int)


// message to the brain to start the plugin
case class StartThisPlugin(plugin: SarahAkkaActorBasedPlugin)
case class HeresANewPlugin(pluginRef: ActorRef)

// messages to the plugins
case class StartPluginMessage(brain: ActorRef)
case class StopPluginMessage
case class SetPluginDir(canonDir: String)



