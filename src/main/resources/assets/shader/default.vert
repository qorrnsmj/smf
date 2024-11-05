#version 330 core

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;

layout(location = 0) in vec3 position;
layout(location = 1) in vec4 color;
//layout(location = 2) in vec2 texcoord;

layout(location = 0) out vec4 vertexColor;
//layout(location = 1) out vec2 textureCoord;

void main() {
    gl_Position = projection * view * model * vec4(position, 1.0);
    vertexColor = color;
    //textureCoord = texcoord;
}
