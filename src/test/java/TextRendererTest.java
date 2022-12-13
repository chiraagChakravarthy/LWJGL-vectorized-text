import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import io.github.chiraagchakravarthy.lwjgl_vectorized_text.TextRenderer;
import io.github.chiraagchakravarthy.lwjgl_vectorized_text.VectorFont;

import java.awt.*;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class TextRendererTest {
    private final int WIDTH = 1200;
    private final int HEIGHT = 678;
    private long window;

    public void run() {
        makeWindow();
        int type = glGetFramebufferAttachmentParameteri(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE);
        System.out.println(type==GL_NONE);
        System.out.println(type==GL_FRAMEBUFFER_DEFAULT);
        System.out.println(type==GL_TEXTURE);
        System.out.println(type==GL_RENDERBUFFER);
        type = glGetFramebufferAttachmentParameteri(GL_FRAMEBUFFER, GL_BACK_LEFT, GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE);
        System.out.println(type==GL_NONE);
        System.out.println(type==GL_FRAMEBUFFER_DEFAULT);
        System.out.println(type==GL_TEXTURE);
        System.out.println(type==GL_RENDERBUFFER);

        TextRenderer.init();
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

        glClearColor(1,1,1, 1.0f);
        glfwShowWindow(window);
    }

    private void render() {
        VectorFont font = new VectorFont("/font/arial.ttf", 5);
        float off = 0;

        do {
            glClear(GL_COLOR_BUFFER_BIT);
            fps();
            for (int i = 0; i < 1; i++) {
                TextRenderer.drawText("Hello", 100, 100+off, font, Color.BLACK);
            }
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