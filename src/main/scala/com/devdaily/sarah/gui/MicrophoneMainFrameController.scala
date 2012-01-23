package com.devdaily.sarah.gui

import com.devdaily.sarah.Sarah
import java.awt.BorderLayout
import javax.swing.SwingUtilities

class MicrophoneMainFrameController(sarah: Sarah) extends BaseMainFrameController {

  val headerPanel = new MainFrameHeaderPanel
  val microphonePanel = new MicrophonePanel
  val logOutputPanel = new LogOutputPanel
  val mainFrame = new MicrophoneMainFrame
  mainFrame.add(headerPanel, BorderLayout.NORTH)
  mainFrame.add(microphonePanel, BorderLayout.CENTER)
  mainFrame.add(logOutputPanel, BorderLayout.SOUTH)
  
  def getMainFrame = mainFrame
  
  def updateUISarahIsSleeping = invokeLater(microphonePanel.setSarahIsSleeping)
  def updateUISarahIsSpeaking = invokeLater(microphonePanel.setSarahIsSpeaking)
  def updateUISarahIsListening = invokeLater(microphonePanel.setSarahIsListening)
  def updateUISarahIsNotListening = invokeLater(microphonePanel.setSarahIsNotListening)

}







