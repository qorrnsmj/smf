#version 330 core

uniform samplerCube texImage;
uniform vec3 skyColor;

layout(location = 0) in vec3 texCoord;

layout(location = 0) out vec4 fragColor;

void main() {
    vec4 color = texture(texImage, texCoord);

    // フォールバック（cubemapが無い時用）
//    if (color.a == 0.0) {
//        color = vec4(skyColor, 1.0);
//    }

    fragColor = color;
}
