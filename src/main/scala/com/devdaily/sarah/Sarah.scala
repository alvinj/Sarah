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


/**
 * I'm going through some extra work in this file for two reasons.
 * First, I need to pass the Brain instance a 'this' reference. This
 * is because I'm trying to get System.exit to work.
 * Second, I'm also trying to learn about Scala companion classes
 * and objects.
 */
object Sarah {
  
  def main(args: Array[String]) {

    val sarah = new Sarah
    sarah.startRunning
    
  }
  
} // end of object


class Sarah {

  /**
   * REMEMBER, all code up here is basically the no-args constructor
   * ---------------------------------------------------------------
   */

  val APP_NAME = "SARAH"
  val EXIT_CODE_CANT_START_MIC = 5
  val EXIT_CODE_NOT_RUNNING_ON_MAC = 2
  val INITIAL_FRAME_COLOR = new Color(170, 194, 156)

  //dieIfNotRunningOnMacOsX

  val mainFrame = new MainFrame
  displayMainFrame
  
  // TODO - merge voice command files
  mergeAllVoiceCommandFiles

  val cm = new ConfigurationManager("sarah.config.xml")
  val recognizer = cm.lookup("recognizer").asInstanceOf[Recognizer]
  val microphone = cm.lookup("microphone").asInstanceOf[Microphone]
  recognizer.allocate
  
  startMicRecordingOrDie(recognizer, microphone)

  // i'm passing in nulls here because i don't know a better way to make 'brain'
  // and 'ears' available to me in setter methods. these two objects need to know about
  // each other.
  val brain = new Brain(this, microphone, recognizer, null)
  val ears  = new Ears(microphone, recognizer, null)
  val earBrainIntermediary = new EarBrainIntermediary(brain, ears)
  
  brain.earBrainIntermediary = earBrainIntermediary
  ears.earBrainIntermediary = earBrainIntermediary

  def startRunning {
    // start the actor threads
    earBrainIntermediary.start
    brain.start
    ears.start
  
    loadPlugins
    
  }
  
  /**
   * Load any/all application plugins that have been defined.
   * TODO implement this with a real plugin directory.
   */
  def loadPlugins {
    try {
    var classLoader = new java.net.URLClassLoader(Array(new File("/Users/al/Projects/Scala/SarahHourlyChime/deploy/HourlyChime.jar").toURI.toURL),
      this.getClass.getClassLoader)
    var pluginInstance:SarahActorBasedPlugin = classLoader.loadClass("com.devdaily.sarah.plugin.hourlychime.HourlyChimePlugin").newInstance.asInstanceOf[SarahActorBasedPlugin]
    
    pluginInstance.connectToBrain(brain)
    pluginInstance.start
    } catch {
      case cce: ClassCastException =>  cce.printStackTrace()
      case e:   Exception =>           e.printStackTrace()
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
 





