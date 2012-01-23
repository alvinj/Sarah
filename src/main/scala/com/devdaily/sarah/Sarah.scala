package com.devdaily.sarah

import java.io._
import java.util._
import _root_.com.devdaily.sarah.agents._
import _root_.com.devdaily.sarah.plugins._
import edu.cmu.sphinx.frontend.util.Microphone
import edu.cmu.sphinx.jsgf.JSGFGrammar
import edu.cmu.sphinx.linguist.dictionary.Word
import edu.cmu.sphinx.linguist.language.grammar.GrammarArc
import edu.cmu.sphinx.linguist.language.grammar.GrammarNode
import edu.cmu.sphinx.recognizer.Recognizer
import edu.cmu.sphinx.result.Result
import edu.cmu.sphinx.util.props.ConfigurationManager
import actors._
import scala.xml._
import collection.Map
import collection.Traversable
import _root_.com.apple.eawt.Application
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.JFrame
import java.awt.Toolkit
import java.awt.Color
import javax.swing.ImageIcon
import com.devdaily.splashscreen.SplashScreen
import scala.collection.mutable.ArrayBuffer
import com.devdaily.sarah.gui.MainFrameHeaderPanel
import java.awt.BorderLayout
import com.devdaily.sarah.gui.MicrophonePanel
import com.devdaily.sarah.gui.LogOutputPanel
import java.util.logging.FileHandler
import java.util.logging.Logger
import com.weiglewilczek.slf4s.Logging
import com.weiglewilczek.slf4s.Logger


/**
 * I'm going through some extra work in this file for two reasons.
 * First, I need to pass the Brain instance a 'this' reference. This
 * is because I'm trying to get System.exit to work.
 * Second, I'm also trying to learn about Scala companion classes
 * and objects.
 */
object Sarah extends Logging {
  
  def main(args: Array[String]) {

    val sarah = new Sarah
    sarah.startRunning
    
  }
  
} // end of object


/**
 * This is the main Sarah class. Along with its companion object, everything starts here.
 * TODO - this class has grown out of control, and needs to be refactored.
 */
class Sarah {

  /**
   * REMEMBER, all code up here is basically the no-args constructor
   * ---------------------------------------------------------------
   */

  val APP_NAME = "SARAH"
  val EXIT_CODE_CANT_START_MIC = 5
  val EXIT_CODE_NOT_RUNNING_ON_MAC = 2
  val INITIAL_FRAME_COLOR = new Color(170, 194, 156)

  // TODO all of these constants probably need to go somewhere else
  val REL_DATA_DIR          = ".sarah/data"
  val REL_LOGFILE_DIR       = ".sarah/logs"
  val REL_PLUGINS_DIR       = ".sarah/plugins"
  val JSGF_FILENAME         = "sarah.config.xml"
  val LOG_FILENAME          = "sarah.log"
  val FILE_PATH_SEPARATOR   = System.getProperty("file.separator")
  val USER_HOME_DIR         = System.getProperty("user.home")
  val CANON_DATA_DIR        = USER_HOME_DIR + FILE_PATH_SEPARATOR + REL_DATA_DIR
  val CANON_LOGFILE_DIR     = USER_HOME_DIR + FILE_PATH_SEPARATOR + REL_LOGFILE_DIR 
  val CANON_LOGFILE         = CANON_LOGFILE_DIR + FILE_PATH_SEPARATOR + LOG_FILENAME 
  val CANON_PLUGINS_DIR     = USER_HOME_DIR + FILE_PATH_SEPARATOR + REL_PLUGINS_DIR
  val CANON_DEBUG_FILENAME  = CANON_LOGFILE_DIR + FILE_PATH_SEPARATOR + LOG_FILENAME
  val SARAH_CONFIG_FILE     = CANON_DATA_DIR + FILE_PATH_SEPARATOR + JSGF_FILENAME
  
  val SPLASH_SCREEN_IMAGE   = "sarah-splash-image.png"

  // TODO get logging going to the sarah.log file
  val log = com.weiglewilczek.slf4s.Logger("Sarah")

  // TODO populate this list of plugins
  var pluginInstances = ArrayBuffer[SarahPlugin]()
  
  val splashImage = new ImageIcon(classOf[com.devdaily.sarah.Sarah].getResource(SPLASH_SCREEN_IMAGE))
  var screen = new SplashScreen(splashImage)
  screen.setLocationRelativeTo(null)
  screen.setProgressMax(100)
  screen.setScreenVisible(true)
  screen.setProgress("Starting SARAH ...", 10)

  screen.setProgress("Finding plugins ...", 15)

  // TODO - merge voice command files
  screen.setProgress("Merging voice commands ...", 25)
  mergeAllVoiceCommandFiles
  
  
  //dieIfNotRunningOnMacOsX

  screen.setProgress("Connecting to microphone ...", 50)
  val cm = new ConfigurationManager(SARAH_CONFIG_FILE)
  val recognizer = cm.lookup("recognizer").asInstanceOf[Recognizer]
  val microphone = cm.lookup("microphone").asInstanceOf[Microphone]
  recognizer.allocate
  startMicRecordingOrDie(recognizer, microphone)

  screen.setProgress("Starting SARAH's brain ...", 75)
  // i'm passing in nulls here because i don't know a better way to make 'brain'
  // and 'ears' available to me in setter methods. these two objects need to know about
  // each other.
  val brain = new Brain(this, microphone, recognizer, null)
  val ears  = new Ears(microphone, recognizer, null)
  val earBrainIntermediary = new EarBrainIntermediary(brain, ears)
  
  brain.earBrainIntermediary = earBrainIntermediary
  ears.earBrainIntermediary = earBrainIntermediary

  screen.setProgress("Starting SARAH's interface ...", 90)
  destroySplashScreen

  val headerPanel = new MainFrameHeaderPanel
  val microphonePanel = new MicrophonePanel(this)
  val logOutputPanel = new LogOutputPanel
  val mainFrame = new MainFrame
  mainFrame.add(headerPanel, BorderLayout.NORTH)
  mainFrame.add(microphonePanel, BorderLayout.CENTER)
  mainFrame.add(logOutputPanel, BorderLayout.SOUTH)
  displayMainFrame


  // END constructor

  // TODO probably a better way to do these
  def getDataFileDirectory = CANON_DATA_DIR
  def getLogFileDirectory  = CANON_LOGFILE_DIR
  def getFilePathSeparator = FILE_PATH_SEPARATOR
  
  def startRunning {
    // start the actor threads
    earBrainIntermediary.start
    brain.start
    ears.start
  
    loadPlugins
    
  }

  /**
   * SARAH is speaking, listening, or not listening.
   * -----------------------------------------------
   */
  def updateUISarahIsSpeaking {
    SwingUtilities.invokeLater(new Runnable() {
      def run() {
        microphonePanel.setSarahIsSpeaking
      }
    });
  }

  def updateUISarahIsListening {
    SwingUtilities.invokeLater(new Runnable() {
      def run() {
        microphonePanel.setSarahIsListening
      }
    });
  }

  def updateUISarahIsNotListening {
    SwingUtilities.invokeLater(new Runnable() {
      def run() {
        microphonePanel.setSarahIsNotListening
      }
    });
  }

  
  def destroySplashScreen {
    screen.setVisible(false)
    screen = null
  }
  
  def loadPlugins {
    // get a list of subdirs in the plugins dir, assume each is a plugin
    log.info("Getting list of PLUGIN subdirectories, looking in '" + CANON_PLUGINS_DIR + "'")
    val pluginDirs = getListOfSubDirectories(CANON_PLUGINS_DIR)
    log.info("pluginDirs.length = " + pluginDirs.length)
    
    // trying to keep things simple here. if anything goes wrong in the functions we call,
    // they will throw an exception, and we'll log the error and skip that exception.
    try {
      log.info("About to loop over pluginDirs ...")
      for (pluginDir <- pluginDirs) {
        val canonPluginDir = CANON_PLUGINS_DIR + FILE_PATH_SEPARATOR + pluginDir
        log.info("Looking at PLUGIN DIR: " + canonPluginDir)
        val pluginInfoFilename = getPluginInfoFilename(canonPluginDir)
        log.info("pluginInfoFilename = " + pluginInfoFilename)
        val pluginProperties = getPluginProperties(canonPluginDir + FILE_PATH_SEPARATOR + pluginInfoFilename)
        log.info("read pluginProperties")
        val pluginJarFilename = getPluginJarFilename(canonPluginDir)
        log.info("pluginJarFilename = " + pluginJarFilename)
        val mainClassName = pluginProperties.get("main_class").get
        log.info("mainClassName = " + mainClassName)
        val canonJarFilename = canonPluginDir + FILE_PATH_SEPARATOR + pluginJarFilename
        log.info("canonJarFilename = " + canonJarFilename)
        val pluginInstance = getPluginInstance(canonJarFilename, mainClassName)
        log.info("created pluginInstance")
//        pluginInstance.mainClass = mainClassName
//        log.info("pluginInstance.mainClass = " + pluginInstance.mainClass)
//        pluginInstance.pluginName = pluginProperties.get("plugin_name").getOrElse("NO NAME")
//        log.info("pluginInstance.name = " + pluginInstance.pluginName)
        log.info("GOT PLUGIN INSTANCE")
        pluginInstances += pluginInstance
      } // end for loop
      log.info("Looping through plugin instances ...")
      for (plugin <- pluginInstances) {
        log.info("Trying to start plugin instance: " + plugin.toString())
        connectInstanceToBrain(plugin)
      }
    } catch {
      case e: Exception => // ignore, and move on to next plugin
           log.error("Had a problem loading a plugin. See previous exceptions.")
    }
  }

  /**
   * Returns the plugin as a ready-to-run instance, or throws an exception.
   */
  def getPluginInstance(canonicalJarFilename: String, mainClassName: String): SarahPlugin = {
    try {
      log.info("creating classLoader ...")
      log.info("  canonicalJarFilename = " + canonicalJarFilename)
      log.info("  mainClassName = " + mainClassName)
      var classLoader = new java.net.URLClassLoader(Array(new File(canonicalJarFilename).toURI.toURL), this.getClass.getClassLoader)
      log.info("creating new plugin instance as a SarahPlugin ...")
      
      // CODE FAILS HERE with AbstractMethodError

      var pluginInstance:SarahPlugin = classLoader.loadClass(mainClassName).newInstance.asInstanceOf[SarahPlugin]

      log.info("returning new plugin instance ...")
      return pluginInstance
    } catch {
      case cce: ClassCastException => log.error(cce.getMessage())
                                      throw cce
      case ame: AbstractMethodError => log.error(ame.getMessage())
                                      throw new Exception("GOT AN AbstractMethodError")
      case e:   Exception =>          log.error(e.getMessage())
                                      throw e
    }
  }
  
  def connectInstanceToBrain(pluginInstance: SarahPlugin) {
    log.info("connecting instance to brain")
    pluginInstance.connectToBrain(brain)
    log.info("starting plugin")
    pluginInstance.startPlugin
    log.info("started plugin")
  }
  
  /**
   * Get the plugin properties (plugin_name, main_class), or throw an exception.
   */
  def getPluginProperties(infoFilename: String): Map[String, String] = {
    try {
      val properties = new Properties
      val in = new FileInputStream(infoFilename)
      properties.load(in)
      in.close()
      val pluginName = properties.getProperty("plugin_name", "[NO NAME]")
      val mainClass = properties.getProperty("main_class", "")
      if (mainClass.trim().equals("")) {
        throw new Exception("main_class not found in .info file (" + infoFilename + ").")
      }
      return Map("main_class" -> mainClass, "plugin_name" -> pluginName)
    } catch {
      case e:Exception => log.error(e.getMessage())
                          throw e
    }
  }

  /**
   * Get a List[String] representing all the sub-directories in the given directory.
   */
  def getListOfSubDirectories(directoryName: String): scala.collection.immutable.List[String] = {
    val folder = new File(directoryName)
    val files = folder.listFiles // File[]
    val dirNames = ArrayBuffer[String]()
    for (file <- files) {
      if (file.isDirectory()) {
        dirNames += file.getName()
      }
    }
    return dirNames.toList
  }

  /**
   * Returns the name of the plugin's ".info" file, or throws an exception.
   * Code assumes there is one .info file in the current dir.
   * Throws an Exception if a file is not found.
   * TODO refactor this function to work with the similar '.jar' function.
   */
  def getPluginInfoFilename(directoryName: String):String = {
    val folder = new File(directoryName)
    val files = folder.listFiles(new FilenameFilter {
      def accept(file: File, filename: String): Boolean = {
        return (filename.endsWith(".info"))
      }
    })
    if (files == null || files.length > 0) {
      return files(0).getName
    } else {
      throw new Exception("No .info file found in directory '" + directoryName + "'")
    }
  }

  /**
   * Returns the name of the plugin's ".jar" file.
   * Code assumes there is one .jar file in the current dir.
   * Throws an Exception if a file is not found.
   * TODO refactor this function to work with the similar '.info' function.
   */
  def getPluginJarFilename(directoryName: String):String = {
    val folder = new File(directoryName)
    val files = folder.listFiles(new FilenameFilter {
      def accept(file: File, filename: String): Boolean = {
        return (filename.endsWith(".jar"))
      }
    })
    if (files == null || files.length > 0) {
      return files(0).getName
    } else {
      throw new Exception("No .jar file found in directory '" + directoryName + "'")
    }
  }
  
  
  /**
   * If the app is not running on mac os x, die right away.
   */
  def dieIfNotRunningOnMacOsX
  {
    val mrjVersionExists = System.getProperty("mrj.version") != null
    val osNameExists = System.getProperty("os.name").startsWith("Mac OS")
    
    if ( !mrjVersionExists || !osNameExists)
    {
      System.err.println("SARAH is not running on a Mac OS X system, terminating.")
      System.exit(EXIT_CODE_NOT_RUNNING_ON_MAC)
    }
  }
  
  def configureForMacOSX
  {
    // set some mac-specific properties; helps when i don't use ant to build the code
    System.setProperty("apple.awt.graphics.EnableQ2DX", "true")
    System.setProperty("apple.laf.useScreenMenuBar", "true")
    System.setProperty("com.apple.mrj.application.apple.menu.about.name", APP_NAME)

    // create an instance of the Mac Application class, so i can handle the 
    // mac quit event with the Mac ApplicationAdapter
    val macApplication = Application.getApplication()
    val macAdapter = new MacApplicationAdapter(this)
    macApplication.addApplicationListener(macAdapter)
    
    // must enable the preferences option manually
    macApplication.setEnabledPreferencesMenu(true)
  }
  
  // TODO implement
  def handleMacQuitAction {
    shutdown
  }

  // TODO implement
  def handleMacPreferencesAction {
  }
  
  // TODO implement
  def handleMacAboutAction {
  }
  
  def displayMainFrame {
    // TODO is this needed?
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    } catch {
      case e: Exception => // ignore
    }
    mainFrame.setColor(INITIAL_FRAME_COLOR)
    mainFrame.setUndecorated(true)
    mainFrame.setResizable(true)   // 'true' so user can move window
    makeFrameFullScreen
    displayMainFrame2
  }
  
  def makeFrameFullScreen {
    mainFrame.setSize(Toolkit.getDefaultToolkit.getScreenSize)
    mainFrame.setLocationRelativeTo(null)
  }
  
  def displayMainFrame2 {
    SwingUtilities.invokeLater(new Runnable()
    {
      def run()
      {
        mainFrame.setVisible(true)
        giveFocusBackToMainFrame
      }
    });
  }
  
  def giveFocusBackToMainFrame
  {
    SwingUtilities.invokeLater(new Runnable()
    {
      def run
      {
        mainFrame.transferFocus
      }
    });
  }
  

  def mergeAllVoiceCommandFiles {
    // TODO implement this
  }
  
  // TODO get this code to work properly. System.exit isn't really exiting.
  def shutdown {
    println("Shutting down.")
    earBrainIntermediary ! Die
    brain ! Die
    //ears  ! Die
    Utils.sleep(1000)
    System.exit(0)
  }
  
  def startMicRecordingOrDie(recognizer: Recognizer, microphone: Microphone) {
    if (!microphone.startRecording()) {
      println("Cannot start the microphone, aborting.");
      recognizer.deallocate();
      System.exit(EXIT_CODE_CANT_START_MIC)
    }
  }
    
}
 





