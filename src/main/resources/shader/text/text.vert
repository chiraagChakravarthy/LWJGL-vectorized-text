#version 330 core

#define padding 2

layout(location=0) in ivec2 glyphCorner;//which corner
layout(location=1) in int index;//which character in the string to render

uniform mat4 u_MVP;
uniform float uPixelSize;
uniform vec2 uTextPos;//pixel space coords of origin of first character in the string

uniform isamplerBuffer uAtlas;//the atlas for the font
uniform isamplerBuffer uString;//the characters and their advances

out vec2 vGlyphPos;//the glyph space position
out float vGlyph;//the codepoint being rendered

void main(){

    int codepoint = texelFetch(uString, index*2).x;
    int advance = texelFetch(uString, index*2+1).x;

    int gx = texelFetch(uAtlas, 257 + codepoint*4 + glyphCorner.x*2).x;
    int gy = texelFetch(uAtlas, 257 + codepoint*4 + glyphCorner.y*2+1).x;

    vec2 sPos = uTextPos +vec2(gx+advance, gy)/uPixelSize;

    vec2 roundedSpos = mix(floor(sPos-vec2(padding)), ceil(sPos+vec2(padding)), equal(glyphCorner, ivec2(1)));

    vec2 delta = (roundedSpos-sPos)*uPixelSize;
    vec2 roundedGlyphPos = vec2(gx, gy)+delta;

    gl_Position = u_MVP * vec4(roundedSpos, 0, 1);
    vGlyphPos = roundedGlyphPos;
    vGlyph = codepoint;
}