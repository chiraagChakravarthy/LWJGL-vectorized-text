#version 330 core
layout (location=0) out vec4 color;

#define epsilon 0.0001
#define scale 1.0

uniform float[400] uAtlas;//at^2+bt+c
uniform int uCount;//how many bezier curves form this glyph
uniform float uPixelSize;
//size of pixel in glyph space
//depending on program to provide this info

in vec2 pos;//position in glyph space

vec2 findRoots(float a, float b, float c){
    vec2 roots = vec2(1);
    if (abs(a) < epsilon && abs(b) > epsilon) {
        roots = vec2(-c/b, 1);
        //otherwise no intercept
    } else {
        float dis = b * b - 4 * a * c;
        if (dis > 0) {
            float sqr = sqrt(dis);
            roots = (vec2(-b)+vec2(sqr, -sqr))/(2*a);
        }
        //otherwise no intercept
    }
    return roots;
}

int[8] sort(float t[8]){
    int index[8];
    for (int i = 0; i < 8; i++) {
       index[i] = i;
    }
    for (int i = 0; i < 7; i++) {
        float minI = i;
        float minVal = t[i];
        for(int j = i+1; j < 8; j++){
            float val = t[j];
            bool ineq = val<minVal;
            minVal = mix(minVal, val, ineq);
            minI = mix(minI, j, ineq);
        }
        int intMini = int(minI);
        t[intMini] = t[i];
        int tempI = index[intMini];
        index[intMini] = index[i];
        index[i] = tempI;
    }
    return index;
}

ivec2 findIo(vec2 roots, float a, float b, int n){
    return ivec2(mix(ivec2(1), ivec2(-1), greaterThan((2*a*roots+vec2(b))*n, vec2(0))));
}

float integrate(float a, float b, float d, float e, float f, float t0, float t1){
    t0 = clamp(t0, 0.0, 1.0);
    t1 = clamp(t1, 0.0, 1.0);

    //vec2 t = vec2(t0, t1);
    //vec2 integral = (t*(b*(6*vec2(f)+t*(3*vec2(e)+2*d*t))+a*t*(6*vec2(f)+t*(4*vec2(e)+3*d*t))));

    float upper = (t1*(b*(6*f+t1*(3*e+2*d*t1))+a*t1*(6*f+t1*(4*e+3*d*t1))))/6;
    float lower = (t0*(b*(6*f+t0*(3*e+2*d*t0))+a*t0*(6*f+t0*(4*e+3*d*t0))))/6;

    return upper-lower;
}

float rectIntegrate(float a, float b, float t0, float t1){
    t0 = clamp(t0, 0.0, 1.0);
    t1 = clamp(t1, 0.0, 1.0);
    return (a*t1*t1+b*t1-a*t0*t0-b*t0)* uPixelSize *scale;
}

float calcArea(){
    float roots[8];
    vec2 maxPos = pos + vec2(uPixelSize)*(0.5+scale/2);
    vec2 minPos = pos + vec2(uPixelSize)*(0.5-scale/2);
    float overlap = 0;

    for(int i = 0; i < uCount; i++) {
        float a = uAtlas[6 * i],
        b = uAtlas[6 * i + 1],
        c = uAtlas[6 * i + 2],
        d = uAtlas[6 * i + 3],
        e = uAtlas[6 * i + 4],
        f = uAtlas[6 * i + 5];

        vec2 roots1 = findRoots(a, b, c - minPos.x);//left
        vec2 roots2 = findRoots(a, b, c - maxPos.x);//right
        vec2 roots3 = findRoots(d, e, f - minPos.y);//bottom
        vec2 roots4 = findRoots(d, e, f - maxPos.y);//top

        roots[0] = roots1.x;
        roots[1] = roots1.y;
        roots[2] = roots2.x;
        roots[3] = roots2.y;
        roots[4] = roots3.x;
        roots[5] = roots3.y;
        roots[6] = roots4.x;
        roots[7] = roots4.y;

        ivec2 io1 = findIo(roots1, a, b, -1);
        ivec2 io2 = findIo(roots2, a, b, 1);
        ivec2 io3 = findIo(roots3, d, e, -1);
        ivec2 io4 = findIo(roots4, d, e, 1);
        int io[8];
        io[0] = io1.x;
        io[1] = io1.y;
        io[2] = io2.x;
        io[3] = io2.y;
        io[4] = io3.x;
        io[5] = io3.y;
        io[6] = io4.x;
        io[7] = io4.y;

        int index[8] = sort(roots);

        int squareDepth = int(mix(0, 1, a==0&&d==0&&( b==0&&c>minPos.x&&c<maxPos.x || e==0&&f>minPos.y&&f<maxPos.y)));
        int aboveDepth = int(mix(0, 1, d>0||d==0&&(e<0||e==0&&f>=maxPos.y)));

        for (int j = 0; j < 7; j++) {
            int i0 = index[j];
            int i1 = index[j+1];
            float t0 = roots[i0];
            float t1 = roots[i1];

            float dx = rectIntegrate(a, b, t0, t1);

            int s0 = i0/2;
            int io0 = io[i0];

            aboveDepth += int(mix(mix(io0, -io0, s0==3), 0, s0==2));

            overlap += mix(0.0, dx, aboveDepth==2);
        }

        float intComp = 0;
        for(int j = 0; j < 3; j++){
            int i0 = index[j*2+1];
            int i1 = index[j*2+2];
            float t0 = roots[i0];
            float t1 = roots[i1];
            int io0 = io[i0];
            intComp += mix(0.0, integrate(a, b, d, e, f-minPos.y, t0, t1), io0==1);
        }
        overlap += mix(integrate(a, b, d, e, f-minPos.y, roots[index[0]], roots[index[1]]), intComp, squareDepth==0);
    }
    return overlap;
}

void main (){
    float area = calcArea();
    area = area/ uPixelSize / uPixelSize /scale/scale;
    area = clamp(area, 0.0, 1.0);
    float shade = 1-area;
    shade = pow(shade, 1.0/2.2);
    color = vec4(vec3(shade),1);
}