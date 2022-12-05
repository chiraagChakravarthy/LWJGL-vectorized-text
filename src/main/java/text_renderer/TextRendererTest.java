package text_renderer;

import gl.*;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTTVertex;

import java.awt.*;
import java.io.IOException;
import java.util.Vector;

import static font_test.FileUtil.loadFont;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.glGetIntegeri;
import static org.lwjgl.opengl.GL31.GL_MAX_FRAGMENT_UNIFORM_BLOCKS;
import static org.lwjgl.opengl.GL31.GL_MAX_UNIFORM_BLOCK_SIZE;
import static org.lwjgl.opengl.GL43C.GL_MAX_SHADER_STORAGE_BLOCK_SIZE;
import static org.lwjgl.stb.STBTruetype.stbtt_GetGlyphBox;
import static org.lwjgl.stb.STBTruetype.stbtt_GetGlyphShape;
import static org.lwjgl.system.MemoryUtil.NULL;

public class TextRendererTest {
    private final int WIDTH = 1200;
    private final int HEIGHT = 678;
    private long window;

    public void run() {
        makeWindow();

        int[] out = new int[1];
        glGetIntegerv(GL_MAX_FRAGMENT_UNIFORM_BLOCKS, out);
        System.out.println(out[0]);

        TextRenderer.init();
        TextRenderer.setViewport(WIDTH, HEIGHT);
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
        //GL.create();
        System.out.println("Using GL Version: " + glGetString(GL_VERSION));

        // Open a window and create its OpenGL context
        GLFWVidMode vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        glfwSetWindowPos(window, (vidMode.width() - WIDTH) / 2, (vidMode.height() - HEIGHT) / 2);
        //glfwSetKeyCallback(window, new KeyInput()); // will use other key systems

        glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        glfwShowWindow(window);
    }

    private void render() {
        VectorFont font;
        try {
            font = new VectorFont("/font/arial.ttf", 100, 0);
        } catch (IOException | FontFormatException e) {
            throw new RuntimeException(e);
        }

        int text = TextRenderer.genTextVao(font, 100, 100, "faggot");
        do {
            fps();
            glClear(GL_COLOR_BUFFER_BIT);
            TextRenderer.drawText(font, text);
            glfwSwapBuffers(window); // Update Window
            glfwPollEvents(); // Key Mouse Input
        } while (glfwGetKey(window, GLFW_KEY_ESCAPE) != GLFW_PRESS && !glfwWindowShouldClose(window));
        glfwTerminate();
    }

    long time;
    public void fps() {
        long now = System.nanoTime();
        System.out.println((now - time) / 1000000f);
        time = now;
    }

    public static void main(String[] args) throws InterruptedException {
        new TextRendererTest().run();
    }
}
