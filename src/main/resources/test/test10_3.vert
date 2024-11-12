#version 330 core

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;

layout(location = 0) in vec3 position;
layout(location = 1) in vec4 color;
layout(location = 2) in vec2 uv;
layout(location = 3) in vec3 normal;

layout(location = 0) out vec4 vertexColor;
layout(location = 1) out vec2 texCoord;
layout(location = 2) out vec3 fragNormal;
layout(location = 3) out vec3 fragPos;
layout(location = 4) out vec3 viewPos; // カメラの位置

void main() {
    // クリップ空間の位置を計算
    gl_Position = projection * view * model * vec4(position, 1.0);

    vertexColor = color;
    texCoord = uv;
    fragNormal = mat3(transpose(inverse(model))) * normal;
    fragPos = vec3(model * vec4(position, 1.0));
    viewPos = vec3(inverse(view)[3]); // カメラの位置: view行列の逆行列の第4列のXYZ成分
}
