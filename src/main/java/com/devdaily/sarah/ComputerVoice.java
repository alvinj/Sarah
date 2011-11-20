package com.devdaily.sarah;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class ComputerVoice
{
  // most human
  public static final String ALEX       = "ALEX";

  // good female voices
  public static final String AGNES       = "AGNES";
  public static final String KATHY       = "KATHY";
  public static final String PRINCESS    = "PRINESS";
  public static final String VICKI       = "VICKI";        // also good
  public static final String VICTORIA    = "VICTORIA";

  // good male voices
  public static final String BRUCE       = "BRUCE";
  public static final String FRED        = "FRED";
  public static final String JUNIOR      = "JUNIOR";
  public static final String RALPH       = "RALPH";

  // other voices
  public static final String ALBERT      = "ALBERT";
  public static final String BAD_NEWS    = "BAD NEWS";
  public static final String BAHH        = "BAHH";
  public static final String BELLS       = "BELLS";
  public static final String BOING       = "BOING";
  public static final String BUBBLES     = "BUBBLES";
  public static final String CELLOS      = "CELLOS";
  public static final String DERANGED    = "DERANGED";
  public static final String GOOD_NEWS   = "GOOD NEWS";
  public static final String HYSTERICAL  = "HYSTERICAL";
  public static final String PIPE_ORGAN  = "PIPE ORGAN";
  public static final String TRINOIDS    = "TRINOIDS";
  public static final String WHISPER     = "WHISPER";
  public static final String ZARVOX      = "ZARVOX";
  
  private static final String[] VOICES = {ALEX, AGNES, KATHY, PRINCESS, VICKI, VICTORIA, BRUCE, FRED, JUNIOR,
                                          RALPH, ALBERT, BAD_NEWS, BAHH, BELLS, BOING, BUBBLES, CELLOS, DERANGED,
                                          GOOD_NEWS, HYSTERICAL, PIPE_ORGAN, TRINOIDS, WHISPER, ZARVOX};
  
//  private static final String[] YES_MASTER = {"Yes, master", 
//                                              "At your command",
//                                              "Okey dokey", 
//                                              "You bet ya",
//                                              "You got it" };
//  
//  public static String getYesMaster()
//  {
//    Random r = new Random();
//    int n = r.nextInt(YES_MASTER.length-1);
//    return YES_MASTER[n];
//  }
  
  public static boolean isValidVoice(String desiredVoice)
  {
    for (String s : VOICES)
    {
      if (s.equals(desiredVoice)) return true;
    }
    return false;
  }

  public static void speakHal(String sentence)
  {
    String thingToSay = "say \"" + sentence + "\" using \"Bruce\" speaking rate 138 modulation 18 pitch 41";
    ScriptEngineManager mgr = new ScriptEngineManager();
    ScriptEngine engine = mgr.getEngineByName("AppleScript");
    try
    {
      engine.eval(thingToSay);
    }
    catch (ScriptException e)
    {
      // TODO deal with this properly
    }
  }

  
  public static void speak(String sentence)
  {
    String thingToSay = "say \"" + sentence + "\"";
    ScriptEngineManager mgr = new ScriptEngineManager();
    ScriptEngine engine = mgr.getEngineByName("AppleScript");
    try
    {
      engine.eval(thingToSay);
    }
    catch (ScriptException e)
    {
      // TODO deal with this properly
    }
  }

  public static void speak(String sentence, String voice)
  {
    String thingToSay = "say \"" + sentence + "\"" + "using \"" + voice + "\"";
    ScriptEngineManager mgr = new ScriptEngineManager();
    ScriptEngine engine = mgr.getEngineByName("AppleScript");
    try
    {
      engine.eval(thingToSay);
    }
    catch (ScriptException e)
    {
      // TODO deal with this properly
    }
  }

}
