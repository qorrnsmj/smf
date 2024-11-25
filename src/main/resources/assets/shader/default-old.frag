#version 330 core

struct Light {
    vec3 position;
    vec3 ambient;
    vec3 diffuse;
    vec3 specular;
    float constant;
    float linear;
    float quadratic;
};

struct Material {
    vec3 diffuseColor;
    vec3 ambientColor;
    vec3 specularColor;
    vec3 emissiveColor;
    float shininess;
    float opacity;
    sampler2D diffuseTexture;
    sampler2D specularTexture;
    sampler2D normalTexture;
};

uniform Light lights[100];
uniform int light_count;
uniform Material material;

layout(location = 0) in vec4 vertexColor;
layout(location = 1) in vec2 texCoord;
layout(location = 2) in vec3 tangent;
layout(location = 3) in vec3 worldPosition;
layout(location = 4) in vec3 worldNormal;
layout(location = 5) in vec3 viewPosition;

layout(location = 0) out vec4 fragColor;

void main() {
    // Calculate TBN matrix
    vec3 tang = normalize(tangent);
    vec3 bitang = normalize(cross(worldNormal, tang));
    vec3 norm = normalize(worldNormal);
    mat3 TBN = mat3(tang, bitang, norm);

    // Normal mapping
    vec3 texNormal = texture(material.normalTexture, texCoord).rgb;
    texNormal = normalize(texNormal * 2.0 - 1.0); // Convert to [-1, 1] range
    norm = normalize(TBN * texNormal); // Apply normal mapping using TBN

    // Process lights
    vec3 viewDir = normalize(viewPosition - worldPosition);
    vec3 result = material.emissiveColor; // Start with emissive color
    for (int i = 0; i < light_count; i++) {
        vec3 lightDir = normalize(lights[i].position - worldPosition);
        float distance = length(lights[i].position - worldPosition);
        float attenuation = 1.0 / (lights[i].constant + lights[i].linear * distance + lights[i].quadratic * (distance * distance));

        // Ambient
        vec3 ambient = lights[i].ambient * material.ambientColor * attenuation;

        // Diffuse
        float diff = max(dot(norm, lightDir), 0.0);
        vec3 diffuseTex = texture(material.diffuseTexture, texCoord).rgb;
        vec3 diffuse = lights[i].diffuse * diffuseTex * diff * attenuation;

        // Specular
        vec3 reflectDir = reflect(-lightDir, norm);
        float spec = pow(max(dot(viewDir, reflectDir), 0.0), material.shininess);
        vec3 specularTex = texture(material.specularTexture, texCoord).rgb;
        vec3 specular = lights[i].specular * specularTex * spec * attenuation;

        result += ambient + diffuse + specular;
    }

    // Final color
    vec3 finalColor = result * material.diffuseColor; // Combine with material's diffuse color
    fragColor = vec4(finalColor, material.opacity); // Apply opacity
}
