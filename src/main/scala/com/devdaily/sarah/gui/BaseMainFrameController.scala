package com.devdaily.sarah.gui

import javax.swing.SwingUtilities

abstract class BaseMainFrameController {
  
  def getMainFrame: BaseMainFrame

  def updateUIBasedOnStates
  
  def invokeLater(callback: => Unit) {
    SwingUtilities.invokeLater(new Runnable() {
      def run() {
        callback
      }
    });
  }



}