#version 330
#extension GL_ARB_separate_shader_objects : enable

uniform sampler2D colorTexture;
uniform float fadeAlpha;
uniform vec3 fadeColor;
uniform float letterboxRatio;

layout(location = 0) in vec2 texCoord;

layout(location = 0) out vec4 fragColor;

void main() {
    vec4 sourceColor = texture(colorTexture, texCoord);
    bool isBar = texCoord.y < letterboxRatio || texCoord.y > 1.0 - letterboxRatio;

    if (isBar) {
        fragColor = vec4(0.0, 0.0, 0.0, sourceColor.a);
    } else {
        fragColor = vec4(mix(sourceColor.rgb, fadeColor, fadeAlpha), sourceColor.a);
    }
}
