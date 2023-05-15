# LWJGL Vectorized Text Renderer
Render text at an arbitrary resolution using fonts from a ttf file.
## Usage
Before making any calls to this library, opengl must be initialized. This is usually done with ```Gl.createCapabilities();```

Next you need to setup a font. This is done with the ```VectorFont``` class, which takes the path to a .ttf file in your resources folder in its constructor. If the constructor is blank, it will load Arial, which comes packaged with this library.

To render plain 2D text, use ```TextRenderer.drawText2D()```. This takes a string for what text you want to draw, xy coordinates (in pixels), a ```VectorFont```, and the px scale for the text.

```TextRenderer``` will render to the currently bound framebuffer (which is the window by default), and use the viewport width and height to scale and position the text (window width and height by default). **Important:** If you bind a framebuffer with different dimensions from the window, you must use ```glViewport``` to specify the new width and height before rendering, otherwise the text may be warped and antialiasing will fail.

## Add to your project
If you have a gradle project, open your ```build.gradle``` and add the following

```
dependencies {
    implementation "io.github.chiraagchakravarthy:lwjgl-vectorized-text:0.0.3"
}
```
Then resync your gradle project. It will automatically download all the needed lwjgl dependencies if they are not present.

If your project does not have gradle, you can download the jar library from the releases page and add it to your project's classpath.
