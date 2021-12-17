package it.units.erallab.hmsrobots.core.snapshots;

import it.units.erallab.hmsrobots.MyKeyListener;
import it.units.erallab.hmsrobots.core.controllers.BasicInteractiveController;
import it.units.erallab.hmsrobots.viewers.FramesImageBuilder;
import it.units.erallab.hmsrobots.viewers.drawers.Drawer;
import org.apache.commons.lang3.time.StopWatch;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferStrategy;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class InteractiveSnapshotListener extends JFrame implements SnapshotListener, KeyListener {
  private final Drawer drawer;
  private final double dT;
  private final BasicInteractiveController controller;

  private final Canvas canvas;
  private StopWatch stopWatch;
  private double lastDrawT;

  // Ottimizzazione: FrameT che indica ogni quanti frame vogliamo disegnare

  private final static int FRAME_RATE = 30;
  private static final Logger L = Logger.getLogger(FramesImageBuilder.class.getName());
  private final static int INIT_WIN_WIDTH = 400;
  private final static int INIT_WIN_HEIGHT = 300;


  public InteractiveSnapshotListener(double dT, Drawer drawer, BasicInteractiveController controller) {
    this.dT = dT;
    this.drawer = drawer;
    this.controller = controller;

    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    Dimension dimension = new Dimension(INIT_WIN_WIDTH, INIT_WIN_HEIGHT);
    canvas = new Canvas();
    canvas.setPreferredSize(dimension);
    canvas.setMinimumSize(dimension);
    canvas.setMaximumSize(dimension);
    getContentPane().add(canvas, BorderLayout.CENTER);
    //pack
    pack();
    setVisible(true);
    canvas.setIgnoreRepaint(true);
    canvas.createBufferStrategy(2);

    addKeyListener(this);
  }

  @Override
  public void listen(double simT, Snapshot s) {

    if (stopWatch == null) {
      stopWatch = StopWatch.createStarted();
    }

    double realT = stopWatch.getTime(TimeUnit.MILLISECONDS) / 1000d;
    double frameDT = (1.0/FRAME_RATE);
    //System.out.println(simT + " " + (realT));

    if (lastDrawT == 0.0d || lastDrawT + frameDT <= realT){
      lastDrawT = realT;
      // Draw
      Graphics2D g = (Graphics2D) canvas.getBufferStrategy().getDrawGraphics();
      g.setClip(0, 0, canvas.getWidth(), canvas.getHeight());
      drawer.draw(simT, s, g);
      g.dispose();
      BufferStrategy strategy = canvas.getBufferStrategy();
      if (!strategy.contentsLost()) {
        strategy.show();
      }
      Toolkit.getDefaultToolkit().sync();
    }



    // Wait
    realT = stopWatch.getTime(TimeUnit.MILLISECONDS) / 1000d;
    long waitMillis = Math.max(Math.round((simT + dT - realT)*1000d), 0);
    if (waitMillis > 0) {
      synchronized (this) {
        //System.out.println(waitMillis);
        try {
          wait(waitMillis);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
  }

  @Override
  public void keyTyped(KeyEvent e) {

  }

  //@Override
  //public void keyPressed(KeyEvent e) {controller.setKeyPressed(true);}
  @Override
  public void keyPressed(KeyEvent e) {
    switch(e.getKeyCode()){
      case KeyEvent.VK_UP:
        controller.setKeyPressed(true, 2);
        break;
      case KeyEvent.VK_DOWN:
        controller.setKeyPressed(true, 3);
        break;
      case KeyEvent.VK_LEFT:
        controller.setKeyPressed(true, 0);
        break;
      case KeyEvent.VK_RIGHT:
        controller.setKeyPressed(true, 1);
        break;
      default:
        System.out.println("key pressed: not an arrow");
        break;
    }
  }

  //@Override
  //public void keyReleased(KeyEvent e) {controller.setKeyPressed(false);}

  @Override
  public void keyReleased(KeyEvent e) {
    switch(e.getKeyCode()){
      case KeyEvent.VK_UP:
        controller.setKeyPressed(false, 2);
        break;
      case KeyEvent.VK_DOWN:
        controller.setKeyPressed(false, 3);
        break;
      case KeyEvent.VK_LEFT:
        controller.setKeyPressed(false, 0);
        break;
      case KeyEvent.VK_RIGHT:
        controller.setKeyPressed(false, 1);
        break;
      default:
        System.out.println("key released: not an arrow");
        break;
    }
  }
}
