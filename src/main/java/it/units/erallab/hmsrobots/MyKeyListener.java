package it.units.erallab.hmsrobots;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class MyKeyListener implements KeyListener {

  @Override
  public void keyPressed(KeyEvent e) {
    switch(e.getKeyCode()){
      case KeyEvent.VK_UP:
        System.out.println("key pressed: UP arrow");
        break;
      case KeyEvent.VK_DOWN:
        System.out.println("key pressed: DOWN arrow");
        break;
      case KeyEvent.VK_LEFT:
        System.out.println("key pressed: LEFT arrow");
        break;
      case KeyEvent.VK_RIGHT:
        System.out.println("key pressed: RIGHT arrow");
        break;
      default:
        System.out.println("key pressed: not an arrow");
        break;
    }
  }

  @Override
  public void keyReleased(KeyEvent e) {
    System.out.println("key released");
  }

  @Override
  public void keyTyped(KeyEvent e) {
    System.out.println("key typed");
  }
}
