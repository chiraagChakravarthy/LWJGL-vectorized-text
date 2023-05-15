#version 330 core

/*
the main pipeline will work as follows

TERMS:
* glyph space: the space in which the beziers are defined
* em space: glyph space scaled by em scale. since this is the unit used to scale fonts normally, it standardizes size
* world space: the 3d space in which objects and models exist
* screen space: maps points to the screen in range (-1,1) on xy. the canonical opengl screen bounds.

* VERTEX
    * we are given the glyph, corner, and glyph space advance
    * find the box in glyph space
    * compute the box's center and find the corners
    * apply em scale to find vertices in em space
        - em space is standardized size
    * Apply pose, and mvp
        - pose maps the point from em space to 3d space
        - mvp maps the point from 3d space to screen space xy [-1, 1]
    * find delta from center to corner, normalize, double, add to corner (screenpoint)
    * pass screen point to frag and gl_Position
    * pass advance to frag
    * pass codepoint to frag

*FRAGMENT
    * we are given screen position, glyph advance, glyph, mvp, pose, viewport
    * compute min, max pos for pixel from viewport and screen pos
    * compute area.
        - For each bezier
            . shift over by advance
            . apply em scale
            . apply pose
            . apply mvp
            . compute area normally


*/

#define padding 2

layout(location=0) in ivec2 glyphCorner;//which corner
layout(location=1) in mat4 pose; //instance
layout(location=5) in int charIndex;//which character in the string to render (instance)
layout(location=6) in vec4 tint;//color (instance)

uniform isamplerBuffer u_Atlas;//the atlas for the font
uniform mat4 u_Mvp;//translates 3d world space -> screen space
uniform vec4 u_Viewport;
uniform int u_FontLen;

out vec2 vScreenPos;//the screen space position
out float vIndex;//index of the character being rendered
out vec4 vTint;
out mat4 vPose;


//compute glyph space min and max point
void main(){

    int j = charIndex*4 + u_FontLen +1;

    vec2 minG = vec2(texelFetch(u_Atlas, j).x, texelFetch(u_Atlas, j+1).x);
    vec2 maxG = vec2(texelFetch(u_Atlas, j+2).x, texelFetch(u_Atlas, j+3).x);

    vec2 g = mix(minG, maxG, glyphCorner);
    vec2 center = mix(minG, maxG, 0.5);

    vec4 sG = u_Mvp * pose * vec4(g, 0, 1);//convert to em space, then world space, then screen space

    vec4 sCenter = u_Mvp * pose * vec4(center, 0, 1);

    vec2 delta = (sG - sCenter).xy;
    sG.xy += normalize(delta)/vec2(u_Viewport.z, u_Viewport.w)*2;

    gl_Position = sG;
    vScreenPos = sG.xy;
    vIndex = charIndex;
    vTint = tint;
    vPose = pose;
}