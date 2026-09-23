#version 330 core

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;

layout(location = 0) in vec3 position;
layout(location = 0) out vec3 domeDirection;

void main() {
    domeDirection = normalize(position);
    vec4 positionClip = projection * view * model * vec4(position, 1.0);
    gl_Position = positionClip.xyww;
}
