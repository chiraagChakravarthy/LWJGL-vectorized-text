package test.alg_test;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferStrategy;

public class Main extends Canvas implements Runnable, KeyListener, MouseListener, MouseMotionListener {
    private static Main instance;

    public static final String TITLE = "Path Tracer";
    //This is the main class. The entire game is ran through this single class.
    private static final long serialVersionUID = 1L;
    public static Window window;
    private static Thread thread;
    private boolean running;
    private static long runningTime = 0;
    public static final boolean[] pressedKeys;

    public static final boolean[] pressedMouse;

    public boolean forceTps = false;

    double mspt;



    static {
        pressedKeys = new boolean[Character.MAX_VALUE];

        pressedMouse = new boolean[3];
    }

    private Main() {
        window = new Window(TITLE, 500, 600, this);
        addKeyListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);
        requestFocus();
        running = true;
        thread = new Thread(this);
        mspt = 0;
        thread.start();
    }

    public static void main(String[] args) {
        instance = new Main();
    }

    private GlyphRenderer4 test;
    private void start(){
        test = new GlyphRenderer4(this);
    }

    public void run() {
        long lastTime = System.nanoTime();
        long timer = System.currentTimeMillis();
        int ticks = 30;
        double ns = 1000000000d / ticks;
        double delta = 0;
        double totalMs = 0;
        int frames = 0;
        int updates = 0;
        //Allows for the logging of the ticks and frames each second
        //Game Loop\\
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        start();
        while (running){
        //Boolean which controls the running of the game loop. Were it to equal false, the game would simply freeze.
            /////////////////////////////
            long now = System.nanoTime();
            delta += (now - lastTime) / ns;
            lastTime = now;
            while (delta > 1) {
                long tickStart = System.nanoTime();
                tick();
                totalMs += ((double)(System.nanoTime()-tickStart))/1000000d;
                updates++;
                delta--;
                frames++;
                if(!forceTps){
                    delta = 0;
                }
            }
            render();
            /////////////////////////////
            //A tick is the game's equivalent of an instant. The code above allows time to be constant in a loop that varies
            //in the length of each iteration

            if (System.currentTimeMillis() - timer >= 1000) {
                double mspt = Math.round(totalMs / updates * 100) / 100d;
                System.out.println("Frames: " + frames + ", Ticks: " + updates + ", MSPT: " + mspt);
                this.mspt = mspt;
                updates = 0;
                frames = 0;
                timer += 1000;
                totalMs = 0;
                //Logs the Frames and the ticks that have passed since the last logging. The minimum time between each
                //logging is a second. (The max being however long the tick and drawTo take to process), so the actual
                //message being logged is a tad misleading
            }
        }
        //Game Loop\\
        stop();
    }

    private void stop() {
        //A method to stop the game loop and kill the game thread.
        running = false;
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void tick() {
        mouseDelta.setLocation(cumulativeMouseDelta);
        cumulativeMouseDelta.setLocation(0, 0);
        test.tick();
        runningTime++;
    }

    public void render() {
        BufferStrategy bs = getBufferStrategy();
        //Instead of drawing directly to the canvas, drawing to a buffer strategy allows for a concept called triple buffering
        if (bs == null) {
            createBufferStrategy(3);
            //triple buffering in the long term greatly increases performance. Instead of replacing every pixel, triple buffering
            //only changes the pixels that weren't present before. It also searches for and remembers patterns in a single runtime
            //iteration.
            return;
        }
        Graphics2D g = (Graphics2D)bs.getDrawGraphics();
        g.setColor(Color.DARK_GRAY);
        g.fillRect(0, 0, window.getWidth(), window.getHeight());
        test.render(g);
        g.dispose();
        bs.show();
    }

    public void keyTyped(KeyEvent e) {
        //Irrelevant to program
    }

    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        pressedKeys[k] = true;
    }

    public void keyReleased(KeyEvent e) {
        int k = e.getKeyCode();
        pressedKeys[k] = false;
        //k is an integer representing the key that was released

    }

    public void mouseClicked(MouseEvent e) {
        //Irrelevant to program
        if(e.getButton()==0){
            test.glyph++;
        }
        if(e.getButton()==1){
            test.glyph--;
        }
    }

    public void mousePressed(MouseEvent e) {
        int b = e.getButton();
        if(b<3){
            pressedMouse[b] = true;
        }
        //The mouse event itself is passed because it contains information of the mouse button pressed and the location of the mouse

    }

    public void mouseReleased(MouseEvent e) {
        int b = e.getButton();
        if(b<3){
            pressedMouse[b] = false;
        }
        //The mouse event itself is passed because it contains information of the mouse button released and the location of the mouse

    }

    public void mouseEntered(MouseEvent e) {
        //Irrelevant to program
    }

    public void mouseExited(MouseEvent e) {
        //Irrelevant to program
    }

    public static long getRunningTime() {
        return runningTime;
    }

    public int width(){
        return window.getWidth();
    }

    public int height(){
        return window.getHeight();
    }

    public static Main getInstance() {
        return instance;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseMoved(e);
    }


    public final Point mouseLocation = new Point(0, 0);
    private boolean recentering = false;
    private final Point center = new Point(0, 0);

    private final Point cumulativeMouseDelta = new Point(0, 0);
    public final Point mouseDelta = new Point(0, 0);

    @Override
    public synchronized void mouseMoved(MouseEvent e) {
        mouseLocation.x = e.getX();
        mouseLocation.y = e.getY();
    }
}