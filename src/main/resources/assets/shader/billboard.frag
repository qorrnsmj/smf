#version 330 core

uniform sampler2D billboardTexture;
uniform vec4 tint;
uniform int alphaSource;
uniform float edgeFade;

layout(location = 0) in vec2 outTexCoord;
layout(location = 0) out vec4 fragColor;

void main() {
    vec4 sampled = texture(billboardTexture, outTexCoord);
    float sourceAlpha = alphaSource == 1 ? sampled.r : sampled.a;

    vec2 edgePosition = outTexCoord * 2.0 - 1.0;
    float edgeDistance = length(edgePosition);
    float edgeMask = edgeFade > 0.0
        ? 1.0 - smoothstep(1.0 - edgeFade, 1.0, edgeDistance)
        : 1.0;
    float alpha = sourceAlpha * tint.a * edgeMask;
    if (alpha < 0.01) discard;

    vec3 color = alphaSource == 1
        ? tint.rgb * mix(0.68, 1.0, sampled.r)
        : sampled.rgb * tint.rgb;
    fragColor = vec4(color, alpha);
}
