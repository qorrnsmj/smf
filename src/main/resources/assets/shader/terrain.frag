#version 330 core

const int MAX_LOCAL_LIGHTS = 30;
const int MAX_POINT_SHADOWS = 8;

uniform sampler2D blendMap;
uniform sampler2D texGrass;
uniform sampler2D texFlower;
uniform sampler2D texDirt;
uniform sampler2D texPath;
uniform sampler2D shadowMap;
uniform vec3 skyColor;
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

layout(location = 0) in vec2 texCoord;
layout(location = 1) in float visibility;
layout(location = 2) in vec4 lightSpacePosition;
layout(location = 3) in vec3 worldNormal;
layout(location = 4) in vec3 worldPosition;

out vec4 fragColor;

float calculateShadowVisibility(vec4 lightSpacePos, float bias) {
    vec3 projected = lightSpacePos.xyz / lightSpacePos.w;
    projected = projected * 0.5 + 0.5;
    if (projected.z > 1.0 || projected.x < 0.0 || projected.x > 1.0 || projected.y < 0.0 || projected.y > 1.0) {
        return 1.0;
    }
    float shadow = 0.0;
    vec2 texelSize = 1.0 / vec2(textureSize(shadowMap, 0));
    for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
            float closestDepth = texture(shadowMap, projected.xy + vec2(x, y) * texelSize).r;
            shadow += projected.z - bias > closestDepth ? 1.0 : 0.0;
        }
    }
    shadow /= 9.0;
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

    vec3 N = normalize(worldNormal);
    vec3 sunL = normalize(-sunLight.direction);
    float sunBias = max(0.00004, 0.00018 * (1.0 - max(dot(N, sunL), 0.0)));
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
    fragColor = mix(vec4(skyColor, 1.0), finalColor, visibility);
}
