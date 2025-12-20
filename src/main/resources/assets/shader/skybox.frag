#version 330 core

uniform samplerCube texImage;
uniform vec3 skyColor;

layout(location = 0) in vec3 texCoord;

layout(location = 0) out vec4 fragColor;

void main() {
    fragColor = texture(texImage, texCoord);
}
