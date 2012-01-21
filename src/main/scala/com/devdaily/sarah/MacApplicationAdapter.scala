package com.devdaily.sarah

import _root_.com.apple.eawt.ApplicationAdapter
import _root_.com.apple.eawt.ApplicationEvent

class MacApplicationAdapter(sarah: Sarah) extends ApplicationAdapter
{
  var handler: Sarah = null
  
  override def handleQuit(e: ApplicationEvent)
  {
    handler.handleMacQuitAction
  }

  override def handlePreferences(e: ApplicationEvent)
  {
    handler.handleMacPreferencesAction
  }

  override def handleAbout(e: ApplicationEvent)
  {
    // tell the system we're handling this, so it won't display
    // the default system "about" dialog after ours is shown.
    e.setHandled(true)
    handler.handleMacAboutAction
  }
}




