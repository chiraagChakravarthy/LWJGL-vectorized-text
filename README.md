# LWJGL Vectorized Text Renderer
Render text at an arbitrary resolution using fonts from a ttf file.
## Usage
Before making any calls to this library, opengl must be initialized. This is usually done with ```Gl.createCapabilities();```

After this, TextRenderer needs to be initialized via ```TextRenderer.init()```. This must be called before rendering any text.

Next you need to setup a font. This is done with the ```VectorFont``` class, which takes the path to a .ttf file in your resources folder in its constructor. If the constructor is blank, it will load Arial, which comes packaged with this library.

To render text, use ```TextRenderer.drawText()```. This takes a string for what text you want to draw, xy coordinates (in pixels), a ```VectorFont```, and the px scale for the text. 

```TextRenderer``` will render to the currently bound framebuffer (which is the window by default), and use the viewport width and height to scale and position the text (window width and height by default). **Important:** If you bind a framebuffer with different dimensions from the window, you must use ```glViewport``` to specify the new width and height before rendering, otherwise the text may be warped and antialiasing will fail.

To render text in a different color, use ```TextRenderer.fillColor()```, which accepts red, green, blue, and alpha values ranged [0,1]. This will be opaque black by default.

## Add to your project
If you have a gradle project, open your ```build.gradle``` and add the following

```
repositories {
    maven {url="https://s01.oss.sonatype.org/content/repositories/snapshots/"}
}

dependencies {
    implementation "io.github.chiraagchakravarthy:lwjgl-vectorized-text:0.0.2-SNAPSHOT"
}
```
Then resync your gradle project. It will automatically download all the needed lwjgl dependencies if they are not present. There is a version of this project (0.0.1) in maven central, but it is nonfunctional so do not use it.

If your project does not have gradle, you can download the jar library from the releases page and add it to your project's classpath.
