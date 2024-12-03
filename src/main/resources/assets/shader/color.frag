#version 330

uniform sampler2D colorTexture;

layout(location = 0) in vec2 texCoord;

layout(location = 0) out vec4 fragColor;

void main() {
	fragColor = texture(colorTexture, texCoord);
	fragColor.rgb = fragColor.rbg;
}
