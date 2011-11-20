package com.devdaily.sarah;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;

import edu.cmu.sphinx.frontend.util.Microphone;
import edu.cmu.sphinx.jsgf.JSGFGrammar;
import edu.cmu.sphinx.linguist.dictionary.Word;
import edu.cmu.sphinx.linguist.language.grammar.GrammarArc;
import edu.cmu.sphinx.linguist.language.grammar.GrammarNode;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.ConfigurationManager;

public class SarahJavaHelper {
  
  private static final String SLEEP_MODE_TAG = "sleepmode";

  public static void loadAllSentenceToCommandMaps(String[] phraseCommandMapFiles, 
      String separator,
      String currentDirectory,
      HashMap<String, String> phraseToCommandMap)
  {
    for (String commandMapFile : phraseCommandMapFiles)
    {
      String canonicalFilename = currentDirectory + File.separator + commandMapFile;
      try
      {
        List<String> lines = readFile(canonicalFilename);
        for (String s: lines)
        {
          // bust each string into a command and an action
          StringTokenizer st = new StringTokenizer(s, separator);
          String commandLabel = st.nextToken().trim();
          String phrase = st.nextToken().trim();
          phraseToCommandMap.put(phrase, commandLabel);
        }
      }
      catch (NoSuchElementException nsee)
      {
        System.err.println("Error trying to read sentence/appleScriptKey map files.");
        nsee.printStackTrace();
      }
      catch (IOException e)
      {
        System.err.println("Error trying to read sentence/appleScriptKey map files.");
        e.printStackTrace();
      }
    }
  }
  

  /**
   * Open and read a file, and return the lines in the file as a list
   * of Strings.
   * (Demonstrates Java FileReader, BufferedReader, and Java5.)
   * @throws Exception 
   */
  private static List<String> readFile(String filename) 
  throws IOException
  {
    List<String> records = new ArrayList<String>();
    try
    {
      BufferedReader reader = new BufferedReader(new FileReader(filename));
      String line;
      while ((line = reader.readLine()) != null)
      {
        // TODO test this
        if (!line.trim().equals("") && !line.startsWith("#"))
        {
          records.add(line);
        }
      }
      reader.close();
      return records;
    }
    catch (IOException e)
    {
      // log this as desired
      throw e;
    }
  }

  
  /**
   * Get a list of files in the given directory that match the given
   * filename extension ("dat", "txt").
   * 
   * For my purposes, get the CWD and pass it to this method.
   */
  public static String[] getAllFilenames(final String directory, final String filenameExtension)
  {
    File theDir = new File(directory);
    return theDir.list(new MyFileFilter(filenameExtension));

  }
  
  public static List<VoiceCommand> getCurrentVoiceCommands(String canonicalFilename, String separator)
  throws IOException
  {
    List<VoiceCommand> standardVoiceCommands = new ArrayList<VoiceCommand>();
    try
    {
      // read the lines from the file
      List<String> lines = readFile(canonicalFilename);

      // break the lines into commands
      for (String s : lines)
      {
        // bust each string into a command and an action
        StringTokenizer st = new StringTokenizer(s, separator);
        String commandLabel = st.nextToken().trim();
        String appleScript = st.nextToken().trim();
        // let the user optionally specify whether the command can work in quiet mode
        boolean worksInQuietMode = false;
        if (st.hasMoreTokens())
        {
          String mode = st.nextToken().trim();
          if (mode.equalsIgnoreCase(SLEEP_MODE_TAG)) worksInQuietMode = true;
        }
        VoiceCommand vc = new VoiceCommand(commandLabel, appleScript, worksInQuietMode);
        standardVoiceCommands.add(vc);
      }
    }
    catch (NoSuchElementException nsee)
    {
      System.err.println("Error trying to read '.commands' files.");
      nsee.printStackTrace();
    }
    catch (IOException ioe)
    {
      System.err.println("Error trying to read '.commands' files.");
      throw ioe;
    }
    return standardVoiceCommands;
  }
  
  
  /**
   * 
   * NOT TESTED YET
   * 
   * Get the Grammar to Command mapping from the user's grammar to 
   * command mapping file.
   * @throws IOException 
   */
  public static Map<String,String> getGrammarToCommandMapping(String g2cFile)
  throws IOException
  {
    Map<String,String> g2cMap = new HashMap<String,String>();
    
    // read the lines from the file
    List<String> lines = readFile(g2cFile);

    // break the lines into commands
    for (String s: lines)
    {
      // bust each string into a command and an action
      StringTokenizer st = new StringTokenizer(s, ":");
      String grammarLabel = st.nextToken().trim();
      String commandLabel = st.nextToken().trim();
      // TODO mismatch here, so i've just made 'mode' always false; fix this
      VoiceCommand vc = new VoiceCommand(grammarLabel, commandLabel, false);
      g2cMap.put(grammarLabel, commandLabel);
    }
    return g2cMap;
  }

  
  /**
   * Returns a random sentence that fits this grammar
   */
  public static String getRandomSentence(final JSGFGrammar jsgfGrammar, Random randomizer)
  {
    StringBuilder sb = new StringBuilder();
    GrammarNode node = jsgfGrammar.getInitialNode();
    while (!node.isFinalNode())
    {
      if (!node.isEmpty())
      {
        Word word = node.getWord();
        if (!word.isFiller())
          sb.append(word.getSpelling()).append(' ');
      }
      node = selectRandomSuccessor(node, randomizer);
    }
    return sb.toString().trim();
  }

  private static GrammarNode selectRandomSuccessor(GrammarNode node, Random randomizer)
  {
    GrammarArc[] arcs = node.getSuccessors();

    // select a transition arc with respect to the arc-probabilities (which are
    // log and we don't have a logMath here
    // which makes the implementation a little bit messy
    if (arcs.length > 1)
    {
      double[] linWeights = new double[arcs.length];
      double linWeightsSum = 0;

      final double EPS = 1E-10;

      for (int i = 0; i < linWeights.length; i++)
      {
        linWeights[i] = (arcs[0].getProbability() + EPS) / (arcs[i].getProbability() + EPS);
        linWeightsSum += linWeights[i];
      }

      for (int i = 0; i < linWeights.length; i++)
      {
        linWeights[i] /= linWeightsSum;
      }

      double selIndex = randomizer.nextDouble();
      int index = 0;
      for (int i = 0; selIndex > EPS; i++)
      {
        index = i;
        selIndex -= linWeights[i];
      }

      return arcs[index].getGrammarNode();

    }
    else
    {
      return arcs[0].getGrammarNode();
    }
  }  
  

  
  /**
   * Taken from the Grammar.java file of the CMU project. I couldn't figure out
   * how to get all the possible sentences any other way (easily), so I copied
   * and modified this code.
   */
  public static void getAllPossibleSentences(int count, 
      List<String> allPossibleSentences, 
      final JSGFGrammar jsgfGrammar,
      Random randomizer)
  {
    Set<String> set = new HashSet<String>();
    for (int i = 0; i < count; i++)
    {
      String s = getRandomSentence(jsgfGrammar, randomizer);
      if (!set.contains(s))
      {
        set.add(s);
      }
    }
    
    allPossibleSentences = new ArrayList<String>(set);
    Collections.sort(allPossibleSentences);
  }

  
  public static void dumpAllPossibleSentences(List<String> allPossibleSentences) {
    for (String sentence : allPossibleSentences) {
      System.out.println(sentence);
    }
  }
  
} // end class

class MyFileFilter implements FilenameFilter
{
  private String filenameExtension;
  public MyFileFilter(String filenameExtension)
  {
    this.filenameExtension = filenameExtension;
  }
  public boolean accept(File file, String s)
  {
    //System.out.format("FILE: %s,  STRING: %s\n", file, s);
    //if (file.isDirectory()) return false;
    if (s.endsWith("." + filenameExtension)) return true;
    return false;
  }
}






