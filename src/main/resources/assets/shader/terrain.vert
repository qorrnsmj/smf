#version 330 core

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;
uniform mat4 lightSpaceMatrix;

layout(location = 0) in vec3 position;
layout(location = 1) in vec2 texCoord;
layout(location = 2) in vec3 normal;

layout(location = 0) out vec2 outTexCoord;
layout(location = 1) out vec3 worldPosition;
layout(location = 2) out vec4 lightSpacePosition;
layout(location = 3) out vec3 worldNormal;

void main() {
    vec4 world = model * vec4(position, 1.0);
    gl_Position = projection * view * world;
    outTexCoord = texCoord;
    worldPosition = vec3(world);
    lightSpacePosition = lightSpaceMatrix * world;
    worldNormal = mat3(transpose(inverse(model))) * normal;
}
