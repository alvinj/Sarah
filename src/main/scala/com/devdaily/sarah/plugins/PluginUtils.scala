package com.devdaily.sarah.plugins

import java.text.DateFormat
import java.util.Calendar
import java.text.SimpleDateFormat
import scala.io.Source
import scala.util.Random

object PluginUtils {
  
  /**
   * Play the given sound file.
   * @see http://www.devdaily.com/java/java-play-sound-file-from-command-line-wav-headless-mode
   */
  def playSound(soundFile: String) {
    import java.io.FileInputStream
    import sun.audio.AudioStream
    import sun.audio.AudioPlayer
    try {
      var in = new FileInputStream(soundFile)
      val audioStream = new AudioStream(in)
      AudioPlayer.player.start(audioStream)
    } catch {
      case e: Exception => e.printStackTrace
    }
  }
  
  /**
   * Gets a random string from the given file. The file is assumed to have one or more
   * lines of strings that are meant to be read in as an array/list.
   */
  def getRandomStringFromFile(canonicalFilename: String): String = {
    val options = Source.fromFile(canonicalFilename).getLines.toList
    return options(Random.nextInt(options.length))
  }
  
  def getFileContentsAsString(canonicalFilename: String): String = {
    return Source.fromFile(canonicalFilename).mkString
  }
  
  def getFileContentsAsList(canonicalFilename: String): List[String] = {
    return Source.fromFile(canonicalFilename).getLines.toList
  }

  val getFilepathSeparator = System.getProperty("file.separator")
  val getUserHomeDir = System.getProperty("user.home")

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





