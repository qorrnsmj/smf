#version 330 core

uniform sampler2D texImage;
uniform vec3 lightDir;          // 光源の方向ベクトル
uniform vec3 ambientColor;      // 環境光の色
uniform float specularStrength; // 反射光の強度
uniform float shininess;        // 光沢度

layout(location = 0) in vec4 vertexColor;
layout(location = 1) in vec2 texCoord;
layout(location = 2) in vec3 fragNormal;
layout(location = 3) in vec3 fragPos;
layout(location = 4) in vec3 viewPos; // カメラの位置

layout(location = 0) out vec4 fragColor;

void main() {
    // 法線ベクトルと光源ベクトルの正規化
    vec3 norm = normalize(fragNormal);
    vec3 light = normalize(-lightDir);

    // 環境光
    vec3 ambient = ambientColor;

    // 拡散光
    float diff = max(dot(norm, light), 0.0);
    vec3 diffuse = vec3(diff, diff, diff);

    // 反射光
    vec3 viewDir = normalize(viewPos - fragPos); // 視線方向ベクトル
    vec3 reflectDir = reflect(-light, norm);     // 光源の反射ベクトル (光源ベクトルは逆向きに?)
    float spec = pow(max(dot(viewDir, reflectDir), 0.0), shininess);
    vec3 specular = specularStrength * spec * vec3(1.0); // 反射光の色は白とする

    // テクスチャカラー
    vec4 texColor = texture(texImage, texCoord);

    // 環境光 + 拡散光 + 反射光
    fragColor = texColor * vertexColor * vec4(ambient + diffuse + specular, 1.0);
}
