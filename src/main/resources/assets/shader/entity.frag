#version 330 core

const int MAX_LOCAL_LIGHTS = 30;
const int MAX_POINT_SHADOWS = 8;

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

struct SunLight {
    vec3 direction;
    vec3 color;
    float intensity;
    vec3 ambientColor;
    float ambientIntensity;
};

struct PbrMaterial {
    vec4 baseColorFactor;
    vec3 emissiveFactor;
    float metallicFactor;
    float roughnessFactor;

    sampler2D baseColorTexture;
    sampler2D metallicRoughnessTexture;
    sampler2D normalTexture;
    sampler2D occlusionTexture;
    sampler2D emissiveTexture;

    float normalScale;
    float occlusionStrength;

    int alphaMode;
    float alphaCutoff;
    bool doubleSided;
};

uniform int lightCount;
uniform Light lights[MAX_LOCAL_LIGHTS];
uniform SunLight sunLight;
uniform vec3 cameraPosition;
uniform vec3 skyColor;
uniform sampler2D shadowMap;
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
uniform PbrMaterial material;

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
layout(location = 1) in vec4 tangent;
layout(location = 2) in vec3 worldPosition;
layout(location = 3) in vec3 worldNormal;
layout(location = 4) in vec3 viewPosition;
layout(location = 5) in vec4 lightSpacePosition;

out vec4 fragColor;

const float PI = 3.14159265359;

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

float DistributionGGX(vec3 N, vec3 H, float roughness) {
    float a = roughness * roughness;
    float a2 = a * a;
    float NdotH = max(dot(N, H), 0.0);
    float denom = (NdotH * NdotH) * (a2 - 1.0) + 1.0;
    return a2 / (PI * denom * denom);
}

float GeometrySchlickGGX(float NdotV, float roughness) {
    float r = roughness + 1.0;
    float k = (r * r) / 8.0;
    return NdotV / (NdotV * (1.0 - k) + k);
}

float GeometrySmith(vec3 N, vec3 V, vec3 L, float roughness) {
    return GeometrySchlickGGX(max(dot(N, V), 0.0), roughness)
         * GeometrySchlickGGX(max(dot(N, L), 0.0), roughness);
}

vec3 FresnelSchlick(float cosTheta, vec3 F0) {
    return F0 + (1.0 - F0) * pow(1.0 - cosTheta, 5.0);
}

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

vec3 calculatePbrLight(
    vec3 N,
    vec3 V,
    vec3 L,
    vec3 radiance,
    vec3 baseColor,
    float metallic,
    float roughness,
    vec3 F0
) {
    vec3 H = normalize(V + L);
    float NDF = DistributionGGX(N, H, roughness);
    float G = GeometrySmith(N, V, L, roughness);
    vec3 F = FresnelSchlick(max(dot(H, V), 0.0), F0);
    vec3 kS = F;
    vec3 kD = (1.0 - kS) * (1.0 - metallic);
    vec3 numerator = NDF * G * kS;
    float denom = 4.0 * max(dot(N, V), 0.0) * max(dot(N, L), 0.0) + 0.001;
    vec3 specular = numerator / denom;
    vec3 diffuse = kD * baseColor / PI;
    float NdotL = max(dot(N, L), 0.0);
    return (specular + diffuse) * radiance * NdotL;
}

void main() {
    vec4 baseColor = texture(material.baseColorTexture, texCoord) * material.baseColorFactor;
    if (material.alphaMode == 1 && baseColor.a < material.alphaCutoff) {
        discard;
    }

    vec3 N = normalize(worldNormal);
    vec3 T = normalize(tangent.xyz);
    vec3 B = normalize(cross(N, T) * tangent.w);
    mat3 TBN = mat3(T, B, N);
    vec3 normalMap = texture(material.normalTexture, texCoord).rgb;
    normalMap = normalMap * 2.0 - 1.0;
    normalMap.xy *= material.normalScale;
    N = normalize(TBN * normalMap);

    vec4 mr = texture(material.metallicRoughnessTexture, texCoord);
    float metallic = clamp(material.metallicFactor * mr.b, 0.0, 1.0);
    float roughness = clamp(material.roughnessFactor * mr.g, 0.08, 1.0);
    vec3 F0 = mix(vec3(0.04), baseColor.rgb, metallic);

    vec3 V = normalize(cameraPosition - worldPosition);
    vec3 sunL = normalize(-sunLight.direction);
    float sunBias = max(0.00004, 0.00018 * (1.0 - max(dot(N, sunL), 0.0)));
    float sunVisibility = shadowEnabled ? calculateShadowVisibility(lightSpacePosition, sunBias) : 1.0;
    float ambientShadow = mix(0.48, 1.0, sunVisibility);
    vec3 ambient = sunLight.ambientColor * sunLight.ambientIntensity * baseColor.rgb * ambientShadow;
    vec3 Lo = ambient;

    vec3 sunRadiance = sunLight.color * sunLight.intensity * sunVisibility;
    Lo += calculatePbrLight(N, V, sunL, sunRadiance, baseColor.rgb, metallic, roughness, F0);

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

        vec3 radiance = lights[i].color * lights[i].intensity * attenuation * spotFactor * lightVisibility;
        Lo += calculatePbrLight(N, V, L, radiance, baseColor.rgb, metallic, roughness, F0);
    }

    float ao = texture(material.occlusionTexture, texCoord).r;
    ao = mix(1.0, ao, material.occlusionStrength);
    vec3 emissive = material.emissiveFactor * texture(material.emissiveTexture, texCoord).rgb;
    fragColor = vec4(Lo * ao + emissive, baseColor.a);
    fragColor = mix(fragColor, vec4(fog.color, 1.0), calculateFogAmount(fog, worldPosition));
}
