#version 330

uniform sampler2D colorTexture;
uniform float intensity;

layout(location = 0) in vec2 texCoord;

layout(location = 0) out vec4 fragColor;

float rand(vec2 co) {
    return fract(sin(dot(co, vec2(12.9898, 78.233))) * 43758.5453);
}

void main() {
    fragColor = texture(colorTexture, texCoord);

    float noise = rand(texCoord) * intensity;
    fragColor.rgb += noise;
}
