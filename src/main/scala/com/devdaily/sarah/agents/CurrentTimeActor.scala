package com.devdaily.sarah.agents

import scala.actors._
import java.util.Calendar
import java.text.SimpleDateFormat
import com.devdaily.sarah.actors._
import com.devdaily.sarah._
import com.devdaily.sarah.actors.PleaseSay

/**
 * This runs as a background thread, and sends a "current time" message to the Brain
 * every hour.
 */
class CurrentTimeActor(brain: Brain) extends Actor {
  
  def act() {
    loop {
      sleepForAMinute
      if (onTheHour) {
        val currentTime = format("%s:%s %s\n", getCurrentHour, getCurrentMinute, getAmOrPm)
        println("(CurrentTimeActor) The current time is " + currentTime)
        brain ! PleaseSay("The current time is " + currentTime)
      }
    }
  }
  
  def sleepForAMinute = Utils.sleep(60*1000)
  
  // returns true if minutes = 0, i.e., the current time is "on the hour"
  def onTheHour :Boolean = {
    val today = Calendar.getInstance().getTime()
    val minuteFormat = new SimpleDateFormat("mm")
    val currentMinuteAsString = minuteFormat.format(today)
    try {
      val currentMinute = Integer.parseInt(currentMinuteAsString)
      //
      // TODO testing
      //
      if (currentMinute % 5 == 0) return true
      //if (currentMinute == 0) return true
      else return false
    } catch {
      case _ => return false
    }
  }

  // returns the current hour as a string
  def getCurrentHour: String = {
    val today = Calendar.getInstance().getTime()
    val hourFormat = new SimpleDateFormat("hh")
    try {
      // returns something like "01" if i just return at this point, so cast it to
      // an int, then back to a string (or return the leading '0' if you prefer)
      val currentHour = Integer.parseInt(hourFormat.format(today))
      return "" + currentHour
    } catch {
      // TODO return Some/None/Whatever
      case _ => return "0"
    }
    return hourFormat.format(today)
  }
  
  // returns the current minute as a string
  def getCurrentMinute: String = {
    val today = Calendar.getInstance().getTime()
    val minuteFormat = new SimpleDateFormat("mm")
    // in this case, returning "01" is okay
    return minuteFormat.format(today)
  }
  
  // returns "AM" or "PM"
  def getAmOrPm: String = {
    val today = Calendar.getInstance().getTime()
    val amPmFormat = new SimpleDateFormat("a")
    return amPmFormat.format(today)
  }
  
}





