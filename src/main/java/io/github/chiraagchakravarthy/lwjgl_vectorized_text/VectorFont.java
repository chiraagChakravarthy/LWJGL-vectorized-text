package io.github.chiraagchakravarthy.lwjgl_vectorized_text;

import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTTVertex;

import java.io.IOException;

import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.GL_R32I;
import static org.lwjgl.opengl.GL31.GL_TEXTURE_BUFFER;
import static org.lwjgl.opengl.GL31.glTexBuffer;
import static org.lwjgl.stb.STBTruetype.*;

public class VectorFont {

    /** em per glyph unit
     * 10px scale means 10 pixels/em
     * thus pixelScale = #px * emScale
     */
    protected final float emScale;

    protected final int atlasTexture;
    protected final STBTTFontinfo font;
    protected final int[] advance, kern;

    /**
     *
     * @param path path to ttf file
     */
    public VectorFont(String path) {
        advance = new int[256];
        kern = new int[256*256];
        try {
            this.font = FileUtil.loadFont(path);
        } catch (IOException e) {
            throw new RuntimeException("Could not find font at \"" + path + "\"");
        }

        emScale = stbtt_ScaleForMappingEmToPixels(font, 1);

        int atlasBuffer = glGenBuffers();
        int[] atlas = genAtlas();

        glBindBuffer(GL_ARRAY_BUFFER, atlasBuffer);
        glBufferData(GL_ARRAY_BUFFER, atlas, GL_STATIC_READ);

        atlasTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_BUFFER, atlasTexture);
        glTexBuffer(GL_TEXTURE_BUFFER, GL_R32I, atlasBuffer);

        makeTables();
    }


    /**
     * uses arial by default
     */
    public VectorFont(){
        this("/font/arial.ttf");
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

        /*int start = atlas[')'], end = atlas[')'+1];
        for (int i = start; i < end; i++) {
            int j = i*6+256*4+257;
            System.out.printf("(%dt^2+%dt+%d,%dt^2+%dt+%d)%n", atlas[j], atlas[j+1], atlas[j+2], atlas[j+3], atlas[j+4], atlas[j+5]);
        }
        System.out.printf("(%dt+%d, %dt+%d)%n", atlas[257+4*')'], atlas[257+4*')'+2]-atlas[257+4*')'], atlas[257+4*')'+1], atlas[257+4*')'+3]-atlas[257+4*')'+1]);
        */
        return atlas;
    }

    private void makeTables(){
        for (int i = 0; i < 256; i++) {
            for (int j = 0; j < 256; j++) {
                //char1 * 256 + char2
                kern[i*256+j] = stbtt_GetCodepointKernAdvance(font, i, j);
            }
            int[] leftBearing = new int[1], advance = new int[1];
            stbtt_GetCodepointHMetrics(font, i, advance, leftBearing);
            this.advance[i] = advance[0];
        }
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
}
