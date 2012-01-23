package com.devdaily.sarah.gui

import javax.swing.JPanel
import javax.swing.ImageIcon
import javax.swing.JLabel
import java.awt.FlowLayout
import com.devdaily.sarah.Sarah
import java.awt.Color

class MicrophonePanel(sarah: Sarah) extends JPanel {
  
  val SARAH_IS_LISTENING_COLOR     = new Color(170, 194, 156)  // green color
  val SARAH_IS_NOT_LISTENING_COLOR = new Color(128, 128, 128)  // grey
  val SARAH_IS_SPEAKING_COLOR      = new Color(255, 250, 205)  // yellow chiffon

  val micImage = new ImageIcon(classOf[com.devdaily.sarah.Sarah].getResource("microphone-image.png"))
  val imageLabel = new JLabel(micImage)
  val flowLayout = new FlowLayout
  flowLayout.setAlignment(FlowLayout.CENTER)
  this.setLayout(flowLayout)
  this.add(imageLabel)
  
  def setSarahIsSpeaking {
    this.setBackground(SARAH_IS_SPEAKING_COLOR)
  }

  def setSarahIsListening {
    this.setBackground(SARAH_IS_LISTENING_COLOR)
  }

  def setSarahIsNotListening {
    this.setBackground(SARAH_IS_NOT_LISTENING_COLOR)
  }

}

