package gl;

import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class GlTest {
    long window;
    int WIDTH = 800, HEIGHT = 500;
    public GlTest(){
        makeWindow();
    }
    public static void main(String[] args) {
        GlTest test = new GlTest();
        test.run();
    }

    private void run() {
        Shader pass1S = new Shader("/shader/gl_test/pass1.vert", "/shader/gl_test/pass1.frag");
        Shader pass2S = new Shader("/shader/gl_test/pass2.vert", "/shader/gl_test/pass2.frag");

        int fbo = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);

        int pass1Out = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, pass1Out);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, 2, 1, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, pass1Out, 0);
        glBindTexture(GL_TEXTURE_2D, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        /*float[] vertex1 = new float[]{
                -0.5f, -0.5f,
                0.5f, -0.5f,
                0.5f, 0.5f,
                -0.5f, 0.5f
        };*/

        float[] vertex1 = new float[]{
                -1, -1,
                1, -1,
                1, 1,
                -1, 1
        };

        float[] vertex2 = new float[]{
                -1, -1, 0, 0,
                -1, 1, 0, 1,
                1, 1, 1, 1,
                1, -1, 1, 0
        };

        int[] index = new int[]{
                0, 1, 2,
                2, 3, 0
        };

        int vao1 = glGenVertexArrays();
        glBindVertexArray(vao1);

        int vbo1 = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo1);
        glBufferData(GL_ARRAY_BUFFER, vertex1, GL_STATIC_DRAW);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 8, 0);

        int ibo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, index, GL_STATIC_DRAW);

        int vao2 = glGenVertexArrays();
        glBindVertexArray(vao2);

        int vbo2 = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo2);
        glBufferData(GL_ARRAY_BUFFER, vertex2, GL_STATIC_DRAW);
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 16, 0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 16, 8);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo);
        glBindVertexArray(0);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        Matrix4f proj = new Matrix4f();
        proj.ortho(-1.0f, 1.0f, -1.0f, 1.0f, -1.0f, 1.0f);

        Matrix4f view = new Matrix4f();

        Matrix4f model = new Matrix4f();

        Matrix4f mvp = proj.mul(view).mul(model);

        pass1S.bind();
        pass1S.setUniformMat4f("u_MVP", mvp);
        pass2S.bind();
        pass2S.setUniformMat4f("u_MVP", mvp);

        float u_k = -1;

        do {
            u_k += .005;
            u_k = ((u_k+1)%2)-1;
            glBindFramebuffer(GL_FRAMEBUFFER, fbo);
            glBindVertexArray(vao1);
            glClear(GL_COLOR_BUFFER_BIT);
            pass1S.bind();
            pass1S.setUniform1f("u_k", Math.abs(u_k)*2-1);
            glViewport(0, 0, 2, 1);

            glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);

            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, pass1Out);

            pass2S.bind();
            pass2S.setUniform1i("uTexture", 0);

            //pass2S.setUniform1f("u_k", u_k);
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
            glClear(GL_COLOR_BUFFER_BIT);
            glBindVertexArray(vao2);
            glViewport(0, 0, WIDTH, HEIGHT);
            glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
            glBindTexture(GL_TEXTURE_2D, 0);

            glfwSwapBuffers(window);
            glfwPollEvents();
        } while (glfwGetKey(window, GLFW_KEY_ESCAPE) != GLFW_PRESS && !glfwWindowShouldClose(window));
        glfwTerminate();
    }

    private void makeWindow() {
        if (!glfwInit()) {
            throw new RuntimeException("Failed to initialize GLFW");
        }
        System.setProperty("java.awt.headless", "true");
        glfwWindowHint(GLFW_SAMPLES, 4);
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

        // Open a window and create its OpenGL context
        GLFWVidMode vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        glfwSetWindowPos(window, (vidMode.width() - WIDTH) / 2, (vidMode.height() - HEIGHT) / 2);
        //glfwSetKeyCallback(window, new KeyInput()); // will use other key systems

        glClearColor(1, 1, 1, 1);
        glfwShowWindow(window);
    }
}
