package com.devdaily.sarah.gui

import java.awt.Color

class MicrophoneMainFrame extends BaseMainFrame {

  def setColor(color: Color) {
    this.getContentPane.setBackground(color)
    this.setBackground(color)
  }

}

