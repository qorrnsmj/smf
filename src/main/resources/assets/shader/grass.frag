#version 330 core

uniform vec3 cameraPosition;
uniform sampler2D shadowMap;
uniform int shadowEnabled;
uniform float shadowStrength;
uniform float shadowFilterRadius;
uniform float shadowMinBias;
uniform sampler2D foliageTexture;
uniform int textured;

const int MAX_LOCAL_LIGHTS = 30;

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
uniform int lightCount;

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

in vec3 worldPosition;
in float tint;
in vec4 lightSpacePosition;
in vec3 foliageColor;
in vec2 foliageTexCoord;
out vec4 fragColor;

float shadowVisibility() {
    if (shadowEnabled == 0 || lightSpacePosition.w <= 0.0) return 1.0;
    vec3 projected = lightSpacePosition.xyz / lightSpacePosition.w;
    projected = projected * 0.5 + 0.5;
    if (projected.z <= 0.0 || projected.z >= 1.0 ||
        projected.x <= 0.0 || projected.x >= 1.0 ||
        projected.y <= 0.0 || projected.y >= 1.0) return 1.0;

    vec2 texel = 1.0 / vec2(textureSize(shadowMap, 0));
    float occluded = 0.0;
    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            float closestDepth = texture(shadowMap,
                projected.xy + vec2(x, y) * texel * shadowFilterRadius).r;
            occluded += projected.z - shadowMinBias > closestDepth ? 1.0 : 0.0;
        }
    }
    return 1.0 - (occluded / 9.0) * clamp(shadowStrength, 0.0, 1.0);
}

float fogAmount() {
    if (fog.enabled == 0) return 0.0;
    float distance = length(cameraPosition - worldPosition);
    float distanceFog = 1.0 - exp(-pow(distance * fog.distanceDensity, fog.distanceGradient));
    float heightRatio = clamp((fog.topY - worldPosition.y) / max(fog.topY - fog.bottomY, 1.0), 0.0, 1.0);
    float heightDistance = smoothstep(fog.heightDistanceStart,
        max(fog.heightDistanceEnd, fog.heightDistanceStart + 1.0), distance);
    float heightFog = fog.heightDensity * pow(heightRatio, fog.heightFalloff) * heightDistance;
    return clamp(1.0 - (1.0 - distanceFog) * (1.0 - heightFog), 0.0, 1.0);
}

void main() {
    vec4 sampled = textured != 0 ? texture(foliageTexture, foliageTexCoord) : vec4(foliageColor, 1.0);
    if (sampled.a < 0.42) discard;
    vec3 normal = normalize(cross(dFdx(worldPosition), dFdy(worldPosition)));
    if (!gl_FrontFacing) normal = -normal;
    normal = normalize(mix(normal, vec3(0.0, 1.0, 0.0), 0.55));

    float sunVisibility = shadowVisibility();
    vec3 sunDirection = normalize(-sunLight.direction);
    float sunDiffuse = 0.35 + 0.65 * max(dot(normal, sunDirection), 0.0);
    vec3 lighting = sunLight.ambientColor * sunLight.ambientIntensity * sunVisibility;
    lighting += sunLight.color * sunLight.intensity * 0.38 * sunDiffuse * sunVisibility;

    for (int i = 0; i < lightCount; i++) {
        vec3 toLight = lights[i].position - worldPosition;
        float distanceToLight = length(toLight);
        vec3 lightDirection = toLight / max(distanceToLight, 0.0001);
        float attenuation = 1.0 / max(
            lights[i].constant
                + lights[i].linear * distanceToLight
                + lights[i].quadratic * distanceToLight * distanceToLight,
            0.0001
        );
        float spotFactor = 1.0;
        if (lights[i].type == 1) {
            float theta = dot(normalize(-lightDirection), normalize(lights[i].direction));
            spotFactor = clamp(
                (theta - lights[i].outerCutOff) / max(lights[i].innerCutOff - lights[i].outerCutOff, 0.0001),
                0.0,
                1.0
            );
        }
        float diffuse = 0.25 + 0.75 * max(dot(normal, lightDirection), 0.0);
        lighting += lights[i].color * lights[i].intensity * attenuation * diffuse * spotFactor;
    }

    vec3 color = sampled.rgb * tint * lighting;
    fragColor = vec4(mix(color, fog.color, fogAmount()), sampled.a);
}
