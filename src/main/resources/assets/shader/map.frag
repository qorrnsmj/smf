#version 330 core

uniform sampler2D mapTexture;
uniform vec3 skyColor;
uniform vec3 cameraPosition;

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
    vec4 finalColor = texture(mapTexture, texCoord);
    float fogAmount = calculateFogAmount(fog, worldPosition);

    fragColor = mix(finalColor, vec4(fog.color, 1.0), fogAmount);
}
