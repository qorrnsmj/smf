#version 330

uniform sampler2D colorTexture;

layout(location = 0) in vec2 texCoord;

layout(location = 0) out vec4 fragColor;

void main() {
    fragColor = texture(colorTexture, texCoord);

    float r = fragColor.r * 0.393 + fragColor.g * 0.769 + fragColor.b * 0.189;
    float g = fragColor.r * 0.349 + fragColor.g * 0.686 + fragColor.b * 0.168;
    float b = fragColor.r * 0.272 + fragColor.g * 0.534 + fragColor.b * 0.131;

    fragColor.rgb = vec3(r, g, b);
}
