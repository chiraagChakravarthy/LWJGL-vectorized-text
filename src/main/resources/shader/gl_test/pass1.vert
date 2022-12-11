#version 330 core

layout (location = 0) in vec4 pos;

uniform float u_k;

uniform mat4 u_MVP;

out vec2 v_coord;

void main() {
    gl_Position = u_MVP*vec4(pos.x*0.1+u_k, pos.yzw);
    v_coord = (pos.xy+vec2(1.0,1.0))*0.5;
}