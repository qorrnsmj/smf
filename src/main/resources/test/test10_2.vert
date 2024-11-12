#version 330 core

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;

layout(location = 0) in vec3 position;
layout(location = 1) in vec4 color;
layout(location = 2) in vec2 uv;
layout(location = 3) in vec3 normal; // 法線

layout(location = 0) out vec4 vertexColor;
layout(location = 1) out vec2 texCoord;
layout(location = 2) out vec3 fragNormal; // フラグメントシェーダに渡す法線

void main() {
    gl_Position = projection * view * model * vec4(position, 1.0);
    vertexColor = color;
    texCoord = uv;
    fragNormal = mat3(transpose(inverse(model))) * normal; // 法線をワールド座標に変換 (逆転置)
}
