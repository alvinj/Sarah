package com.devdaily.sarah.plugins

import java.text.DateFormat
import java.util.Calendar
import java.text.SimpleDateFormat
import scala.io.Source
import scala.util.Random
import java.util.Properties
import java.io.FileInputStream
import java.io.File
import java.io.FileFilter

object PluginUtils {
  
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
  
  /**
 * Read a Java properties file and return it as a Properties object.
 */
  def readPropertiesFile(filename: String):Properties = {
    val properties = new Properties
    properties.load(new FileInputStream(filename))
    return properties
  }
  

  /**
   * Returns an Array[File] of all files in the given dir.
   */
  def getListOfFiles(dirName: String):Array[File] = {
    return new File(dirName).listFiles
  }

  /**
   * Returns an Array[File] of all files that match the FileFilter,
   * such as the SoundFileFilter included in this class.
   * 
   * Related answer from StackOverflow:
   * new java.io.File(dirName).listFiles.filter(_.getName.endsWith(".txt"))
   */
  def getListOfFiles(dirName: String, fileFilter:FileFilter):Array[File] = {
    return new File(dirName).listFiles(fileFilter)
  }

  // note that this is extends, not "implements", as in java
  def getSoundFileFilter = new FileFilter {
    val okFileExtensions = Array("wav", "mp3")
    def accept(file: File):Boolean = {
      for (extension <- okFileExtensions) {
        if (file.getName.toLowerCase.endsWith(extension)) return true
      }
      return false
    }
  }
  
  /**
   * Get a recursive listing of all files underneath the given directory.
   * from http://stackoverflow.com/questions/2637643/how-do-i-list-all-files-in-a-subdirectory-in-scala
   */
  def getRecursiveListOfFiles(dir: File): Array[File] = {
    val these = dir.listFiles
    these ++ these.filter(_.isDirectory).flatMap(getRecursiveListOfFiles)
  }
  
  
}








