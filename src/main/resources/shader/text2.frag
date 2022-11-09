#version 330 core
layout (location=0) out vec4 color;

#define epsilon 0.0001

uniform float[300] uAtlas;//at^2+bt+c
uniform int uCount;//how many bezier curves form this glyph
uniform float uZoom;
//size of pixel in glyph space
//depending on program to provide this info

in vec2 pos;//position in glyph space


float doSample(vec2 q, vec2 s){
    float enter = -1;
    float exit = -1;
    s = normalize(s);
    float aaRange = clamp(-0.00875*uZoom+1.675, 1.0, 1.5)*uZoom;
    q = q-s*aaRange/2;

    for(int i = 0; i < uCount; i++){
        vec2 A = vec2(uAtlas[6*i], uAtlas[6*i+3]);
        vec2 B = vec2(uAtlas[6*i+1], uAtlas[6*i+4]);
        vec2 C = vec2(uAtlas[6*i+2], uAtlas[6*i+5]);

        vec2 smallAxis = mix(vec2(1, 1), vec2(0, 0), lessThan(abs(s), vec2(epsilon)));
        bool noSmallAxis = dot(smallAxis, vec2(1, 1))==0;
        float a = mix(dot(A, smallAxis), dot(A/s, vec2(1, -1)), noSmallAxis);
        float b = mix(dot(B, smallAxis), dot(B/s, vec2(1, -1)), noSmallAxis);
        float c = mix(dot(C-q, smallAxis), dot((C-q)/s, vec2(1, -1)), noSmallAxis);

        float dis = b*b-4*a*c;
        float sqr = sqrt(dis);

        vec2 tVec = mix((vec2(-b)+vec2(sqr, -sqr))/2/a, vec2(-c/b), bvec2(abs(a)<epsilon));
        float t = tVec.x;
        bool tInRange = -epsilon*epsilon< t && t <1+epsilon*epsilon && bool((mix(int(dis>0), int(abs(b)>epsilon),abs(a)<epsilon)));

        vec2 intPos = A*t*t+B*t+C;
        vec2 intTan = 2*A*t+B;
        vec2 intNorm = vec2(intTan.y, -intTan.x);
        vec2 uvec = (intPos-q)/s;
        float u = mix(uvec.x, uvec.y, s.x<epsilon);
        float dir = dot(intNorm, s);
        exit = mix(exit, u, u>0 && dir<0 && (u<exit||exit<0)&& tInRange);
        enter = mix(enter, u, u>0 && dir>0 && (u<enter||enter<0)&& tInRange);

        t = tVec.y;
        tInRange = -epsilon*epsilon< t && t <1+epsilon*epsilon && bool((mix(int(dis>0), int(abs(b)>epsilon),abs(a)<epsilon)));
        intPos = A*t*t+B*t+C;
        intTan = 2*A*t+B;
        intNorm = vec2(intTan.y, -intTan.x);
        uvec = (intPos-q)/s;
        u = mix(uvec.x, uvec.y, s.x<epsilon);

        exit = mix(exit, u, u>0 && dir<0 && (u<exit||exit<0)&& tInRange);
        enter = mix(enter, u, u>0 && dir>0 && (u<enter||enter<0)&& tInRange);
    }
    if(exit<0){
        return 1;
    }
    //entered then exited
    if(enter<exit&&enter<aaRange){
        return (aaRange-min(exit-enter, aaRange))/aaRange;
    }

    if(exit<enter){
        return (aaRange-(exit<aaRange?exit+max(aaRange-enter,0.0):aaRange))/aaRange;
    }
    return 1;
}




void main (){
    int samples = 8;
    float shade = 0;
    for(int i = 0; i < samples; i++){
        float a = (3.14159/samples*i+0.01);
        vec2 dir = vec2(cos(a), sin(a));
        float s = doSample(pos, dir);
        shade += s;
    }
    shade = shade/samples;
    shade = pow(shade, 1.0/2.2);
    color = vec4(vec3(shade), 1);
}