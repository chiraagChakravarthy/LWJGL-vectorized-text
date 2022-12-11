#version 330 core
layout (location=0) out vec4 color;

#define epsilon 0.0001
#define window 1.0
//scaling factor of intersect window for pixel

uniform float uPixelSize;
uniform isamplerBuffer uAtlas;

//size of pixel in glyph space
//depending on program to provide this info

in vec2 vGlyphPos;//position in glyph space
in float vGlyph;//which glyph to render
in vec3 vTint;

vec2 findRoots(float a, float b, float c, int s){
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

    ivec2 bits = floatBitsToInt(roots);
    bits = (bits & ivec2(-4)) | ivec2(s);
    roots = intBitsToFloat(bits);

    return roots;
}

ivec4 findSide(vec4 t){
    return floatBitsToInt(t)&ivec4(3);
}

ivec4 findIo(vec4 t, ivec4 side, float a, float b, float d, float e){
    ivec4 n = (side&ivec4(1))*2-1;
    bvec4 ineq = lessThan(side, ivec4(2));
    vec4 A = mix(vec4(d), vec4(a), ineq);
    vec4 B = mix(vec4(e), vec4(b), ineq);
    return ivec4(mix(vec4(1), vec4(-1), greaterThan((2*A*t+B)*n, vec4(0))));
}

float integrate(float a, float b, float d, float e, float f, float t0, float t1){
    t0 = clamp(t0, 0.0, 1.0);
    t1 = clamp(t1, 0.0, 1.0);

    float upper = (t1*(b*(6*f+t1*(3*e+2*d*t1))+a*t1*(6*f+t1*(4*e+3*d*t1))))/6;
    float lower = (t0*(b*(6*f+t0*(3*e+2*d*t0))+a*t0*(6*f+t0*(4*e+3*d*t0))))/6;

    return upper-lower;
}

float rectArea(float a, float b, float t0, float t1){
    t0 = clamp(t0, 0.0, 1.0);
    t1 = clamp(t1, 0.0, 1.0);
    return (a*t1*t1+b*t1-a*t0*t0-b*t0)* uPixelSize * window;
}

float calcArea(){
    vec2 maxPos = vGlyphPos + vec2(uPixelSize)*(0.5+ window /2);
    vec2 minPos = vGlyphPos + vec2(uPixelSize)*(0.5- window /2);
    float overlap = 0;

    int iGlyph = int(vGlyph);

    //iterate through beziers
    int start = texelFetch(uAtlas, iGlyph).x, end = texelFetch(uAtlas, iGlyph+1).x;

    for (int i = start; i < end; i++) {
        int j = i*6+257;
        float a = texelFetch(uAtlas, j).x, b = texelFetch(uAtlas, j+1).x, c = texelFetch(uAtlas, j+2).x,
                d = texelFetch(uAtlas, j+3).x, e = texelFetch(uAtlas, j+4).x, f = texelFetch(uAtlas, j+5).x;

        vec2 roots1 = findRoots(a, b, c - minPos.x, 0);//left
        vec2 roots2 = findRoots(a, b, c - maxPos.x, 1);//right
        vec2 roots3 = findRoots(d, e, f - minPos.y, 2);//bottom
        vec2 roots4 = findRoots(d, e, f - maxPos.y, 3);//top



        /*SORT
        [a b c d e f g h]

        LAYER 1
        if e>a: swap
        if f>b: swap
        if g>c: swap
        if h>d: swap

        layer 1: [[0,4], [1,5], [2,6], [3,7]]
        layer 2: [[0,2], [1,3], [4,6], [5,7]]
        layer 3: [[2,4], [3,5], [0,1], [6,7]]
        layer 4: [[2,3], [4,5]]
        layer 5: [[1,4], [3,6]]
        layer 6: [[1,2], [3,4], [5,6]]
        */

        //min([0 4 5 2], [1 3 6 7]) = [0 3 5 2]
        //max([0 4 5 2], [1 3 6 7]) = [1 4 6 7]

        //greaterThan([0 4 5 2], [1 3 6 7]) = [0>1, 4>3, 5>6, 2>7] = [false, true, true, false] = [0 1 1 0]
        //mix(a, b, t/f): selection function (false selects a, true selects b)

        //LAYER 1
        vec4 va = vec4(roots1, roots2);//0123
        vec4 vb = vec4(roots3, roots4);//4567
        bvec4 ineq = greaterThan(va, vb);
        vec4 ta = mix(va, vb, ineq);
        vec4 tb = mix(vb, va, ineq);

        //LAYER 2
        va = vec4(ta.xy, tb.xy);//0145
        vb = vec4(ta.zw, tb.zw);//2367
        ineq = greaterThan(va, vb);
        ta = mix(va, vb, ineq);
        tb = mix(vb, va, ineq);

        //LAYER 3
        va = vec4(tb.xy, ta.x, tb.z);//2306
        vb = vec4(ta.zwy, tb.w);//4517
        ineq = greaterThan(va, vb);
        ta = mix(va, vb, ineq);
        tb = mix(vb, va, ineq);

        //LAYER 4
        vec2 a2 = vec2(ta.x, tb.x);//24
        vec2 b2 = vec2(ta.y, tb.y);//35
        bvec2 ineq2 = greaterThan(a2, b2);
        vec2 ta2 = mix(a2, b2, ineq2);
        vec2 tb2 = mix(b2, a2, ineq2);

        //LAYER 5
        vec2 a3 = vec2(tb.z, tb2.x);//13
        vec2 b3 = vec2(ta2.y, ta.w);//46
        bvec2 ineq3 = greaterThan(a3, b3);
        vec2 ta3 = mix(a3, b3, ineq3);
        vec2 tb3 = mix(b3, a3, ineq3);

        //LAYER 6
        vec3 a4 = vec3(ta3.xy, tb2.y);//135
        vec3 b4 = vec3(ta2.x, tb3.xy);//246
        bvec3 ineq4 = greaterThan(a4, b4);
        vec3 ta4 = mix(a4, b4, ineq4);
        vec3 tb4 = mix(b4, a4, ineq4);

        va = vec4(ta.z, ta4.x, tb4.x, ta4.y);//0123
        vb = vec4(tb4.y, ta4.z, tb4.z, tb.w);//4567
        //END SORT


        //retrieve intersection sides
        ivec4 sa = findSide(va);
        ivec4 sb = findSide(vb);

        //compute exit/enter
        ivec4 ioa = findIo(va, sa, a, b, d, e);
        ivec4 iob = findIo(vb, sb, a, b, d, e);

        //initial depth
        int squareDepth = int(mix(0.0, 1.0, a==0&&d==0&&( b==0&&c>minPos.x&&c<maxPos.x || e==0&&f>minPos.y&&f<maxPos.y)));
        int aboveDepth = int(mix(0.0, 1.0, d>0||d==0&&(e<0||e==0&&f>=maxPos.y)));

        float intComp = 0;//integral component

        float t0, t1, dx;
        int io0, s0;

        //INTEGRATE

        //t0-t1 above
        t0 = va.x;
        t1 = va.y;
        dx = rectArea(a, b, t0, t1);
        io0 = ioa.x;
        s0 = sa.x;
        aboveDepth += int(mix(mix(float(io0), -float(io0), s0==3), 0.0, s0==2));
        overlap += mix(0.0, dx, aboveDepth==2);

        //t1-t2 above
        t0 = va.y;
        t1 = va.z;
        dx = rectArea(a, b, t0, t1);
        io0 = ioa.y;
        s0 = sa.y;
        aboveDepth += int(mix(mix(float(io0), -float(io0), s0==3), 0.0, s0==2));
        overlap += mix(0.0, dx, aboveDepth==2);
        intComp += mix(0.0, integrate(a, b, d, e, f-minPos.y, t0, t1), io0==1); //t1-t2 below

        //t2-t3 above
        t0 = va.z;
        t1 = va.w;
        dx = rectArea(a, b, t0, t1);
        io0 = ioa.z;
        s0 = sa.z;
        aboveDepth += int(mix(mix(float(io0), -float(io0), s0==3), 0.0, s0==2));
        overlap += mix(0.0, dx, aboveDepth==2);

        //t3-t4 above
        t0 = va.w;
        t1 = vb.x;
        dx = rectArea(a, b, t0, t1);
        io0 = ioa.w;
        s0 = sa.w;
        aboveDepth += int(mix(mix(float(io0), -float(io0), s0==3), 0.0, s0==2));
        overlap += mix(0.0, dx, aboveDepth==2);
        intComp += mix(0.0, integrate(a, b, d, e, f-minPos.y, t0, t1), io0==1); //t3-t4 below

        //t4-t5 above
        t0 = vb.x;
        t1 = vb.y;
        dx = rectArea(a, b, t0, t1);
        io0 = iob.x;
        s0 = sb.x;
        aboveDepth += int(mix(mix(float(io0), -float(io0), s0==3), 0.0, s0==2));
        overlap += mix(0.0, dx, aboveDepth==2);

        //t5-t6 above
        t0 = vb.y;
        t1 = vb.z;
        dx = rectArea(a, b, t0, t1);
        io0 = iob.y;
        s0 = sb.y;
        aboveDepth += int(mix(mix(float(io0), -float(io0), s0==3), 0.0, s0==2));
        overlap += mix(0.0, dx, aboveDepth==2);
        intComp += mix(0.0, integrate(a, b, d, e, f-minPos.y, t0, t1), io0==1); //t3-t4 below

        //t6-t7 above
        t0 = vb.z;
        t1 = vb.w;
        dx = rectArea(a, b, t0, t1);
        io0 = iob.z;
        s0 = sb.z;
        aboveDepth += int(mix(mix(float(io0), -float(io0), s0==3), 0.0, s0==2));
        overlap += mix(0.0, dx, aboveDepth==2);

        overlap += mix(integrate(a, b, d, e, f-minPos.y, va.x, va.y), intComp, squareDepth==0);
    }
    return overlap;
}

void main () {
    float area = calcArea();
    area = area / uPixelSize / uPixelSize / window / window;
    area = clamp(area, 0.0, 1.0);
    float shade = area;
    //shade = pow(shade, 1);
    color = vec4(vTint, shade);
}
