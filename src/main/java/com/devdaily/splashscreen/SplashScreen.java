package com.devdaily.splashscreen;

import javax.swing.*;
import java.awt.*;

public class SplashScreen extends JWindow {
  BorderLayout borderLayout1 = new BorderLayout();
  JLabel imageLabel = new JLabel();
  JPanel southPanel = new JPanel();
  FlowLayout southPanelFlowLayout = new FlowLayout();
  JProgressBar progressBar = new JProgressBar();
  ImageIcon imageIcon;

  public SplashScreen(ImageIcon imageIcon) {
    this.imageIcon = imageIcon;
    try {
      init();
    }
    catch(Exception ex) {
      ex.printStackTrace();
    }
  }

  void init() throws Exception {
    imageLabel.setIcon(imageIcon);
    this.getContentPane().setLayout(borderLayout1);
    southPanel.setLayout(southPanelFlowLayout);
    southPanel.setBackground(Color.BLACK);
    this.getContentPane().add(imageLabel, BorderLayout.CENTER);
    this.getContentPane().add(southPanel, BorderLayout.SOUTH);
    southPanel.add(progressBar, null);
    this.pack();
  }

  public void setProgressMax(int maxProgress)
  {
    progressBar.setMaximum(maxProgress);
  }

  public void setProgress(int progress)
  {
    final int theProgress = progress;
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        progressBar.setValue(theProgress);
      }
    });
  }

  public void setProgress(String message, int progress)
  {
    final int theProgress = progress;
    final String theMessage = message;
    setProgress(progress);
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        progressBar.setValue(theProgress);
        setMessage(theMessage);
      }
    });
  }

  public void setScreenVisible(boolean b)
  {
    final boolean boo = b;
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        setVisible(boo);
      }
    });
  }

  private void setMessage(String message)
  {
    if (message==null)
    {
      message = "";
      progressBar.setStringPainted(false);
    }
    else
    {
      progressBar.setStringPainted(true);
    }
    progressBar.setString(message);
  }
}