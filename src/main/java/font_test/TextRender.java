package font_test;

import gl.*;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTTVertex;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.stb.STBTruetype.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class TextRender {
    private final int WIDTH = 1024;
    private final int HEIGHT = 768;

    private long window;
    private Shader shader;
    private VertexArray va;
    private IndexBuffer ib;
    private STBTTFontinfo fontinfo;

    private float[] atlas;//beziers coefficients
    private int[] uLoops;//winding number, count, winding number, count...
    private int x0, y0, x1, y1;//bounds of glyph in glyph space
    private int count;//number of beziers in the glyph


    private float zoom = 4;//how many glyph space units per screen pixel
    private float gx=100, gy=100;//pixel space coordinates of the origin of the glyph

    public void run() {
        makeWindow();
        try {
            initFont("/font/arial.ttf");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        initGlyph(68);

        vertex();
        setupShaders();
        render();
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
        window = glfwCreateWindow(600, 900, "OpenGL window", NULL, NULL);
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
        stbtt_GetGlyphBox(fontinfo, glyph, x0, y0, x1, y1);
        this.x0 = x0[0];
        this.x1 = x1[0];
        this.y0 = y0[0];
        this.y1 = y1[0];

        STBTTVertex.Buffer vertices = stbtt_GetGlyphShape(fontinfo, glyph);
        if(vertices!=null){
            float[] atlas = this.atlas = new float[300];
            count = vertices.remaining();
            int j = 0;

            for (int i = count-1; i >=0; i--) {
                STBTTVertex vertex = vertices.get(i);
                int ax = vertex.x(), ay = vertex.y();
                byte type = vertex.type();

                if(type==3){
                    int bx = vertex.cx(), by = vertex.cy();
                    STBTTVertex nextVertex = vertices.get(i-1);
                    int cx = nextVertex.x(), cy = nextVertex.y();
                    atlas[j] = ax-2*bx+cx;
                    atlas[j+1] = 2*bx-2*cx;
                    atlas[j+2] = cx;
                    atlas[j+3] = ay-2*by+cy;
                    atlas[j+4] = 2*by-2*cy;
                    atlas[j+5] = cy;
                } else if(type==2){
                    STBTTVertex nextVertex = vertices.get(i-1);
                    int bx = nextVertex.x(), by = nextVertex.y();
                    atlas[j] = 0;
                    atlas[j+1] = ax-bx;
                    atlas[j+2] = bx;
                    atlas[j+3] = 0;
                    atlas[j+4] = ay-by;
                    atlas[j+5] = by;
                }
                j += 6;
            }
        }
    }

    private void initFont(String font) throws IOException {
        InputStream input = FontTest.class.getResourceAsStream(font);

        byte[] file = input.readAllBytes();

        ByteBuffer buffer = BufferUtils.createByteBuffer(file.length);
        buffer = buffer.put(file).flip();

        fontinfo = STBTTFontinfo.create();
        stbtt_InitFont(fontinfo, buffer);
    }

    void vertex() {
        /*
        GIVEN:
        * glyph bounds
        * zoom
        * position of glyph origin on screen
        we want to find the screenspace coords for each corner of the glyph bounds
         */


        float gx0 = gx+this.x0/zoom,
                gy0 = gy+this.y0/zoom,
                gx1 = gx+this.x1/zoom,
                gy1 = gy+this.y1/zoom;

        float[] vertices = new float[]{
                gx0, gy0, x0, y0,//bottom left
                gx1, gy0, x1, y0,//top left
                gx1, gy1, x1, y1,//top right
                gx0, gy1, x0, y1,//bottom right
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

    float floor(float a){
        return (float)Math.floor(a);
    }

    float ceil(float a){
        return (float)Math.ceil(a);
    }

    private void setupShaders() {
        shader = new Shader("/shader/text.vert", "/shader/text.frag");
    }

    private void render() {
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_DST_ALPHA);
        glEnable(GL_BLEND);

        Renderer renderer = new Renderer();

        Matrix4f proj = new Matrix4f();
        proj.ortho(0, WIDTH, HEIGHT, 0, -1.0f, 1.0f);

        Matrix4f view = new Matrix4f();

        Matrix4f model = new Matrix4f();

        Matrix4f mvp = proj.mul(view).mul(model);

        shader.setUniformMat4f("u_MVP", mvp);
        shader.setUniformFloatArray("uAtlas", atlas);
        shader.setUniform1f("uZoom", zoom);
        shader.setUniform1i("uCount", count);

        do {
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
        new TextRender().run();
    }
}
