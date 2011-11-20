package com.devdaily.sarah.actors

/**
 * A wrapper class to help me identify messages.
 */
case class MessageFromEars(textFromUser: String)

/**
 * A wrapper class to help the Brain tell the Ears to listen to the user.
 */
case class MessageFromBrain(message: String)

/**
 * A helper class to request that the Brain say the given string.
 */
case class PleaseSay(textToSay: String)

/**
 * Used to send a "die" message to the actors.
 */
case class Die
