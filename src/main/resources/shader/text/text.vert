#version 330 core
layout(location=0) in vec4 position;//pixel space position
layout(location=1) in vec2 glyphPos;//glyph space position
layout(location=2) in int glyph;//which glyph to render

uniform mat4 u_MVP;

out vec2 vGlyphPos;
out int vGlyph;

void main(){
    gl_Position = u_MVP*position;
    vGlyphPos = glyphPos;
    vGlyph = glyph;
}