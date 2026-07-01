#version 330 core

uniform sampler2D blendMap;
uniform sampler2D texGrass;
uniform sampler2D texFlower;
uniform sampler2D texDirt;
uniform sampler2D texPath;
uniform vec3 skyColor;
uniform vec3 cameraPosition;
uniform bool useSingleTexture;

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

out vec4 fragColor;

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
        // Single texture mode: use only texGrass
        finalColor = texture(texGrass, tiledCoord);
    } else {
        // Blended texture mode: blend using blend map
        vec4 blendMapColor = texture(blendMap, texCoord);
        float grassAmount = max(0.0, 1.0 - (blendMapColor.r + blendMapColor.g + blendMapColor.b));

        vec4 grassColor = texture(texGrass, tiledCoord) * grassAmount;
        vec4 flowerColor = texture(texFlower, tiledCoord) * blendMapColor.g;
        vec4 dirtColor = texture(texDirt, tiledCoord) * blendMapColor.r;
        vec4 pathColor = texture(texPath, tiledCoord) * blendMapColor.b;

        finalColor = grassColor + flowerColor + dirtColor + pathColor;
    }

    fragColor = mix(finalColor, vec4(fog.color, 1.0), calculateFogAmount(fog, worldPosition));
}
