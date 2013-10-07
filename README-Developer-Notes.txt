
SARAH Concepts
==============

Notes for SARAH programmers, and SARAH plugin programmers.

Sarah
-----

The class Sarah starts things kicks things off, reads any necessary configuration 
files (future), loads and starts the plugins,  

Ears
----

The role of the ears is to listen to the user, and pass whatever they 
think the user said to the Brain.


Brain
-----

My main reason for writing this document today is to make these notes about
the Brain class:

* The Brain should be able to respond to multiple requests as
  fast as possible. As a result, it has been implemented as an Actor.

* The Brain needs a way to tell the mouth to stop speaking, that is, to
  interrupt it as needed. This can be because a critical message has come
  in, or because the user wants Sarah to quit speaking. The user may
  say this, or press a button in the UI (future) to get Sarah to stop
  speaking.
   
Mouth
-----

I used to have a class named Mouth, and may return to that design.
An important concept is that the mouth can only say one thing at a time.

Plugins
-------

* Always call the brain on a thread, such as through an Akka Future:
  
  implicit val actorSystem = ActorSystem("CurrentTimeActorSystem")
  val f = Future { brain ! PleaseSay("The current time is " + currentTime) }

  Calls not made in this way seem to have a slightly higher priority, as if they're
  made on the main Java thread, and it has a higher priority. For example, this is a
  problem if you want to say "Stand by", and then speak your results a few moments
  later, presumably after you've retrieved them from the internet.

  This approach requires the following import statements:
  
  import akka.dispatch.{ Await, Future }
  import akka.util.duration._
  import akka.actor.ActorSystem


Scala/Dispatch OAuth
--------------------

Dispatch OAuth examples:

   * https://gist.github.com/437512
   * http://richard.dallaway.com/databinder-dispatch-for-http-services

The URL that got me started onto Dispatch for a REST client:

   * http://aloiscochard.blogspot.com/2011/05/simple-rest-web-service-client-in-scala.html

The Dispatch URL:

   * http://dispatch.databinder.net/


Required Jar Files
------------------

Until I get this all working with SBT, here are notes on required jar files.

For JavaMail:

   * dsn
   * imap
   * mailapi
   * pop3
   * smtp

For Lift-JSON:

   * lift-json*
   * paranamer


