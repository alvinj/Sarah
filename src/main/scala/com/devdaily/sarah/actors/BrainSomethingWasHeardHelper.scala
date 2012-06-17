package com.devdaily.sarah.actors

import akka.actor._
import akka.pattern.ask
import akka.dispatch.Await
import akka.util.Timeout
import akka.util.duration._
import com.weiglewilczek.slf4s.Logging
import com.weiglewilczek.slf4s.Logger
import com.devdaily.sarah.plugins.PleaseSay
import collection.JavaConversions._
import java.util.ArrayList
import java.util.HashMap
import com.devdaily.sarah.VoiceCommand
import javax.script.ScriptEngineManager
import com.devdaily.sarah.plugins.PluginUtils
import java.util.StringTokenizer
import com.devdaily.sarah.SarahJavaHelper
import java.io.IOException
import scala.collection.mutable.ArrayBuffer
import javax.script.ScriptException
import edu.cmu.sphinx.frontend.util.Microphone
import com.devdaily.sarah.Sarah
import java.io.File

case class SomethingWasHeard(whatWeHeard: String,
                             inSleepMode: Boolean,
                             awarenessState: Int)

class BrainSomethingWasHeardHelper(sarah: Sarah, microphone: Microphone)
extends Actor
with Logging
{

  val log = Logger("BrainPleaseSayHelper")
  val brain:ActorRef = context.parent
  var mouth:ActorRef = _

  // map(sentence, appleScriptKey)
  var phraseCommandMapFiles:Array[String] = null
  var allPossibleSentences:List[String] = null
  
  //val greetStrings = new Array[String](3)
  var commandFiles:Array[String] = null
  
  // these need to be initialized
  val allVoiceCommands = new ArrayList[VoiceCommand]
  var phraseToCommandMap = new HashMap[String, String]
  
  def receive = {
    case SomethingWasHeard(whatWeHeard, inSleepMode, awarenessState) => 
         handleSomethingWeHeard(whatWeHeard, inSleepMode, awarenessState)

    case unknown => 
         log.info(format("got an unknown request(%s), ignoring it", unknown.toString))
  }
  
  /**
   * The main handler for when the ears hear something and send it to us.
   */
  private def handleSomethingWeHeard(whatWeHeard: String,
                                     inSleepMode: Boolean,
                                     awarenessState: Int) {
    if (inSleepMode)
    {
      // if we're sleeping, the only request we respond to is "wake up"
      brain ! SetEarsState(Brain.EARS_STATE_HEARD_SOMETHING)
      handleWakeUpRequestIfReceived(whatWeHeard, awarenessState)
    }
    else
    {
      // TODO review this code (too tired right now)
      // not in sleep mode, so handle whatever we heard
      brain ! SetEarsState(Brain.EARS_STATE_NOT_LISTENING)
      handleVoiceCommand(whatWeHeard)
      brain ! SetEarsState(Brain.EARS_STATE_LISTENING)
    }
  }
  
  /**
   * handle the wake-up request if it was received.
   * otherwise, go back to sleep.
   */
  private def handleWakeUpRequestIfReceived(whatTheComputerThinksISaid: String,
                                            awarenessState: Int) {
    val prevSleepState = awarenessState
    if (whatTheComputerThinksISaid.matches(".*wake up.*")) {
      doWakeUpActions
    } else {
      brain ! SetEarsState(Brain.EARS_STATE_LISTENING)
    }
  }

  def getAppleScriptEngine: javax.script.ScriptEngine = {
    val scriptEngineManager = new ScriptEngineManager
    return scriptEngineManager.getEngineByName("AppleScript")
  }

  // handle the text the computer thinks the user said
  private def handleVoiceCommand(whatTheComputerThinksISaid: String) {

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
        brain ! SetBrainStates(getAwarenessState, Brain.EARS_STATE_LISTENING, Brain.MOUTH_STATE_NOT_SPEAKING)
      }
      // this function doesn't care if it was handled (refactor)
    }
  }
  
  // TODO there's probably a better way to do this
  def getAwarenessState:Int = {
    implicit val timeout = Timeout(2 seconds)
    val future = brain ? GetAwarenessState
    val result = Await.result(future, timeout.duration).asInstanceOf[Int]
    result
  }

  // TODO there's probably a better way to do this
  def getEarsState:Int = {
    implicit val timeout = Timeout(2 seconds)
    val future = brain ? GetEarsState
    val result = Await.result(future, timeout.duration).asInstanceOf[Int]
    result
  }

  // TODO there's probably a better way to do this
  def inSleepMode:Boolean = {
    implicit val timeout = Timeout(2 seconds)
    val future = brain ? GetInSleepMode
    val result = Await.result(future, timeout.duration).asInstanceOf[Boolean]
    result
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

  /**
   * handle the wake-up request if it was received.
   * otherwise, go back to sleep.
   */
  private def handleWakeUpRequestIfReceived(whatTheComputerThinksISaid: String) {
    val prevSleepState = getAwarenessState
    if (whatTheComputerThinksISaid.matches(".*wake up.*")) {
      doWakeUpActions
    } else {
      brain ! SetEarsState(Brain.EARS_STATE_LISTENING)
      brain ! SetAwarenessState(prevSleepState)
    }
  }
  
  // TODO this is probably a bug here; probably need to separate sleeping and listening concepts
  private def doGoToSleepActions {
    speak("Going to sleep")
    // always need to tell intermediary to start listening after we've finished speaking
    brain ! SetBrainStates(Brain.AWARENESS_STATE_LIGHT_SLEEP, Brain.EARS_STATE_LISTENING, Brain.MOUTH_STATE_NOT_SPEAKING)
    printMode
  }

  private def doWakeUpActions {
    speak("I'm awake now.")
    brain ! SetBrainStates(Brain.AWARENESS_STATE_AWAKE, Brain.EARS_STATE_LISTENING, Brain.MOUTH_STATE_NOT_SPEAKING)
  }

  /**
   * "speak" functionality has been moved to the mouth,
   * just passing it through here
   * -------------------------------------------------
   */
  private def speak(textToSpeak: String) {
    println(format("(BRAIN) SENDING MSG (%s) TO MOUTH AT (%d)", textToSpeak, System.currentTimeMillis))
    if (mouth == null) {
      mouth = context.actorFor("../../Mouth")
    }
    mouth ! SpeakMessageFromBrain(textToSpeak)
  }
  
  // TODO move to Utils
  def getCurrentTime = System.currentTimeMillis

  /**
   * 
   * TODO Get this method out of the Brain.
   * 
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
    val prevAwarenessState = getAwarenessState
    brain ! SetBrainStates(prevAwarenessState, Brain.EARS_STATE_NOT_LISTENING, Brain.MOUTH_STATE_SPEAKING)
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
      brain ! SetBrainStates(prevAwarenessState, Brain.EARS_STATE_LISTENING, Brain.MOUTH_STATE_NOT_SPEAKING)
      brain ! MouthIsFinishedSpeaking
      log.info("finished appleScriptEngine.eval(command)")
      log.info(format("  timestamp = %d", getCurrentTime))
      // TODO should be able to get rid of this at some point
      PluginUtils.sleep(Brain.SHORT_DELAY)
      log.info("LEAVING appleScriptEngine.eval(command)")
      log.info(format("  timestamp = %d", getCurrentTime))
    }
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
  
  private def printMode() {
    System.out.format ("listeningMode: %s\n", if (inSleepMode) "QUIET/SLEEP" else "NORMAL")
  }  
  
  private def replyToUserSayingThankYou {
    val textToSay = PluginUtils.getRandomStringFromFile(sarah.getDataFileDirectory + "/" + Brain.REPLY_TO_THANK_YOU_FILE)
    speak(textToSay)
  }
  
  private def replyToUserSayingComputer {
    val textToSay = PluginUtils.getRandomStringFromFile(sarah.getDataFileDirectory + "/" + Brain.SAY_YES_FILE)
    speak(textToSay)
  }  
  
}
























