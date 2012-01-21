package com.devdaily.sarah

import javax.swing.JFrame
import java.awt.Color

class MainFrame extends JFrame {

  def setColor(color: Color) {
    this.getContentPane.setBackground(color)
    this.setBackground(color)
  }

}