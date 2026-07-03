#version 330 core

uniform sampler2D blendMap;
uniform sampler2D texGrass;
uniform sampler2D texFlower;
uniform sampler2D texDirt;
uniform sampler2D texPath;
uniform sampler2D shadowMap;
uniform vec3 skyColor;
uniform vec3 cameraPosition;
uniform bool useSingleTexture;
uniform bool shadowEnabled;

struct Fog {
    int enabled;
    vec3 color;
    float distanceDensity;
    float distanceGradient;
    float heightDensity;
    float bottomY;
    float topY;
    float heightFalloff;
};

uniform Fog fog;

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
    vec2 texelSize = 1.0 / vec2(textureSize(shadowMap, 0));
    for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
            float closestDepth = texture(shadowMap, projected.xy + vec2(x, y) * texelSize).r;
            shadow += projected.z - bias > closestDepth ? 1.0 : 0.0;
        }
    }
    shadow /= 9.0;

    return mix(1.0, 0.55, shadow);
}

float calculateFogAmount(Fog settings, vec3 position) {
    if (settings.enabled == 0) {
        return 0.0;
    }

    float distance = length(cameraPosition - position);
    float distanceFog = 1.0 - exp(-pow(distance * settings.distanceDensity, settings.distanceGradient));

    float heightRange = max(settings.topY - settings.bottomY, 1.0);
    float heightRatio = clamp((settings.topY - position.y) / heightRange, 0.0, 1.0);
    float heightFog = settings.heightDensity * pow(heightRatio, settings.heightFalloff);

    return clamp(1.0 - (1.0 - distanceFog) * (1.0 - heightFog), 0.0, 1.0);
}

void main() {
    vec4 finalColor;
    vec2 tiledCoord = texCoord * 50.0;

    if (useSingleTexture) {
        finalColor = texture(texGrass, tiledCoord);
    } else {
        vec4 blendMapColor = texture(blendMap, texCoord);
        float grassAmount = max(0.0, 1.0 - (blendMapColor.r + blendMapColor.g + blendMapColor.b));

        vec4 grassColor = texture(texGrass, tiledCoord) * grassAmount;
        vec4 flowerColor = texture(texFlower, tiledCoord) * blendMapColor.g;
        vec4 dirtColor = texture(texDirt, tiledCoord) * blendMapColor.r;
        vec4 pathColor = texture(texPath, tiledCoord) * blendMapColor.b;

        finalColor = grassColor + flowerColor + dirtColor + pathColor;
    }

    if (shadowEnabled) {
        finalColor.rgb *= calculateShadow(lightSpacePosition);
    }

    fragColor = mix(finalColor, vec4(fog.color, 1.0), calculateFogAmount(fog, worldPosition));
}
