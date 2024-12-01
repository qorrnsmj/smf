#version 330

const float contrast = 2.0;

uniform sampler2D colorTexture;

layout(location = 0) in vec2 texCoord;

layout(location = 0) out vec4 fragColor;

void main() {
	fragColor = texture(colorTexture, texCoord);
	fragColor.rgb = (fragColor.rgb - 0.5) * contrast + 0.5;
}
