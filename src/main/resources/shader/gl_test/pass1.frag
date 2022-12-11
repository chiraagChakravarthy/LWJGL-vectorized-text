#version 330 core
layout(location = 0) out vec4 color;

in vec2 v_coord;

void main() {
    color = vec4(vec3(0), 1);
}