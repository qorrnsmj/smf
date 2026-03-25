#version 330 core

in vec2 v_texCoords;
out vec4 FragColor;

uniform sampler2D u_glyphTexture;
uniform vec3 u_textColor;

void main() {    
    // Sample the red channel (alpha) from the glyph texture
    float alpha = texture(u_glyphTexture, v_texCoords).r;
    
    // Use the text color with the glyph alpha
    FragColor = vec4(u_textColor, alpha);
}