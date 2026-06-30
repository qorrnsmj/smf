#version 330 core

uniform sampler2D mapTexture;
uniform vec3 skyColor;
uniform vec3 cameraPosition;
uniform float fogDensity;
uniform float fogGradient;
uniform sampler2D shadowMap;
uniform bool shadowEnabled;

layout(location = 0) in vec2 texCoord;
layout(location = 1) in vec3 worldPosition;
layout(location = 2) in vec4 lightSpacePosition;

out vec4 fragColor;

float calculateShadow(vec4 lightSpacePos) {
    vec3 projected = lightSpacePos.xyz / lightSpacePos.w;
    projected = projected * 0.5 + 0.5;

    if (projected.z > 1.0 || projected.x < 0.0 || projected.x > 1.0 || projected.y < 0.0 || projected.y > 1.0) {
        return 1.0;
    }

    float bias = 0.003;
    float shadow = 0.0;
    vec2 texelSize = 1.0 / textureSize(shadowMap, 0);
    for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
            float closestDepth = texture(shadowMap, projected.xy + vec2(x, y) * texelSize).r;
            shadow += projected.z - bias > closestDepth ? 1.0 : 0.0;
        }
    }
    shadow /= 9.0;

    return mix(1.0, 0.55, shadow);
}

void main() {
    vec4 finalColor = texture(mapTexture, texCoord);
    if (shadowEnabled) {
        finalColor.rgb *= calculateShadow(lightSpacePosition);
    }

    float distance = length(cameraPosition - worldPosition);
    float visibility = exp(-pow(distance * fogDensity, fogGradient));
    visibility = clamp(visibility, 0.0, 1.0);

    fragColor = mix(vec4(skyColor, 1.0), finalColor, visibility);
}
