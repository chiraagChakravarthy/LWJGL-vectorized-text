#version 330 core
layout(location = 0) out vec4 color;

uniform sampler2D uTexture;

in vec2 v_texCoord;
uniform float u_k;

void main() {
    color = texture(uTexture, v_texCoord);
}