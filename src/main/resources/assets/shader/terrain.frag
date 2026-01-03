#version 330 core

uniform sampler2D blendMap;
uniform sampler2D texGrass;
uniform sampler2D texFlower;
uniform sampler2D texDirt;
uniform sampler2D texPath;
uniform vec3 skyColor;
uniform bool useSingleTexture;

layout(location = 0) in vec2 texCoord;
layout(location = 1) in float visibility;

out vec4 fragColor;

void main() {
    vec4 finalColor;
    vec2 tiledCoord = texCoord * 50.0;

    if (useSingleTexture) {
        // Single texture mode: use only texGrass
        finalColor = texture(texGrass, tiledCoord);
    } else {
        // Blended texture mode: blend using blend map
        vec4 blendMapColor = texture(blendMap, texCoord);
        float grassAmount = max(0.0, 1.0 - (blendMapColor.r + blendMapColor.g + blendMapColor.b));

        vec4 grassColor = texture(texGrass, tiledCoord) * grassAmount;
        vec4 flowerColor = texture(texFlower, tiledCoord) * blendMapColor.g;
        vec4 dirtColor = texture(texDirt, tiledCoord) * blendMapColor.r;
        vec4 pathColor = texture(texPath, tiledCoord) * blendMapColor.b;

        finalColor = grassColor + flowerColor + dirtColor + pathColor;
    }

    fragColor = mix(vec4(skyColor, 1.0), finalColor, visibility);
}
