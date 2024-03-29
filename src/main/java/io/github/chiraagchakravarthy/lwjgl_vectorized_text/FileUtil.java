package io.github.chiraagchakravarthy.lwjgl_vectorized_text;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTFontinfo;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static org.lwjgl.stb.STBTruetype.stbtt_InitFont;

class FileUtil {

    public static byte[] readFile(String font) throws IOException {
        InputStream input = FileUtil.class.getResourceAsStream(font);
        byte[] bytes = new byte[(int) input.available()];
        DataInputStream dataInputStream = new DataInputStream(input);
        dataInputStream.readFully(bytes);
        input.close();
        return bytes;
    }
}
