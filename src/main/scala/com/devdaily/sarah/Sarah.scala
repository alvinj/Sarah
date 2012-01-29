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
import java.awt.BorderLayout
import java.util.logging.FileHandler
import java.util.logging.Logger
import com.weiglewilczek.slf4s.Logging
import com.weiglewilczek.slf4s.Logger
import com.devdaily.sarah.gui.MicrophoneMainFrameController
import com.devdaily.sarah.gui.Hal9000MainFrameController
import javax.swing.JEditorPane
import java.awt.Dimension
import java.awt.Insets
import javax.swing.JScrollPane
import javax.swing.JOptionPane


/**
 * I'm going through some extra work in this file for two reasons.
 * First, I need to pass the Brain instance a 'this' reference. This
 * is because I'm trying to get System.exit to work.
 * Second, I'm also trying to learn about Scala companion classes
 * and objects.
 */
object Sarah extends Logging {
  
  val APP_NAME = "SARAH"
  val EXIT_CODE_CANT_START_MIC = 5
  val EXIT_CODE_NOT_RUNNING_ON_MAC = 2
  val INITIAL_FRAME_COLOR = new Color(170, 194, 156)

  val REL_DATA_DIR          = "Sarah/data"
  val REL_LOGFILE_DIR       = "Sarah/logs"
  val REL_PLUGINS_DIR       = "Sarah/plugins"
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
  
  // states sarah can be in
  val SARAH_IS_LISTENING                    = 1
  val SARAH_IS_NOT_LISTENING                = 2
  val SARAH_IS_SLEEPING                     = 3
  val SARAH_IS_SLEEPING_BUT_HEARD_SOMETHING = 4
  val SARAH_IS_SPEAKING                     = 5
  
  val SPLASH_SCREEN_IMAGE   = "sarah-splash-image.png"

  /* kick off the app, and hold on */
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

  /** this is a shared state variable that all actors can access */
  private var state = Sarah.SARAH_IS_NOT_LISTENING
  
  // TODO get logging going to the sarah.log file
  val log = com.weiglewilczek.slf4s.Logger("Sarah")

  // TODO populate this list of plugins
  var pluginInstances = ArrayBuffer[SarahPlugin]()
  
  val splashImage = new ImageIcon(classOf[com.devdaily.sarah.Sarah].getResource(Sarah.SPLASH_SCREEN_IMAGE))
  var screen = new SplashScreen(splashImage)
  screen.setLocationRelativeTo(null)
  screen.setProgressMax(100)
  screen.setScreenVisible(true)
  screen.setProgress("Starting SARAH ...", 10)

  // TODO move plugins back up here?
  screen.setProgress("Finding plugins ...", 15)

  // TODO - merge voice command files
  screen.setProgress("Merging voice commands ...", 25)
  mergeAllVoiceCommandFiles
  
  
  //dieIfNotRunningOnMacOsX

  screen.setProgress("Connecting to microphone ...", 50)
  val cm = new ConfigurationManager(Sarah.SARAH_CONFIG_FILE)
  val recognizer = cm.lookup("recognizer").asInstanceOf[Recognizer]
  val microphone = cm.lookup("microphone").asInstanceOf[Microphone]
  recognizer.allocate
  startMicRecordingOrDie(recognizer, microphone)

  screen.setProgress("Starting SARAH's brain ...", 75)
  // i'm passing in nulls here because i don't know a better way to make 'brain'
  // and 'ears' available to me in setter methods. these two objects need to know about
  // each other.
  val brain = new Brain(this, microphone, recognizer, null)
  val ears  = new Ears(this, microphone, recognizer, brain)
  val mouth = new Mouth(this, brain)
  brain.mouth = mouth

  screen.setProgress("Starting SARAH's interface ...", 90)
  destroySplashScreen

  //val mainFrameController = new Hal9000MainFrameController(this)
  val mainFrameController = new MicrophoneMainFrameController(this)
  val mainFrame = mainFrameController.getMainFrame
  displayMainFrame


  // END constructor

  // TODO probably a better way to do these
  def getDataFileDirectory = Sarah.CANON_DATA_DIR
  def getLogFileDirectory  = Sarah.CANON_LOGFILE_DIR
  def getFilePathSeparator = Sarah.FILE_PATH_SEPARATOR
  
  def startRunning {
    
    brain.start; PluginUtils.sleep(250)
    mouth.start; PluginUtils.sleep(250)

    loadPlugins
    
    ears.start
    
    PluginUtils.sleep(1000)
    setCurrentState(Sarah.SARAH_IS_LISTENING)
    brain ! PleaseSay("Hello, Al")
  
  }

  /* sarah is always in one state or another; the brain, mouth, and ears can change the state */
  def setCurrentState(currentState: Int) {
    state = currentState
    state match {
      case Sarah.SARAH_IS_LISTENING =>                    mainFrameController.updateUISarahIsListening
      case Sarah.SARAH_IS_NOT_LISTENING =>                mainFrameController.updateUISarahIsNotListening
      case Sarah.SARAH_IS_SLEEPING =>                     mainFrameController.updateUISarahIsSleeping
      case Sarah.SARAH_IS_SLEEPING_BUT_HEARD_SOMETHING => mainFrameController.updateUISarahIsSleepingButHeardSomething
      case Sarah.SARAH_IS_SPEAKING =>                     mainFrameController.updateUISarahIsSpeaking
    }
  }
  
  def getState = state
  
  def clearMicrophone {
    microphone.clear
  }
  
  def destroySplashScreen {
    screen.setVisible(false)
    screen = null
  }
  
  def displayAvailableVoiceCommands(voiceCommands: scala.collection.immutable.List[String]) {
    mainFrameController.displayAvailableVoiceCommands(voiceCommands)
  }
  
  def tryToHandleTextWithPlugins(textTheUserSaid: String): Boolean = {
    log.info("tryToHandleTextWithPlugins, TEXT = " + textTheUserSaid)
    // loop through the plugins, and see if any can handle what was said
    for (plugin <- pluginInstances) {
      val handled = plugin.handlePhrase(textTheUserSaid)
      if (handled) return true
    }
    return false
  }
  
  
  def loadPlugins {
    // get a list of subdirs in the plugins dir, assume each is a plugin
    log.info("Getting list of PLUGIN subdirectories, looking in '" + Sarah.CANON_PLUGINS_DIR + "'")
    val pluginDirs = getListOfSubDirectories(Sarah.CANON_PLUGINS_DIR)
    log.info("pluginDirs.length = " + pluginDirs.length)
    
    // trying to keep things simple here. if anything goes wrong in the functions we call,
    // they will throw an exception, and we'll log the error and skip that exception.
    try {
      log.info("About to loop over pluginDirs ...")
      for (pluginDir <- pluginDirs) {
        val canonPluginDir = Sarah.CANON_PLUGINS_DIR + Sarah.FILE_PATH_SEPARATOR + pluginDir
        log.info("Looking at PLUGIN DIR: " + canonPluginDir)
        val pluginInfoFilename = getPluginInfoFilename(canonPluginDir)
        log.info("pluginInfoFilename = " + pluginInfoFilename)
        val pluginProperties = getPluginProperties(canonPluginDir + Sarah.FILE_PATH_SEPARATOR + pluginInfoFilename)
        log.info("read pluginProperties")
        val pluginJarFilename = getPluginJarFilename(canonPluginDir)
        log.info("pluginJarFilename = " + pluginJarFilename)
        val mainClassName = pluginProperties.get("main_class").get
        log.info("mainClassName = " + mainClassName)
        val canonJarFilename = canonPluginDir + Sarah.FILE_PATH_SEPARATOR + pluginJarFilename
        log.info("canonJarFilename = " + canonJarFilename)
        val pluginInstance = getPluginInstance(canonJarFilename, mainClassName)
        log.info("created pluginInstance, setting canonPluginDir")
        pluginInstance.setPluginDirectory(canonPluginDir)
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
      case e: RuntimeException =>
           log.error("Got a RuntimeException loading a plugin." + e.getMessage)
      case e: Error =>
           log.error("Got an Error loading a plugin." + e.getMessage)
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
   * Get a list representing all the sub-directories in the given directory.
   */
  def getListOfSubDirectories(directoryName: String): Array[String] = {
    return (new File(directoryName)).listFiles.filter(_.isDirectory).map(_.getName)
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
      System.exit(Sarah.EXIT_CODE_NOT_RUNNING_ON_MAC)
    }
  }
  
  def configureForMacOSX
  {
    // set some mac-specific properties; helps when i don't use ant to build the code
    System.setProperty("apple.awt.graphics.EnableQ2DX", "true")
    System.setProperty("apple.laf.useScreenMenuBar", "true")
    System.setProperty("com.apple.mrj.application.apple.menu.about.name", Sarah.APP_NAME)

    // create an instance of the Mac Application class, so i can handle the 
    // mac quit event with the Mac ApplicationAdapter
    val macApplication = Application.getApplication
    val macAdapter = new MacApplicationAdapter(this)
    macApplication.addApplicationListener(macAdapter)
    
    // TODO - enable when ready (must enable the preferences option manually)
    //macApplication.setEnabledPreferencesMenu(true)
  }
  
  // TODO implement
  def handleMacQuitAction {
    shutdown
  }

  // TODO implement
  def handleMacPreferencesAction {
  }
  
  def handleMacAboutAction {
    // used html here so i can add a hyperlink later
    val ABOUT_DIALOG_MESSAGE = "<html><center><p>SARAH</p></center>\n\n" + "<center><p>Created by Alvin Alexander, devdaily.com</p><center>\n"
    val editor = new JEditorPane
    editor.setContentType("text/html")
    editor.setEditable(false)
    editor.setSize(new Dimension(400,300))
    editor.setFont(UIManager.getFont("EditorPane.font"))
    // note: had to include this line to get it to use my font
    editor.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
    editor.setMargin(new Insets(5,15,25,15))
    editor.setText(ABOUT_DIALOG_MESSAGE)
    editor.setCaretPosition(0)
    val scrollPane = new JScrollPane(editor)
    // display our message
    JOptionPane.showMessageDialog(mainFrameController.getMainFrame, scrollPane,
        "About Hyde", JOptionPane.INFORMATION_MESSAGE);
  }
  
  def displayMainFrame {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    } catch {
      case e: Exception => // ignore
    }
    //mainFrame.setColor(INITIAL_FRAME_COLOR)
    mainFrame.setUndecorated(true)
    mainFrame.setResizable(true)   // 'true' so user can move window
    makeFrameFullScreen
    displayMainFrame2
  }
  
  def makeFrameFullScreen {
    mainFrame.setSize(Toolkit.getDefaultToolkit.getScreenSize)
    mainFrame.setLocationRelativeTo(null)
  }
  
  // TODO clean this up with the new invokeLater approach
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
  
  // TODO clean this up with the new invokeLater approach
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
    brain ! Die
    //ears  ! Die
    PluginUtils.sleep(500)
    System.exit(0)
  }
  
  def startMicRecordingOrDie(recognizer: Recognizer, microphone: Microphone) {
    if (!microphone.startRecording()) {
      println("Cannot start the microphone, aborting.");
      recognizer.deallocate();
      System.exit(Sarah.EXIT_CODE_CANT_START_MIC)
    }
  }
    
}
 





