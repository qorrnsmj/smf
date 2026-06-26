#version 330 core

uniform mat4 model;
uniform mat4 lightSpaceMatrix;

layout(location = 0) in vec3 position;

void main() {
    gl_Position = lightSpaceMatrix * model * vec4(position, 1.0);
}
