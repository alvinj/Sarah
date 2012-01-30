package com.devdaily.sarah.gui

import com.devdaily.sarah.Sarah
import java.awt.BorderLayout
import javax.swing.SwingUtilities
import java.awt.Color
import javax.swing.JPanel
import javax.swing.ImageIcon
import javax.swing.JLabel
import java.awt.FlowLayout
import com.devdaily.sarah.plugins.PluginUtils
import com.devdaily.sarah.actors.Brain
import javax.swing.JEditorPane
import java.awt.Dimension
import javax.swing.UIManager
import java.awt.Insets
import javax.swing.JScrollPane
import javax.swing.JOptionPane

class MicrophoneMainFrameController(sarah: Sarah) extends BaseMainFrameController {

  val SLEEP_TO_AVOID_FLICKER = 250
  
  // constructor
  val headerPanel = new MicrophoneMainFrameHeaderPanel
  val microphonePanel = new MicrophonePanel
  val mainFrame = new MicrophoneMainFrame
  mainFrame.add(headerPanel, BorderLayout.NORTH)
  mainFrame.add(microphonePanel, BorderLayout.CENTER)
  updateUIBasedOnStates
  
  def getMainFrame = mainFrame

  def updateUIBasedOnStates {
    // get mouth state
    val mouthState = sarah.getMouthState
    val earsState = sarah.getEarsState
    val awarenessState = sarah.getAwarenessState

    // TODO clean up this decision block
    
    // easy one - any time sarah is speaking, use that color
    if (mouthState == Sarah.MOUTH_STATE_SPEAKING) {
      invokeLater(microphonePanel.setSarahIsSpeaking)
      invokeLater(headerPanel.setSarahIsSpeaking)
      PluginUtils.sleep(SLEEP_TO_AVOID_FLICKER)
    }

    // now branch off, initially based on awareness
    awarenessState match {
      case Sarah.AWARENESS_STATE_AWAKE => handleAwarenessStateAwake(earsState)
      case Sarah.AWARENESS_STATE_LIGHT_SLEEP => handleAwarenessStateLightSleep(earsState)
      case Sarah.AWARENESS_STATE_DEEP_SLEEP =>handleAwarenessStateDeepSleep(earsState)
    }
  }
  
  private def handleAwarenessStateAwake(earsState: Int) {
    earsState match {
      case Sarah.EARS_STATE_LISTENING => updateUISarahIsAwakeAndListening
      case Sarah.EARS_STATE_NOT_LISTENING => updateUISarahIsAwakeButNotListening
      case Sarah.EARS_STATE_HEARD_SOMETHING => updateUISarahIsSleepingButHeardSomething
    }
  }

  private def handleAwarenessStateLightSleep(earsState: Int) {
    earsState match {
      case Sarah.EARS_STATE_LISTENING => updateUISarahIsSleeping
      case Sarah.EARS_STATE_NOT_LISTENING => updateUISarahIsSleeping
      case Sarah.EARS_STATE_HEARD_SOMETHING => updateUISarahIsSleepingButHeardSomething
    }
  }

  // just a pass-thru method for now
  private def handleAwarenessStateDeepSleep(earsState: Int) { 
    handleAwarenessStateLightSleep(earsState)
  }

  private def updateUISarahIsAwakeButNotListening {
    invokeLater(microphonePanel.setSarahIsNotListening)
    invokeLater(headerPanel.setSarahIsNotListening)
    PluginUtils.sleep(SLEEP_TO_AVOID_FLICKER)
  }

  private def updateUISarahIsAwakeAndListening {
    invokeLater(microphonePanel.setSarahIsListening)
    invokeLater(headerPanel.setSarahIsListening)
    PluginUtils.sleep(SLEEP_TO_AVOID_FLICKER)
  }

  private def updateUISarahIsSleeping {
    invokeLater(microphonePanel.setSarahIsSleeping)
    invokeLater(headerPanel.setSarahIsSleeping)
    PluginUtils.sleep(SLEEP_TO_AVOID_FLICKER)
  }

  private def updateUISarahIsSleepingButHeardSomething {
    invokeLater(microphonePanel.setSarahIsSleepingButHeardSomething)
    invokeLater(headerPanel.setSarahIsSleepingButHeardSomething)
    PluginUtils.sleep(SLEEP_TO_AVOID_FLICKER)
  }



  def displayAvailableVoiceCommands(voiceCommands: List[String]) {
    val sb = new StringBuilder
    for (s <- voiceCommands) {
      sb.append(s)
      sb.append("\n")
    }
    val textToShow = sb.toString
    val window = new DisplayTextWindow(getMainFrame, textToShow)
    invokeLater(window.setVisible(true))
    // TODO really want to let the user dispose this window
    PluginUtils.sleep(5000)
    invokeLater(window.setVisible(false))
  }
  
}

/**
 * HEADER
 */
class MicrophoneMainFrameHeaderPanel extends JPanel {

  val sarahImage = new ImageIcon(classOf[com.devdaily.sarah.Sarah].getResource("sarah-header-image.png"))
  val imageLabel = new JLabel(sarahImage)
  val flowLayout = new FlowLayout
  flowLayout.setAlignment(FlowLayout.CENTER)
  this.setLayout(flowLayout)
  this.add(imageLabel)

  def setSarahIsSleepingButHeardSomething {
    this.setBackground(MicrophoneMainFrameController.SARAH_IS_SLEEPING_BUT_HEARD_SOMETHING_COLOR)
  }

  def setSarahIsSleeping {
    this.setBackground(MicrophoneMainFrameController.SARAH_IS_SLEEPING_COLOR)
  }

  def setSarahIsSpeaking {
    this.setBackground(MicrophoneMainFrameController.SARAH_IS_SPEAKING_COLOR)
  }

  def setSarahIsListening {
    this.setBackground(MicrophoneMainFrameController.SARAH_IS_LISTENING_COLOR)
  }

  def setSarahIsNotListening {
    this.setBackground(MicrophoneMainFrameController.SARAH_IS_NOT_LISTENING_COLOR)
  }

}

// TODO sort out these color issues related to the "states"
object MicrophoneMainFrameController {
  
  val GREEN  = new Color(170, 194, 156) // pastel green
  val YELLOW = new Color(255, 250, 205) // yellow chiffon
  
  val SARAH_IS_SLEEPING_BUT_HEARD_SOMETHING_COLOR = Color.LIGHT_GRAY
  val SARAH_IS_SLEEPING_COLOR      = Color.DARK_GRAY
  val SARAH_IS_SPEAKING_COLOR      = YELLOW
  val SARAH_IS_LISTENING_COLOR     = GREEN
  val SARAH_IS_NOT_LISTENING_COLOR = Color.LIGHT_GRAY

  val SARAH_IS_AWAKE_COLOR           = GREEN
  val SARAH_IS_IN_LIGHT_SLEEP_COLOR  = Color.DARK_GRAY
  val SARAH_IS_IN_DEEP_SLEEP_COLOR   = Color.DARK_GRAY
}






