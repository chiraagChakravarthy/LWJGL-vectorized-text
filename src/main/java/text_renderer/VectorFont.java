package text_renderer;

import font_test.FileUtil;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTTVertex;

import java.awt.*;
import java.io.IOException;

import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.GL_R32I;
import static org.lwjgl.opengl.GL31.GL_TEXTURE_BUFFER;
import static org.lwjgl.opengl.GL31.glTexBuffer;
import static org.lwjgl.stb.STBTruetype.stbtt_GetCodepointShape;
import static org.lwjgl.stb.STBTruetype.stbtt_ScaleForMappingEmToPixels;

public class VectorFont {
    protected final float size, scale;
    protected final int atlasTexture;
    protected final STBTTFontinfo font;

    /**
     *
     * @param path path to ttf file
     * @param size pixel height of font
     * @param style bold/italicized
     */
    public VectorFont(String path, float size, int style) throws IOException, FontFormatException {
        this.size = size;
        this.font = FileUtil.loadFont(path);

        this.scale = stbtt_ScaleForMappingEmToPixels(font, size);
        int atlasBuffer = glGenBuffers();
        int[] atlasArray = genAtlas();

        glBindBuffer(GL_ARRAY_BUFFER, atlasBuffer);
        glBufferData(GL_ARRAY_BUFFER, atlasArray, GL_STATIC_READ);

        atlasTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_BUFFER, atlasTexture);
        glTexBuffer(GL_TEXTURE_BUFFER, GL_R32I, atlasBuffer);
        //glDeleteBuffers(atlasBuffer); not sure
    }

    private int[] genAtlas(){
        int[][] glyphs = new int[256][];
        int[] index = new int[257];

        int curr = 0;
        index[0] = 257;

        //all unicode chars
        for (int i = 0; i < 255; i++) {
            STBTTVertex.Buffer glyphShape = stbtt_GetCodepointShape(font, i);
            if(glyphShape == null){

                continue;
            }
            int count = glyphShape.remaining();
            curr += count;
            index[i+1] = curr+257;
            int[] glyph = new int[count*6];
            int k = 0;

            for(int j = count-1; j >= 0; j--){
                STBTTVertex vertex = glyphShape.get(k);
                int ax = vertex.x(), ay = vertex.y();
                byte type = vertex.type();
                STBTTVertex nextVertex = glyphShape.get(i-1);
                if(type==3){
                    int bx = vertex.cx(), by = vertex.cy();
                    int cx = nextVertex.x(), cy = nextVertex.y();
                    glyph[k] =  ax-2*bx+cx;
                    glyph[k+1] = 2*bx-2*cx;
                    glyph[k+2] = cx;
                    glyph[k+3] = ay-2*by+cy;
                    glyph[k+4] = 2*by-2*cy;
                    glyph[k+5] = cy;
                } else if(type==2){
                    int bx = nextVertex.x(), by = nextVertex.y();
                    glyph[k] = 0;
                    glyph[k+1] = ax-bx;
                    glyph[k+2] = bx;
                    glyph[k+3] = 0;
                    glyph[k+4] = ay-by;
                    glyph[k+5] = by;
                }
                k += 6;
            }
            glyphs[i] = glyph;
        }
        int[] atlas = new int[257+curr*6];
        System.arraycopy(index, 0, atlas, 0, 257);
        curr = 0;
        for(int[] glyph : glyphs){
            System.arraycopy(glyph, 0, atlas, curr, glyph.length);
            curr += glyph.length;
        }

        return atlas;
    }

    public float getSize() {
        return size;
    }
}
