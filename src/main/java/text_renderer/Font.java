package text_renderer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL31.GL_TEXTURE_BUFFER;

public class Font {
    private final float size;
    private final int atlasBuffer;

    public Font(String path, float size, int style){
        this.size = size;
        atlasBuffer = glGenTextures();
    }

    private void init(String path){

    }

    public void bind(){
        glBindTexture(GL_TEXTURE_BUFFER, atlasBuffer);
    }

    public float getSize() {
        return size;
    }
}
