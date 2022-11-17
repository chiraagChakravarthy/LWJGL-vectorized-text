#version 330 core
layout (location=0) out vec4 color;

#define epsilon 0.0001

uniform float[300] uAtlas;//at^2+bt+c
uniform int uCount;//how many bezier curves form this glyph
uniform float uZoom;
//size of pixel in glyph space
//depending on program to provide this info

in vec2 pos;//position in glyph space


//value: abs(x)-floor(abs(x))
//side: int(abs(x))/2
//out: x>0
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

float integrate(float a, float b, float d, float e, float f, float y, float t0, float t1){
    f -= y;
    float upper = (t1*(b*(6*f+t1*(3*e+2*d*t1))+a*t1*(6*f+t1*(4*e+3*d*t1))))/6;
    float lower = (t0*(b*(6*f+t0*(3*e+2*d*t0))+a*t0*(6*f+t0*(4*e+3*d*t0))))/6;

    return upper-lower;
}
int sortNet[38] = int[](0, 2, 1, 3, 4, 6, 5, 7, 0, 4, 1, 5, 2, 6, 3, 7, 0, 1, 2, 3, 4, 5, 6, 7, 2, 4, 3, 5, 1, 4, 3, 6, 1, 2, 3, 4, 5, 6);

int side(float t){
    return int(abs(t))/2;
}

float val(float t){
    return abs(t)-floor(abs(t));
}

//sorts by t
float[8] sort(float roots[8]){
    for(int i = 0; i < 19; i++){
        int a = sortNet[i*2];
        int b = sortNet[i*2+1];
        float ta = roots[a];
        float tb = roots[b];//swap if tb<ta
        bool ineq = val(tb)<val(ta);
        roots[a] = mix(ta, tb, ineq);
        roots[b] = mix(tb, ta, ineq);
    }
    return roots;
}

struct ret {
    int[8] index;
    float[8] t;
};

ret sort2(float roots[8]){
    int index[8];
    for (int i = 0; i < 8; i++) {
       index[i] = i;
    }

    for (int i = 0; i < 8; i++) {
        int minI = i;
        float minVal = val(roots[minI]);
        for(int j = i; j < 8; j++){
            float val = val(roots[j]);
            bool ineq = val<minVal;
            minI = int(mix(minI, j, ineq));
            minVal = mix(minVal, val, ineq);
        }
        float temp = roots[i];
        roots[i] = roots[minI];
        roots[minI] = temp;

        int temp2 = index[i];
        index[i] = index[minI];
        index[minI] = temp2;
    }
    return ret(index, roots);
}

float calcArea(){
    float roots[8];
    vec2 maxPos = pos+vec2(uZoom);
    float overlap = 0;

    for(int i = 0; i < uCount; i++) {
        float a = uAtlas[6 * i],
        b = uAtlas[6 * i + 1],
        c = uAtlas[6 * i + 2],
        d = uAtlas[6 * i + 3],
        e = uAtlas[6 * i + 4],
        f = uAtlas[6 * i + 5];

        vec2 A = vec2(a, d);
        vec2 B = vec2(b, e);
        vec2 C = vec2(c, f);

        vec2 roots1 = findRoots(a, b, c - pos.x);//left
        vec2 roots2 = findRoots(a, b, c - maxPos.x);//right
        vec2 roots3 = findRoots(d, e, f - pos.y);//bottom
        vec2 roots4 = findRoots(d, e, f - maxPos.y);//top

        roots[0] = roots1.x;
        roots[1] = roots1.y;
        roots[2] = roots2.x;
        roots[3] = roots2.y;
        roots[4] = roots3.x;
        roots[5] = roots3.y;
        roots[6] = roots4.x;
        roots[7] = roots4.y;

        ret sorted = sort2(roots);

        roots = sorted.t;
        int index[8] = sorted.index;

        int squareDepth = int(mix(0, 1, a==0&&d==0&&( b==0&&c>pos.x&&c<maxPos.x || e==0&&f>pos.y&&f<maxPos.y)));
        int aboveDepth = int(mix(0, 1, d>0||d==0&&(e<0||e==0&&f>=maxPos.y)));
    }
    return overlap;
}

void main (){
    float area = calcArea();
    area = area/ uZoom / uZoom;
    area = clamp(area, 0.0, 1.0);
    float shade = 1-area;
    shade = pow(shade, 1.0/2.2);
    color = vec4(vec3(shade),1);
}