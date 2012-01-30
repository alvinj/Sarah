package com.devdaily.sarah.gui

import java.awt.BorderLayout
import javax.swing.SwingUtilities
import javax.swing.JPanel
import javax.swing.ImageIcon
import javax.swing.JLabel
import java.awt.FlowLayout
import java.awt.Color
import com.devdaily.sarah.Sarah

class Hal9000MainFrameController(sarah: Sarah) extends BaseMainFrameController {

  val microphonePanel = new Hal9000MainPanel
  val mainFrame = new Hal9000MainFrame
  mainFrame.add(microphonePanel, BorderLayout.CENTER)
  
  def getMainFrame = mainFrame
  
  // do nothing ... yet
  def updateUIBasedOnStates {}
  
}

/**
 * The JPanel with the HAL 9000 red eye.
 */
class Hal9000MainPanel extends JPanel {
  val hal9000Image = new ImageIcon(classOf[com.devdaily.sarah.Sarah].getResource("hal9000-image.png"))
  val imageLabel = new JLabel(hal9000Image)
  imageLabel.setBackground(Color.BLACK)
  this.setBackground(Color.BLACK)
  val borderLayout = new BorderLayout
  setLayout(borderLayout)
  add(imageLabel, BorderLayout.CENTER)
}

/**
 * The HAL 9000 main frame ... JFrame, that is.
 */
class Hal9000MainFrame extends BaseMainFrame {
  // nothing yet
}







