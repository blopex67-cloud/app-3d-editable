package com.zapia.modela3d;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;
import android.opengl.GLES20;
import android.opengl.Matrix;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.concurrent.atomic.AtomicReference;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class ReliefView extends GLSurfaceView {
    private final Renderer3D renderer;
    private float lastX,lastY,downDist; private int pointerCount;
    public ReliefView(Context c){super(c);setEGLContextClientVersion(2);renderer=new Renderer3D();setRenderer(renderer);setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);}
    public void setImage(Bitmap b,float depth){renderer.setImage(b,depth);}
    @Override public boolean onTouchEvent(MotionEvent e){int action=e.getActionMasked();if(action==MotionEvent.ACTION_DOWN){lastX=e.getX();lastY=e.getY();pointerCount=1;return true;}if(action==MotionEvent.ACTION_POINTER_DOWN){pointerCount=2;downDist=dist(e);return true;}if(action==MotionEvent.ACTION_MOVE){if(e.getPointerCount()>1){float d=dist(e);if(downDist>0)renderer.zoom(downDist-d);downDist=d;}else{renderer.rotate((e.getX()-lastX)*.7f,(e.getY()-lastY)*.7f);}lastX=e.getX();lastY=e.getY();return true;}if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL){pointerCount=0;return true;}return true;}
    private float dist(MotionEvent e){float x=e.getX(0)-e.getX(1),y=e.getY(0)-e.getY(1);return (float)Math.sqrt(x*x+y*y);}
    static class Renderer3D implements GLSurfaceView.Renderer {
        private static final String VS="attribute vec3 aPos; attribute vec2 aUv; uniform mat4 uMvp; varying vec2 vUv; varying float vShade; void main(){vUv=aUv;vShade=0.65+0.35*max(0.0,normalize(vec3(-0.35,0.55,0.76)).z);gl_Position=uMvp*vec4(aPos,1.0);} ";
        private static final String FS="precision mediump float; uniform sampler2D uTex; varying vec2 vUv; varying float vShade; void main(){vec4 c=texture2D(uTex,vUv);gl_FragColor=vec4(c.rgb*vShade,c.a);} ";
        private int program,tex;private FloatBuffer vb;private ShortBuffer ib;private int indexCount;private int posLoc,uvLoc,mvpLoc,texLoc;private final AtomicReference<ImageDepth> pending=new AtomicReference<>();private volatile float rx=-12,ry=20,zoom=3.1f;private float[] proj=new float[16],view=new float[16],model=new float[16],mvp=new float[16];
        Renderer3D(){Matrix.setIdentityM(model,0);} void setImage(Bitmap b,float d){pending.set(new ImageDepth(b,d));}void rotate(float x,float y){ry+=x;rx=Math.max(-80,Math.min(80,rx+y));}void zoom(float delta){zoom=Math.max(1.4f,Math.min(7f,zoom+delta*.008f));}
        public void onSurfaceCreated(GL10 gl,EGLConfig cfg){GLES20.glClearColor(.075f,.086f,.11f,1);GLES20.glEnable(GLES20.GL_DEPTH_TEST);program=GLES20.glCreateProgram();int v=shader(GLES20.GL_VERTEX_SHADER,VS),f=shader(GLES20.GL_FRAGMENT_SHADER,FS);GLES20.glAttachShader(program,v);GLES20.glAttachShader(program,f);GLES20.glLinkProgram(program);posLoc=GLES20.glGetAttribLocation(program,"aPos");uvLoc=GLES20.glGetAttribLocation(program,"aUv");mvpLoc=GLES20.glGetUniformLocation(program,"uMvp");texLoc=GLES20.glGetUniformLocation(program,"uTex");int[] t=new int[1];GLES20.glGenTextures(1,t,0);tex=t[0];GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,tex);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_CLAMP_TO_EDGE);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_CLAMP_TO_EDGE);}
        private int shader(int type,String src){int s=GLES20.glCreateShader(type);GLES20.glShaderSource(s,src);GLES20.glCompileShader(s);return s;}
        public void onSurfaceChanged(GL10 gl,int w,int h){GLES20.glViewport(0,0,w,h);Matrix.perspectiveM(proj,0,45f,Math.max(.2f,w/(float)h),.1f,30f);}
        public void onDrawFrame(GL10 gl){GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);ImageDepth d=pending.getAndSet(null);if(d!=null)build(d);if(vb==null||indexCount==0)return;Matrix.setLookAtM(view,0,0,0,zoom,0,0,0,0,1,0);Matrix.setIdentityM(model,0);Matrix.rotateM(model,0,rx,1,0,0);Matrix.rotateM(model,0,ry,0,1,0);float[] mv=new float[16];Matrix.multiplyMM(mv,0,view,0,model,0);Matrix.multiplyMM(mvp,0,proj,0,mv,0);GLES20.glUseProgram(program);GLES20.glUniformMatrix4fv(mvpLoc,1,false,mvp,0);GLES20.glActiveTexture(GLES20.GL_TEXTURE0);GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,tex);GLES20.glUniform1i(texLoc,0);vb.position(0);GLES20.glVertexAttribPointer(posLoc,3,GLES20.GL_FLOAT,false,20,vb);GLES20.glEnableVertexAttribArray(posLoc);vb.position(3);GLES20.glVertexAttribPointer(uvLoc,2,GLES20.GL_FLOAT,false,20,vb);GLES20.glEnableVertexAttribArray(uvLoc);GLES20.glDrawElements(GLES20.GL_TRIANGLES,indexCount,GLES20.GL_UNSIGNED_SHORT,ib);}
        private void build(ImageDepth d){Bitmap b=d.b;int w=100,h=Math.max(12,Math.round(100f*b.getHeight()/b.getWidth()));if(h>160){w=Math.max(12,Math.round(160f*b.getWidth()/b.getHeight()));h=160;}int cols=w+1,rows=h+1;float[] data=new float[cols*rows*5];short[] inds=new short[w*h*6];int k=0;for(int y=0;y<=h;y++)for(int x=0;x<=w;x++){float u=x/(float)w,v=y/(float)h;int px=Math.min(b.getWidth()-1,(int)(u*b.getWidth())),py=Math.min(b.getHeight()-1,(int)(v*b.getHeight()));int c=b.getPixel(px,py);float lum=(android.graphics.Color.red(c)*.299f+android.graphics.Color.green(c)*.587f+android.graphics.Color.blue(c)*.114f)/255f;float nx=u*2-1,ny=v*2-1,rad=(float)Math.sqrt(nx*nx+ny*ny);float z=d.depth*(.18f*(lum-.5f)+.32f*Math.max(0,1-rad));data[k++]=nx;data[k++]=-ny;data[k++]=z;data[k++]=u;data[k++]=1-v;}
            int q=0;for(int y=0;y<h;y++)for(int x=0;x<w;x++){int a=y*cols+x,bb=a+1,c=a+cols,dd=c+1;inds[q++]=(short)a;inds[q++]=(short)c;inds[q++]=(short)bb;inds[q++]=(short)bb;inds[q++]=(short)c;inds[q++]=(short)dd;}ByteBuffer buf=ByteBuffer.allocateDirect(data.length*4).order(ByteOrder.nativeOrder());vb=buf.asFloatBuffer();vb.put(data).position(0);ByteBuffer ibuf=ByteBuffer.allocateDirect(inds.length*2).order(ByteOrder.nativeOrder());ib=ibuf.asShortBuffer();ib.put(inds).position(0);indexCount=inds.length;GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,tex);android.opengl.GLUtils.texImage2D(GLES20.GL_TEXTURE_2D,0,b,0);}
        static class ImageDepth{Bitmap b;float depth;ImageDepth(Bitmap b,float d){this.b=b;depth=d;}}
    }
}
