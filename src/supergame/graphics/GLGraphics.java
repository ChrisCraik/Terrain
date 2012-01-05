
package supergame.graphics;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import supergame.Camera;
import supergame.Config;

import java.nio.FloatBuffer;

public class GLGraphics extends Graphics {
    public GLGraphics() {
        init2d();
        init3d();
    }

    public static FloatBuffer makeFB(float[] floatarray) {
        FloatBuffer fb = BufferUtils.createFloatBuffer(floatarray.length);
        fb.put(floatarray).flip();
        return fb;
    }

    private void init2d() {
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    private void init3d() {
        GL11.glEnable(GL11.GL_TEXTURE_2D); // Enable Texture Mapping
        GL11.glShadeModel(GL11.GL_SMOOTH); // Enable Smooth Shading
        GL11.glClearColor(0.6f, 0.6f, 0.9f, 0.0f); // Sky
        GL11.glClearDepth(1.0); // Depth Buffer Setup
        GL11.glEnable(GL11.GL_DEPTH_TEST); // Enables Depth Testing
        GL11.glDepthFunc(GL11.GL_LEQUAL); // The Type Of Depth Testing To Do
        GL11.glEnable(GL11.GL_CULL_FACE); // Don't render the backs of triangles

        // Really Nice Perspective Calculations
        GL11.glHint(GL11.GL_PERSPECTIVE_CORRECTION_HINT, GL11.GL_NICEST);

        // Fog!
        GL11.glFogi(GL11.GL_FOG_MODE, GL11.GL_LINEAR);
        GL11.glFog(GL11.GL_FOG_COLOR, makeFB(new float[] {
                0.6f, 0.6f, 0.9f, 1
        }));
        GL11.glFogf(GL11.GL_FOG_DENSITY, 0.1f);
        GL11.glFogf(GL11.GL_FOG_START, 0.75f * Camera.farD);
        GL11.glFogf(GL11.GL_FOG_END, Camera.farD);
        GL11.glEnable(GL11.GL_FOG);

        // Lighting!
        int light = GL11.GL_LIGHT0;
        GL11.glLight(light, GL11.GL_DIFFUSE, makeFB(new float[] {
                1, 1, 1, 1
        })); // color of the direct illumination
        GL11.glLight(light, GL11.GL_SPECULAR, makeFB(new float[] {
                1, 1, 1, 1
        })); // color of the highlight
        GL11.glLight(light, GL11.GL_AMBIENT, makeFB(new float[] {
                1, 1, 1, 1
        })); // color of the reflected light
        GL11.glLight(light, GL11.GL_POSITION, makeFB(new float[] {
                0, 1, 0, 0
        })); // from above!
        GL11.glEnable(light);
        GL11.glEnable(GL11.GL_NORMALIZE);
        // GL11.glMaterial(GL11.GL_FRONT, GL11.GL_DIFFUSE, makeFB(new float[] {
        // 0, 0, 0, 1 }));
        GL11.glMaterial(GL11.GL_FRONT, GL11.GL_SPECULAR, makeFB(new float[] {
                0, 0, 0, 1
        }));
        GL11.glMaterial(GL11.GL_FRONT, GL11.GL_AMBIENT, makeFB(new float[] {
                0, 0, 0, 1
        }));
        GL11.glEnable(GL11.GL_LIGHTING);
    }

    @Override
    public void switchTo3d(float displayRatio) {
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_BLEND);

        // projection
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        org.lwjgl.util.glu.GLU.gluPerspective(Camera.FOV,
                displayRatio, 0.1f, Camera.farD);

        // modelview
        GL11.glMatrixMode(GL11.GL_MODELVIEW); // Select The Modelview Matrix
        GL11.glLoadIdentity();
    }

    @Override
    public void switchTo2d() {
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND);

        // projection
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(0, Config.RESOLUTION_X, Config.RESOLUTION_Y, 0, 1, -1);

        // modelview
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
    }

    @Override
    public void clear() {
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
    }

    @Override
    public void loadIdentity() {
        GL11.glLoadIdentity(); // Reset The Current Modelview Matrix
    }

    @Override
    public void createLight() {
        GL11.glLight(GL11.GL_LIGHT0, GL11.GL_POSITION,
                makeFB(new float[] {
                        0, 1, 0.5f, 0
                })); // from above!
    }
}
