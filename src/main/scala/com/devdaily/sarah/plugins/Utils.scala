package com.devdaily.sarah.plugins

import java.text.DateFormat
import java.util.Calendar
import java.text.SimpleDateFormat

object Utils {
  
  /**
   * sleepTime is in millis.
   */
  def sleep(sleepTime: Int) {
    try {
      Thread.sleep(sleepTime)
    }
    catch {
      case _ => // do nothing
    }
  }

  /**
   * As "Wednesday, October 20"
   */
  def getCurrentDate:String = {
    return getCurrentDateTime("EEEE, MMMM d")
  }

  /**
   * As "6:20 p.m."
   */
  def getCurrentTime: String = {
    return getCurrentDateTime("K:m aa")
  }

  /**
   * As "Wednesday, October 20, 6:20 p.m."
   */
  def getCurrentDateAndTime: String = {
    return getCurrentDateTime("EEEE, MMMM d, K:m aa")
  }

  /**
   * A common function used by other date/time functions.
   */
  private def getCurrentDateTime(dateTimeFormat: String): String = {
    val dateFormat = new SimpleDateFormat(dateTimeFormat)
    val cal = Calendar.getInstance()
    return dateFormat.format(cal.getTime())
  }
  
  
}





