#version 330 core

uniform float time;
uniform float cloudScale;
uniform float cloudCoverage;
uniform float cloudSoftness;
uniform vec3 horizonColor;
uniform vec3 zenithColor;
uniform vec3 cloudColor;
uniform int useCloudTextures;
uniform sampler2D cloudBaseTexture;
uniform sampler2D cloudDetailTexture;
uniform int useSunTexture;
uniform sampler2D sunTexture;
uniform vec3 sunDirection;
uniform vec3 sunColor;
uniform float sunAngularSize;
uniform float sunHorizonGlowStrength;
uniform int useMoonTexture;
uniform sampler2D moonTexture;
uniform vec3 moonColor;
uniform float moonAngularSize;

layout(location = 0) in vec3 domeDirection;
layout(location = 0) out vec4 fragColor;

float hash(vec2 point) {
    return fract(sin(dot(point, vec2(127.1, 311.7))) * 43758.5453123);
}

float noise(vec2 point) {
    vec2 cell = floor(point);
    vec2 offset = fract(point);
    vec2 blend = offset * offset * offset * (offset * (offset * 6.0 - 15.0) + 10.0);
    float a = hash(cell);
    float b = hash(cell + vec2(1.0, 0.0));
    float c = hash(cell + vec2(0.0, 1.0));
    float d = hash(cell + vec2(1.0, 1.0));
    return mix(mix(a, b, blend.x), mix(c, d, blend.x), blend.y);
}

vec2 rotate2d(vec2 point, float angle) {
    float sine = sin(angle);
    float cosine = cos(angle);
    return vec2(cosine * point.x - sine * point.y, sine * point.x + cosine * point.y);
}

float fbm(vec2 point) {
    float value = 0.0;
    float amplitude = 0.5;
    float totalAmplitude = 0.0;
    for (int octave = 0; octave < 5; octave++) {
        value += amplitude * noise(point);
        totalAmplitude += amplitude;
        point = rotate2d(point * 2.02 + vec2(17.3, -9.2), 0.74);
        amplitude *= 0.5;
    }
    return value / totalAmplitude;
}

float proceduralCloudField(vec2 uv) {
    vec2 warp = vec2(
        fbm(uv * 0.55 + vec2(4.7, 1.8)),
        fbm(uv * 0.55 + vec2(-2.3, 8.1))
    );
    uv += (warp - 0.5) * 1.35;
    float broad = fbm(uv * 0.58);
    float middle = fbm(uv * 1.22 + broad * 0.65);
    float wisps = fbm(rotate2d(uv * 3.4, 0.45) + middle);
    return smoothstep(0.08, 0.92, broad * 0.58 + middle * 0.32 + wisps * 0.10);
}

float textureCloudField(vec2 uv) {
    vec2 baseUv = uv * 0.28 + vec2(time * 0.010, time * 0.004);
    vec2 detailUv = uv * 0.74 + vec2(time * 0.018, -time * 0.006);
    vec2 highUv = rotate2d(uv * 1.42, 0.27) + vec2(-time * 0.007, time * 0.014);
    float base = texture(cloudBaseTexture, baseUv).r;
    float detail = texture(cloudDetailTexture, detailUv).r;
    float high = texture(cloudDetailTexture, highUv).r;
    float wispyCut = smoothstep(0.18, 0.82, detail);
    float body = base * 0.82 + detail * 0.16 + high * 0.02;
    return smoothstep(0.05, 0.95, body - (1.0 - wispyCut) * 0.18);
}

void main() {
    vec3 direction = normalize(domeDirection);
    vec3 normalizedSunDirection = normalize(sunDirection);
    float height = clamp(direction.y, 0.0, 1.0);
    vec3 sky = mix(horizonColor, zenithColor, pow(height, 0.75));

    float sunHorizontalLength = length(normalizedSunDirection.xz);
    float viewHorizontalLength = length(direction.xz);
    if (sunHorizontalLength > 0.001 && viewHorizontalLength > 0.001) {
        vec2 sunAzimuth = normalizedSunDirection.xz / sunHorizontalLength;
        vec2 viewAzimuth = direction.xz / viewHorizontalLength;
        float azimuthAlignment = dot(viewAzimuth, sunAzimuth);
        float horizonBand = 1.0 - smoothstep(0.04, 0.92, height);
        float twilight = 1.0 - smoothstep(0.12, 0.88, abs(normalizedSunDirection.y));
        float broadSunSide = pow(
            clamp(azimuthAlignment * 0.5 + 0.5, 0.0, 1.0),
            1.35
        );
        float sunSideGlow = broadSunSide * horizonBand * twilight;
        float oppositeSide = pow(max(-azimuthAlignment, 0.0), 1.5)
            * horizonBand * twilight;
        sky = mix(sky, sunColor, sunSideGlow * sunHorizonGlowStrength);
        sky = mix(
            sky,
            zenithColor * 0.55,
            oppositeSide * sunHorizonGlowStrength * 0.58
        );
    }

    float sunAlpha = 0.0;
    if (useSunTexture == 1) {
        vec3 referenceAxis = abs(normalizedSunDirection.z) > 0.98
            ? vec3(1.0, 0.0, 0.0)
            : vec3(0.0, 0.0, 1.0);
        vec3 sunRight = normalize(cross(referenceAxis, normalizedSunDirection));
        vec3 sunUp = normalize(cross(normalizedSunDirection, sunRight));
        vec2 sunPlane = vec2(
            dot(direction, sunRight),
            dot(direction, sunUp)
        ) / max(sunAngularSize, 0.001);
        vec2 sunUv = sunPlane * 0.5 + 0.5;
        bool insideSunQuad = dot(direction, normalizedSunDirection) > 0.0 &&
            all(greaterThanEqual(sunUv, vec2(0.0))) &&
            all(lessThanEqual(sunUv, vec2(1.0)));
        if (insideSunQuad) {
            vec4 sampledSun = texture(sunTexture, sunUv);
            sunAlpha = sampledSun.a;
            vec3 tintedSun = sampledSun.rgb * mix(vec3(1.0), sunColor, 0.35);
            sky = mix(sky, tintedSun, sunAlpha);
        }
    }

    float moonAlpha = 0.0;
    if (useMoonTexture == 1) {
        vec3 normalizedMoonDirection = -normalize(sunDirection);
        vec3 referenceAxis = abs(normalizedMoonDirection.z) > 0.98
            ? vec3(1.0, 0.0, 0.0)
            : vec3(0.0, 0.0, 1.0);
        vec3 moonRight = normalize(cross(referenceAxis, normalizedMoonDirection));
        vec3 moonUp = normalize(cross(normalizedMoonDirection, moonRight));
        vec2 moonPlane = vec2(
            dot(direction, moonRight),
            dot(direction, moonUp)
        ) / max(moonAngularSize, 0.001);
        vec2 moonUv = moonPlane * 0.5 + 0.5;
        bool insideMoonQuad = dot(direction, normalizedMoonDirection) > 0.0 &&
            all(greaterThanEqual(moonUv, vec2(0.0))) &&
            all(lessThanEqual(moonUv, vec2(1.0)));
        if (insideMoonQuad) {
            vec4 sampledMoon = texture(moonTexture, moonUv);
            float cleanCircle = 1.0 - smoothstep(0.42, 0.44, length(moonUv - 0.5));
            moonAlpha = sampledMoon.a * cleanCircle;
            vec3 tintedMoon = sampledMoon.rgb * moonColor;
            sky = mix(sky, tintedMoon, moonAlpha);
        }
    }

    vec2 cloudUv = direction.xz / max(direction.y + 0.22, 0.18);
    cloudUv = cloudUv * cloudScale + vec2(time * 0.18, time * 0.07);
    float cloudField = useCloudTextures == 1
        ? textureCloudField(cloudUv)
        : proceduralCloudField(cloudUv);
    float feather = max(cloudSoftness, 0.22);
    float cloudMask = smoothstep(cloudCoverage, cloudCoverage + feather, cloudField);
    cloudMask *= smoothstep(cloudCoverage - 0.18, cloudCoverage + feather, cloudField);
    cloudMask *= smoothstep(0.08, 0.28, height);
    cloudMask *= 1.0 - smoothstep(0.9, 1.0, height) * 0.35;
    float shade = fbm(cloudUv * 1.8 + cloudField);
    vec3 litCloud = mix(cloudColor * 0.74, cloudColor, smoothstep(0.22, 0.82, shade));
    float celestialAlpha = max(sunAlpha, moonAlpha);
    float cloudOpacity = cloudMask * mix(0.76, 1.0, celestialAlpha);
    fragColor = vec4(mix(sky, litCloud, cloudOpacity), 1.0);
}
