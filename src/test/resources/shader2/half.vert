#version 330 core

layout(location=0) in vec4 pos;
layout(location=1) in vec2 texPos;

uniform vec2 screenBounds;
void main() {

    gl_Position = pos;
}