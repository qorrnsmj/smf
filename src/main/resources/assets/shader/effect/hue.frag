#version 330

uniform sampler2D colorTexture;
uniform float hueShift;

layout(location = 0) in vec2 texCoord;

layout(location = 0) out vec4 fragColor;

vec3 rgbToHsv(vec3 c) {
    float k = 0.0;

    if (c.g < c.b) {
        c = vec3(c.b, c.g, c.r);
        k = -1.0;
    }

    if (c.r < c.g) {
        c = vec3(c.g, c.r, c.b);
        k = -2.0 / 6.0 - k;
    }

    float chroma = c.r - min(c.b, c.g);
    return vec3(abs(k + (c.g - c.b) / (6.0 * chroma + 1e-10)), chroma / (c.r + 1e-10), c.r);
}

vec3 hsvToRgb(vec3 c) {
    float r = abs(c.x * 6.0 - 3.0) - 1.0;
    float g = 2.0 - abs(c.x * 6.0 - 2.0);
    float b = 2.0 - abs(c.x * 6.0 - 4.0);

    return vec3(r, g, b);
}

void main() {
    fragColor = texture(colorTexture, texCoord);

    vec3 hsv = rgbToHsv(fragColor.rgb);
    hsv.x = mod(hsv.x + hueShift, 1.0); // Shift hue
    fragColor.rgb = hsvToRgb(hsv);
}
