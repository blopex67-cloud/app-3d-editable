package com.zapia.modela3d;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.opengl.GLSurfaceView;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.io.ByteArrayOutputStream;

public class MainActivity extends Activity {
    private static final int PICK_IMAGE=10, SAVE_ZIP=11;
    private ReliefView view3d;
    private Bitmap image;
    private float depth=0.42f;
    private TextView hint, depthLabel;

    @Override public void onCreate(Bundle b){ super.onCreate(b); getWindow().setStatusBarColor(Color.rgb(12,14,19)); getWindow().setNavigationBarColor(Color.rgb(12,14,19)); buildUi(); }
    private int dp(float n){ return (int)(n*getResources().getDisplayMetrics().density+0.5f); }
    private TextView label(String s,int size,int color){ TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);return t; }
    private Button button(String s){ Button b=new Button(this);b.setText(s);b.setTextColor(Color.WHITE);b.setAllCaps(false);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(37,43,55)));return b; }
    private void buildUi(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(16),dp(12),dp(16),dp(12));root.setBackgroundColor(Color.rgb(12,14,19));setContentView(root);
        TextView title=label("MODELA 3D",21,Color.WHITE);title.setGravity(Gravity.CENTER_VERTICAL);title.setTypeface(null,1);root.addView(title,new LinearLayout.LayoutParams(-1,dp(40)));
        TextView sub=label("Relieve 3D texturizado · funciona sin internet",12,Color.rgb(160,171,190));root.addView(sub,new LinearLayout.LayoutParams(-1,dp(26)));
        LinearLayout stage=new LinearLayout(this);stage.setGravity(Gravity.CENTER);stage.setBackgroundColor(Color.rgb(19,22,29));LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,0,1);sp.topMargin=dp(10);sp.bottomMargin=dp(10);root.addView(stage,sp);
        view3d=new ReliefView(this);stage.addView(view3d,new LinearLayout.LayoutParams(-1,-1));
        hint=label("Elige una imagen para crear tu relieve",14,Color.rgb(190,198,212));hint.setGravity(Gravity.CENTER);stage.addView(hint,new LinearLayout.LayoutParams(-1,-1));
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER);root.addView(row,new LinearLayout.LayoutParams(-1,dp(58)));
        Button pick=button("＋  Imagen");Button generate=button("Crear 3D");Button export=button("Exportar");
        LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,dp(50),1);bp.setMargins(dp(3),0,dp(3),0);row.addView(pick,bp);row.addView(generate,bp);row.addView(export,bp);
        LinearLayout sliderRow=new LinearLayout(this);sliderRow.setGravity(Gravity.CENTER_VERTICAL);root.addView(sliderRow,new LinearLayout.LayoutParams(-1,dp(55)));
        depthLabel=label("Relieve",12,Color.LTGRAY);sliderRow.addView(depthLabel,new LinearLayout.LayoutParams(dp(58),-2));SeekBar seek=new SeekBar(this);seek.setMax(100);seek.setProgress(42);sliderRow.addView(seek,new LinearLayout.LayoutParams(0,dp(45),1));
        TextView note=label("Una foto permite crear una placa/relieve; no revela el reverso real del objeto.",11,Color.rgb(139,148,165));note.setGravity(Gravity.CENTER);root.addView(note,new LinearLayout.LayoutParams(-1,dp(34)));
        pick.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("image/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,PICK_IMAGE);});
        generate.setOnClickListener(v->{if(image==null){toast("Primero elige una imagen");return;}view3d.setImage(image,depth);hint.setVisibility(View.GONE);toast("Relieve actualizado");});
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean u){depth=0.08f+(p/100f)*0.92f;depthLabel.setText("Relieve");if(image!=null)view3d.setImage(image,depth);}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});
        export.setOnClickListener(v->{if(image==null){toast("Carga una imagen antes de exportar");return;}Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/zip");i.putExtra(Intent.EXTRA_TITLE,"modelo-3d.zip");startActivityForResult(i,SAVE_ZIP);});
    }
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    @Override protected void onActivityResult(int r,int c,Intent d){super.onActivityResult(r,c,d);if(c!=RESULT_OK||d==null||d.getData()==null)return;Uri uri=d.getData();try{
        if(r==PICK_IMAGE){try{getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){} InputStream in=getContentResolver().openInputStream(uri);Bitmap raw=BitmapFactory.decodeStream(in);if(in!=null)in.close();if(raw==null){toast("No se pudo leer esa imagen");return;}float scale=Math.min(1f,1024f/Math.max(raw.getWidth(),raw.getHeight()));image=scale<1?Bitmap.createScaledBitmap(raw,Math.max(1,(int)(raw.getWidth()*scale)),Math.max(1,(int)(raw.getHeight()*scale)),true):raw;hint.setVisibility(View.GONE);view3d.setImage(image,depth);toast("Imagen lista; puedes ajustar el relieve");}
        else if(r==SAVE_ZIP){OutputStream out=getContentResolver().openOutputStream(uri);ZipOutputStream zip=new ZipOutputStream(out);writeZip(zip,"modelo.obj",makeObj(image,depth));writeZip(zip,"modelo.mtl",("newmtl foto\nKa 1.000 1.000 1.000\nKd 1.000 1.000 1.000\nmap_Kd textura.png\n").getBytes("UTF-8"));ByteArrayOutputStream png=new ByteArrayOutputStream();image.compress(Bitmap.CompressFormat.PNG,100,png);writeZip(zip,"textura.png",png.toByteArray());zip.close();toast("Exportado: modelo OBJ + material + textura");}
    }catch(Exception e){toast("No se pudo completar: "+e.getMessage());}}
    private void writeZip(ZipOutputStream z,String name,byte[] bytes)throws Exception{z.putNextEntry(new ZipEntry(name));z.write(bytes);z.closeEntry();}
    private byte[] makeObj(Bitmap bmp,float dep)throws Exception{int w=96,h=Math.max(16,Math.round(96f*bmp.getHeight()/bmp.getWidth()));if(h>128){w=Math.max(16,Math.round(128f*bmp.getWidth()/bmp.getHeight()));h=128;}StringBuilder s=new StringBuilder("mtllib modelo.mtl\nusemtl foto\n");for(int y=0;y<=h;y++)for(int x=0;x<=w;x++){float u=x/(float)w,v=1-y/(float)h;int px=Math.min(bmp.getWidth()-1,(int)(u*bmp.getWidth())),py=Math.min(bmp.getHeight()-1,(int)((1-v)*bmp.getHeight()));int col=bmp.getPixel(px,py);float lum=(Color.red(col)*.299f+Color.green(col)*.587f+Color.blue(col)*.114f)/255f;float nx=(u-.5f)*2,ny=(y/(float)h-.5f)*2,rad=(float)Math.sqrt(nx*nx+ny*ny);float z=dep*(.18f*(lum-.5f)+.32f*Math.max(0,1-rad));s.append(String.format(java.util.Locale.US,"v %.5f %.5f %.5f\n",nx,-ny,z));}for(int y=0;y<=h;y++)for(int x=0;x<=w;x++)s.append(String.format(java.util.Locale.US,"vt %.5f %.5f\n",x/(float)w,1-y/(float)h));int stride=w+1;for(int y=0;y<h;y++)for(int x=0;x<w;x++){int a=y*stride+x+1,b=a+1,c=a+stride,d=c+1;s.append("f ").append(a).append('/').append(a).append(' ').append(c).append('/').append(c).append(' ').append(b).append('/').append(b).append('\n');s.append("f ").append(b).append('/').append(b).append(' ').append(c).append('/').append(c).append(' ').append(d).append('/').append(d).append('\n');}return s.toString().getBytes("UTF-8");}
}
