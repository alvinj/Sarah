package com.devdaily.sarah.gui

import java.awt.Dimension
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.JScrollPane
import javax.swing.ScrollPaneConstants
import java.awt.BorderLayout

class LogOutputPanel extends JPanel {

  // put a textarea in a scrollpane
  val textArea = new JTextArea
  textArea.setText("Hello, world")
  textArea.setPreferredSize(new Dimension(600, 100))
  textArea.setLineWrap(false)
  val scrollPane = new JScrollPane(textArea, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS)

  // put the scrollpane in a borderlayout in our panel
  val ourLayout = new BorderLayout
  this.setLayout(ourLayout)
  this.add(scrollPane, BorderLayout.CENTER)

  def appendText(text: String) {
    textArea.append(text)
  }

  def clearTextArea {
    textArea.setText("")
  }
}


