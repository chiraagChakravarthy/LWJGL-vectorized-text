package text_renderer;

import gl.Shader;
import org.joml.Matrix4f;

import java.awt.*;
import java.io.IOException;

import static font_test.FileUtil.readFile;
import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.GL_TEXTURE_BUFFER;
import static org.lwjgl.opengl.GL31.glTexBuffer;
import static org.lwjgl.stb.STBTruetype.stbtt_GetCodepointHMetrics;
import static org.lwjgl.stb.STBTruetype.stbtt_GetCodepointKernAdvance;

/**
 * renders vectorized text
 *
 */
public class TextRenderer {
    static Shader shader;
    static int textVao;

    static int stringBuffer, stringBufferTex;

    public static final int MAX_LEN = 1000;

    /**
     * call after initializing window, before rendering text
     */
    public static void init(){//inits shaders
        initShaders();
        initVao();
        initStringBuffer();
    }

    private static void initStringBuffer(){
        int stringBuffer = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, stringBuffer);
        glBufferData(GL_ARRAY_BUFFER, MAX_LEN*2, GL_STATIC_READ);
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        int bufferTex = glGenTextures();
        glBindTexture(GL_TEXTURE_BUFFER, bufferTex);
        glTexBuffer(GL_TEXTURE_BUFFER, GL_R32I, stringBuffer);
        glBindTexture(GL_TEXTURE_BUFFER, 0);

        TextRenderer.stringBuffer = stringBuffer;
        TextRenderer.stringBufferTex = bufferTex;
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
        shader.bind();
        shader.setUniform1i("uAtlas", 0);
        shader.setUniform1i("uString", 1);
        shader.unbind();
    }

    private static void initVao(){
        int vao = glGenVertexArrays();
        glBindVertexArray(vao);

        int vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        int[] vertexAr = new int[MAX_LEN*12];

        int[] vertex = new int[]{
          0, 0, 0,
          0, 1, 0,
          1, 1, 0,
          1, 0, 0
        };

        for (int i = 0; i < MAX_LEN; i++) {
            int j = i*12;
            vertex[2] = i;
            vertex[5] = i;
            vertex[8] = i;
            vertex[11] = i;
            System.arraycopy(vertex, 0, vertexAr, j, 12);
        }

        glBufferData(GL_ARRAY_BUFFER, vertexAr, GL_STATIC_DRAW);
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);
        glVertexAttribIPointer(0, 2, GL_INT, 12, 0);
        glVertexAttribIPointer(1, 1, GL_INT, 12, 8);

        int ibo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo);

        int[] indexAr = new int[6*MAX_LEN];
        for (int i = 0; i < MAX_LEN; i++) {
            int j = i*6,
                    k = i*4;
            indexAr[j] = k;
            indexAr[j+1] = k+1;
            indexAr[j+2] = k+2;
            indexAr[j+3] = k+2;
            indexAr[j+4] = k+3;
            indexAr[j+5] = k;
        }

        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexAr, GL_STATIC_DRAW);
        glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
        TextRenderer.textVao = vao;
    }

    public static void drawText(String text, float x, float y, VectorFont font, Color color){
        int len = Math.min(text.length(), MAX_LEN);

        uploadString(text, len, font);

        drawText(font, x, y, len, color);
    }

    public static void drawText(VectorFont font, float x, float y, int len, Color color){
        shader.bind();
        setUniforms(color, font, x, y);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_BUFFER, font.atlasTexture);
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_BUFFER, stringBufferTex);

        glBindVertexArray(textVao);

        glDrawElements(GL_TRIANGLES, len*6, GL_UNSIGNED_INT, 0);
    }

    private static void setUniforms(Color color, VectorFont font, float x, float y){
        int[] viewport = new int[4];
        glGetIntegerv(GL_VIEWPORT, viewport);
        int vx = viewport[0];
        int vy = viewport[1];
        int vw = viewport[2];
        int vh = viewport[3];

        Matrix4f mvp = new Matrix4f().ortho(vx, vx+vw, vy, vy+vh, -1, 1);
        shader.setUniformMat4f("u_MVP", mvp);

        shader.setUniform4f("u_tint",
                color.getRed()/256f,
                color.getGreen()/256f,
                color.getBlue()/256f,
                color.getAlpha()/256f);
        shader.setUniform1f("uPixelSize", 1f/font.scale);
        shader.setUniform2f("uTextPos", x, y);
    }

    private static void uploadString(String text, int len, VectorFont font){
        char[] codepoints = text.toCharArray();

        int[] data = new int[len*2];

        data[0] = codepoints[0];
        data[1] = 0;

        int prevCodepoint = codepoints[0];

        int advance = 0;
        for (int i = 1; i < len; i++) {
            int[] charWidth = new int[1], leftBearing = new int[1];
            stbtt_GetCodepointHMetrics(font.font, prevCodepoint, charWidth, leftBearing);
            advance += charWidth[0];

            int codepoint = codepoints[i];
            int kern = stbtt_GetCodepointKernAdvance(font.font, prevCodepoint, codepoint);
            advance += kern;

            data[i*2] = codepoint;
            data[i*2+1] = advance;

            prevCodepoint = codepoint;
        }
        glBindBuffer(GL_ARRAY_BUFFER, stringBuffer);
        glBufferSubData(GL_ARRAY_BUFFER, 0, data);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
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
