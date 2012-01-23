package com.devdaily.sarah.gui

import javax.swing.JPanel
import javax.swing.ImageIcon
import java.awt.FlowLayout
import javax.swing.JLabel

class MainFrameHeaderPanel extends JPanel {

  val sarahImage = new ImageIcon(classOf[com.devdaily.sarah.Sarah].getResource("sarah-header-image.png"))
  val imageLabel = new JLabel(sarahImage)
  val flowLayout = new FlowLayout
  flowLayout.setAlignment(FlowLayout.CENTER)
  this.setLayout(flowLayout)
  this.add(imageLabel)

}


