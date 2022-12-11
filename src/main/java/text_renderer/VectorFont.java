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
import static org.lwjgl.stb.STBTruetype.*;

public class VectorFont {

    public final float size;
    protected final float scale;
    protected final int atlasTexture;
    protected final STBTTFontinfo font;

    /**
     *
     * @param path path to ttf file
     * @param size pixel height of font
     */
    public VectorFont(String path, float size) {
        this.size = size;
        try {
            this.font = FileUtil.loadFont(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.scale = stbtt_ScaleForMappingEmToPixels(font, size);
        int atlasBuffer = glGenBuffers();
        int[] atlas = genAtlas();

        glBindBuffer(GL_ARRAY_BUFFER, atlasBuffer);
        glBufferData(GL_ARRAY_BUFFER, atlas, GL_STATIC_READ);

        atlasTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_BUFFER, atlasTexture);
        glTexBuffer(GL_TEXTURE_BUFFER, GL_R32I, atlasBuffer);
    }

    private int[] genAtlas(){
        int[][] glyphs = new int[256][];
        int[] index = new int[257];

        int curr = 0;
        index[0] = 0;

        //all unicode chars
        for (int i = 0; i < 256; i++) {
            STBTTVertex.Buffer glyphShape = stbtt_GetCodepointShape(font, i);
            int[] glyph = null;
            if(glyphShape != null){
                int count = glyphShape.remaining();
                glyph = new int[count*6];
                curr += extractGlyphShape(glyphShape, count, glyph);
            }
            index[i+1] = curr;
            glyphs[i] = glyph;
        }

        int[] atlas = new int[257+256*4+curr*6];
        System.arraycopy(index, 0, atlas, 0, 257);

        for(int i = 0; i < glyphs.length; i++){
            int[] glyph = glyphs[i];
            int start = index[i], end = index[i+1];
            if(glyph != null) {
                System.arraycopy(glyph, 0, atlas, start*6+257+256*4, (end-start)*6);
                addBounds(atlas, i);
            }
        }

        return atlas;
    }

    private void addBounds(int[] atlas, int i) {
        int[] x0a = new int[1], x1a = new int[1], y0a = new int[1], y1a = new int[1];
        stbtt_GetCodepointBox(font, i, x0a, y0a, x1a, y1a);
        int x0 = x0a[0], x1 = x1a[0], y0 = y0a[0], y1 = y1a[0];
        atlas[257+ i *4] = x0;
        atlas[257+ i *4+1] = y0;
        atlas[257+ i *4+2] = x1;
        atlas[257+ i *4+3] = y1;
    }

    private static int extractGlyphShape(STBTTVertex.Buffer glyphShape, int count, int[] glyph) {
        int k = 0;
        for(int j = count - 1; j >= 0; j--){
            STBTTVertex vertex = glyphShape.get(j);
            int ax = vertex.x(), ay = vertex.y();
            byte type = vertex.type();
            if(type==3){
                STBTTVertex nextVertex = glyphShape.get(j - 1);
                int bx = vertex.cx(), by = vertex.cy();
                int cx = nextVertex.x(), cy = nextVertex.y();
                glyph[k] =  ax-2*bx+cx;
                glyph[k+1] = 2*bx-2*cx;
                glyph[k+2] = cx;
                glyph[k+3] = ay-2*by+cy;
                glyph[k+4] = 2*by-2*cy;
                glyph[k+5] = cy;
                k += 6;
            } else if(type==2){
                STBTTVertex nextVertex = glyphShape.get(j - 1);
                int bx = nextVertex.x(), by = nextVertex.y();
                glyph[k] = 0;
                glyph[k+1] = ax-bx;
                glyph[k+2] = bx;
                glyph[k+3] = 0;
                glyph[k+4] = ay-by;
                glyph[k+5] = by;
                k += 6;
            }
        }
        return k/6;
    }

    public float getSize() {
        return size;
    }
}
