#version 330

uniform sampler2D colorTexture;

layout(location = 0) in vec2 blurTexCoords[11];

layout(location = 0) out vec4 fragColor;

void main() {
	fragColor = vec4(0.0);
	fragColor += texture(colorTexture, blurTexCoords[0]) * 0.0093;
    fragColor += texture(colorTexture, blurTexCoords[1]) * 0.028002;
    fragColor += texture(colorTexture, blurTexCoords[2]) * 0.065984;
    fragColor += texture(colorTexture, blurTexCoords[3]) * 0.121703;
    fragColor += texture(colorTexture, blurTexCoords[4]) * 0.175713;
    fragColor += texture(colorTexture, blurTexCoords[5]) * 0.198596;
    fragColor += texture(colorTexture, blurTexCoords[6]) * 0.175713;
    fragColor += texture(colorTexture, blurTexCoords[7]) * 0.121703;
    fragColor += texture(colorTexture, blurTexCoords[8]) * 0.065984;
    fragColor += texture(colorTexture, blurTexCoords[9]) * 0.028002;
    fragColor += texture(colorTexture, blurTexCoords[10]) * 0.0093;
}
