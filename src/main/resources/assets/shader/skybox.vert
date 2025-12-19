#version 330 core

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;

layout(location = 0) in vec3 position;

layout(location = 0) out vec3 texCoord;

void main() {
    // positionを方向ベクトルとして使う
    texCoord = position;

    // 通常の変換
    vec4 pos = projection * view * model * vec4(position, 1.0);

    // ★ 無限遠に固定（超重要）
    gl_Position = pos.xyww;
}
