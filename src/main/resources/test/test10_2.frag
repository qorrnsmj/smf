#version 330 core

uniform sampler2D texImage;
uniform vec3 lightDir;     // 光源の方向ベクトル
uniform vec3 ambientColor; // 環境光の色

layout(location = 0) in vec4 vertexColor;
layout(location = 1) in vec2 texCoord;
layout(location = 2) in vec3 fragNormal;

layout(location = 0) out vec4 fragColor;

void main() {
    vec3 norm = normalize(fragNormal);
    vec3 light = normalize(-lightDir);

    // 拡散光
    float diff = max(dot(norm, light), 0.0);
    vec3 diffuse = vec3(diff, diff, diff); // ベクトルに

    // 環境光
    vec3 ambient = ambientColor;

    // テクスチャカラー
    vec4 texColor = texture(texImage, texCoord);

    // 環境光 + 拡散光
    fragColor = texColor * vertexColor * vec4(ambient + diffuse, 1.0);
}
