#version 330 core

struct Light {
    vec3 position;
    vec3 ambient;
    vec3 diffuse;
    vec3 specular;
    float shininess;
    float constant;
    float linear;
    float quadratic;
};

uniform sampler2D texImage;
uniform Light lights[100];
uniform int light_count;

layout(location = 0) in vec4 color;
layout(location = 1) in vec2 texCoord;
layout(location = 2) in vec3 worldPosition;
layout(location = 3) in vec3 worldNormal;
layout(location = 4) in vec3 viewPosition;

layout(location = 0) out vec4 fragColor;

void main() {
    vec3 norm = normalize(worldNormal);
    vec3 viewDir = normalize(viewPosition - worldPosition);
    vec3 result = vec3(0.0);

    for (int i = 0; i < light_count; i++) {
        vec3 lightDir = normalize(lights[i].position - worldPosition);
        float distance = length(lights[i].position - worldPosition);
        float attenuation = 1.0 / (lights[i].constant + lights[i].linear * distance + lights[i].quadratic * (distance * distance));

        // Ambient
        vec3 ambient = lights[i].ambient * attenuation;

        // Diffuse
        float diff = max(dot(norm, lightDir), 0.0);
        vec3 diffuse = lights[i].diffuse * diff * attenuation;

        // Specular
        vec3 reflectDir = reflect(-lightDir, norm);
        float spec = pow(max(dot(viewDir, reflectDir), 0.0), lights[i].shininess);
        vec3 specular = lights[i].specular * spec * attenuation;

        result += ambient + diffuse + specular;
    }

    vec4 texColor = texture(texImage, texCoord);
    fragColor = texColor * color * vec4(result, 1.0);
}
