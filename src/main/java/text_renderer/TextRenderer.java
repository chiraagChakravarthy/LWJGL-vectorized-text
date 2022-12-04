package text_renderer;

import java.io.IOException;

import static font_test.FileUtil.readFile;
import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL20.glDeleteShader;

//renders vectorized text
public class TextRenderer {
    private static int shader, uMvp, uAtlas, uPixelSize;

    public static void init(){//inits shaders
        String vsCode = readShader("shader/text/text.vert"),
                fsCode = readShader("shader/text/text.frag");
        shader = createShader(vsCode, fsCode);
        uMvp = glGetUniformLocation(shader, "u_MVP");
        uAtlas = glGetUniformLocation(shader, "uAtlas");
        uPixelSize = glGetUniformLocation(shader, "uFontSize");
    }

    public static void render(VectorFont font, float x, float y, String text){

    }

    public static String readShader(String path) {
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
