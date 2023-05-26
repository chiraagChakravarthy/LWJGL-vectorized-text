# LWJGL Vectorized Text Renderer
Render text at an arbitrary resolution using fonts from a ttf file.
## Usage
Before making any calls to this library, opengl must be initialized. This is usually done with ```Gl.createCapabilities();```

Next you need to setup a font. This is done with the ```VectorFont``` class, which takes the path to a .ttf file in your resources folder in its constructor. If the constructor is blank, it will load Arial, which comes packaged with this library. After this you can initialize a ```TextRenderer``` object.

To draw plain 2D text, use ```TextRenderer.drawText2D()```. This takes a string for what text you want to draw, xy coordinates (in pixels), text alignment, and the pixel scale for the text.

To draw text rotated and scaled in 3d space, use ```TextRenderer.drawText()```. To use this, you need to provide a model-view-projection matrix, and a pose matrix, which describes its position scale and rotation in 3d space

Finally to render to the screen, use ```TextRenderer.render()```. This will render all queued text to the currently bound framebuffer (which is the window by default).

**Note:** If you intend to draw to a framebuffer with different dimensions from your window, you must use ```glViewport``` to specify the new width and height before calling any methods in this class, otherwise the text may be warped or mis-alligned and antialiasing will fail.

## Add to your project
If you have a gradle project, open your ```build.gradle``` and add the following

```
dependencies {
    implementation "io.github.chiraagchakravarthy:lwjgl-vectorized-text:0.0.4"
}
```
Then resync your gradle project. It will automatically download all the needed lwjgl dependencies if they are not present.

If your project does not have gradle, you can download the jar library from the releases page and add it to your project's classpath.
