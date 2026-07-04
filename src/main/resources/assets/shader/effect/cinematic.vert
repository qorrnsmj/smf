#version 330
#extension GL_ARB_separate_shader_objects : enable

layout(location = 0) in vec3 position;
layout(location = 1) in vec2 texCoord;

layout(location = 0) out vec2 outTexCoord;

void main() {
    gl_Position = vec4(position, 1.0);
    outTexCoord = texCoord;
}
