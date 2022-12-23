package io.github.chiraagchakravarthy.lwjgl_vectorized_text;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.joml.Vector4i;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.nio.FloatBuffer;

import static io.github.chiraagchakravarthy.lwjgl_vectorized_text.FileUtil.readFile;
import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL31.*;

/**
 * renders vectorized text
 *
 */
public class TextRenderer {
    public static final int MAX_LEN = 1000;
    private static int shader, u_Atlas, u_String, u_EmScale, u_Mvp, u_Tint, u_Pose, u_Viewport, u_FontLen;
    private static int textVao;
    private static boolean initialized = false;
    private static final FloatBuffer matrixBuffer = MemoryUtil.memAllocFloat(16);
    private static int stringBuffer, stringBufferTex;
    private Matrix4f mvp;

    public TextRenderer(Matrix4f mvp){
        this.mvp = mvp;
    }

    public TextRenderer(){
        mvp = new Matrix4f().ortho(-1, 1, -1, 1, -1, 1);
    }

    /**
     * call after creating opengl capabilities
     * loads shaders, initializes buffers and vertex array
     */
    private static void init(){
        initShaders();
        initVao();
        initStringBuffer();
        initialized = true;
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
        shader = createShader("/shader/text.vert", "/shader/text.frag");

        u_Atlas = glGetUniformLocation(shader, "u_Atlas");
        u_String = glGetUniformLocation(shader, "u_String");

        u_EmScale = glGetUniformLocation(shader, "u_EmScale");
        u_Mvp = glGetUniformLocation(shader, "u_Mvp");
        u_Pose = glGetUniformLocation(shader, "u_Pose");
        u_Tint = glGetUniformLocation(shader, "u_Tint");
        u_Viewport = glGetUniformLocation(shader, "u_Viewport");
        u_FontLen = glGetUniformLocation(shader, "u_FontLen");

        glUseProgram(shader);
        glUniform1i(u_Atlas, 0);
        glUniform1i(u_String, 1);
        glUseProgram(0);
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

    private static void assertInitialized(){
        if(!initialized){
            init();
        }
    }

    /**
     * @param mvp model view projection matrix (camera)
     */
    public void setMvp(Matrix4f mvp){
        this.mvp = mvp;
    }

    /** Draw some text!
     *
     * @param text string what drawn
     * @param x window pixel x
     * @param y window pixel y
     * @param font the typeface
     * @param pxScale the pixel scale of the text
     * @param  color rgba color (0,1)
     * ignores the current mvp
     */
    public void drawText2D(String text, float x, float y, float pxScale, VectorFont font, Vector4f color){
        if(text.isEmpty()){
            return;
        }
        assertInitialized();
        int[] viewport = new int[4];
        glGetIntegerv(GL_VIEWPORT, viewport);
        int vx = viewport[0];
        int vy = viewport[1];
        int vw = viewport[2];
        int vh = viewport[3];
        Matrix4f mvp = new Matrix4f().ortho(vx, vx+vw, vy, vy+vh, -1, 1);
        Matrix4f pose = new Matrix4f().translate(x, y, 0).scale(pxScale);

        int len = Math.min(MAX_LEN, text.length());
        uploadString(text, len, font);
        drawText(len, font, pose, color, mvp, viewport);
    }

    /** Draw text with different alignment
     *
     * @param text string what drawn
     * @param pose position, scale, rotation
     * @param align where within the string is its 'position'
     *              (-1, -1): bottom left
     *              (1, 1) top right
     * @param alignType align with the geometric string bounds, or with fixed height bounds
     * @param font typeface to render with
     * @param color rgba color (0,1)
     */
    public void drawTextAligned(String text, Matrix4f pose, Vector2f align, TextBoundType alignType, VectorFont font, Vector4f color){
        if(text.isEmpty()){
            return;
        }
        assertInitialized();
        int len = Math.min(MAX_LEN, text.length());
        int[] bounds = new int[4];
        uploadStringAndGetBounds(text, len, font, bounds);

        int x0 = bounds[0], y0 = bounds[1], x1 = bounds[2], y1 = bounds[3];
        if(alignType == TextBoundType.BASELINE){
            y0 = 0;
            y1 = font.ascent;
        }

        float cx = (x1-x0)/2f*font.emScale, cy = (y1-y0)/2f*font.emScale;
        float dx = cx*(align.x+1), dy = cy*(align.y+1);
        Vector4f oPos = pose.transform(new Vector4f());//translate component
        Vector4f cPos = pose.transform(new Vector4f(dx, dy, 0, 1));//center transformed
        pose = new Matrix4f().translate(oPos.x-cPos.x, oPos.y-cPos.y, oPos.z-cPos.z).mul(pose);

        int[] viewport = new int[4];
        glGetIntegerv(GL_VIEWPORT, viewport);

        drawText(len, font, pose, color, mvp, viewport);
    }


    //returns the string size at 1px scale
    public Vector4f getStringBounds(String text, VectorFont font, TextBoundType type){
        char[] codepoints = text.toCharArray();
        int len = Math.min(MAX_LEN, text.length());

        int y0 = font.bound(codepoints[0], 1);
        int y1 = font.bound(codepoints[0], 3);

        char prevCodepoint = codepoints[0];
        int advance = 0;

        for (int i = 1; i < len; i++) {
            char codepoint = codepoints[i];

            advance += font.advance(prevCodepoint);
            advance += font.kern(prevCodepoint, codepoint);
            prevCodepoint = codepoint;
            y0 = Math.min(font.bound(codepoint, 1), y0);
            y1 = Math.max(font.bound(codepoint, 3), y1);
        }
        int x0 = font.bound(codepoints[0], 0);
        int x1 = font.bound(codepoints[len-1], 2)+advance;
        if(type == TextBoundType.BASELINE){
            y0 = 0;
            y1 = font.ascent;
        }
        return new Vector4f(x0, y0, x1, y1).mul(font.emScale);
    }

    private void uploadStringAndGetBounds(String text, int len, VectorFont font, int[] bounds){
        char[] codepoints = text.toCharArray();

        int[] data = new int[len*2];

        data[0] = font.indexOf(codepoints[0]);
        data[1] = 0;

        int y0 = font.bound(codepoints[0], 1);
        int y1 = font.bound(codepoints[0], 3);

        char prevCodepoint = codepoints[0];

        int advance = 0;

        for (int i = 1; i < len; i++) {
            char codepoint = codepoints[i];
            advance += font.advance(prevCodepoint);
            advance += font.kern(prevCodepoint, codepoint);

            data[i*2] = font.indexOf(codepoint);
            data[i*2+1] = advance;

            prevCodepoint = codepoint;

            y0 = Math.min(font.bound(codepoint, 1), y0);
            y1 = Math.max(font.bound(codepoint, 3), y1);
        }

        glBindBuffer(GL_ARRAY_BUFFER, stringBuffer);
        glBufferSubData(GL_ARRAY_BUFFER, 0, data);
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        int x0 = font.bound(codepoints[0], 0);
        int x1 = advance + font.bound(codepoints[codepoints.length-1], 2);
        bounds[0] = x0;
        bounds[1] = y0;
        bounds[2] = x1;
        bounds[3] = y1;
    }

    /** A more comprehensive draw text
     *
     * @param text string what drawn
     * @param font the typeface
     * @param pose position, scale, and rotation
     * @param color rgba color (0,1)
     */
    public void drawText(String text, Matrix4f pose, VectorFont font, Vector4f color){
        if(text.isEmpty()){
            return;
        }
        assertInitialized();
        int[] viewport = new int[4];
        glGetIntegerv(GL_VIEWPORT, viewport);
        int len = Math.min(MAX_LEN, text.length());
        uploadString(text, len, font);
        drawText(len, font, pose, color, mvp, viewport);
    }


    private void drawText(int len, VectorFont font, Matrix4f pose, Vector4f color, Matrix4f mvp, int[] viewport){
        glUseProgram(shader);

        glUniform1i(u_FontLen, font.len);
        glUniform4f(u_Tint, color.x, color.y, color.z, color.w);
        glUniformMatrix4fv(u_Pose, false, pose.get(matrixBuffer));
        glUniformMatrix4fv(u_Mvp, false, mvp.get(matrixBuffer));
        glUniform4f(u_Viewport, viewport[0], viewport[1], viewport[2], viewport[3]);
        glUniform1f(u_EmScale, font.emScale);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_BUFFER, font.atlasTexture);
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_BUFFER, stringBufferTex);

        glBindVertexArray(textVao);

        glDrawElements(GL_TRIANGLES, len * 6, GL_UNSIGNED_INT, 0);
    }
    private void uploadString(String text, int len, VectorFont font){
        char[] codepoints = text.toCharArray();

        int[] data = new int[len*2];

        data[0] = codepoints[0];
        data[1] = 0;

        char prevCodepoint = codepoints[0];

        int advance = 0;
        for (int i = 1; i < len; i++) {
            char codepoint = codepoints[i];
            advance += font.advance(prevCodepoint);
            advance += font.kern(prevCodepoint, codepoint);

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

    private static int createShader(String vsPath, String fsPath) {
        int program = glCreateProgram();

        String vsCode = readShader(vsPath);
        String fsCode = readShader(fsPath);

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

    public enum TextBoundType {
        BASELINE,
        BOUNDING_BOX
    }
}