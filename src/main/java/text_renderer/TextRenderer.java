package text_renderer;

import gl.IndexBuffer;
import gl.Shader;
import org.joml.Matrix4f;
import org.lwjgl.stb.STBTTFontinfo;

import java.io.IOException;

import static font_test.FileUtil.readFile;
import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.GL_TEXTURE_BUFFER;
import static org.lwjgl.stb.STBTruetype.*;

/**
 * renders vectorized text
 *
 */
public class TextRenderer {
    static IndexBuffer ib;
    static Shader shader;

    public static final int MAX_LEN = 1000;

    /**
     * call after initializing window, before rendering text
     */
    public static void init(){//inits shaders
        initShaders();
    }

    private static void initShaders() {
        /*String vsCode = readShader("/shader/text/text.vert"),
                fsCode = readShader("/shader/text/text.frag");
        shader = createShader(vsCode, fsCode);
        uMvp = glGetUniformLocation(shader, "u_MVP");
        int uAtlas = glGetUniformLocation(shader, "uAtlas");
        uPixelSize = glGetUniformLocation(shader, "uFontSize");
        glUniform1i(uAtlas, 0);*/
        shader = new Shader("/shader/text/text.vert", "/shader/text/text.frag");
    }

    public static void drawText(VectorFont font, float x, float y, String text){
        int textVao = genTextVao(font, x, y, text);
        drawText(font, textVao);
        glDeleteVertexArrays(textVao);
    }

    public static void drawText(VectorFont font, int textVao){
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        shader.setUniform1f("uPixelSize", 1f/font.scale);
        setMvp();

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_BUFFER, font.atlasTexture);

        glBindVertexArray(textVao);
        glUseProgram(shader.program);
        glDrawElements(GL_TRIANGLES, MAX_LEN*6, GL_UNSIGNED_INT, 0);
    }

    private static void setMvp(){
        int[] viewport = new int[4];
        glGetIntegerv(GL_VIEWPORT, viewport);
        int x = viewport[0];
        int y = viewport[1];
        int w = viewport[2];
        int h = viewport[3];

        Matrix4f mvp = new Matrix4f().ortho(x, x+w, y, y+h, -1, 1);
        shader.setUniformMat4f("u_MVP", mvp);
    }

    private static int genTextVao(VectorFont font, float x, float y, String text){
        float scale = font.scale;
        //x, y are in pixels
        //1 glyph unit = 1 pixel * scale
        char[] characters = text.toCharArray();
        int len = Math.min(characters.length, MAX_LEN);

        float[] vertices = new float[len*4*4];//sx, sy, gx, gy
        int[] glyphs = new int[len*4];

        populateVertices(font, x, y, scale, len, characters, vertices, glyphs);

        int vao = assembleVao(vertices, glyphs);

        return vao;
    }

    private static int assembleVao(float[] vertices, int[] glyphs) {
        int vao = glGenVertexArrays();
        glBindVertexArray(vao);

        int buffer = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, buffer);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 16, 0);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 16, 8);

        int buffer2 = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, buffer2);
        glBufferData(GL_ARRAY_BUFFER, glyphs, GL_STATIC_DRAW);
        glEnableVertexAttribArray(2);
        glVertexAttribIPointer(2, 1, GL_INT, 4, 0);

        int[] indexArray = new int[glyphs.length/4*6];
        for (int i = 0; i < indexArray.length/6; i++) {
            indexArray[i*6] = i*4;
            indexArray[i*6+1] = i*4+1;
            indexArray[i*6+2] = i*4+2;
            indexArray[i*6+3] = i*4+2;
            indexArray[i*6+4] = i*4+3;
            indexArray[i*6+5] = i*4;
        }
        int ibo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexArray, GL_STATIC_DRAW);
        glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
        return vao;
    }

    private static void populateVertices(VectorFont font, float x, float y, float scale, int len, char[] characters, float[] vertex, int[] glyphs) {
        STBTTFontinfo info = font.font;
        int current = 0;
        for (int i = 0; i < len; i++) {
            int code = characters[i];

            int[] x0a = new int[1], x1a = new int[1], y0a = new int[1], y1a = new int[1];
            stbtt_GetCodepointBox(info, code, x0a, y0a, x1a, y1a);
            float x0 = x0a[0], x1 = x1a[0], y0 = y0a[0], y1 = y1a[0];

            int[] advance = new int[1], leftBearing = new int[1];
            stbtt_GetCodepointHMetrics(info, code, advance, leftBearing);

            float gx = x + current*scale, gy = y;
            appendGlyphVertices(scale, vertex, glyphs, i, code, x0, x1, y0, y1, gx, gy);

            //glyph space origin: (curr, 0)
            //screen space origin: (x+curr*scale, y)
            int kern = 0;
            if(i<len-1) {
                kern = stbtt_GetGlyphKernAdvance(info, code, characters[i + 1]);
            }

            current += advance[0] + kern;
        }
    }

    private static void appendGlyphVertices(float scale, float[] vertices, int[] glyphs, int i, int character, float x0, float x1, float y0, float y1, float gx, float gy) {
        float gx0 = gx + x0 * scale,
                gy0 = gy + y0 * scale,
                gx1 = gx + x1 * scale,
                gy1 = gy + y1 * scale;
        float pad = 2;

        float agx0 = (float) (Math.floor(gx0) - pad), agy0 = (float) (Math.floor(gy0) - pad),
                agx1 = (float) (Math.ceil(gx1) + pad), agy1 = (float) (Math.ceil(gy1) + pad);

        float dx0 = (gx0 - agx0) / scale, dx1 = (agx1 - gx1) / scale,
                dy0 = (gy0 - agy0) / scale, dy1 = (agy1 - gy1) / scale;
        x0 -= dx0;
        y0 -= dy0;
        x1 += dx1;
        y1 += dy1;
        float[] glyph = new float[]{
                agx0, agy0, x0, y0,//bottom left
                agx1, agy0, x1, y0,//bottom right
                agx1, agy1, x1, y1,//top right
                agx0, agy1, x0, y1,//top left
        };
        System.arraycopy(glyph, 0, vertices, i * 16, 16);
        for (int j = 0; j < 4; j++) {
            glyphs[i * 4 + j] = character;
        }
    }

    private static String readShader(String path) {
        String data;
        try {
            data = new String(readFile(path));
        } catch (IOException e) {
            System.out.println("Failed to read file: " + path);
            e.printStackTrace();
            throw new RuntimeException("Shader failed to load.");
        }
        return data;
    }

    private static int createShader(String vsCode, String fsCode) {
        int program = glCreateProgram();

        int vs = compileShader(GL_VERTEX_SHADER, vsCode);
        int fs = compileShader(GL_FRAGMENT_SHADER, fsCode);

        glAttachShader(program, vs);
        glAttachShader(program, fs);
        glLinkProgram(program);
        glValidateProgram(program);

        glDeleteShader(vs);
        glDeleteShader(fs);
        return program;
    }
    private static int compileShader(int type, String src) {
        int id = glCreateShader(type);
        glShaderSource(id, src);
        glCompileShader(id);
        int[] result = new int[1];
        glGetShaderiv(id, GL_COMPILE_STATUS, result);
        if (result[0] == GL_FALSE) {
            String message = glGetShaderInfoLog(id);
            System.out.println("Failed to compile " + (type == GL_VERTEX_SHADER ? "vertex" : "fragment") + " shader");
            System.out.println(message);
            glDeleteShader(id);
            return 0;
        }
        return id;
    }
}
