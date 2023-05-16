package io.github.chiraagchakravarthy.lwjgl_vectorized_text;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.io.IOException;

import static io.github.chiraagchakravarthy.lwjgl_vectorized_text.FileUtil.readFile;
import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL33.glVertexAttribDivisor;

/**
 * renders vectorized text
 *
 */
public class TextRenderer {

    public static final Vector2f ALIGN_MIDDLE = new Vector2f(),
            ALIGN_TOP_MIDDLE = new Vector2f(0, 1),
            ALIGN_BOTTOM_MIDDLE = new Vector2f(0, -1),
            ALIGN_BOTTOM_LEFT = new Vector2f(-1, -1),
            ALIGN_BOTTOM_RIGHT = new Vector2f(1, -1);
    public static final int MAX_LEN = 10000;
    private static int shader, u_Atlas, u_EmScale, u_Viewport, u_FontLen;
    private static int textVao, instanceVbo;
    private static boolean initialized = false;
    private static final float[] matrixArray = new float[16];

    private final float[] instanceBuffer = new float[21*MAX_LEN];
    private int len;
    private final VectorFont font;
    public TextRenderer(VectorFont font){
        this.font = font;
    }

    public TextRenderer(){
        this(new VectorFont());
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
    }

    private static void initShaders() {
        shader = createShader("/shader/text.vert", "/shader/text.frag");

        u_Atlas = glGetUniformLocation(shader, "u_Atlas");

        u_EmScale = glGetUniformLocation(shader, "u_EmScale");
        u_Viewport = glGetUniformLocation(shader, "u_Viewport");
        u_FontLen = glGetUniformLocation(shader, "u_FontLen");

        glUseProgram(shader);
        glUniform1i(u_Atlas, 0);
        glUseProgram(0);
    }

    private static void initVao(){
        int vao = glGenVertexArrays();
        glBindVertexArray(vao);

        int vertexBuffer = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer);
        int[] vertex = new int[]{
          0, 0,
          0, 1,
          1, 1,
          1, 0,
        };
        glBufferData(GL_ARRAY_BUFFER, vertex, GL_STATIC_DRAW);
        glEnableVertexAttribArray(0);
        glVertexAttribIPointer(0, 2, GL_INT, 8, 0);

        instanceVbo = glGenBuffers();//pose (1, 16), charIndex (5, 1), tint (6, 4)
        glBindBuffer(GL_ARRAY_BUFFER, instanceVbo);

        glEnableVertexAttribArray(1);//pose
        glEnableVertexAttribArray(2);
        glEnableVertexAttribArray(3);
        glEnableVertexAttribArray(4);
        glVertexAttribPointer(1, 4, GL_FLOAT, false, 21*4, 0);
        glVertexAttribPointer(2, 4, GL_FLOAT, false, 21*4, 16);
        glVertexAttribPointer(3, 4, GL_FLOAT, false, 21*4, 32);
        glVertexAttribPointer(4, 4, GL_FLOAT, false, 21*4, 48);
        glVertexAttribDivisor(1, 1);
        glVertexAttribDivisor(2, 1);
        glVertexAttribDivisor(3, 1);
        glVertexAttribDivisor(4, 1);

        glEnableVertexAttribArray(5);//charIndex
        glVertexAttribIPointer(5, 1, GL_INT, 21*4, 64);
        glVertexAttribDivisor(5, 1);

        glEnableVertexAttribArray(6);//tint
        glVertexAttribPointer(6, 4, GL_FLOAT, false, 21*4, 68);
        glVertexAttribDivisor(6, 1);

        glBufferData(GL_ARRAY_BUFFER, 21*4*MAX_LEN, GL_DYNAMIC_DRAW);


        int ibo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo);
        int[] indexAr = new int[]{0, 1, 2, 2, 3, 0};
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

    public void drawText2D(String text, float x, float y, float pxScale, Vector2f align, TextBoundType alignType, Vector4f color){
        if(text.isEmpty()){
            return;
        }
        assertInitialized();
        int[] viewport = new int[4];
        glGetIntegerv(GL_VIEWPORT, viewport);
        int vx = viewport[0],
                vy = viewport[1],
                vw = viewport[2],
                vh = viewport[3];
        Matrix4f mvp = new Matrix4f().ortho(vx, vx+vw, vy, vy+vh, -1, 1);
        Matrix4f pose = new Matrix4f().translate(x, y, 0).scale(pxScale);

        drawText(text, pose, mvp, align, alignType, color);
    }



    /** Draw text with different alignment
     *
     * @param text string what drawn
     * @param pose position, scale, rotation
     * @param align where within the string is its 'position'
     *              (-1, -1): bottom left
     *              (1, 1) top right
     *              (0, 0) centered
     * @param alignType align with the geometric string bounds, or with fixed height bounds
     * @param color rgba color (0,1)
     */
    public void drawText(String text, Matrix4f pose, Matrix4f mvp, Vector2f align, TextBoundType alignType, Vector4f color){
        if(text.isEmpty()){
            return;
        }
        assertInitialized();
        int len = Math.min(MAX_LEN-this.len, text.length());
        int[] bounds = new int[4];
        int[] advance = new int[len];
        getBoundAndAdvance(text, len, font, bounds, advance);

        int x0 = bounds[0], y0 = bounds[1], x1 = bounds[2], y1 = bounds[3];
        if(alignType == TextBoundType.BASELINE){
            y0 = 0;
            y1 = font.ascent;
        }

        float cx = (x1-x0)/2f*font.emScale, cy = (y1-y0)/2f*font.emScale;//center of text in em space
        float dx = cx*(align.x+1)+x0*font.emScale, dy = cy*(align.y+1)+y0*font.emScale;
        Vector4f oPos = pose.transform(new Vector4f());//translate component
        Vector4f cPos = pose.transform(new Vector4f(dx, dy, 0, 1));//center transformed
        pose = new Matrix4f().translate(oPos.x-cPos.x, oPos.y-cPos.y, oPos.z-cPos.z).mul(pose);
        pose = pose.mul(new Matrix4f().scale(font.emScale));

        Matrix4f transform = new Matrix4f(mvp).mul(pose);

        char[] characters = text.toCharArray();
        for(int i = this.len; i < MAX_LEN && i < this.len+text.length(); i++){
            Matrix4f letterTransform = new Matrix4f(transform).mul(new Matrix4f().translate(advance[i-this.len], 0, 0));
            appendToInstanceBuffer(letterTransform, font.indexOf(characters[i-this.len]), color, i);
        }

        this.len = Math.min(MAX_LEN, this.len+text.length());
    }


    private void appendToInstanceBuffer(Matrix4f pose, int index, Vector4f tint, int i){
        int j = i*21;
        System.arraycopy(pose.get(matrixArray), 0, instanceBuffer, j, 16);
        instanceBuffer[j+16] = Float.intBitsToFloat(index);
        for (int k = 0; k < 4; k++) {
            instanceBuffer[j+k+17] = tint.get(k);
        }
    }

    public void render(){
        glUseProgram(shader);

        int[] viewport = new int[4];
        glGetIntegerv(GL_VIEWPORT, viewport);

        glUniform1i(u_FontLen, font.len);
        glUniform4f(u_Viewport, viewport[0], viewport[1], viewport[2], viewport[3]);
        glUniform1f(u_EmScale, font.emScale);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_BUFFER, font.atlasTexture);

        glBindBuffer(GL_ARRAY_BUFFER, instanceVbo);
        glBufferSubData(GL_ARRAY_BUFFER, 0, instanceBuffer);

        glBindVertexArray(textVao);


        glDrawElementsInstanced(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0, len);
        len = 0;
    }

    //returns bounds and advance at 1px scale, in pixels
    public void getBoundAndAdvance(String text, int len, VectorFont font, int[] bounds, int[] advance){
        char[] codepoints = text.toCharArray();

        int y0 = font.bound(codepoints[0], 1);
        int y1 = font.bound(codepoints[0], 3);

        char prevCodepoint = codepoints[0];

        int currAdvance = 0;

        for (int i = 1; i < len; i++) {
            char codepoint = codepoints[i];
            currAdvance += font.advance(prevCodepoint);
            currAdvance += font.kern(prevCodepoint, codepoint);
            advance[i] = currAdvance;



            prevCodepoint = codepoint;

            y0 = Math.min(font.bound(codepoint, 1), y0);
            y1 = Math.max(font.bound(codepoint, 3), y1);
        }

        int x0 = font.bound(codepoints[0], 0);
        int x1 = currAdvance + font.bound(codepoints[codepoints.length-1], 2);
        bounds[0] = x0;
        bounds[1] = y0;
        bounds[2] = x1;
        bounds[3] = y1;
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