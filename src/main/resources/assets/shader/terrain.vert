#version 330 core

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;
uniform mat4 lightSpaceMatrix;
uniform float fogDensity;
uniform float fogGradient;

layout(location = 0) in vec3 position;
layout(location = 1) in vec2 texCoord;

layout(location = 0) out vec2 outTexCoord;
layout(location = 1) out float visibility;
layout(location = 2) out vec4 lightSpacePosition;

void main() {
    vec4 world = model * vec4(position, 1.0);
    gl_Position = projection * view * world;
    outTexCoord = texCoord;
    lightSpacePosition = lightSpaceMatrix * world;

    // fog visibility
    vec3 worldPosition = vec3(world);
    vec3 viewPosition = vec3(inverse(view)[3]);
    float distance = length(viewPosition - worldPosition);
    visibility = exp(-pow(distance * fogDensity, fogGradient));
    visibility = clamp(visibility, 0.0, 1.0);
}
