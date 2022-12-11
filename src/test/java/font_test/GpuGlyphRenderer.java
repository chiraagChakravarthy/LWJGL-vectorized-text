package font_test;

import gl.*;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTTVertex;
import text_renderer.Shader;

import java.io.IOException;

import static text_renderer.FileUtil.loadFont;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.stb.STBTruetype.stbtt_GetCodepointBox;
import static org.lwjgl.stb.STBTruetype.stbtt_GetCodepointShape;
import static org.lwjgl.system.MemoryUtil.NULL;

public class GpuGlyphRenderer {
    private final int WIDTH = 1200;
    private final int HEIGHT = 678;

    private long window;
    private text_renderer.Shader shader;
    private VertexArray va;
    private IndexBuffer ib;
    private STBTTFontinfo fontinfo;

    private float[] atlas;//beziers coefficients
    private float x0, y0, x1, y1;//bounds of glyph in glyph space
    private int count;//number of beziers in the glyph
    private float pixelSize = 1;//how many glyph space units per screen pixel
    private float gx=101f, gy=101f;//pixel space coordinates of the origin of the glyph

    public void run() {
        makeWindow();
        int[] viewport = new int[2];
        glGetIntegerv(GL_VIEWPORT, viewport);
        int width = viewport[0];
        int height = viewport[1];

        try {
            fontinfo = loadFont("/font/arial.ttf");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        initGlyph('/');

        vertex();
        setupShaders();
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

        // Open a window and create its OpenGL context
        GLFWVidMode vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        glfwSetWindowPos(window, (vidMode.width() - WIDTH) / 2, (vidMode.height() - HEIGHT) / 2);
        //glfwSetKeyCallback(window, new KeyInput()); // will use other key systems

        glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        glfwShowWindow(window);
    }

    void initGlyph(int glyph) {
        int[] x0 = new int[1], x1 = new int[1], y0 = new int[1], y1 = new int[1];
        stbtt_GetCodepointBox(fontinfo, glyph, x0, y0, x1, y1);
        this.x0 = x0[0];
        this.x1 = x1[0];
        this.y0 = y0[0];
        this.y1 = y1[0];
        STBTTVertex.Buffer vertices = stbtt_GetCodepointShape(fontinfo, glyph);
        if(vertices!=null){
            float[] atlas = this.atlas = new float[400];
            count = vertices.remaining();
            int j = 0;

            for (int i = count-1; i >=0; i--) {
                STBTTVertex vertex = vertices.get(i);
                int ax = vertex.x(), ay = vertex.y();
                byte type = vertex.type();

                float w = 0.01f;

                if(type==3){
                    int bx = vertex.cx(), by = vertex.cy();
                    STBTTVertex nextVertex = vertices.get(i-1);
                    int cx = nextVertex.x(), cy = nextVertex.y();
                    atlas[j] = (ax-2*bx+cx)*w;
                    atlas[j+1] = (2*bx-2*cx)*w;
                    atlas[j+2] = cx*w;
                    atlas[j+3] = ay-2*by+cy;
                    atlas[j+4] = 2*by-2*cy;
                    atlas[j+5] = cy;
                } else if(type==2){
                    STBTTVertex nextVertex = vertices.get(i-1);
                    int bx = nextVertex.x(), by = nextVertex.y();
                    atlas[j] = 0;
                    atlas[j+1] = (ax-bx)*w;
                    atlas[j+2] = bx*w;
                    atlas[j+3] = 0;
                    atlas[j+4] = ay-by;
                    atlas[j+5] = by;
                }
                j += 6;
            }
        }
    }

    void vertex() {
        /*
        GIVEN:
        * glyph bounds
        * pixel size
        * position of glyph origin on screen
        we want to find the screenspace coords for each corner of the glyph bounds
         */

        float gx0 = gx+this.x0/ pixelSize,
                gy0 = gy+this.y0/ pixelSize,
                gx1 = gx+this.x1/ pixelSize,
                gy1 = gy+this.y1/ pixelSize;

        float pad = 1;

        float agx0 = (float) (gx0-pad), agy0 = (float) (gy0-pad),
                agx1 = (float) ((gx1)+pad), agy1 = (float) ((gy1)+pad);

        float dx0 = (gx0-agx0)*pixelSize, dx1 = (agx1-gx1)*pixelSize,
                dy0 = (gy0 - agy0)*pixelSize, dy1 = (agy1-gy1)*pixelSize;

        float[] vertices = new float[]{
                agx0, agy0, x0-dx0, y0-dy0,//bottom left
                agx1, agy0, x1+dx1, y0-dy0,//bottom right
                agx1, agy1, x1+dx1, y1+dy1,//top right
                agx0, agy1, x0-dx0, y1+dy1,//top left
        };

        int[] indexes = new int[]{
                0, 1, 2,
                2, 3, 0
        };

        va = new VertexArray();
        VertexBuffer vb = new VertexBuffer(vertices);
        ib = new IndexBuffer(indexes);
        VertexBufferLayout layout = new VertexBufferLayout();
        layout.addFloat(2); //pixel coords
        layout.addFloat(2); //glyph coords
        va.addVertexBuffer(vb, layout);
    }

    private void setupShaders() {
        shader = new Shader("/text_test/text.vert", "/text_test/text3.frag");
    }

    private void render() {
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        Renderer renderer = new Renderer();

        Matrix4f proj = new Matrix4f();
        //proj.ortho(-1.0f * r, r, -1.0f, 1.0f, -1.0f, 1.0f);

        proj = proj.ortho(0, WIDTH, 0, HEIGHT, -1.0f, 1.0f);

        Matrix4f view = new Matrix4f();

        Matrix4f model = new Matrix4f();

        Matrix4f mvp = proj.mul(view).mul(model);
        System.out.println(mvp.toString());

        shader.setUniformMat4f("u_MVP", mvp);
        shader.setUniformFloatArray("uAtlas", atlas);
        shader.setUniform1i("uCount", count);

        do {
            shader.setUniform1f("uPixelSize", pixelSize);
            fps();
            renderer.clear();
            renderer.draw(va, ib, shader);
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
        new GpuGlyphRenderer().run();
    }
}
