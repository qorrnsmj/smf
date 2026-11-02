#version 330
#extension GL_ARB_separate_shader_objects : enable

uniform sampler2D colorTexture;
uniform float exposure;
uniform float saturation;
uniform vec3 lift;
uniform vec3 tint;

layout(location = 0) in vec2 texCoord;

layout(location = 0) out vec4 fragColor;

void main() {
	fragColor = texture(colorTexture, texCoord);
	vec3 color = fragColor.rgb * exposure;
	float luminance = dot(color, vec3(0.2126, 0.7152, 0.0722));
	color = mix(vec3(luminance), color, saturation);
	fragColor.rgb = clamp((color + lift) * tint, 0.0, 1.0);
}
