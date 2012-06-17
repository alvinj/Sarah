package com.devdaily.sarah.actors

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

