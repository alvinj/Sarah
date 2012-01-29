package com.devdaily.sarah.actors

import scala.actors._
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
            recognizer: Recognizer, 
            var mouth: Mouth) 
extends Actor
with Logging
{
  
  val REPLY_TO_THANK_YOU_FILE = "thank_you_replies.data"
  val REPLY_TO_COMPUTER_FILE  = "computer_replies.data"
  val SAY_YES_FILE            = "say_yes.data"

  /**
   * SarahPlugin support
   * -------------------
   */
  private val pluginModules = new ListBuffer[SarahPlugin]
  
  private def addPluginModule(plugin: SarahPlugin) {
    log.info("adding pluginModule: " + plugin.toString())
    pluginModules += plugin
  }

  def inSleepMode: Boolean = if (sarah.getState == Sarah.SARAH_IS_SLEEPING) true else false

  // use these two to help track when sarah last spoke.
  // the mouth needs access to these.
  private var lastTimeSarahSpoke = System.currentTimeMillis 
  def getCurrentTime = System.currentTimeMillis
  var delayAfterSpeaking = 500
  
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

  // TODO yes, i know this code is a mess and needs to be refactored. 
  //      there are a lot of kludges to handle the problem
  //      where i can't turn the microphone off.
  def act() {
    log.info("in act()")
    loop {
      log.info("")
      log.info("********  THE BRAIN IS READY  ********")
      log.info("")
      react {
        case message: MessageFromEars =>
             if (sarah.getState == Sarah.SARAH_IS_SPEAKING) 
             {
               log.info(format("sarah is speaking, ignoring message from ears (%s)", message.textFromUser))
             } 
             else if (sarahJustFinishedSpeaking)
             {
               log.info(format("sarah just spoke, ignoring message from ears (%s)", message.textFromUser))
             } 
             else
             {
               // conditions are okay, evaluate what the ears sent us
               handleSomethingWeHeard(message.textFromUser)
             }
        case pleaseSay: PleaseSay =>
             handlePleaseSayRequest(pleaseSay)
        case plugin: SarahPlugin =>
             addPluginModule(plugin)
        case string: String =>
             log.info(format("got String message (%s), ignoring it", string))
        case Die =>
             log.info("got Die message, exiting")
             exit
        case unknown => 
             log.info(format("got an unknown request(%s), ignoring it", unknown.toString))
      }
    }
  }
  
  /**
   * determine whether we think sarah just finished speaking.
   * if so, return true, else false.
   */
  def sarahJustFinishedSpeaking: Boolean = {
    val timeSinceSarahLastSpoke = getCurrentTime - lastTimeSarahSpoke
    log.info(format("timeSinceSarahLastSpoke = %d", timeSinceSarahLastSpoke))
    val minimumWaitTime = 1250
    if (timeSinceSarahLastSpoke < minimumWaitTime)
      return true
    else
      return false
  }

  /**
   * The main handler for PleaseSay requests, which typically come from plugins,
   * and specifically not from the user.
   * 
   * Note: Plugins should be allowed to override Sarah's sleepMode, such as in
   * the case of an alarm clock or reminder.
   */
  private def handlePleaseSayRequest(pleaseSay: PleaseSay) {
    if (inSleepMode) {
      // TODO check to see if the plugin has permission to speak while
      // sarah is in sleep mode
      log.info("got a PleaseSay request while sarah is in sleep mode (ignoring)")
      log.info("PleaseSay request: " + pleaseSay.textToSay)
    }
    else {
      log.info(format("handling PleaseSay request (%s)", pleaseSay.textToSay))
      // it's not right for us to set state here, so we don't. 
      // we don't know when sarah will actually say this, because requests to speak
      // may be backed up.
      speak(pleaseSay.textToSay)
    }
  }

  /**
   * The main handler for when the ears hear something and send it to us.
   */
  private def handleSomethingWeHeard(whatWeHeard: String) {
    if (inSleepMode)
    {
      // if we're sleeping, the only request we respond to is "wake up"
      sarah.setCurrentState(Sarah.SARAH_IS_SLEEPING_BUT_HEARD_SOMETHING)
      handleWakeUpRequestIfReceived(whatWeHeard)
    }
    else
    {
      // TODO review this code (too tired right now)
      // not in sleep mode, so handle whatever we heard
      sarah.setCurrentState(Sarah.SARAH_IS_NOT_LISTENING)
      handleVoiceCommand(whatWeHeard)
      // even if sarah goes to sleep, it needs to listen for commands, in particular
      // the 'wake up' command
      sarah.setCurrentState(Sarah.SARAH_IS_LISTENING)
      // handling the voice command may have flipped us into sleep mode
      updateUiBasedOnCurrentSleepMode
     }
  }
  
  private def updateUiBasedOnCurrentSleepMode {
    if (inSleepMode)
      sarah.setCurrentState(Sarah.SARAH_IS_NOT_LISTENING)
    else
      sarah.setCurrentState(Sarah.SARAH_IS_LISTENING)
  }

  /**
   * handle the wake-up request if it was received.
   * otherwise, go back to sleep.
   */
  private def handleWakeUpRequestIfReceived(whatTheComputerThinksISaid: String) {
    if (whatTheComputerThinksISaid.matches(".*wake up.*")) {
      doWakeUpActions
    } else {
      // didn't get a 'wake up' request, so make sure this is set back to sleeping
      sarah.setCurrentState(Sarah.SARAH_IS_SLEEPING)
    }
  }
  
  // TODO this is probably a bug here; probably need to separate sleeping and listening concepts
  private def doGoToSleepActions {
    sarah.setCurrentState(Sarah.SARAH_IS_SLEEPING)
    speak("Going to sleep")
    // always need to tell intermediary to start listening after we've finished speaking
    sarah.setCurrentState(Sarah.SARAH_IS_LISTENING)
    printMode
  }

  private def doWakeUpActions {
    speak("Okay, I'm awake")
    sarah.setCurrentState(Sarah.SARAH_IS_LISTENING)
    printMode()
  }


  /**
   * "speak" functionality has been moved to the mouth,
   * just passing it through here
   * -------------------------------------------------
   */
  private def speak(textToSpeak: String) {
    mouth ! SpeakMessageFromBrain(textToSpeak)
  }
  
  /**
   * Run the AppleScript command encapsulated in the AppleScriptCommand object.
   * (This is currently just a wrapper around a string.)
   * 
   * TODO speaking can happen through here also, which is a problem.
   * 
   */
  private def runAppleScriptCommand(command: String) {
    log.info("ENTERED runAppleScriptCommand FUNCTION")
    // sarah is probably going to speak here
    sarah.setCurrentState(Sarah.SARAH_IS_SPEAKING)
    val previousState = sarah.getState
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
      sarah.setCurrentState(previousState)
      markThisAsTheLastTimeSarahSpoke
      log.info("finished appleScriptEngine.eval(command)")
      log.info(format("  timestamp = %d", getCurrentTime))
      // TODO should be able to get rid of this at some point
      PluginUtils.sleep(delayAfterSpeaking)
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
    val textToSay = PluginUtils.getRandomStringFromFile(sarah.getDataFileDirectory + "/" + REPLY_TO_THANK_YOU_FILE)
    speak(textToSay)
  }
  
  private def replyToUserSayingComputer {
    val textToSay = PluginUtils.getRandomStringFromFile(sarah.getDataFileDirectory + "/" + SAY_YES_FILE)
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







