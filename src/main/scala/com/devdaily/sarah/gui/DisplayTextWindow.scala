package com.devdaily.sarah.gui

import javax.swing.JPanel
import javax.swing.JButton
import javax.swing.JFrame
import java.awt.GridBagLayout
import javax.swing.JTextArea
import java.awt.Dimension
import java.awt.Insets
import javax.swing.JScrollPane
import java.awt.BorderLayout
import javax.swing.JLayeredPane
import javax.swing.BorderFactory
import java.awt.Graphics
import java.awt.AlphaComposite
import java.awt.Graphics2D

/**
 * TODO - fix layout stuff here. i'm going through great lengths b/c i can't remember 
 *        swing layouts, and i'm in a rush tonight.
 */
class DisplayTextWindow(frame: BaseMainFrame, textToShow: String) {

  def getEmptyPanel: ClearPanel = {
    val p = new ClearPanel
    p.setSize(new Dimension(300,300))
    p.setPreferredSize(new Dimension(300,300))
    p.setMinimumSize(new Dimension(300,300))
    p.setMaximumSize(new Dimension(300,300))
    return p
  }
  
  // use this as filler/padding
  val headerPanel = getEmptyPanel
  val footerPanel = getEmptyPanel
  val westPanel = getEmptyPanel
  val eastPanel = getEmptyPanel

  // create the text area and scroll pane
  val size = new Dimension(400,300)
  val editor = new JTextArea
  editor.setEditable(false)
  editor.setSize(size)
  editor.setPreferredSize(size)
  editor.setMaximumSize(size)
  editor.setMinimumSize(size)
  editor.setMargin(new Insets(5,15,25,15))
  editor.setText(textToShow)

  val scrollPane = new JScrollPane(editor)
  scrollPane.setPreferredSize(size)
  scrollPane.setMaximumSize(size)

  // add the scroll panel to a new panel
  val panel = new ClearPanel
  panel.setLayout(new BorderLayout)
  panel.add(scrollPane, BorderLayout.CENTER)
  panel.add(headerPanel, BorderLayout.NORTH)
  panel.add(footerPanel, BorderLayout.SOUTH)
  panel.add(westPanel, BorderLayout.WEST)
  panel.add(eastPanel, BorderLayout.EAST)
  
  // add the panel to the glass pane, but don't show it
  frame.setGlassPane(panel)

  def setVisible(b: Boolean) {
    println("SET VISIBLE WAS CALLED")
    frame.getGlassPane.setVisible(b)
  }

}

/**
 * A slightly clear (translucent) panel. Source code from p. 228 of Filthy Rich Clients.
 */
class ClearPanel extends JPanel {
  override def paintComponent(g: Graphics) {
    val clip = g.getClipBounds
    val alpha = AlphaComposite.SrcOver.derive(0.65f)
    val g2 = g.asInstanceOf[Graphics2D]
    g2.setComposite(alpha)
    g2.setColor(getBackground)
    g2.fillRect(clip.x, clip.y, clip.width, clip.height)
  }
}







