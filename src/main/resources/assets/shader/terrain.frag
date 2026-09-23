#version 330 core
#extension GL_ARB_separate_shader_objects : enable

const int MAX_LOCAL_LIGHTS = 30;
const int MAX_POINT_SHADOWS = 8;

uniform sampler2D blendMap;
uniform sampler2D texGrass;
uniform sampler2D texFlower;
uniform sampler2D texDirt;
uniform sampler2D texPath;
uniform sampler2D shadowMap;
uniform vec3 skyColor;
uniform vec3 cameraPosition;
uniform bool grayView;
uniform vec4 grayColor;
uniform bool useSingleTexture;
uniform bool shadowEnabled;
uniform float shadowStrength;
uniform sampler2DArray localShadowMap;
uniform int localShadowCount;
uniform mat4 localShadowMatrices[MAX_LOCAL_LIGHTS];
uniform float localShadowStrengths[MAX_LOCAL_LIGHTS];
uniform int localLightShadowIndices[MAX_LOCAL_LIGHTS];
uniform samplerCube pointShadowMaps[MAX_POINT_SHADOWS];
uniform int pointShadowCount;
uniform float pointShadowFarPlanes[MAX_POINT_SHADOWS];
uniform float pointShadowStrengths[MAX_POINT_SHADOWS];
uniform int pointLightShadowIndices[MAX_LOCAL_LIGHTS];
uniform int lightCount;

struct SunLight {
    vec3 direction;
    vec3 color;
    float intensity;
    vec3 ambientColor;
    float ambientIntensity;
};
uniform SunLight sunLight;

struct Light {
    vec3 position;
    vec3 color;
    float intensity;
    float constant;
    float linear;
    float quadratic;
    int type;
    vec3 direction;
    float innerCutOff;
    float outerCutOff;
};
uniform Light lights[MAX_LOCAL_LIGHTS];

struct Fog {
    int enabled;
    vec3 color;
    float distanceDensity;
    float distanceGradient;
    float heightDensity;
    float bottomY;
    float topY;
    float heightFalloff;
    float heightDistanceStart;
    float heightDistanceEnd;
};

uniform Fog fog;

layout(location = 0) in vec2 texCoord;
layout(location = 1) in vec3 worldPosition;
layout(location = 2) in vec4 lightSpacePosition;
layout(location = 3) in vec3 worldNormal;

out vec4 fragColor;

float calculateShadowVisibility(vec4 lightSpacePos, float bias) {
    vec3 projected = lightSpacePos.xyz / lightSpacePos.w;
    projected = projected * 0.5 + 0.5;

    // Compare each PCF tap with the receiver plane at that texel, not its centre depth.
    vec3 dx = dFdx(projected);
    vec3 dy = dFdy(projected);
    float determinant = dx.x * dy.y - dx.y * dy.x;
    vec2 depthGradient = vec2(0.0);
    float derivativeScale = dot(dx.xy, dx.xy) * dot(dy.xy, dy.xy);
    if (derivativeScale > 1e-30) {
        // Regularize nearly parallel derivatives continuously at grazing angles.
        float inverseDeterminant = determinant / (determinant * determinant + derivativeScale * 1e-6);
        depthGradient = vec2(dy.y * dx.z - dx.y * dy.z, dx.x * dy.z - dy.x * dx.z) * inverseDeterminant;
    }
    if (projected.z < 0.0 || projected.z > 1.0 || projected.x < 0.0 || projected.x > 1.0 || projected.y < 0.0 || projected.y > 1.0) {
        return 1.0;
    }
    float shadow = 0.0;
    vec2 texelSize = 1.0 / vec2(textureSize(shadowMap, 0));
    vec2 texelPosition = projected.xy / texelSize - 0.5;
    vec2 baseTexel = floor(texelPosition);
    vec2 fraction = fract(texelPosition);
    float totalWeight = 0.0;
    for (int x = -1; x <= 2; x++) {
        for (int y = -1; y <= 2; y++) {
            vec2 offset = vec2(x, y);
            vec2 axisWeight = max(vec2(0.0), vec2(2.0) - abs(offset - fraction));
            float weight = axisWeight.x * axisWeight.y;
            vec2 sampleUV = (baseTexel + offset + 0.5) * texelSize;
            float closestDepth = texture(shadowMap, sampleUV).r;
            float receiverDepth = projected.z + dot(depthGradient, sampleUV - projected.xy);
            shadow += weight * (receiverDepth - bias > closestDepth ? 1.0 : 0.0);
            totalWeight += weight;
        }
    }
    shadow /= totalWeight;
    float edgeDistance = min(min(projected.x, 1.0 - projected.x), min(projected.y, 1.0 - projected.y));
    shadow *= smoothstep(0.0, 0.06, edgeDistance);
    return mix(1.0, 1.0 - shadowStrength, shadow);
}

float calculateLocalShadowVisibility(int shadowIndex, vec3 position, float bias) {
    vec4 lightSpacePos = localShadowMatrices[shadowIndex] * vec4(position, 1.0);
    vec3 projected = lightSpacePos.xyz / lightSpacePos.w;
    projected = projected * 0.5 + 0.5;
    if (projected.z > 1.0 || projected.x < 0.0 || projected.x > 1.0 || projected.y < 0.0 || projected.y > 1.0) {
        return 1.0;
    }
    float shadow = 0.0;
    vec2 texelSize = 1.0 / vec2(textureSize(localShadowMap, 0).xy);
    for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
            float closestDepth = texture(localShadowMap, vec3(projected.xy + vec2(x, y) * texelSize, float(shadowIndex))).r;
            shadow += projected.z - bias > closestDepth ? 1.0 : 0.0;
        }
    }
    shadow /= 9.0;
    return mix(1.0, 1.0 - localShadowStrengths[shadowIndex], shadow);
}

float samplePointShadowDepth(int shadowIndex, vec3 direction) {
    switch (shadowIndex) {
        case 0: return texture(pointShadowMaps[0], direction).r;
        case 1: return texture(pointShadowMaps[1], direction).r;
        case 2: return texture(pointShadowMaps[2], direction).r;
        case 3: return texture(pointShadowMaps[3], direction).r;
        case 4: return texture(pointShadowMaps[4], direction).r;
        case 5: return texture(pointShadowMaps[5], direction).r;
        case 6: return texture(pointShadowMaps[6], direction).r;
        case 7: return texture(pointShadowMaps[7], direction).r;
    }
    return 1.0;
}

float calculatePointShadowVisibility(int shadowIndex, vec3 lightPosition, vec3 position, float bias) {
    float farPlane = pointShadowFarPlanes[shadowIndex];
    if (farPlane <= 0.0) {
        return 1.0;
    }
    vec3 fragToLight = position - lightPosition;
    float currentDepth = length(fragToLight) / farPlane;
    float closestDepth = samplePointShadowDepth(shadowIndex, fragToLight);
    float shadow = currentDepth - bias > closestDepth ? 1.0 : 0.0;
    return mix(1.0, 1.0 - pointShadowStrengths[shadowIndex], shadow);
}

float calculateFogAmount(Fog settings, vec3 position) {
    if (settings.enabled == 0) {
        return 0.0;
    }

    float distance = length(cameraPosition - position);
    float distanceFog = 1.0 - exp(-pow(distance * settings.distanceDensity, settings.distanceGradient));

    float heightRange = max(settings.topY - settings.bottomY, 1.0);
    float heightRatio = clamp((settings.topY - position.y) / heightRange, 0.0, 1.0);
    float heightDistance = smoothstep(
        settings.heightDistanceStart,
        max(settings.heightDistanceEnd, settings.heightDistanceStart + 1.0),
        distance
    );
    float heightFog = settings.heightDensity
        * pow(heightRatio, settings.heightFalloff)
        * heightDistance;

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
    if (grayView) {
        finalColor = grayColor;
    }

    vec3 N = normalize(worldNormal);
    vec3 sunL = normalize(-sunLight.direction);
    float sunBias = 0.00002;
    float sunVisibility = shadowEnabled ? calculateShadowVisibility(lightSpacePosition, sunBias) : 1.0;
    float sunNdotL = max(dot(N, sunL), 0.0);
    float ambientShadow = mix(0.48, 1.0, sunVisibility);
    vec3 lighting = sunLight.ambientColor * sunLight.ambientIntensity * ambientShadow;
    lighting += sunLight.color * sunLight.intensity * 0.38 * sunNdotL * sunVisibility;

    for (int i = 0; i < lightCount; i++) {
        vec3 L = normalize(lights[i].position - worldPosition);
        float distance = length(lights[i].position - worldPosition);
        float attenuation = 1.0 / (
            lights[i].constant
            + lights[i].linear * distance
            + lights[i].quadratic * distance * distance
        );
        float spotFactor = 1.0;
        if (lights[i].type == 1) {
            float theta = dot(normalize(-L), normalize(lights[i].direction));
            spotFactor = clamp((theta - lights[i].outerCutOff) / (lights[i].innerCutOff - lights[i].outerCutOff), 0.0, 1.0);
        }
        float lightVisibility = 1.0;
        int localShadowIndex = localLightShadowIndices[i];
        if (localShadowIndex >= 0 && localShadowIndex < localShadowCount) {
            float localBias = max(0.00005, 0.00022 * (1.0 - max(dot(N, L), 0.0)));
            lightVisibility = calculateLocalShadowVisibility(localShadowIndex, worldPosition, localBias);
        }
        int pointShadowIndex = pointLightShadowIndices[i];
        if (pointShadowIndex >= 0 && pointShadowIndex < pointShadowCount) {
            float pointBias = max(0.00008, 0.00018 * (1.0 - max(dot(N, L), 0.0)));
            lightVisibility = calculatePointShadowVisibility(pointShadowIndex, lights[i].position, worldPosition, pointBias);
        }
        float diffuse = max(dot(N, L), 0.0);
        lighting += lights[i].color * lights[i].intensity * attenuation * diffuse * spotFactor * lightVisibility;
    }

    finalColor.rgb *= lighting;
    fragColor = mix(finalColor, vec4(fog.color, 1.0), calculateFogAmount(fog, worldPosition));
}
