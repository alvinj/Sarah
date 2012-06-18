package com.devdaily.sarah.actors

import akka.actor._
import scala.util.Random
import scala.collection.mutable.ListBuffer
import edu.cmu.sphinx.frontend.util.Microphone
import collection.JavaConversions._
import java.util._
import java.io.IOException
import java.io.File
import javax.script.ScriptEngineManager
import javax.script.ScriptException
import edu.cmu.sphinx.recognizer.Recognizer
import javax.sound.sampled._
import _root_.com.weiglewilczek.slf4s.Logging
import _root_.com.weiglewilczek.slf4s.Logger
import _root_.com.devdaily.sarah._
import _root_.com.devdaily.sarah.plugins._
import scala.io.Source
import scala.collection.mutable.ArrayBuffer

object Brain {

  val SHORT_DELAY = 500
  val REPLY_TO_THANK_YOU_FILE = "thank_you_replies.data"  // when the user says "thank you"
  val REPLY_TO_COMPUTER_FILE  = "computer_replies.data"   // when the user says "computer"
  val SAY_YES_FILE            = "say_yes.data"            // different ways of saying "yes"
    
  // mouth states
  val MOUTH_STATE_SPEAKING     = 1
  val MOUTH_STATE_NOT_SPEAKING = 2
  
  // ear states
  val EARS_STATE_LISTENING       = 100  
  val EARS_STATE_NOT_LISTENING   = 200  
  val EARS_STATE_HEARD_SOMETHING = 300
  
  // sarah's states of being awake of asleep
  val AWARENESS_STATE_AWAKE       = 10
  val AWARENESS_STATE_LIGHT_SLEEP = 20
  val AWARENESS_STATE_DEEP_SLEEP  = 30
  
}

/**
 * The Brain has the responsibility of deciphering whatever input it
 * is given, then taking action on that input, as desired.
 * 
 * The Ears send us whatever they hear. If they hear something we just
 * said, we should ignore it. If Sarah is sleeping, we'll wake up if
 * they heard "wake up".
 * 
 * This actor has the responsibility of running whatever command it is given.
 * If necessary, the Brain will also tell the Mouth what to say, so when
 * running iTunes, the Brain may tell the Mouth to say that it's about to
 * run iTunes, and then it will do whatever it needs to do to run
 * iTunes.
 */
class Brain(sarah: Sarah, 
            microphone: Microphone, 
            recognizer: Recognizer) 
extends Actor
with Logging
{
  
  val log = Logger("Brain")

  // actors we collaborate with
  var ears:ActorRef = _
  var mouth:ActorRef = _
  var brainSomethingWasHeardHelper = context.actorOf(Props(new BrainSomethingWasHeardHelper(sarah, microphone)), name = "BrainSomethingWasHeardHelper")
  
  // states we maintain
  private var mouthIsSpeaking = false
  private var mouthState = Brain.MOUTH_STATE_NOT_SPEAKING
  private var awarenessState = Brain.AWARENESS_STATE_AWAKE
  private var earsState = Brain.EARS_STATE_LISTENING
  
  def receive = {
    
    // initialization
    
    case ConnectToSiblings =>
         handleConnectToSiblingsMessage

    case SetMinimumWaitTimeAfterSpeaking(waitTime) =>
         setMinimumWaitAfterSpeakingTime(waitTime)
         
    // actions

    case message: MessageFromEars =>
         handleMessageFromEars(message)

    case pleaseSay: PleaseSay =>
         handlePleaseSayRequest(pleaseSay)

    case playSoundFileRequest: PlaySoundFileRequest =>
         handlePlaySoundFileRequest(playSoundFileRequest)

    // state

    case MouthIsSpeaking =>
         handleMouthIsSpeakingMessage
         
    case MouthIsFinishedSpeaking =>
         handleMouthIsFinishedSpeakingMessage
         
    case SetAwarenessState(state) =>
         setAwarenessState(state)
         
    case SetEarsState(state) =>
         setEarsState(state)
         
    case SetMouthState(state) =>
         setMouthState(state)

    case GetAwarenessState => sender ! getAwarenessState
    case GetEarsState => sender ! getEarsState
    case GetMouthState => sender ! getMouthState
    case GetInSleepMode => sender ! inSleepMode
    
    // other
    
    case Die =>
         log.info("*** GOT DIE MESSAGE, EXITING ***")
         context.stop(self)

    case unknown => 
         log.info(format("got an unknown request(%s), ignoring it", unknown.toString))
  }
  
  def handleConnectToSiblingsMessage {
    ears = context.actorFor("../Ears")
    mouth = context.actorFor("../Mouth")
  }

  def handleMouthIsSpeakingMessage {
    ears ! StopListeningMessage
    mouthIsSpeaking = true
    setEarsState(Brain.EARS_STATE_NOT_LISTENING)
  }
  
  def handleMouthIsFinishedSpeakingMessage {
    markThisAsTheLastTimeSarahSpoke
    mouthIsSpeaking = false
    setEarsState(Brain.EARS_STATE_LISTENING)
    ears ! StartListeningMessage
  }
  

  /**
   * State Management Code 
   * ---------------------------------------
   */
  
  def getEarsState = earsState
  def getMouthState = mouthState
  def getAwarenessState = awarenessState
  
  def setAwarenessState(state: Int) {
    awarenessState = state
  }
  
  def setEarsState(state: Int) {
    earsState = state
  }
  
  def setMouthState(state: Int) {
    mouthState = state
  }

  // use this method when setting multiple states at the same time
  def setStates(awareness: Int, ears: Int, mouth: Int) {
    awarenessState = awareness
    earsState = ears
    mouthState = mouth
    sarah.setStates(awareness, ears, mouth)  // TODO remove this
  }
  
  def inSleepMode = if (getAwarenessState == Brain.AWARENESS_STATE_AWAKE) false else true
  def sarahIsAwake = if (getAwarenessState == Brain.AWARENESS_STATE_AWAKE) false else true
  def sarahIsInLightSleep = if (getAwarenessState == Brain.AWARENESS_STATE_LIGHT_SLEEP) false else true
  def sarahIsInDeepSleep = if (getAwarenessState == Brain.AWARENESS_STATE_DEEP_SLEEP) false else true

  // use these two to help track when sarah last spoke.
  private var lastTimeSarahSpoke = System.currentTimeMillis 
  def getCurrentTime = System.currentTimeMillis

  /**
   * determine whether we think sarah just finished speaking.
   * if so, return true, else false.
   */
  def sarahJustFinishedSpeaking: Boolean = {
    val timeSinceSarahLastSpoke = getCurrentTime - lastTimeSarahSpoke
    log.info(format("timeSinceSarahLastSpoke = %d", timeSinceSarahLastSpoke))
    if (timeSinceSarahLastSpoke < minimumWaitTime) true else false
  }

  // let sarah set this with her new properties file
  var minimumWaitTime = 1250
  def setMinimumWaitAfterSpeakingTime(t: Int) {
    minimumWaitTime = t
  }

  def markThisAsTheLastTimeSarahSpoke {
    lastTimeSarahSpoke = getCurrentTime
    log.info(format("lastTimeSarahSpoke = %d", getCurrentTime))
  }
  
  def handleMessageFromEars(message: MessageFromEars) {
    log.info("entered handleMessageFromEars")
    if (mouthIsSpeaking) {
      log.info(format("sarah is speaking, ignoring message from ears (%s)", message.textFromUser))
    }
    else if (sarahJustFinishedSpeaking) {
      log.info(format("sarah just spoke, ignoring message from ears (%s)", message.textFromUser))
    } 
    else {
      log.info(format("passing MessageFromEars to brainSomethingWasHeardHelper (%s)", message.textFromUser))
      brainSomethingWasHeardHelper ! SomethingWasHeard(message.textFromUser, inSleepMode, awarenessState)
    }
  }
  
  // all we do now is pass this on to another actor
  private def handlePleaseSayRequest(pleaseSay: PleaseSay) {
    if (inSleepMode) {
      log.info(format("in sleep mode, NOT passing on PleaseSay request (%s)", pleaseSay.textToSay))
    }
    else {
      log.info(format("passing PleaseSay request to brainSomethingWasHeardHelper (%s)", pleaseSay.textToSay))
      //brainPleaseSayHelper ! SomethingWasHeard(pleaseSay.textToSay, inSleepMode, awarenessState)
      brainSomethingWasHeardHelper ! SomethingWasHeard(pleaseSay.textToSay, inSleepMode, awarenessState)
    }
  }

  private def handlePlaySoundFileRequest(playSoundFileRequest: PlaySoundFileRequest) {
    if (inSleepMode) {
      log.info(format("in sleep mode, NOT passing on PlaySoundFile request (%s)", playSoundFileRequest.soundFile))
    }
    else {
      log.info(format("passing SoundFileRequest to Mouth (%s)", playSoundFileRequest.soundFile))
      brainSomethingWasHeardHelper ! playSoundFileRequest
    }
  }

  
}







