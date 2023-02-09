package io.github.chiraagchakravarthy.lwjgl_vectorized_text;

import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTTVertex;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

import static java.lang.Math.*;
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
     *
     * charIndex: the index of the character in the atlas
     */

    public static VectorFont shapes;

    static {
        int[][] square = new int[][]{
                new int[]{0, -1000, 1000, 0, 0, 0},
                new int[]{0, 0, 0, 0, 1000, 0},
                new int[]{0, 1000, 0, 0, 0, 1000},
                new int[]{0, 0, 1000, 0, -1000, 1000}
        };

        int[][] triangle = new int[][]{
                new int[]{0, -1000, 1000, 0, 0, 0},
                new int[]{0, 500, 0, 0, 866, 0},
                new int[]{0, 500, 500, 0, -866, 866}
        };

        int n = 8;
        int[][] circle = new int[n][6];
        float alpha = (float) (PI/n);
        float r = 500;
        float R = (float) (r/cos(alpha));

        for (int i = 0; i < n; i++) {
            int p1x = (int) round(r*sin(2*i*alpha)),
                    p1y = (int) round(-r*cos(2*i*alpha)),
                    p2x = (int) round(R*sin((2*i+1)*alpha)),
                    p2y = (int) round(-R*cos((2*i+1)*alpha)),
                    p3x = (int) round(r*sin((2*i+2)*alpha)),
                    p3y = (int) round(-r*cos((2*i+2)*alpha));
            int[] bezier = new int[]{
                    p1x-2*p2x+p3x,
                    2*p2x-2*p3x,
                    p3x,
                    p1y-2*p2y+p3y,
                    2*p2y-2*p3y,
                    p3y
            };
            circle[i] = bezier;
        }

        int[][][] shapes = new int[][][]{
          square, triangle, circle
        };

        VectorFont.shapes = new VectorFont(shapes, new char[]{'s', 't', 'c'}, .001f);
    }

    public static final String ASCII, DEFAULT;

    static {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < 128; i++) {
            b.append((char)i);
        }
        ASCII = b.toString();
        for (int i = 128; i < 256; i++) {
            b.append((char)i);
        }
        DEFAULT = b.toString();
    }

    private final HashMap<Character, Integer> indexMap;//maps codepoints to their index in the atlas
    protected final float emScale;

    protected final int atlasTexture;
    protected final int[] advance, kern, bounds;

    protected final int ascent;
    protected final int len;

    private final int[] atlas;

    /*shapes:

     */

    /** This constructor is to allow you to generate your own shapes to render
     *
     *
     * @param shapes [shape1: [bezier1: [a, b, c, d, e, f], bezier2...], shape2...]
     * @param characters the characters associated with the provided shapes
     * @param scale since the bezier coeffecients are integers, the scale of the provided shapes may need to
     *              grow quite large to accurately represent something. the provided scale can be set to counter this
     */
    public VectorFont(int[][][] shapes, char[] characters, float scale){
        len = characters.length;
        indexMap = new HashMap<>();
        for (int i = 0; i < characters.length; i++) {
            indexMap.put(characters[i], i);
        }
        this.advance = new int[len];
        this.kern = new int[len*len];
        bounds = new int[len*4];
        this.emScale = scale;

        atlas = genAtlas(shapes);
        int atlasBuffer = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, atlasBuffer);
        glBufferData(GL_ARRAY_BUFFER, atlas, GL_STATIC_READ);

        atlasTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_BUFFER, atlasTexture);
        glTexBuffer(GL_TEXTURE_BUFFER, GL_R32I, atlasBuffer);

        this.ascent = 0;
    }

    public VectorFont(String path, String characters){
        STBTTFontinfo font;
        try {
            font = FileUtil.loadFont(path);
        } catch (IOException e) {
            throw new RuntimeException("Could not find font at \"" + path + "\"");
        }

        indexMap = new HashMap<>();
        char[] chars = characters.toCharArray();
        this.len = removeDuplicates(chars);
        advance = new int[len];
        kern = new int[len*len];
        bounds = new int[len*4];

        atlas = genAtlas(chars, font);

        int atlasBuffer = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, atlasBuffer);
        glBufferData(GL_ARRAY_BUFFER, atlas, GL_STATIC_READ);

        atlasTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_BUFFER, atlasTexture);
        glTexBuffer(GL_TEXTURE_BUFFER, GL_R32I, atlasBuffer);

        makeTables(chars, font);

        int[] ascent = new int[1], descent = new int[1], lineGap = new int[1];
        stbtt_GetFontVMetrics(font, ascent, descent, lineGap);
        this.ascent = ascent[0];
        emScale = stbtt_ScaleForMappingEmToPixels(font, 1);
    }

    private int removeDuplicates(char[] chars){
        Arrays.sort(chars);
        char prev = chars[0];
        int len = 1;

        for (int i = 1; i < chars.length; i++) {
            char curr = chars[i];
            if(curr == prev){
                continue;
            }
            chars[len] = curr;
            prev = curr;
            len++;
        }
        return len;
    }

    /**
     *
     * @param path path to ttf file
     */
    public VectorFont(String path) {
        this(path, DEFAULT);
    }

    //debug
    public void print(char codepoint){
        int start = atlas[indexOf(codepoint)];
        int end = atlas[indexOf(codepoint)+1];
        for (int i = start; i < end; i++) {
            int j = i*6+len*4+len+1;
            System.out.printf("(%dt^2+%dt+%d,%dt^2+%dt+%d)%n", atlas[j], atlas[j+1], atlas[j+2], atlas[j+3], atlas[j+4], atlas[j+5]);
        }
    }


    /**
     * uses arial by default
     */
    public VectorFont(){
        this("/font/arial.ttf");
    }

    private int[] genAtlas(int[][][] shapes){
        int length = len*5+1;
        for (int i = 0; i < len; i++) {
            length += shapes[i].length*6;
        }
        int[] atlas = new int[length];
        int curr = 0;
        for (int i = 0; i < len; i++) {
            int[][] shape = shapes[i];
            for (int[] bezier : shape) {
                System.arraycopy(bezier, 0, atlas, curr * 6 + len * 5 + 1, 6);
                curr++;
            }
            atlas[i+1] = curr;
            addBounds(atlas, i);
        }
        return atlas;
    }

    private int[] genAtlas(char[] chars, STBTTFontinfo font){
        int[][] glyphs = new int[len][];
        int[] index = new int[len+1];

        int curr = 0;
        index[0] = 0;

        //all unicode chars
        for (int i = 0; i < len; i++) {
            char codepoint = chars[i];
            indexMap.put(codepoint, i);

            STBTTVertex.Buffer glyphShape = stbtt_GetCodepointShape(font, codepoint);
            int[] glyph = null;
            if(glyphShape != null){
                int count = glyphShape.remaining();
                glyph = new int[count*6];
                curr += extractGlyphShape(glyphShape, count, glyph);
            }
            index[i+1] = curr;
            glyphs[i] = glyph;
        }

        int[] atlas = new int[len+1+len*4+curr*6];

        System.arraycopy(index, 0, atlas, 0,len+1);

        for(int i = 0; i < glyphs.length; i++){
            int[] glyph = glyphs[i];
            int start = index[i], end = index[i+1];
            if(glyph != null) {
                System.arraycopy(glyph, 0, atlas, start*6+len+1+len*4, (end-start)*6);
                addBounds(atlas, i);
            }
        }
        return atlas;
    }

    private void makeTables(char[] chars, STBTTFontinfo font){
        for (int i = 0; i < len; i++) {
            char codepointi = chars[i];
            for (int j = 0; j < len; j++) {
                char codepointj = chars[j];
                kern[i*len+j] = stbtt_GetCodepointKernAdvance(font, codepointi, codepointj);
            }

            int[] leftBearing = new int[1], advance = new int[1];
            stbtt_GetCodepointHMetrics(font, codepointi, advance, leftBearing);

            this.advance[i] = advance[0];
        }
    }

    private void addBounds(int[] atlas, int i){
        int start = atlas[i],
                end = atlas[i+1];

        float minX=Float.POSITIVE_INFINITY,
                minY=Float.POSITIVE_INFINITY,
                maxX=Float.NEGATIVE_INFINITY,
                maxY=Float.NEGATIVE_INFINITY;
        for (int j = start; j < end; j++) {
            int k = j*6+len*5+1;
            int a = atlas[k],
                    b = atlas[k+1],
                    c = atlas[k+2],
                    d = atlas[k+3],
                    e = atlas[k+4],
                    f = atlas[k+5];
            //2at+b=0
            float tx = a==0?0:clamp(-b/(2f*a)),
                    ty = d==0?0:clamp(-e/(2f*d)),
                    pxx = a*tx*tx+b*tx+c,
                    pxy = d*tx*tx+e*tx+f,
                    pyx = a*ty*ty+b*ty+c,
                    pyy = d*ty*ty+e*ty+f,
                    p0x = c,
                    p0y = f,
                    p1x = a+b+c,
                    p1y = d+e+f;
            minX = min(min(min(pxx, pyx), min(p0x, p1x)), minX);
            minY = min(min(min(pxy, pyy), min(p0y, p1y)), minY);
            maxX = max(max(max(pxx, pyx), max(p0x, p1x)), maxX);
            maxY = max(max(max(pxy, pyy), max(p0y, p1y)), maxY);
        }
        atlas[len+1+4*i]= (int) floor(minX);
        atlas[len+1+4*i+1] = (int) floor(minY);
        atlas[len+1+4*i+2] = (int) ceil(maxX);
        atlas[len+1+4*i+3] = (int) ceil(maxY);
        bounds[4*i]= (int) floor(minX);
        bounds[4*i+1] = (int) floor(minY);
        bounds[4*i+2] = (int) ceil(maxX);
        bounds[4*i+3] = (int) ceil(maxY);
    }

    private float clamp(float t){
        return min(max(t, 0), 1);
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

    public int bound(char codepoint, int i) {
        return bounds[indexMap.get(codepoint)*4+i];
    }

    public int advance(char codepoint) {
        return advance[indexMap.get(codepoint)];
    }

    public int kern(char char0, char char1) {
        return kern[indexMap.get(char0)*len+indexMap.get(char1)];
    }

    public int indexOf(char codepoint) {
        return indexMap.get(codepoint);
    }
}