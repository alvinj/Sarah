package com.devdaily.sarah.actors

import scala.actors._
import edu.cmu.sphinx.frontend.util.Microphone
import java.util._
import collection.JavaConversions._
import java.io.IOException
import java.io.File
import javax.script.ScriptEngineManager
import javax.script.ScriptException
import edu.cmu.sphinx.recognizer.Recognizer
import com.devdaily.sarah._

// working on microphone
import javax.sound.sampled._

/**
 * This actor has the responsibility of running whatever command it is given.
 * If necessary, the Brain will also tell the Mouth what to say, so when
 * running iTunes, the Brain may tell the Mouth to say that it's about to
 * run iTunes, and then it will do whatever it needs to do to run
 * iTunes.
 */
class Brain(sarah: Sarah, microphone:Microphone, recognizer:Recognizer, var earBrainIntermediary: EarBrainIntermediary) extends Actor {

  val randomizer = new Random(56)
  val currentDirectory = System.getProperty("user.dir")
  var inSleepMode = false
  val SLEEP_AFTER_SPEAKING            = 1500
  val SLEEP_AFTER_APPLESCRIPT_COMMAND = 1500
  val SLEEP_SHORT_PAUSE               = 3000
  
  // map(sentence, appleScriptKey)
  var phraseCommandMapFiles:Array[String] = null
  var allPossibleSentences:List[String] = null
  
  //val greetStrings = new Array[String](3)
  var commandFiles:Array[String] = null
  
  // these need to be initialized
  val allVoiceCommands = new ArrayList[VoiceCommand]
  var phraseToCommandMap = new HashMap[String,String]

  def act() {
    loop {
      println("\n\n" + "+-------------------------------+")
      println(         "|       THE BRAIN IS READY      |")
      println(         "+-------------------------------+" + "\n")
      react {
        case whatPersonSaid: String =>  
             println("(Brain) about to handle voice command: \"" + whatPersonSaid + "\"")
             handleVoiceCommand(whatPersonSaid)
             shortPause
             println("(Brain) telling ears to listen again")
             sendMessageToIntermediary(MessageFromBrain("START LISTENING"))
        case pleaseSay: PleaseSay => 
             println("(Brain) got a PleaseSay request: \"" + pleaseSay.textToSay + "\"")
             println("(Brain) telling intermediary to stop listening")
             sendMessageToIntermediary(MessageFromBrain("STOP LISTENING"))
             speak(pleaseSay.textToSay)
             shortPause
             println("(Brain) telling intermediary to start listening")
             sendMessageToIntermediary(MessageFromBrain("START LISTENING"))
        case Die =>
             println("Brain got Die message")
             exit
        case unknown => 
             println("(Brain) got an unknown request, ignoring it.")
      }
    }
  }

  def sendMessageToIntermediary(message: MessageFromBrain) {
    earBrainIntermediary ! message
    //shortPause
  }
  
  def shortPause {
    Utils.sleep(SLEEP_SHORT_PAUSE)
    // TODO this isn't the right place for this, just testing
    microphone.clear
  }
  
  def turnMicOff() {
//    println("(Brain) Turning the Mic OFF" + System.currentTimeMillis())
    //microphone.stopRecording()
    //val info = new DataLine.Info(TargetDataLine.asInstanceOf[TargetDataLine], format)
//    val info = new DataLine.Info(classOf[TargetDataLine], null)
//    if (AudioSystem.isLineSupported(Port.Info.MICROPHONE)) 
//    {
//      try {
//        val line = AudioSystem.getLine(Port.Info.MICROPHONE).asInstanceOf[Port]
//        line.close
//      } catch {
//        case e: LineUnavailableException => println("Tried to close mic, exception happened:")
//                e.printStackTrace()
//      }
//    }
  }
  
  def turnMicOn() {
    //microphone.startRecording()
//    println("(Brain) Turning the Mic ON: " + System.currentTimeMillis())
//    if (AudioSystem.isLineSupported(Port.Info.MICROPHONE)) 
//    {
//      try {
//        val line = AudioSystem.getLine(Port.Info.MICROPHONE).asInstanceOf[Port]
//        line.open
//      } catch {
//        case e: LineUnavailableException => println("Tried to close mic, exception happened:")
//                e.printStackTrace()
//      }
//    }
  }
  
  
  /**
   * A function to speak a string of text to the user.
   * This function replaces the former Mouth class.
   */
  def speak(textToSpeak: String) {
    println("(Brain) ENTERED Brain::speak FUNCTION")
    println("(Brain)    speak: Sarah is about to say: " + textToSpeak)
    turnMicOff()
    ComputerVoice.speak(textToSpeak)
    println("(Brain)    speak: after ComputerVoice.speak, about to sleep")
    Utils.sleep(SLEEP_AFTER_SPEAKING)
    println("(Brain)    speak: after sleep pause")
    turnMicOn()
    microphone.clear
    println("(Brain) LEAVING Brain::speak FUNCTION")
  }
  
  /**
   * Run the AppleScript command encapsulated in the AppleScriptCommand object.
   * (This is currently just a wrapper around a string.)
   */
  def runAppleScriptCommand(command: String) {
    println("(Brain) ENTERED Brain::runAppleScriptCommand FUNCTION")
    turnMicOff()
    val scriptEngineManager = new ScriptEngineManager
    val appleScriptEngine = scriptEngineManager.getEngineByName("AppleScript")
    try {
      // TODO working here; "say" command may be executed in a script
      println("(Brain)    calling appleScriptEngine.eval(command)")
      appleScriptEngine.eval(command)
      Utils.sleep(SLEEP_AFTER_APPLESCRIPT_COMMAND)
      microphone.clear
    } catch {
      case e: ScriptException => e.printStackTrace
    }
    turnMicOn()
    println("(Brain) LEAVING Brain::runAppleScriptCommand FUNCTION")
  }

  // handle the text the computer thinks the user said
  def handleVoiceCommand(whatTheComputerThinksISaid:String):Unit = {

    if (whatTheComputerThinksISaid==null || whatTheComputerThinksISaid.trim().equals("")) return
    val textTheUserSaid = whatTheComputerThinksISaid.toLowerCase()
    // re-load these to let the user change commands while we run
    loadAllUserConfigurationFilesOrDie

    if (handleSpecialVoiceCommands(textTheUserSaid)) {
      println("(Brain) Handled a special voice command, returning.")
      return
    }

    // if the command phrase is in the map, do some work
    if (phraseToCommandMap.containsKey(textTheUserSaid)) {
      // handle whatever the user said
      println("(Brain) handleVoiceCommand, found your phrase in the map: " + textTheUserSaid)
      handleUserDefinedVoiceCommand(textTheUserSaid)
      return
    }
    else {
      System.out.println("Sorry, could not handle command.")
    }
  }

  /**
   * A function to handle "special commands" that are not available to the 
   * user via configuration files, like "go to sleep", "wake up", and
   * "shut down". Returns true if the voice command was handled.
   */
  def handleSpecialVoiceCommands(textTheUserSaid: String):Boolean = {
    if (textTheUserSaid.equals("thanks") || textTheUserSaid.trim().equals("")) { 
        println("(Brain) I think you said 'thanks', I'm going to ignore that.")
        return true
    }

    else if (textTheUserSaid.equals("soy lent green is people") ) {
      speak("Live long, and prosper.")
      sarah.shutdown
      return true
    }

    // special 'go to sleep' action
    else if (!inSleepMode && textTheUserSaid.matches(".*go to sleep.*")) {
      doGoToSleepActions
      return true
    }

    else if (!inSleepMode && textTheUserSaid.matches(".*what can i say.*")) {
      listAvailableVoiceCommands
      return true
    }

    // special 'wake up' action
    else if (inSleepMode && textTheUserSaid.matches(".*wake up.*")) {
      doWakeUpActions
      return true
    }
    
    return false
  }
  
  /**
   * List all the voice command the user can say.
   */
  def listAvailableVoiceCommands() {
    println("(Brain) Entered Brain::listAvailableVoiceCommands")
    loadAllUserConfigurationFilesOrDie
    allVoiceCommands.foreach{ voiceCommand =>
      val voiceCommandKey = voiceCommand.getCommand()
      printf("COMMAND: %s\n", voiceCommandKey)
    }
    printf("COMMAND: %s\n", "go to sleep")
    printf("COMMAND: %s\n", "wake up")
    printf("COMMAND: %s\n", "what can i say")
    printf("COMMAND: %s\n", "soylent green is people")
  }
  
  def handleUserDefinedVoiceCommand(textTheUserSaid: String) {
    println("(Brain) Entered Brain::handleUserDefinedVoiceCommand")
    val commandFileKey = phraseToCommandMap.get(textTheUserSaid)  // ex: COMPUTER, JUST_CHECKING
    println("(Brain) Brain::handleUserDefinedVoiceCommand, commandFileKey = " + commandFileKey)
    // foreach is enabled by importing JavaConversions._ above
    allVoiceCommands.foreach{ voiceCommand =>
      val voiceCommandKey = voiceCommand.getCommand()
      if (voiceCommandKey.equalsIgnoreCase(commandFileKey)) {
        if (!inSleepMode || voiceCommand.worksInSleepMode()) {
          runUserDefinedCommand(voiceCommand)
          printMode()
          return
        }
        else
        {
          printMode()
          System.out.println("In sleep mode, ignoring command.")
          return
        }
      }
    }
  }
  
  
  /**
   * Runs the AppleScript command given by the VoiceCommand.
   * I moved this function here from the AppleScriptUtils class 
   * because of multithreading concerns.
   */
  def runUserDefinedCommand(vc: VoiceCommand) {
    println("(Brain) vc.command:     " + vc.getCommand())
    println("(Brain) vc.applescript: " + vc.getAppleScript())
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
  
  def doGoToSleepActions {
    inSleepMode = true
    speak("Going to sleep")
    printMode
  }

  def doWakeUpActions {
    inSleepMode = false
    speak("I'm awake")
    printMode()
  }

  def printMode() {
    System.out.format ("(Brain) ListeningMode:        %s\n", if (inSleepMode) "QUIET/SLEEP" else "NORMAL")
  }  
  
  def loadAllUserConfigurationFilesOrDie() {
    if (allVoiceCommands != null) allVoiceCommands.clear()
    if (phraseToCommandMap != null) phraseToCommandMap.clear()

    // (appleScriptKey, appleScriptToExecute)
    commandFiles = SarahJavaHelper.getAllFilenames(currentDirectory, "commands")
    if (commandFiles.length == 0) {
      System.err.println("Could not find any command files, aborting.")
      System.exit(1)
    }
    
    loadAllVoiceCommands()
    // load the map of sentences to commands (sentence, appleScriptKey)
    phraseCommandMapFiles = SarahJavaHelper.getAllFilenames(currentDirectory, "c2p")
    if (phraseCommandMapFiles.length == 0) {
      System.err.println("Could not find any phrase command map files, aborting.")
      System.exit(1)
    }

    SarahJavaHelper.loadAllSentenceToCommandMaps(phraseCommandMapFiles, ":", currentDirectory, phraseToCommandMap);
  }

  
  def loadAllVoiceCommands() {
    for (cmdFile <- commandFiles) {
      var canonFilename = currentDirectory + File.separator + cmdFile
      try
      {
        var commands = SarahJavaHelper.getCurrentVoiceCommands(canonFilename, ":")
        allVoiceCommands.addAll(commands)
      }
      catch
      {
        case e:IOException => println("Error trying to load voice commands.")
                              e.printStackTrace()
      }
    }
  }

  
  
}







