#version 330 core

uniform sampler2D texImage;
uniform vec3 lightDir; // 光源の方向ベクトル

layout(location = 0) in vec4 vertexColor;
layout(location = 1) in vec2 texCoord;
layout(location = 2) in vec3 fragNormal; // 法線ベクトル

layout(location = 0) out vec4 fragColor;

void main() {
    vec3 norm = normalize(fragNormal); // 法線の正規化
    vec3 light = normalize(-lightDir); // 光源の方向を反転して正規化 (光源の方向に向かうベクトルが必用)
    float diff = max(dot(norm, light), 0.0); // 拡散反射の計算

    vec4 texColor = texture(texImage, texCoord);
    fragColor = texColor * vertexColor * diff; // ライティング適用後の色
}
