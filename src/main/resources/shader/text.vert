#version 330 core
layout(location=0) in vec4 position;//pixel space position
layout(location=1) in vec2 glyphPos;//glyph space position

uniform mat4 u_MVP;

out vec2 pos;

void main(){
    gl_Position = u_MVP*position;
    pos = glyphPos;
}