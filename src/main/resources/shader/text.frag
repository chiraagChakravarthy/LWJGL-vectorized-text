#version 330 core
layout (location=0) out vec4 color;

#define epsilon 0.0001

uniform float[300] uAtlas;//at^2+bt+c
uniform int uCount;//how many bezier curves form this glyph
uniform float uZoom;
//size of pixel in glyph space
//depending on program to provide this info

in vec2 pos;//position in glyph space

struct intersect {
    float t;
    int side;
};

void findRoots(float a, float b, float c, intersect roots[10], int i){
    roots[i] = intersect(2, 4);
    roots[i+1] = intersect(2, 4);
    //want to solve at^2+bt+c=0 for t

    if (abs(a) < epsilon) {
        if (abs(b) > epsilon) {
            intersect t = roots[i];
            t.t = -c/b;
            t.side = int(i/2);
        }
        //otherwise no intercept
    } else {
        float dis = b * b - 4 * a * c;
        if (dis > 0) {
            float sqr = sqrt(dis);
            intersect t = roots[i];
            t.t = (-b + sqr) / (2.0 * a);
            t.side = int(i/2);
            t = roots[i+1];
            t.t = (-b - sqr) / (2.0 * a);
            t.side = int(i/2);
        }
        //otherwise no intercept
    }
}

float integrate(float a, float b, float d, float e, float f, float y, float t0, float t1){
    f -= y;
    float upper = (t1*(b*(6*f+t1*(3*e+2*d*t1))+a*t1*(6*f+t1*(4*e+3*d*t1))))/6;
    float lower = (t0*(b*(6*f+t0*(3*e+2*d*t0))+a*t0*(6*f+t0*(4*e+3*d*t0))))/6;

    return upper-lower;
}

//sorts by t
void sort(intersect roots[10]){
    //fucking hell why

    for(int i = 1; i < 10; ++i)
    {
        intersect val = roots[i];
        int j = i;
        while(j > 0 && val.t < roots[j-1].t)
        {
            roots[j] = roots[j-1];
            --j;
        }
        roots[j] = val;
    }
}

float calcArea(){
    intersect roots[10];
    for(int i = 0; i < 10; i++){
        roots[i] = intersect(2, 0);
    }
    vec2 maxPos = pos+vec2(uZoom);
    float overlap = 0;


    for(int i = 0; i < uCount; i++){
        float a = uAtlas[6*i],
                b = uAtlas[6*i+1],
                c = uAtlas[6*i+2],
                d = uAtlas[6*i+3],
                e = uAtlas[6*i+4],
                f = uAtlas[6*i+5];

        vec2 A = vec2(a, d);
        vec2 B = vec2(b, e);
        vec2 C = vec2(c, f);

        findRoots(a, b, c-pos.x, roots, 0);
        findRoots(a, b, c-maxPos.x, roots, 2);
        findRoots(d, e, f-pos.y, roots, 4);
        findRoots(d, e, f-maxPos.y, roots, 6);
        roots[8] = intersect(0, 4);
        roots[9] = intersect(1, 4);
        sort(roots);
        for(int i = 0; i < 10; i++){
            intersect s = roots[i];
            float t = s.t;
            int side = s.side;
            if(0<t||t>=1){
                continue;
            }
            vec2 ixy = A*t*t+B*t+C;
            vec2 dxy = 2*A*t*t+B;

            bool doClamp = false;
            bool bounds = true;
            if(side==2||side==3) {
                if (side == 2 && dxy.y < 0) {
                    bounds = false;
                }

                if((ixy.x<pos.x+epsilon&&dxy.x<0)||ixy.x<pos.x-epsilon||(ixy.x>maxPos.x-epsilon&&dxy.x>0)||ixy.x>maxPos.x+epsilon){
                    bounds = false;
                }

                if (side == 3 && dxy.y > 0) {
                    doClamp = true;
                }
            }

            if(side==0||side==1){
                if(side==0&&dxy.x<0){
                    bounds = false;
                }

                if(side==1&&dxy.x>0){
                    bounds = false;
                }

                if((ixy.y < pos.y+epsilon&&dxy.y<0)||ixy.y<pos.y-epsilon){
                    bounds = false;
                }
                if((ixy.y>maxPos.y-epsilon&&dxy.y>0)||ixy.y > maxPos.y+epsilon){
                    doClamp = true;
                }
            }
            if(side==4){
                if((ixy.x<pos.x+epsilon&&dxy.x<0)||ixy.x<pos.x-epsilon||(ixy.x>maxPos.x-epsilon&&dxy.x>0)||ixy.x>maxPos.x+epsilon){
                    bounds = false;
                }
                if((ixy.y < pos.y+epsilon&&dxy.y<0)||ixy.y<pos.y-epsilon){
                    bounds = false;
                }
                if((ixy.y>maxPos.y-epsilon&&dxy.y>0)||ixy.y > maxPos.y+epsilon){
                    doClamp = true;
                }
            }
            if(bounds){
                float t1 = roots[i+1].t;
                float nextIx = a*t1*t1+b*t1+c;
                if(doClamp){
                    overlap += (nextIx-ixy.x)* uZoom;
                } else {
                    overlap += integrate(a, b, d, e, f, pos.y, t, t1);
                }
            }
        }
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