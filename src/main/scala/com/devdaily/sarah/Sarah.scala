package com.devdaily.sarah

import java.io._
import java.util._
import com.devdaily.sarah.agents._
import edu.cmu.sphinx.frontend.util.Microphone
import edu.cmu.sphinx.jsgf.JSGFGrammar
import edu.cmu.sphinx.linguist.dictionary.Word
import edu.cmu.sphinx.linguist.language.grammar.GrammarArc
import edu.cmu.sphinx.linguist.language.grammar.GrammarNode
import edu.cmu.sphinx.recognizer.Recognizer
import edu.cmu.sphinx.result.Result
import edu.cmu.sphinx.util.props.ConfigurationManager
import actors._
import scala.xml._
import collection.Map
import collection.Traversable


/**
 * I'm going through some extra work in this file for two reasons.
 * First, I need to pass the Brain instance a 'this' reference. This
 * is because I'm trying to get System.exit to work.
 * Second, I'm also trying to learn about Scala companion classes
 * and objects.
 */
object Sarah {

  def main(args: Array[String]) {

    val sarah = new Sarah
    sarah.startRunning
    
  }
  
} // end of object


class Sarah {

  // all code here is basically the no-args constructor
  val cm = new ConfigurationManager("sarah.config.xml")
  val recognizer = cm.lookup("recognizer").asInstanceOf[Recognizer]
  val microphone = cm.lookup("microphone").asInstanceOf[Microphone]
  recognizer.allocate
  
  startMicRecordingOrDie(recognizer, microphone)

  // i'm passing in nulls here because i don't know a better way to make 'brain'
  // and 'ears' available to me in setter methods. these two objects need to know about
  // each other.
  val brain = new Brain(this, microphone, recognizer, null)
  val ears  = new Ears(microphone, recognizer, null)
  val earBrainIntermediary = new EarBrainIntermediary(brain, ears)
  
  brain.earBrainIntermediary = earBrainIntermediary
  ears.earBrainIntermediary = earBrainIntermediary

  def startRunning {
    // start the actor threads
    earBrainIntermediary.start
    brain.start
    ears.start
  
    // TODO use reflection to load actors like this
    val currentTimeActor = new CurrentTimeActor(brain); currentTimeActor.start
    
    ears ! "Hello, Al"
  }

  // TODO get this code to work properly. System.exit isn't really exiting.
  //      how do i properly stop Scala Actors?
  //      also, i should "join" the threads to make sure they die.
  def shutdown {
    println("Shutting down.")
    earBrainIntermediary ! Die
    brain  ! Die
    //ears  ! Die
    Utils.sleep(1000)
    System.exit(0)
  }
  
  def startMicRecordingOrDie(recognizer: Recognizer, microphone: Microphone) {
    if (!microphone.startRecording()) {
      println("Cannot start the microphone, aborting.");
      recognizer.deallocate();
      System.exit(1)
    }
  }
    
}
 





