#version 330 core

uniform sampler2D mapTexture;
uniform vec3 skyColor;
uniform vec3 cameraPosition;
uniform float fogDensity;
uniform float fogGradient;

layout(location = 0) in vec2 texCoord;
layout(location = 1) in vec3 worldPosition;

out vec4 fragColor;

void main() {
    vec4 finalColor = texture(mapTexture, texCoord);
    float distance = length(cameraPosition - worldPosition);
    float visibility = exp(-pow(distance * fogDensity, fogGradient));
    visibility = clamp(visibility, 0.0, 1.0);

    fragColor = mix(vec4(skyColor, 1.0), finalColor, visibility);
}
