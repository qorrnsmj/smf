#version 330

uniform int targetHeight;
uniform float blurStrength;

layout(location = 0) in vec2 position;

layout(location = 0) out vec2 blurTexCoords[11];

void main() {
    int test = 1600;
	gl_Position = vec4(position, 0.0, 1.0);
	vec2 centerTexCoords = position * 0.5 + 0.5;
    float texelSize = 1.0 / (test / blurStrength);
//    float texelSize = 1.0 / (1600 / blurStrength);

    for (int i = -5; i <= 5; i++) {
        blurTexCoords[i + 5] = centerTexCoords + vec2(i * texelSize, 0.0);
    }
}
