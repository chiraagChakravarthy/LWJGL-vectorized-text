package test.alg_test;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTFontinfo;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static org.lwjgl.stb.STBTruetype.stbtt_InitFont;

class FileUtil {
    public static STBTTFontinfo loadFont(String font) throws IOException {
        byte[] bytes = readFile(font);

        ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length);
        buffer = (ByteBuffer) buffer.put(bytes).flip();

        STBTTFontinfo fontinfo = STBTTFontinfo.create();
        stbtt_InitFont(fontinfo, buffer);
        return fontinfo;
    }

    public static byte[] readFile(String font) throws IOException {
        InputStream input = FileUtil.class.getResourceAsStream(font);
        byte[] bytes = new byte[(int) input.available()];
        DataInputStream dataInputStream = new DataInputStream(input);
        dataInputStream.readFully(bytes);
        input.close();
        return bytes;
    }
}
