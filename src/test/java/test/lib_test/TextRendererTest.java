package test.lib_test;

import io.github.chiraagchakravarthy.lwjgl_vectorized_text.TextRenderer;
import io.github.chiraagchakravarthy.lwjgl_vectorized_text.VectorFont;
import org.joml.Matrix4f;
import org.joml.Random;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class TextRendererTest {
    private final int WIDTH = 1000;
    private final int HEIGHT = 700;
    private long window;

    public void run() {
        makeWindow();
        render();
    }

    private void makeWindow() {
        if (!glfwInit()) {
            throw new RuntimeException("Failed to initialize GLFW");
        }
        System.setProperty("java.awt.headless", "true");
        glfwWindowHint(GLFW_SAMPLES, 1);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE); // To make MacOS happy; should not be needed
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        window = glfwCreateWindow(WIDTH, HEIGHT, "OpenGL window", NULL, NULL);
        if (window == NULL) {
            glfwTerminate();
            throw new RuntimeException("Failed to open GLFW window. If you have an Intel GPU, they are not 3.3 compatible. Try the 2.1 version of the tutorials.");
        }
        glfwMakeContextCurrent(window);
        GL.createCapabilities();
        System.out.println("Using GL Version: " + glGetString(GL_VERSION));
        //glfwSwapInterval(0);
        // Open a window and create its OpenGL context
        GLFWVidMode vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        glfwSetWindowPos(window, (vidMode.width() - WIDTH) / 2, (vidMode.height() - HEIGHT) / 2);
        //glfwSetKeyCallback(window, new KeyInput()); // will use other key systems

        glClearColor(1,1,1, 1.0f);
        glfwShowWindow(window);
    }

    private void render() {
        //VectorFont font = new VectorFont("/font/ariblk.ttf");

        //VectorFont font = new VectorFont(shapes, new char[]{'o'}, 100);
        VectorFont font = new VectorFont("/font/arial.ttf", VectorFont.DEFAULT);

        Vector4f color = new Vector4f(0,0,0,1);

        TextRenderer completelyUnnecessaryObject = new TextRenderer(new Matrix4f().ortho(0, WIDTH, 0, HEIGHT, -1000, 1000));
        Matrix4f pose = new Matrix4f().translate(WIDTH/2f, HEIGHT/2f, 0).scale(500);
        do {
            pose.scale(1.001f);
            //pose.rotate(.05f, 0.5773502692f, 0.5773502692f, 0.5773502692f);
            fps();
            glClear(GL_COLOR_BUFFER_BIT);
            completelyUnnecessaryObject.drawTextAligned("e", pose, new Vector2f(0, 0), TextRenderer.TextBoundType.BOUNDING_BOX, font, color);
            glfwSwapBuffers(window); // Update Window
            glfwPollEvents(); // Key Mouse Input
        } while (glfwGetKey(window, GLFW_KEY_ESCAPE) != GLFW_PRESS && !glfwWindowShouldClose(window));
        glfwTerminate();
    }
    long lastTime = -1;
    int frames;
    public void fps() {
        frames++;
        long now = System.nanoTime();
        if(lastTime == -1){
            lastTime = now;
            return;
        }
        if(now-lastTime>=1000000000) {
            System.out.println((float)(now - lastTime) / frames / 1000000f);
            lastTime += 1000000000;
            frames = 0;
        }
    }

    public static void main(String[] args) {
        new TextRendererTest().run();
    }
}