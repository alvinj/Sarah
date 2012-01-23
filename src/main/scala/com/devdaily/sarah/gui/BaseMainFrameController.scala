package com.devdaily.sarah.gui

abstract class BaseMainFrameController {
  
  def getMainFrame: BaseMainFrame

  def updateUISarahIsSleeping
  def updateUISarahIsSpeaking
  def updateUISarahIsListening
  def updateUISarahIsNotListening

}