package com.devdaily.sarah.gui

import javax.swing.JPanel
import javax.swing.ImageIcon
import javax.swing.JLabel
import java.awt.FlowLayout
import com.devdaily.sarah.Sarah
import java.awt.Color

class MicrophonePanel extends JPanel {
  
  val micImage = new ImageIcon(classOf[com.devdaily.sarah.Sarah].getResource("microphone-image.png"))
  val imageLabel = new JLabel(micImage)
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

