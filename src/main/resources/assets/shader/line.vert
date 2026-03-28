#version 330 core

layout(location = 0) in vec3 position;
layout(location = 1) in vec4 color;

out vec4 v_color;

uniform mat4 u_mvpMatrix;

void main() {
    gl_Position = u_mvpMatrix * vec4(position, 1.0);
    v_color = color;
}