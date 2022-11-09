package font_test;

import javax.swing.*;
import java.awt.*;

public class Window {
    //Window was a custom made class that adds more functionality to a basic JFrame. Specifically the ability to easily create a fullscreen
    //window.
    private int fsm = 0;
    private JFrame frame;
    //Main window the rest of the functionality runs on
    private GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();

    public Window(String title, int fsm, Main game) {
        //This constructor takes advantage of the fullscreen functionality of the class
        this.fsm = fsm;
        frame = new JFrame(title);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(game);
        setFullScreen();
    }

    public Window(String title, int width, int height, Main game) {
        //Allows for a custom sized window.
        frame = new JFrame(title);
        frame.setTitle(title);
        /////////////////////////////////////////////////
        frame.setMinimumSize(new Dimension(width, height));
        frame.setPreferredSize(new Dimension(width, height));
        frame.setMaximumSize(new Dimension(width, height));
        /////////////////////////////////////////////////
        //Makes the window unable to be drag-resized by the user
        frame.setLocationRelativeTo(null);
        //Sets window in the middle of screen
        frame.setUndecorated(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(game);
        //Adds the game to the window, allowing whatever being drawn on Game to reflect onto the it
        frame.setVisible(true);
    }

    private void setFullScreen() {
        switch (fsm) {
            case 1:
                frame.dispose();
                frame.setSize(new Dimension(device.getDisplayMode().getWidth(), device.getDisplayMode().getHeight()));
                frame.setResizable(true);
                frame.setUndecorated(false);
                frame.setVisible(true);
                break;
            //Creates a resizable non-fullScreen window the size of the display
            case 2:
                frame.dispose();
                frame.setMinimumSize(new Dimension(device.getDisplayMode().getWidth(), device.getDisplayMode().getHeight()));
                frame.setPreferredSize(new Dimension(device.getDisplayMode().getWidth(), device.getDisplayMode().getHeight()));
                frame.setMaximumSize(new Dimension(device.getDisplayMode().getWidth(), device.getDisplayMode().getHeight()));
                frame.setUndecorated(true);
                frame.setResizable(false);
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
                break;
            //Creates a non-resizable window which takes up the entire desktop
            case 3:
                frame.dispose();
                if (device.isFullScreenSupported())
                    device.setFullScreenWindow(frame);
                else
                    System.out.println("FullScreen mode is not supported.");
                break;

            default:
                System.out.println("FullScreen mode not supported.");
                setFullScreen(1);
                //Creates a window which covers the entire display. Cannot be swiped away from. Creates best gaming experience.
        }
    }

    public void setFullScreen(int fsm) {
        this.fsm = fsm;
        setFullScreen();
    }

    public String getTitle() {
        return frame.getTitle();
    }

    public void setTitle(String title) {
        frame.setTitle(title);
    }

    public int getWidth() {
        return frame.getWidth();
    }

    public int getHeight() {
        return frame.getHeight();
    }

    public void dispose() {
        frame.dispose();
    }

    public JFrame getFrame() {
        return frame;
    }
}
