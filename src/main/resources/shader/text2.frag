#version 330 core
layout (location=0) out vec4 color;

#define epsilon 0.0001
#define scale 1.0

uniform float[400] uAtlas;//at^2+bt+c
uniform int uCount;//how many bezier curves form this glyph
uniform float uZoom;
//size of pixel in glyph space
//depending on program to provide this info

in vec2 pos;//position in glyph space

vec2 findRoots(float a, float b, float c, int n){
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

    ivec2 depth = ivec2(mix(ivec2(1), ivec2(0), greaterThan((2*a*roots+vec2(b))*n, vec2(0)))); //0: exit, 1: enter

    ivec2 bits = floatBitsToInt(roots);
    bits = (bits & vec2(-2)) | depth;
    roots = intBitsToFloat(bits);

    return roots;
}

int findIo(float t){
    return (floatBitsToInt(t)&1)*2-1;
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
    return (a*t1*t1+b*t1-a*t0*t0-b*t0)*uZoom*scale;
}

float calcArea(){
    vec2 maxPos = pos + vec2(uZoom)*(0.5+scale/2);
    vec2 minPos = pos + vec2(uZoom)*(0.5-scale/2);
    float overlap = 0;

    for(int i = 0; i < uCount; i++) {
        float a = uAtlas[6 * i],
        b = uAtlas[6 * i + 1],
        c = uAtlas[6 * i + 2],
        d = uAtlas[6 * i + 3],
        e = uAtlas[6 * i + 4],
        f = uAtlas[6 * i + 5];

        vec2 roots1 = findRoots(a, b, c - minPos.x, -1);//left
        vec2 roots2 = findRoots(a, b, c - maxPos.x, 1);//right
        vec2 roots3 = findRoots(d, e, f - minPos.y, -1);//bottom
        vec2 roots4 = findRoots(d, e, f - maxPos.y, 1);//top

        float t0 = roots1.x, t1 = roots1.y,
                t2 = roots2.x, t3 = roots2.y,
                t4 = roots3.x, t5 = roots3.y,
                t6 = roots4.x, t7 = roots4.y;

        //sort
        float temp;
        bool ineq;
        ineq = t2<t0;
        temp = t0;
        t0 = min(t0, t2);
        t2 = max(temp, t2);
        ineq = t3<t1;
        temp = t1;
        t1 = min(t1, t3);
        t3 = max(temp, t3);
        ineq = t6<t4;
        temp = t4;
        t4 = min(t4, t6);
        t6 = max(temp, t6);
        ineq = t7<t5;
        temp = t5;
        t5 = min(t5, t7);
        t7 = max(temp, t7);
        ineq = t4<t0;
        temp = t0;
        t0 = min(t0, t4);
        t4 = max(temp, t4);
        ineq = t5<t1;
        temp = t1;
        t1 = min(t1, t5);
        t5 = max(temp, t5);
        ineq = t6<t2;
        temp = t2;
        t2 = min(t2, t6);
        t6 = max(temp, t6);
        ineq = t7<t3;
        temp = t3;
        t3 = min(t3, t7);
        t7 = max(temp, t7);
        ineq = t1<t0;
        temp = t0;
        t0 = min(t0, t1);
        t1 = max(temp, t1);
        ineq = t3<t2;
        temp = t2;
        t2 = min(t2, t3);
        t3 = max(temp, t3);
        ineq = t5<t4;
        temp = t4;
        t4 = min(t4, t5);
        t5 = max(temp, t5);
        ineq = t7<t6;
        temp = t6;
        t6 = min(t6, t7);
        t7 = max(temp, t7);
        ineq = t4<t2;
        temp = t2;
        t2 = min(t2, t4);
        t4 = max(temp, t4);
        ineq = t5<t3;
        temp = t3;
        t3 = min(t3, t5);
        t5 = max(temp, t5);
        ineq = t4<t1;
        temp = t1;
        t1 = min(t1, t4);
        t4 = max(temp, t4);
        ineq = t6<t3;
        temp = t3;
        t3 = min(t3, t6);
        t6 = max(temp, t6);
        ineq = t2<t1;
        temp = t1;
        t1 = min(t1, t2);
        t2 = max(temp, t2);
        ineq = t4<t3;
        temp = t3;
        t3 = min(t3, t4);
        t4 = max(temp, t4);
        ineq = t6<t5;
        temp = t5;
        t5 = min(t5, t6);
        t6 = max(temp, t6);



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
    area = area/ uZoom / uZoom/scale/scale;
    area = clamp(area, 0.0, 1.0);
    float shade = 1-area;
    shade = pow(shade, 1.0/2.2);
    color = vec4(vec3(shade),1);
}