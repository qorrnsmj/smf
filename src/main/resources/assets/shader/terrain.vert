#version 330 core

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;
uniform float fogDensity;
uniform float fogGradient;

layout(location = 0) in vec3 position;
layout(location = 1) in vec2 texCoord;

layout(location = 0) out vec2 outTexCoord;
layout(location = 1) out float visibility;

void main() {
    gl_Position = projection * view * model * vec4(position, 1.0);
    outTexCoord = texCoord;

    // fog visibility
    vec3 worldPosition = vec3(model * vec4(position, 1.0));
    vec3 viewPosition = vec3(inverse(view)[3]);
    float distance = length(viewPosition - worldPosition);
    visibility = exp(-pow(distance * fogDensity, fogGradient));
    visibility = clamp(visibility, 0.0, 1.0);
}
