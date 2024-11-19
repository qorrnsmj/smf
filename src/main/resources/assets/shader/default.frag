#version 330 core

uniform sampler2D texImage;
uniform vec3 lightPos;          // 点光源の位置
uniform vec3 ambientColor;      // 環境光の色
uniform float specularStrength; // 反射光の強度
uniform float shininess;        // 光沢度
uniform float constant;         // 減衰係数(定数項)
uniform float linear;           // 減衰係数(線形項)
uniform float quadratic;        // 減衰係数(二次項)

layout(location = 0) in vec4 color;
layout(location = 1) in vec2 texCoords;
layout(location = 2) in vec3 worldPosition;
layout(location = 3) in vec3 worldNormal;
layout(location = 4) in vec3 viewPosition;

layout(location = 0) out vec4 fragColor;

void main() {
    // 法線ベクトルの正規化
    vec3 norm = normalize(worldNormal);

    // 点光源からフラグメントまでのベクトルと距離
    vec3 lightDir = normalize(lightPos - worldPosition);
    float distance = length(lightPos - worldPosition);

    // 減衰の計算
    float attenuation = 1.0 / (constant + linear * distance + quadratic * (distance * distance));

    // 環境光
    vec3 ambient = ambientColor * attenuation;

    // 拡散光
    float diff = max(dot(norm, lightDir), 0.0);
    vec3 diffuse = vec3(diff * attenuation);

    // 反射光
    vec3 viewDir = normalize(viewPosition - worldPosition); // 視線方向ベクトル
    vec3 reflectDir = reflect(-lightDir, norm);  // 光源の反射ベクトル (光源ベクトルは逆向きに)
    float spec = pow(max(dot(viewDir, reflectDir), 0.0), shininess);
    vec3 specular = specularStrength * spec * attenuation * vec3(1.0); // 反射光の色は白

    // テクスチャカラー
    vec4 texColor = texture(texImage, texCoords);

    // 環境光 + 拡散光 + 反射光
    fragColor = vec4(0.5, 1.0, 0.0, 1.0) /**texColor * color * vec4(ambient + diffuse + specular, 1.0)*/;
}
