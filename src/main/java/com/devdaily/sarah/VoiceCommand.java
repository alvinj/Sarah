package com.devdaily.sarah;

import java.util.List;

/**
 * This class was created to mimic the current needs for standard,
 * well-known voice commands. The format of my 'commands' file
 * currently looks like this:
 * 
 * ITUNES_PLAY : tell application "iTunes" to play
 * ITUNES_PAUSE : tell application "iTunes" to pause
 */
public class VoiceCommand
{
  String commandLabel;
  String appleScript;
  List<String> possibleUserPhrases;    // "next track", "next station", "next song"
  private boolean worksInSleepMode;
  
  public VoiceCommand(String commandLabel, String action, boolean worksInQuietMode)
  {
    this.commandLabel = commandLabel;
    this.appleScript = action;
    this.worksInSleepMode = worksInQuietMode;
  }
  public boolean worksInSleepMode()
  {
    return worksInSleepMode;
  }
  public void setWorksInSleepMode(boolean worksInSleepMode)
  {
    this.worksInSleepMode = worksInSleepMode;
  }
  public void clearPossibleUserPhrase()
  {
    possibleUserPhrases.clear();
  }
  public void addPossibleUserPhrase(String phrase)
  {
    possibleUserPhrases.add(phrase);
  }
  public boolean matches(String text)
  {
    return possibleUserPhrases.contains(text);
  }
  public String getCommand()
  {
    return commandLabel;
  }
  public void setCommand(String command)
  {
    this.commandLabel = command;
  }
  public String getAppleScript()
  {
    return appleScript;
  }
  public void setAppleScript(String appleScript)
  {
    this.appleScript = appleScript;
  }
}
