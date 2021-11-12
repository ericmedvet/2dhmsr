package it.units.erallab.hmsrobots.core.snapshots;

import it.units.erallab.hmsrobots.viewers.FramesImageBuilder;
import it.units.erallab.hmsrobots.viewers.drawers.Drawer;
import org.apache.commons.lang3.time.StopWatch;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferStrategy;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class RealTimeSnapshotListener extends JFrame implements SnapshotListener {
  private final Drawer drawer;
  private final double dT;

  private final Canvas canvas;
  private StopWatch stopWatch;

  // FrameT che indica ogni quanti frame vogliamo disegnare, ottimizzazione
  // last time (lastDrawT + FramedT < realT ) disegna oppure no e aggiorna lastT

  private static final Logger L = Logger.getLogger(FramesImageBuilder.class.getName());
  private final static int INIT_WIN_WIDTH = 400;
  private final static int INIT_WIN_HEIGHT = 300;


  public RealTimeSnapshotListener(double initialT, double finalT, double dT, FramesImageBuilder.Direction direction, int w, int h, Drawer drawer) {
    this.dT = dT;
    this.drawer = drawer;

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

  }

  @Override
  public void listen(double simT, Snapshot s) {
    if (stopWatch == null) {
      stopWatch = StopWatch.createStarted();
    }

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




    double realT = stopWatch.getTime(TimeUnit.MILLISECONDS) / 1000d;
    System.out.println(simT + " " + (realT));

    // Wait
    long waitMillis = Math.max(Math.round((simT + dT - realT)*1000d), 0);
    if (waitMillis > 0) {
      synchronized (this) {
        System.out.println(waitMillis);
        try {
          wait(waitMillis);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
  }
}