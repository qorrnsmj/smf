#version 330 core

uniform mat4 view;
uniform mat4 projection;
uniform vec3 center;
uniform vec2 size;
uniform vec3 billboardRight;
uniform vec3 billboardUp;

layout(location = 0) in vec2 position;
layout(location = 1) in vec2 texCoord;
layout(location = 0) out vec2 outTexCoord;

void main() {
    vec3 worldPosition = center
        + billboardRight * position.x * size.x
        + billboardUp * position.y * size.y;
    gl_Position = projection * view * vec4(worldPosition, 1.0);
    outTexCoord = texCoord;
}
