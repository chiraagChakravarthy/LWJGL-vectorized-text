#version 330 core
layout(location=0) in vec2 position;//pixel space position
layout(location=1) in vec2 glyphPos;//glyph space position
layout(location=2) in int glyph;//which glyph to render

uniform mat4 u_MVP;

out vec2 vGlyphPos;
out int vGlyph;
out vec3 vTint;

void main(){
    gl_Position = u_MVP*vec4(position, 0, 1);
    vGlyphPos = glyphPos;
    vGlyph = glyph;
    vTint = vec3(0);
}