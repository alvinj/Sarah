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
    
}

case class MinimumWaitTimeAfterSpeaking(waitTime: Int)

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
            recognizer: Recognizer, 
            var mouth: Mouth) 
extends akka.actor.Actor
with Logging
{
  
  // this is set later, when the Mouth reference is set
  var brainPleaseSayHelper:BrainPleaseSayHelper = null

  /**
   * SarahPlugin support
   * -------------------
   * 
   * Is there a better way to get these states out of the brain? They're written like this
   * so callers can get an immediate idea of Sarah's state, but this violates the actor model.
   * The problem is that if the caller has to wait to get the state by using the actor
   * mailbox, the state may have changed while it waited for the response. The solution may be
   * that plugins should not make these calls; they should just do what they're written to
   * do, and send the information via the mailbox, but that seems wasteful, such as at
   * night when Sarah should just be sleeping.
   * 
   */

  // @deprecated
  def inSleepMode: Boolean = {
    if (sarah.getAwarenessState == Sarah.AWARENESS_STATE_AWAKE)
      return false
    else
      return true
  }
  
  def sarahIsAwake: Boolean = {
    if (sarah.getAwarenessState == Sarah.AWARENESS_STATE_AWAKE)
      return false
    else
      return true
  }
  
  def sarahIsInLightSleep: Boolean = {
    if (sarah.getAwarenessState == Sarah.AWARENESS_STATE_LIGHT_SLEEP)
      return false
    else
      return true
  }
  
  def sarahIsInDeepSleep: Boolean = {
    if (sarah.getAwarenessState == Sarah.AWARENESS_STATE_DEEP_SLEEP)
      return false
    else
      return true
  }

  // need to use a method like this b/c all the actors won't have references
  // when they're first created
  def setMouth(m: Mouth) {
    this.mouth = m   
    // trying to offload functionality to other actors so the brain
    // can respond faster
    
    // TODO this is an actor, change the way it is created
    brainPleaseSayHelper = new BrainPleaseSayHelper(mouth)
  }
  

  // use these two to help track when sarah last spoke.
  // the mouth needs access to these.
  private var lastTimeSarahSpoke = System.currentTimeMillis 
  def getCurrentTime = System.currentTimeMillis

  // let sarah set this with her new properties file
  var minimumWaitTime = 1250
  def setMinimumWaitAfterSpeakingTime(t: Int) {
    minimumWaitTime = t
  }

  def markThisAsTheLastTimeSarahSpoke {
    lastTimeSarahSpoke = getCurrentTime
    log.info(format("lastTimeSarahSpoke = %d", getCurrentTime))
  }
  
  //def getLastTimeSarahSpoke = lastTimeSarahSpoke


  // map(sentence, appleScriptKey)
  var phraseCommandMapFiles:Array[String] = null
  var allPossibleSentences:List[String] = null
  
  //val greetStrings = new Array[String](3)
  var commandFiles:Array[String] = null
  
  // these need to be initialized
  val allVoiceCommands = new ArrayList[VoiceCommand]
  var phraseToCommandMap = new HashMap[String,String]
  
  val log = Logger("Brain")

  def receive = {
    case MinimumWaitTimeAfterSpeaking(waitTime) =>
         setMinimumWaitAfterSpeakingTime(waitTime)
    case message: MessageFromEars =>
         if (sarah.getMouthState == Sarah.MOUTH_STATE_SPEAKING) {
           log.info(format("sarah is speaking, ignoring message from ears (%s)", message.textFromUser))
         } 
         else if (sarahJustFinishedSpeaking) {
           log.info(format("sarah just spoke, ignoring message from ears (%s)", message.textFromUser))
         } 
         else {
           // conditions are okay, evaluate what the ears sent us
           handleSomethingWeHeard(message.textFromUser)
         }
    case pleaseSay: PleaseSay =>
         log.info(format("got a 'PleaseSay' request (%s) at (%d)", pleaseSay.textToSay, System.currentTimeMillis))
         handlePleaseSayRequest(pleaseSay)
    case playSoundFileRequest: PlaySoundFileRequest =>
         log.info(format("got PlaySoundFile request (%s) at (%d)", playSoundFileRequest.soundFile, System.currentTimeMillis))
         handlePlaySoundFileRequest(playSoundFileRequest)
    case string: String =>
         log.info(format("got String message (%s), ignoring it", string))
    case Die =>
         log.info("*** GOT DIE MESSAGE, EXITING ***")
         exit
    case unknown => 
         log.info(format("got an unknown request(%s), ignoring it", unknown.toString))
  } // end receive
  
  override def finalize {
    log.error("*** BRAIN DIED OR GOT FINALIZE MESSAGE ***")
  }
  
  /**
   * determine whether we think sarah just finished speaking.
   * if so, return true, else false.
   */
  def sarahJustFinishedSpeaking: Boolean = {
    val timeSinceSarahLastSpoke = getCurrentTime - lastTimeSarahSpoke
    log.info(format("timeSinceSarahLastSpoke = %d", timeSinceSarahLastSpoke))
    if (timeSinceSarahLastSpoke < minimumWaitTime)
      return true
    else
      return false
  }

  // all we do now is pass this on to another actor
  private def handlePleaseSayRequest(pleaseSay: PleaseSay) {
    try {
      if (inSleepMode) {
        log.info(format("in sleep mode, NOT passing on PleaseSay request (%s)", pleaseSay.textToSay))
      }
      else {
        log.info(format("passing PleaseSay request to BrainPleaseSayHelper (%s)", pleaseSay.textToSay))
        // TODO add this back in
        //brainPleaseSayHelper ! pleaseSay
      }
    } catch {
      case e: Throwable => log.error("EXCEPTION in handlePleaseSayRequest")
                           e.printStackTrace
    }
  }

  // TODO duplication with handlePleaseSayRequest, refactor
  private def handlePlaySoundFileRequest(playSoundFileRequest: PlaySoundFileRequest) {
    try {
      if (inSleepMode) {
        log.info(format("in sleep mode, NOT passing on PlaySoundFile request (%s)", playSoundFileRequest.soundFile))
      }
      else {
        log.info(format("passing PleaseSay request to BrainPleaseSayHelper (%s)", playSoundFileRequest.soundFile))
        // TODO add this back in
        //mouth ! playSoundFileRequest
      }
    } catch {
      case e: Throwable => log.error("EXCEPTION in handlePlaySoundFileRequest")
                           e.printStackTrace
    }
  }


  /**
   * The main handler for when the ears hear something and send it to us.
   */
  private def handleSomethingWeHeard(whatWeHeard: String) {
    try {
      if (inSleepMode)
      {
        // if we're sleeping, the only request we respond to is "wake up"
        sarah.setEarsState(Sarah.EARS_STATE_HEARD_SOMETHING)
        handleWakeUpRequestIfReceived(whatWeHeard)
      }
      else
      {
        // TODO review this code (too tired right now)
        // not in sleep mode, so handle whatever we heard
        sarah.setEarsState(Sarah.EARS_STATE_NOT_LISTENING)
        handleVoiceCommand(whatWeHeard)
        sarah.setEarsState(Sarah.EARS_STATE_LISTENING)
       }
    } catch {
      case e: Throwable => log.error("EXCEPTION in handleSomethingWeHeard")
                           e.printStackTrace
    }
  }
  
  /**
   * handle the wake-up request if it was received.
   * otherwise, go back to sleep.
   */
  private def handleWakeUpRequestIfReceived(whatTheComputerThinksISaid: String) {
    val prevSleepState = sarah.getAwarenessState
    if (whatTheComputerThinksISaid.matches(".*wake up.*")) {
      doWakeUpActions
    } else {
      sarah.setEarsState(Sarah.EARS_STATE_LISTENING)
      sarah.setAwarenessState(prevSleepState)
    }
  }
  
  // TODO this is probably a bug here; probably need to separate sleeping and listening concepts
  private def doGoToSleepActions {
    speak("Going to sleep")
    // always need to tell intermediary to start listening after we've finished speaking
    sarah.setStates(Sarah.AWARENESS_STATE_LIGHT_SLEEP, Sarah.EARS_STATE_LISTENING, Sarah.MOUTH_STATE_NOT_SPEAKING)
    printMode
  }

  private def doWakeUpActions {
    speak("I'm awake now.")
    sarah.setStates(Sarah.AWARENESS_STATE_AWAKE, Sarah.EARS_STATE_LISTENING, Sarah.MOUTH_STATE_NOT_SPEAKING)
  }

  /**
   * "speak" functionality has been moved to the mouth,
   * just passing it through here
   * -------------------------------------------------
   */
  private def speak(textToSpeak: String) {
    println(format("(BRAIN) SENDING MSG (%s) TO MOUTH AT (%d)", textToSpeak, System.currentTimeMillis))
    // TODO add this back in
    //mouth ! SpeakMessageFromBrain(textToSpeak)
  }
  
  /**
   * Run the AppleScript command encapsulated in the AppleScriptCommand object.
   * (This is currently just a wrapper around a string.)
   * 
   * TODO speaking can happen through here also, which is a problem.
   * 
   */
  private def runAppleScriptCommand(command: String) {
    // TODO handle the sarah awareness states properly
    log.info("ENTERED runAppleScriptCommand FUNCTION")
    // sarah is probably going to speak here
    val prevAwarenessState = sarah.getAwarenessState
    sarah.setStates(prevAwarenessState, Sarah.EARS_STATE_NOT_LISTENING, Sarah.MOUTH_STATE_SPEAKING)
    try {
      log.info("calling appleScriptEngine.eval(command)")
      log.info(format("  timestamp = %d", getCurrentTime))
      val appleScriptEngine = getAppleScriptEngine
      appleScriptEngine.eval(command)
      microphone.clear
    } catch {
      case e: ScriptException => log.error(e.getMessage)
    } finally {
      // TODO is it correct to set this back to the previous state, or
      //      should i set it to 'listening'?
      sarah.setStates(prevAwarenessState, Sarah.EARS_STATE_LISTENING, Sarah.MOUTH_STATE_NOT_SPEAKING)
      markThisAsTheLastTimeSarahSpoke
      log.info("finished appleScriptEngine.eval(command)")
      log.info(format("  timestamp = %d", getCurrentTime))
      // TODO should be able to get rid of this at some point
      PluginUtils.sleep(Brain.SHORT_DELAY)
      log.info("LEAVING appleScriptEngine.eval(command)")
      log.info(format("  timestamp = %d", getCurrentTime))
    }
  }
  
  def getAppleScriptEngine: javax.script.ScriptEngine = {
    val scriptEngineManager = new ScriptEngineManager
    return scriptEngineManager.getEngineByName("AppleScript")
  }

  // handle the text the computer thinks the user said
  private def handleVoiceCommand(whatTheComputerThinksISaid:String) {

    if (whatTheComputerThinksISaid==null || whatTheComputerThinksISaid.trim().equals("")) {
      return
    }
    val textTheUserSaid = whatTheComputerThinksISaid.toLowerCase
    // re-load these to let the user change commands while we run
    loadAllUserConfigurationFilesOrDie

    if (handleSpecialVoiceCommands(textTheUserSaid)) {
      log.info("Handled a special voice command, returning.")
      return
    }

    // if the command phrase is in the map, do some work
    if (phraseToCommandMap.containsKey(textTheUserSaid)) {
      log.info("phraseToCommandMap contained key, trying to process")
      // handle whatever the user said
      log.info("handleVoiceCommand, found your phrase in the map: " + textTheUserSaid)
      val handled = handleUserDefinedVoiceCommand(textTheUserSaid)
    }
    else {
      // there were no matches; check the plugins registered with sarah
      log.info(format("phraseToCommandMap didn't have key (%s), trying plugins", textTheUserSaid))
      val handled = sarah.tryToHandleTextWithPlugins(textTheUserSaid)
      if (handled) {
        sarah.setStates(sarah.getAwarenessState, Sarah.EARS_STATE_LISTENING, Sarah.MOUTH_STATE_NOT_SPEAKING)
      }
      // this function doesn't care if it was handled (refactor)
    }
  }

  /**
   * A function to handle "special commands" that are not available to the 
   * user via configuration files, like "go to sleep", "wake up", and
   * "shut down". Returns true if the voice command was handled.
   */
  private def handleSpecialVoiceCommands(textTheUserSaid: String):Boolean = {
    
    if (textTheUserSaid.trim().equals("")) { 
      log.info("(Brain) Got a blank string from Ears, ignoring it.")
      return true
    }

    if (textTheUserSaid.trim.equals("thanks") || textTheUserSaid.trim.equals("thank you")) { 
      replyToUserSayingThankYou
      return true
    }

    if (textTheUserSaid.trim.equals("computer")) { 
      replyToUserSayingComputer
      return true
    }

    else if (textTheUserSaid.equals("soy lent green is people") ) {
      speak("Live long, and prosper.")
      PluginUtils.sleep(500)
      sarah.shutdown
      return true
    }

    else if (!inSleepMode && textTheUserSaid.matches(".*go to sleep.*")) {
      doGoToSleepActions
      return true
    }

    else if (!inSleepMode && textTheUserSaid.matches(".*what can i say.*")) {
      listAvailableVoiceCommands
      return true
    }
    
    return false
  }
  
  private def replyToUserSayingThankYou {
    val textToSay = PluginUtils.getRandomStringFromFile(sarah.getDataFileDirectory + "/" + Brain.REPLY_TO_THANK_YOU_FILE)
    speak(textToSay)
  }
  
  private def replyToUserSayingComputer {
    val textToSay = PluginUtils.getRandomStringFromFile(sarah.getDataFileDirectory + "/" + Brain.SAY_YES_FILE)
    speak(textToSay)
  }
  
  /**
   * List all the voice command the user can say.
   */
  private def listAvailableVoiceCommands() {
    // get all voice commands from the config files (populates allVoiceCommands)
    loadAllUserConfigurationFilesOrDie
    
    val voiceCommandsAsStrings = allVoiceCommands.map(_.getCommand)
    val voiceCommandListForSarah = ArrayBuffer[String]()
    voiceCommandListForSarah.addAll(voiceCommandsAsStrings)
    voiceCommandListForSarah += "go to sleep"
    voiceCommandListForSarah += "wake up"
    voiceCommandListForSarah += "what can i say?"
    voiceCommandListForSarah += "soylent green is people"
    voiceCommandListForSarah += "please listen"
      
    sarah.displayAvailableVoiceCommands(voiceCommandListForSarah.toList)
  }
  
  private def handleUserDefinedVoiceCommand(textTheUserSaid: String): Boolean = {
    log.info("Entered Brain::handleUserDefinedVoiceCommand")
    val commandFileKey = phraseToCommandMap.get(textTheUserSaid)  // ex: COMPUTER, JUST_CHECKING
    log.info("Brain::handleUserDefinedVoiceCommand, commandFileKey = " + commandFileKey)
    // foreach is enabled by importing JavaConversions._ above
    allVoiceCommands.foreach{ voiceCommand =>
      val voiceCommandKey = voiceCommand.getCommand()
      if (voiceCommandKey.equalsIgnoreCase(commandFileKey)) {
        if (voiceCommand.getAppleScript==null || voiceCommand.getAppleScript.trim.equals("")) {
          log.info("handleUserDefinedVoiceCommand, appleScript is not defined, passing on it")
          return false
        }
        if (!inSleepMode || voiceCommand.worksInSleepMode()) {
          log.info("running runUserDefinedCommand(voiceCommand)")
          runUserDefinedCommand(voiceCommand)
          printMode
          return true
        }
        else
        {
          printMode
          log.info("In sleep mode, ignoring command.")
          return false
        }
      }
    }
    return false
  }
  
  
  /**
   * Runs the AppleScript command given by the VoiceCommand.
   * I moved this function here from the AppleScriptUtils class 
   * because of multithreading concerns.
   */
  private def runUserDefinedCommand(vc: VoiceCommand) {
    log.info("(Brain) vc.command:     " + vc.getCommand())
    log.info("(Brain) vc.applescript: " + vc.getAppleScript())
    var appleScriptCommand = vc.getAppleScript()
    // split up multiline commands:
    // tell app iTunes to play next track | say "Next track"
    if (appleScriptCommand.indexOf("|") >0)
    {
      val sb = new StringBuilder()
      // create a newline wherever there was a pipe symbol
      val st = new StringTokenizer(appleScriptCommand, "|")
      while (st.hasMoreTokens()) {
        sb.append(st.nextToken().trim())
        if (st.hasMoreTokens()) sb.append("\n")
      }
      appleScriptCommand = sb.toString
    }
    
    runAppleScriptCommand(appleScriptCommand)
  }
  
  private def printMode() {
    System.out.format ("listeningMode: %s\n", if (inSleepMode) "QUIET/SLEEP" else "NORMAL")
  }  
  
  private def loadAllUserConfigurationFilesOrDie() {
    if (allVoiceCommands != null) allVoiceCommands.clear()
    if (phraseToCommandMap != null) phraseToCommandMap.clear()

    // (appleScriptKey, appleScriptToExecute)
    commandFiles = SarahJavaHelper.getAllFilenames(sarah.getDataFileDirectory, "commands")
    if (commandFiles.length == 0) {
      log.error("Could not find any command files, aborting.")
      System.exit(1)
    }
    
    loadAllVoiceCommands()
    // load the map of sentences to commands (sentence, appleScriptKey)
    phraseCommandMapFiles = SarahJavaHelper.getAllFilenames(sarah.getDataFileDirectory, "c2p")
    if (phraseCommandMapFiles.length == 0) {
      log.error("Could not find any phrase command map files, aborting.")
      System.exit(1)
    }

    SarahJavaHelper.loadAllSentenceToCommandMaps(phraseCommandMapFiles, ":", sarah.getDataFileDirectory, phraseToCommandMap);
  }

  
  private def loadAllVoiceCommands() {
    for (cmdFile <- commandFiles) {
      var canonFilename = sarah.getDataFileDirectory + File.separator + cmdFile
      try
      {
        var commands = SarahJavaHelper.getCurrentVoiceCommands(canonFilename, ":")
        allVoiceCommands.addAll(commands)
      }
      catch
      {
        case e:IOException => log.info("Error trying to load voice commands.")
                              e.printStackTrace()
      }
    }
  }

  
  
}







