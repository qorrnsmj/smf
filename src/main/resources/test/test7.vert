#version 330 core

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;

layout(location = 0) in vec3 pos;
layout(location = 1) in vec4 color;
layout(location = 0) out vec4 vertexColor;

void main() {
    gl_Position = projection * view * model * vec4(pos, 1.0);
    vertexColor = color;
}
