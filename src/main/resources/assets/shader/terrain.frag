#version 330 core

uniform sampler2D texImage;
uniform vec3 skyColor;

layout(location = 0) in vec2 texCoord;
layout(location = 1) in float visibility;

out vec4 fragColor;

void main() {
    fragColor = texture(texImage, texCoord);
    fragColor = mix(vec4(skyColor, 1.0), fragColor, visibility); // Apply sky color
}
