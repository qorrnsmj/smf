#version 330 core
#extension GL_ARB_separate_shader_objects : enable

uniform mat4 model;
uniform mat4 lightSpaceMatrix;

layout(location = 0) in vec3 position;
layout(location = 0) out vec3 worldPosition;

void main() {
    vec4 world = model * vec4(position, 1.0);
    worldPosition = world.xyz;
    gl_Position = lightSpaceMatrix * world;
}
