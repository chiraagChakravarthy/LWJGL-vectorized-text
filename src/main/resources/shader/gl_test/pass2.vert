#version 330 core

layout (location = 0) in vec4 pos;
layout (location = 1) in vec2 texCoord;

out vec2 v_texCoord;
uniform mat4 u_MVP;
uniform float u_k;

void main() {
    gl_Position = u_MVP*pos;
    v_texCoord = texCoord;
}