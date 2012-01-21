package com.devdaily.sarah.utils

/**
 * This code copied, currently without permission, from this url:
 * http://www.qualitybrain.com/?p=84
 */
class SystemProcessRunner {

  import java.io._
  import scala.actors._
  import scala.actors.Actor._
   
  private val caller = self
  private val WAIT_TIME = 2000
 
  private val reader = actor {
    println("created actor: " + Thread.currentThread)
    var continue = true
    loopWhile(continue){
    reactWithin(WAIT_TIME) {
      case TIMEOUT =>
        caller ! "react timeout"
      case proc:Process =>
        println("entering first actor " + Thread.currentThread)
        val streamReader = new java.io.InputStreamReader(proc.getInputStream)
        val bufferedReader = new java.io.BufferedReader(streamReader)
        val stringBuilder = new java.lang.StringBuilder()
        var line:String = null
        while({line = bufferedReader.readLine; line != null}){
          stringBuilder.append(line)
          stringBuilder.append("\n")
        }
        bufferedReader.close
        caller ! stringBuilder.toString
      }
    }
  }

  /**
   * Returns "TIMEOUT" if the call fails, and the result if the call succeeds.
   */
  def run(command:String):String = {
    //println("gonna runa a command: " + Thread.currentThread)
    val args = command.split(" ")
    val processBuilder = new ProcessBuilder(args: _* )
    processBuilder.redirectErrorStream(true)
    val proc = processBuilder.start()
 
    //Send the proc to the actor, to extract the console output.
    reader ! proc
 
    //Receive the console output from the actor.
    receiveWithin(WAIT_TIME) {
      case TIMEOUT => return "TIMEOUT"
        case result:String => return result
      }
    }
 
    def main(args: Array[String]) :Unit = {
      val consumerKey = "dj0yJmk9eUhmTGFLNzFoNGkwJmQ9WVdrOWVuZFFNMVpVTXpJbWNHbzlNVGt3TXpZd05qZzJNZy0tJnM9Y29uc3VtZXJzZWNyZXQmeD1iZA--"
      val consumerSecret = "3cad8594622dae1dfd41730fcdcd67346ea840fb"
      run("php /Users/al/Desktop/phpsample/GetUrl.php json " + consumerKey + " " + consumerSecret)
      //System.exit(0)
  }
}






