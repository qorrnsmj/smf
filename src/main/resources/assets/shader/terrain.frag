#version 330 core

uniform sampler2D texImage;

layout(location = 0) in vec2 texCoord;

out vec4 fragColor;

void main() {
    fragColor = texture(texImage, texCoord);
    //fragColor = vec4(1.0, 0.0, 0.0, 1.0);
}
