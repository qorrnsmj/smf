#version 330 core

struct Light {
    vec3 position;
    vec3 color;
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

    // 0: OPAQUE, 1: MASK, 2: BLEND
    int alphaMode;
    float alphaCutoff;
    bool doubleSided; // TODO
};

uniform int lightCount;
uniform Light lights[30];
uniform vec3 cameraPosition;
uniform vec3 skyColor;
uniform PbrMaterial material;

layout(location = 0) in vec2 texCoord;
layout(location = 1) in vec4 tangent;
layout(location = 2) in vec3 worldPosition;
layout(location = 3) in vec3 worldNormal;
layout(location = 4) in vec3 viewPosition;
layout(location = 5) in float visibility;

out vec4 fragColor;

const float PI = 3.14159265359;

/* PBR Utils */

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

/* Main */

void main() {
    vec4 baseColor = texture(material.baseColorTexture, texCoord)
        * material.baseColorFactor;

    // alpha test (MASK)
    // TODO: ifdef ALPHA_MASK
    if (material.alphaMode == 1) {
        if (baseColor.a < material.alphaCutoff) {
            discard;
        }
    }

    // tangent space to world space
    vec3 N = normalize(worldNormal);
    vec3 T = normalize(tangent.xyz);
    vec3 B = normalize(cross(N, T) * tangent.w);
    mat3 TBN = mat3(T, B, N);

    // final normal with normal map
    vec3 normalMap = texture(material.normalTexture, texCoord).rgb;
    normalMap = normalMap * 2.0 - 1.0; // [0,1] -> [-1,1]
    normalMap.xy *= material.normalScale;
    N = normalize(TBN * normalMap);

    // metallic and roughness
    vec4 mr = texture(material.metallicRoughnessTexture, texCoord);
    float metallic  = material.metallicFactor  * mr.b;
    float roughness = material.roughnessFactor * mr.g;
    vec3 F0 = mix(vec3(0.04), baseColor.rgb, metallic);
    // if metallic is 1.0, baseColor is treated as reflectance
    // if metallic is 0.0, baseColor is treated as albedo

    // lighting
    vec3 Lo = vec3(0.0);
    vec3 V = normalize(cameraPosition - worldPosition);
    for (int i = 0; i < lightCount; i++) {
        // light direction
        vec3 L = normalize(lights[i].position - worldPosition);
        vec3 H = normalize(V + L);

        // cook-torrance BRDF
        float NDF = DistributionGGX(N, H, roughness);
        float G   = GeometrySmith(N, V, L, roughness);
        vec3  F   = FresnelSchlick(max(dot(H, V), 0.0), F0);

        // coefficients of specular and diffuse
        vec3 kS = F;
        vec3 kD = (1.0 - kS) * (1.0 - metallic);

        // specular
        vec3 numerator = NDF * G * kS;
        float denom = 4.0 * max(dot(N, V), 0.0) * max(dot(N, L), 0.0) + 0.001;
        vec3 specular = numerator / denom;

        // diffuse and NdotL
        vec3 diffuse = kD * baseColor.rgb / PI;
        float NdotL = max(dot(N, L), 0.0);

        Lo += (specular + diffuse) * lights[i].color * NdotL;
    }

    // ambient occlusion
    float ao = texture(material.occlusionTexture, texCoord).r;
    ao = mix(1.0, ao, material.occlusionStrength);

    // emissive
    vec3 emissive = material.emissiveFactor
        * texture(material.emissiveTexture, texCoord).rgb;

    float exposure = 5.0; // TEST: remove exposure later and use intensity
    fragColor = vec4(Lo * ao * exposure + emissive, baseColor.a);
    fragColor = mix(vec4(skyColor, 1.0), fragColor, visibility);
}
