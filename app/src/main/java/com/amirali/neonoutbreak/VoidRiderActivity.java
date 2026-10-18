package com.amirali.neonoutbreak;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import java.util.Random;

public class VoidRiderActivity extends Activity {
    RiderView game;
    ToneGenerator tone;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        try { tone=new ToneGenerator(AudioManager.STREAM_MUSIC, 32); } catch(Exception ignored){}
        immersive();
        game = new RiderView(this);
        setContentView(game);
    }
    void immersive() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }
    @Override public void onWindowFocusChanged(boolean focus) {
        super.onWindowFocusChanged(focus); if(focus) immersive();
    }
    void sound(int type) {
        if(tone==null)return;
        try {
            if(type==0)tone.startTone(ToneGenerator.TONE_PROP_BEEP,48);
            if(type==1)tone.startTone(ToneGenerator.TONE_PROP_ACK,48);
            if(type==2)tone.startTone(ToneGenerator.TONE_PROP_NACK,110);
        } catch(Exception ignored){}
    }
    @Override public void onBackPressed() {
        if(game!=null)game.goBack();else super.onBackPressed();
    }
    @Override protected void onDestroy() {
        if(tone!=null){tone.release();tone=null;}
        super.onDestroy();
    }

    static class Obj {
        float z; int lane,kind; boolean used;
        Obj(float z,int lane,int kind){this.z=z;this.lane=lane;this.kind=kind;}
    }
    static class Part {
        float x,y,vx,vy,life,maxLife,size;int color;
        Part(float x,float y,float vx,float vy,float life,float size,int color){
            this.x=x;this.y=y;this.vx=vx;this.vy=vy;this.life=this.maxLife=life;this.size=size;this.color=color;
        }
    }
    static class Star {
        float x,y,s,blink;
        Star(float x,float y,float s,float blink){this.x=x;this.y=y;this.s=s;this.blink=blink;}
    }

    class RiderView extends View {
        static final int MENU=0,SELECT=1,GARAGE=2,PLAY=3,PAUSE=4,RESULT=5;
        final int WHITE=0xffe7f6ff, MUTED=0xff8094b2,GOLD=0xffffd477,
                  CYAN=0xff31f5ff,PINK=0xffff4b9b,GREEN=0xff61ffa2;
        final int[][] PALETTES = {
                {0xff0c133b,0xff2e165a,0xff15d8eb,0xff8b5dff},
                {0xff2b1631,0xff802e34,0xffffae4d,0xffff546f},
                {0xff0b2c46,0xff222b64,0xff63dbff,0xff78a6ff},
                {0xff091f2c,0xff16514d,0xff7cffac,0xffffe575},
                {0xff120d32,0xff511b5c,0xffff63d8,0xff6f8fff}};
        final String[] SECTORS={"SKYLINE ZERO","CRIMSON DUNES","FROST ARRAY","BIOFORGE DELTA","THE BLACK SUN"};
        final String[] STORIES={
                "A CITY BUILT ABOVE THE CLOUDS",
                "RUN THE FORGOTTEN DESERT",
                "THE MACHINES NEVER SLEEP",
                "ESCAPE THE LIVING FACTORY",
                "THE FINAL RIFT AWAITS"
        };
        final Random rng=new Random(123789);
        final Random spawnRandom=new Random();
        final Paint p=new Paint(3);
        final Path path=new Path();
        final ArrayList<Obj> objects=new ArrayList<>();
        final ArrayList<Part> particles=new ArrayList<>();
        final Star[] stars=new Star[92];
        final SharedPreferences prefs;
        int state=MENU,sector=0,unlocked=0,bank=0,record=0,shipskin=0;
        int engineLv=0,armorLv=0,chronoLv=0,magnetLv=0;
        int lives=3,earned=0,runCoins=0,combo=0,bestCombo=0;
        int lane=0;float lanePosition=0,shipLean=0;
        float time=0,lastFrame=0,meters=0,nextSpawn=0,speed=52;
        float hpFlash=0,shake=0,overdrive=0,overdriveMeter=22,warp=0,warpCooldown=0;
        float invulnerable=0,magnetFlash=0,intro=3.3f,resultClock=0;
        float bossWarning=-1,bossShot=-1,bossFire=0,bossHealth=100,bossTimer=0;
        boolean bossActive=false,bossDead=false,won=false;
        int bossLane=0;
        int animCoins=0;
        long frameNano=0;
        float touchDownX,touchDownY;
        int downPointer=-1;
        String banner="";

        RiderView(Context c) {
            super(c);
            setLayerType(View.LAYER_TYPE_SOFTWARE,null);
            prefs=c.getSharedPreferences("voidrider_1",0);
            bank=prefs.getInt("bank",0);
            record=prefs.getInt("record",0);
            unlocked=prefs.getInt("unlocked",0);
            sector=prefs.getInt("sector",0);
            engineLv=prefs.getInt("engine",0);
            armorLv=prefs.getInt("armor",0);
            chronoLv=prefs.getInt("chrono",0);
            magnetLv=prefs.getInt("magnet",0);
            shipskin=prefs.getInt("skin",0);
            for(int i=0;i<stars.length;i++)
                stars[i]=new Star(rng.nextInt(960),rng.nextInt(255),1+rng.nextFloat()*2,rng.nextFloat()*6);
            p.setTypeface(android.graphics.Typeface.create("sans-serif-condensed",1));
        }
        void persist(){
            prefs.edit().putInt("bank",bank).putInt("record",record)
                 .putInt("unlocked",unlocked).putInt("sector",sector)
                 .putInt("engine",engineLv).putInt("armor",armorLv)
                 .putInt("chrono",chronoLv).putInt("magnet",magnetLv)
                 .putInt("skin",shipskin).apply();
        }
        float goal(){return 1750+sector*380;}
        int maxLives(){return 3+armorLv;}
        float warpDuration(){return 2.3f+chronoLv*.85f;}
        void begin(){
            state=PLAY;frameNano=System.nanoTime();lastFrame=0;
            objects.clear();particles.clear();lives=maxLives();
            lane=0;lanePosition=0;shipLean=0;meters=0;earned=0;runCoins=0;
            combo=bestCombo=0;speed=52+sector*6+engineLv*2;
            nextSpawn=30;intro=3.1f;invulnerable=2;
            warp=0;warpCooldown=0;overdrive=0;overdriveMeter=24;
            bossActive=false;bossDead=false;bossHealth=100;bossTimer=0;bossWarning=-1;bossShot=-1;
            won=false;resultClock=0;hpFlash=0;banner="";
        }
        void end(boolean success){
            won=success;state=RESULT;resultClock=0;
            int bonus=success?400+sector*125:0;
            earned=runCoins+bonus;
            bank+=earned;
            if((int)meters>record)record=(int)meters;
            if(success && sector==unlocked && unlocked<4)unlocked++;
            persist();
            sound(success?1:2);
        }
        void goBack(){
            if(state==PLAY)state=PAUSE;
            else if(state==PAUSE){state=PLAY;frameNano=System.nanoTime();}
            else if(state==MENU)finish();
            else state=MENU;
        }
        @Override protected void onDraw(Canvas real){
            super.onDraw(real);
            long now=System.nanoTime();
            if(frameNano==0)frameNano=now;
            float dt=Math.min(.04f,(now-frameNano)/1e9f);
            frameNano=now;time+=dt;
            if(state==PLAY)update(dt);
            if(state==RESULT)resultClock+=dt;
            Canvas c=real;
            c.save();c.scale(getWidth()/960f,getHeight()/540f);
            drawSky(c);
            if(state==MENU)drawMenu(c);
            else if(state==SELECT)drawSelect(c);
            else if(state==GARAGE)drawGarage(c);
            else {
                c.save();
                if(shake>0)c.translate((rng.nextFloat()-.5f)*shake,(rng.nextFloat()-.5f)*shake);
                drawWorld(c);
                c.restore();
                drawHud(c);
                if(state==PAUSE)drawPause(c);
                if(state==RESULT)drawResult(c);
            }
            c.restore();
            postInvalidateOnAnimation();
        }
        void update(float dt){
            intro=Math.max(0,intro-dt);
            invulnerable=Math.max(0,invulnerable-dt);
            hpFlash=Math.max(0,hpFlash-dt*2.5f);
            shake=Math.max(0,shake-dt*31);
            warpCooldown=Math.max(0,warpCooldown-dt);
            warp=Math.max(0,warp-dt);
            overdrive=Math.max(0,overdrive-dt);
            if(bossShot>=0)bossShot-=dt;
            if(bossWarning>=0)bossWarning-=dt;
            if(magnetFlash>0)magnetFlash-=dt;
            float f=(warp>0?.30f:1f)*(overdrive>0?1.9f:1f);
            float travel=speed*dt*f;
            meters+=travel;
            lanePosition+=(lane-lanePosition)*Math.min(1,dt*12);
            shipLean+=(lane-lanePosition)*Math.min(1,dt*15);
            if(overdrive<=0)overdriveMeter=Math.min(100,overdriveMeter+dt*(2+engineLv*.6f));
            nextSpawn-=travel;
            if(nextSpawn<=0&&!bossActive){
                createRow();
                nextSpawn=89-(sector*4)+spawnRandom.nextInt(42);
            }
            for(int i=objects.size()-1;i>=0;i--){
                Obj o=objects.get(i);
                o.z-=travel;
                if(!o.used && o.z<=18 && o.z>1){
                    if(o.kind==3||o.kind==4){
                        if(Math.abs(lanePosition-o.lane)<(.67f+magnetLv*.17f)){
                            if(o.kind==3){runCoins+=12;combo++;overdriveMeter=Math.min(100,overdriveMeter+4);burst(o.lane,GOLD,15);sound(0);}
                            else {overdriveMeter=Math.min(100,overdriveMeter+25);burst(o.lane,CYAN,22);sound(1);}
                            o.used=true;
                            bestCombo=Math.max(combo,bestCombo);
                        }
                    }else if(Math.abs(lanePosition-o.lane)<.53f){
                        o.used=true;
                        if(overdrive>0||invulnerable>0){burst(o.lane,CYAN,14);}
                        else crash();
                    }
                }
                if(o.z<-15)objects.remove(i);
            }
            if(meters>goal()*.70f && !bossActive){
                bossActive=true;bossTimer=0;bossHealth=100;bossWarning=0;
                banner="INCOMING TITAN";
            }
            if(bossActive&&!bossDead){
                bossTimer+=dt;
                bossHealth=Math.max(0,100-dt*(3.6f+engineLv*1.15f));
                if(bossWarning<0&&bossShot<0){
                    bossLane=lane;
                    bossWarning=1.25f;bossShot=1.6f;
                }
                if(bossShot>=0&&bossShot<.37f){
                    if(Math.abs(lanePosition-bossLane)<.53f&&invulnerable<=0&&overdrive<=0){
                        crash();invulnerable=.8f;
                    }
                    bossShot=-1;bossWarning=-1;
                }
                if(bossHealth<=0){bossDead=true;runCoins+=300;shake=17;burst(0,PINK,55);}
            }
            if(meters>=goal()&&(!bossActive||bossDead)){end(true);return;}
            for(int i=particles.size()-1;i>=0;i--){
                Part a=particles.get(i);
                a.x+=a.vx*dt;a.y+=a.vy*dt;a.life-=dt;
                if(a.life<=0)particles.remove(i);
            }
            if(spawnRandom.nextFloat()<.77f && particles.size()<180){
                float x=laneX(lanePosition,17);
                particles.add(new Part(x+(rng.nextFloat()-.5f)*31,458, (rng.nextFloat()-.5f)*22,
                        70+rng.nextFloat()*180,.23f+rng.nextFloat()*.22f,2+rng.nextFloat()*4,
                        overdrive>0?GOLD:PALETTES[sector][2]));
            }
        }
        void crash(){
            lives--;combo=0;hpFlash=1;shake=18;invulnerable=1.5f;
            burst(lane,PINK,35);sound(2);
            if(lives<=0){end(false);}
        }
        void createRow(){
            int safe=spawnRandom.nextInt(3)-1;
            int difficulty=sector+((int)meters/480);
            int n=spawnRandom.nextInt(100)<Math.min(80,37+difficulty*5)?2:1;
            if(n==2){
                for(int v=-1;v<=1;v++)if(v!=safe)objects.add(new Obj(176,v,hazard()));
            }else{
                int l=safe+(spawnRandom.nextBoolean()?1:2); if(l>1)l-=3;
                objects.add(new Obj(176,l,hazard()));
            }
            objects.add(new Obj(195,safe,spawnRandom.nextInt(100)<16?4:3));
            if(spawnRandom.nextBoolean())objects.add(new Obj(217,safe,3));
        }
        int hazard(){return spawnRandom.nextInt(100)<42?0:spawnRandom.nextBoolean()?1:2;}
        void burst(float ln,int color,int count){
            float x=laneX(ln,17);
            for(int i=0;i<count&&particles.size()<220;i++){
                float a=rng.nextFloat()*6.283f;
                float v=48+rng.nextFloat()*250;
                particles.add(new Part(x,445,(float)Math.cos(a)*v,(float)Math.sin(a)*v,
                        .2f+rng.nextFloat()*.45f,1+rng.nextFloat()*5,color));
            }
        }
        void shift(){
            if(warpCooldown>0||warp>0||state!=PLAY)return;
            warp=warpDuration();warpCooldown=15-chronoLv*.8f;invulnerable=Math.max(invulnerable,.3f);
            shake=5;burst(lane,CYAN,45);sound(1);
        }
        void boost(){
            if(overdriveMeter<55||state!=PLAY)return;
            overdriveMeter-=55;overdrive=2.2f+engineLv*.22f;
            invulnerable=overdrive+0.05f;shake=8;burst(lane,GOLD,40);sound(1);
        }

        float perspective(float z){return 1/(1+Math.max(0,z)/34f);}
        float roadY(float z){return 188+383*perspective(z);}
        float roadHalf(float z){return 16+422*perspective(z);}
        float laneX(float ln,float z){return 480+ln*roadHalf(z)*.56f;}
        int tint(int col,int alpha){return (col&0x00ffffff)|(Math.min(255,Math.max(0,alpha))<<24);}
        void box(Canvas c,float x,float y,float x2,float y2,float r,int col){
            p.setStyle(Paint.Style.FILL);p.setColor(col);p.setShader(null);
            c.drawRoundRect(x,y,x2,y2,r,r,p);
        }
        void outline(Canvas c,float x,float y,float x2,float y2,float r,int col,float width){
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(width);p.setColor(col);
            c.drawRoundRect(x,y,x2,y2,r,r,p);p.setStyle(Paint.Style.FILL);
        }
        void text(Canvas c,String s,float x,float y,int size,int color,boolean center){
            p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(color);
            p.setTypeface(android.graphics.Typeface.create("sans-serif-condensed",1));
            p.setTextAlign(center?Paint.Align.CENTER:Paint.Align.LEFT);
            p.setTextSize(size);c.drawText(s,x,y,p);
        }
        void poly(Canvas c,int color,float... v){
            path.reset();path.moveTo(v[0],v[1]);
            for(int i=2;i<v.length;i+=2)path.lineTo(v[i],v[i+1]);
            path.close();
            p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(color);c.drawPath(path,p);
        }
        void line(Canvas c,float x1,float y1,float x2,float y2,int col,float width){
            p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(width);
            p.setColor(col);c.drawLine(x1,y1,x2,y2,p);p.setStyle(Paint.Style.FILL);
        }
        void ellipse(Canvas c,float x,float y,float r,int color){
            p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(color);c.drawCircle(x,y,r,p);
        }
        void drawSky(Canvas c){
            int[] pal=PALETTES[sector];
            p.setStyle(Paint.Style.FILL);
            p.setShader(new LinearGradient(0,0,0,400,new int[]{0xff030a19,pal[0],pal[1]},null,Shader.TileMode.CLAMP));
            c.drawRect(0,0,960,540,p);p.setShader(null);
            float sunX=675+(float)Math.sin(time*.12f)*28;
            for(int i=0;i<6;i++)ellipse(c,sunX,165,121-i*17,tint(pal[3],7+i*3));
            ellipse(c,sunX,165,38,tint(pal[2],73));
            ellipse(c,sunX,165,22,tint(GOLD,135));
            if(sector==4){
                ellipse(c,675,161,62,0xffec57ca);
                ellipse(c,675,156,53,0xff140c29);
                line(c,588,161,763,161,0x66ff9aea,2);
            }
            for(Star s:stars){
                int alpha=(int)(100+85*Math.sin(time*1.7+s.blink));
                ellipse(c,s.x,(s.y+time*(.7f+s.s*.5f))%288,s.s*.60f,tint(WHITE,alpha));
            }
            drawCity(c);
        }
        void drawCity(Canvas c){
            int[] pal=PALETTES[sector];
            for(int layer=0;layer<2;layer++){
                float shift=time*(layer==0?1.0f:2.6f);
                for(int i=-2;i<17;i++){
                    int seed=(i+30)*31+layer*57;
                    float w=53+(seed*17)%59;
                    float x=i*81-(shift%(81))+(layer==0?0:41);
                    float tall=38+(seed*13)%122;
                    float top=283-tall-(layer*19);
                    poly(c,layer==0?0xaa08162f:0xff081426,
                            x,291,x,top,x+w,top,x+w,291);
                    if(layer==1){
                        for(int j=0;j<5;j++){
                            float wy=top+12+j*15;
                            line(c,x+10,wy,x+w-13,wy,tint(pal[2],35+(seed+j)%45),1);
                        }
                        line(c,x,top,x+w,top,tint(pal[3],70),1.4f);
                    }
                }
            }
            // Mountains, vapor and the distant ruined horizon
            poly(c,0xff091a2a,0,302,0,257,110,287,173,248,225,287,337,237,412,284,
                    553,247,654,287,747,252,837,282,960,247,960,321);
            line(c,0,302,960,302,tint(pal[2],55),2);
            if(sector==1){
                for(int i=0;i<22;i++){
                    float x=(i*51+time*45)%960;
                    line(c,x,155+(i*19)%140,x-16,170+(i*19)%140,0x66ffaf77,1);
                }
            }else if(sector==2){
                for(int i=0;i<20;i++){
                    float x=(i*71+time*10)%960,y=(i*31+time*20)%230;
                    ellipse(c,x,y,2,0xaaccf8ff);
                }
            }
        }
        void drawWorld(Canvas c){
            int[] pal=PALETTES[sector];
            poly(c,0xff07121d,0,298,960,298,960,540,0,540);
            // The multilayer, vanishing-point highway is built in depth.
            for(int z=220;z>=0;z-=9){
                float y1=roadY(z), y2=roadY(z-9);
                float h1=roadHalf(z),h2=roadHalf(z-9);
                int k=(int)((z+meters)%72/36);
                poly(c,k==0?0xff0e2135:0xff11283d,
                        480-h1,y1,480+h1,y1,480+h2,y2,480-h2,y2);
                if((int)((z+meters)/35)%2==0){
                    int c1=tint(pal[2],65);
                    poly(c,c1,480-h1,y1,480-h1+3,y1,480-h2+7,y2,480-h2,y2);
                    poly(c,tint(pal[3],95),480+h1-3,y1,480+h1,y1,480+h2,y2,480+h2-7,y2);
                }
            }
            // Lane dividers and distance pulses.
            for(int l=-1;l<=1;l+=2){
                for(int z=180;z>=0;z-=20){
                    float a=z+(meters%20),b=a-11;
                    float x1=480+l*roadHalf(a)*.31f;
                    float x2=480+l*roadHalf(b)*.31f;
                    line(c,x1,roadY(a),x2,roadY(b),tint(pal[2],170),Math.max(1,6*perspective(a)));
                }
            }
            for(int z=180;z>=0;z-=17){
                float a=z+(meters%17),b=a-5;
                for(int side=-1;side<=1;side+=2){
                    float xa=480+side*(roadHalf(a)+20),xb=480+side*(roadHalf(b)+20);
                    line(c,xa,roadY(a),xb,roadY(b),tint(pal[3],90),1.6f);
                }
            }
            if(overdrive>0||warp>0){
                int co=overdrive>0?GOLD:CYAN;
                for(int i=0;i<19;i++){
                    float x=rng.nextInt(960),y=rng.nextInt(540);
                    line(c,x,y,x+(x-480)*.18f,y+22,tint(co,70+rng.nextInt(140)),1.5f);
                }
            }
            drawBoss(c);
            // Far objects first, near objects last.
            for(int i=0;i<objects.size();i++){
                Obj o=objects.get(i);
                if(o.z<235&&!o.used)drawObject(c,o);
            }
            drawShip(c,laneX(lanePosition,17),445,1.0f,true);
            for(Part a:particles){
                float fraction=a.life/a.maxLife;
                ellipse(c,a.x,a.y,a.size*fraction,tint(a.color,(int)(fraction*210)));
            }
            if(hpFlash>0)box(c,0,0,960,540,0,tint(PINK,(int)(hpFlash*44)));
        }
        void drawBoss(Canvas c){
            if(!bossActive||bossDead)return;
            int co=PALETTES[sector][3];
            float bx=480+(float)Math.sin(time*.66)*48;
            float by=142+(float)Math.cos(time*.9f)*9;
            for(int i=0;i<4;i++)ellipse(c,bx,by,130-i*20,tint(co,6+i*7));
            poly(c,0xff050c18,bx-138,by+8,bx-88,by-26,bx-45,by-17,
                    bx,by-49,bx+45,by-17,bx+88,by-26,bx+138,by+8,
                    bx+94,by+40,bx,by+49,bx-94,by+40);
            poly(c,0xff314365,bx-112,by+4,bx-53,by-5,bx,by-40,bx+53,by-5,bx+112,by+4,
                    bx+63,by+24,bx,by+35,bx-63,by+24);
            poly(c,co,bx-39,by-6,bx,by-24,bx+39,by-6,bx+26,by+21,bx-26,by+21);
            ellipse(c,bx,by,15,0xff101630);
            ellipse(c,bx,by,10,GOLD);
            line(c,bx-60,by+17,bx+60,by+17,tint(co,195),3);
            box(c,331,68,629,85,8,0xaa07101e);
            box(c,334,71,334+292*bossHealth/100f,82,4,co);
            text(c,"RIFT TITAN   "+(int)bossHealth+"%",480,62,18,WHITE,true);
            if(bossWarning>0){
                float xx=laneX(bossLane,17);
                float a=(float)(.5+.5*Math.sin(time*18));
                poly(c,tint(PINK,(int)(55+110*a)),xx-67,500,xx+67,500,
                        480+bossLane*40,230,480+bossLane*40,230);
                box(c,xx-58,477,xx+58,511,8,tint(PINK,130));
                text(c,"DODGE!",xx,500,18,WHITE,true);
            }else if(bossShot>=0&&bossShot<.4f){
                float xx=laneX(bossLane,17);
                line(c,bx,by,xx,505,PINK,22);
                line(c,bx,by,xx,505,WHITE,6);
            }
        }
        void drawObject(Canvas c,Obj o){
            float z=Math.max(0,o.z);
            float f=perspective(z);
            float x=laneX(o.lane,z),y=roadY(z);
            float w=76*f+4,h=82*f+6;
            int[] pal=PALETTES[sector];
            if(o.kind==3||o.kind==4){
                float bob=(float)Math.sin(time*7+z*.1)*6*f;
                float r=o.kind==3?17*f+2:21*f+3;
                int col=o.kind==3?GOLD:CYAN;
                for(int i=4;i>=1;i--)ellipse(c,x,y-h*.6f+bob,r+i*3*f,tint(col,16));
                ellipse(c,x,y-h*.6f+bob,r,col);
                ellipse(c,x,y-h*.6f+bob,r*.63f,0xff0b2339);
                ellipse(c,x,y-h*.6f+bob,r*.36f,WHITE);
                if(o.kind==4){
                    line(c,x-r,y-h*.6f+bob,x+r,y-h*.6f+bob,WHITE,Math.max(1,3*f));
                    line(c,x,y-h*.6f+bob-r,x,y-h*.6f+bob+r,WHITE,Math.max(1,3*f));
                }
                return;
            }
            ellipse(c,x,y,42*f,tint(PINK,20));
            if(o.kind==0){ // Electromagnetic barrier
                poly(c,0xff243149,x-w,y-h,x+w,y-h,x+w*1.2f,y-5,x-w*1.2f,y-5);
                poly(c,0xffe13e6b,x-w,y-h,x+w,y-h,x+w,y-h+14*f,x-w,y-h+14*f);
                for(int i=0;i<3;i++){
                    float ty=y-h+22*f+i*18*f;
                    line(c,x-w*.8f,ty,x+w*.8f,ty,tint(PINK,210),Math.max(1,4*f));
                }
                text(c,"!",x,y-h*.3f,Math.max(11,(int)(36*f)),WHITE,true);
            } else if(o.kind==1){ // Security laser ring
                poly(c,0xff263d54,x-w,y-h,x-w*.65f,y-h,x-w*.65f,y,x-w,y);
                poly(c,0xff263d54,x+w*.65f,y-h,x+w,y-h,x+w,y,x+w*.65f,y);
                line(c,x-w*.72f,y-h*.60f,x+w*.72f,y-h*.60f,tint(PINK,210),Math.max(2,9*f));
                line(c,x-w*.72f,y-h*.30f,x+w*.72f,y-h*.30f,tint(PINK,180),Math.max(2,6*f));
                ellipse(c,x,y-h*.5f,13*f,tint(PINK,120));
            } else { // Hunter drone
                float by=y-h*.60f+(float)Math.sin(time*7+z)*5*f;
                for(int i=3;i>=1;i--)ellipse(c,x,by,37*f+i*4*f,tint(PINK,13));
                poly(c,0xff192a40,x-52*f,by,x-20*f,by-22*f,
                        x+20*f,by-22*f,x+52*f,by,x+20*f,by+17*f,x-20*f,by+17*f);
                ellipse(c,x,by,12*f,0xffee347d);
                ellipse(c,x,by,6*f,WHITE);
                line(c,x-42*f,by+6*f,x+42*f,by+6*f,tint(pal[2],190),3*f+1);
            }
        }
        void drawShip(Canvas c,float x,float y,float scale,boolean gameplay){
            int co=shipskin==0?CYAN:shipskin==1?GOLD:shipskin==2?PINK:GREEN;
            c.save();c.translate(x,y);c.scale(scale,scale);
            c.rotate(shipLean*12+(float)Math.sin(time*2)*1.5f);
            // Engines and exhaust
            float flame=14+(float)Math.sin(time*26)*5+(overdrive>0?18:0);
            for(int i=-1;i<=1;i+=2){
                poly(c,tint(co,45),i*20,25,i*9,25,i*13,47+flame,i*29,48+flame);
                poly(c,tint(GOLD,190),i*20,26,i*14,27,i*17,31+flame,i*25,32+flame);
            }
            for(int q=0;q<4;q++)ellipse(c,0,5,62-q*11,tint(co,4+q*5));
            // shadow plane and distinct layered silhouette
            poly(c,0xff070c1c,0,-61,-20,-27,-79,21,-70,41,-23,28,0,45,
                    23,28,70,41,79,21,20,-27);
            poly(c,0xff263857,0,-58,-15,-26,-67,21,-59,33,-21,20,0,36,
                    21,20,59,33,67,21,15,-26);
            poly(c,co,0,-57,-9,-26,-44,12,-53,23,-17,11,0,24,
                    17,11,53,23,44,12,9,-26);
            poly(c,0xff081a34,0,-44,-11,-15,-12,23,0,34,12,23,11,-15);
            poly(c,0xffd2faff,0,-36,-5,-14,-5,13,0,20,5,13,5,-14);
            poly(c,0xff4a5881,-58,21,-37,11,-23,26,-49,32);
            poly(c,0xff4a5881,58,21,37,11,23,26,49,32);
            line(c,-54,19,-31,19,GOLD,3);
            line(c,54,19,31,19,GOLD,3);
            ellipse(c,0,-41,5,GOLD);
            if(gameplay&&invulnerable>0){
                for(int i=0;i<3;i++)ellipse(c,0,0,66+i*8,tint(CYAN,14));
                p.setColor(tint(CYAN,130));p.setStrokeWidth(2);p.setStyle(Paint.Style.STROKE);
                c.drawCircle(0,0,67,p);p.setStyle(Paint.Style.FILL);
            }
            c.restore();
        }
        void button(Canvas c,float x,float y,float w,float h,String s,int col){
            box(c,x,y,x+w,y+h,13,0xee152741);
            outline(c,x,y,x+w,y+h,13,tint(col,175),2);
            text(c,s,x+w/2,y+h/2+8,23,col,true);
        }
        void panel(Canvas c,float x,float y,float w,float h){
            box(c,x,y,x+w,y+h,18,0xf00b1528);
            outline(c,x,y,x+w,y+h,18,0x6749c9ee,1.5f);
        }
        void drawMenu(Canvas c){
            // showcase hero craft
            for(int i=0;i<5;i++)ellipse(c,635,311,132-i*18,tint(CYAN,4+i*5));
            drawShip(c,635,319,2.25f,false);
            box(c,36,51,540,482,22,0xd70a132a);
            outline(c,36,51,540,482,22,0x6641faff,2);
            text(c,"A CINEMATIC SCI-FI ESCAPE",63,105,18,CYAN,false);
            text(c,"VOID",58,207,97,WHITE,false);
            text(c,"RIDER",58,288,97,PINK,false);
            text(c,"RIFT RUN  /  2098",66,323,24,GOLD,false);
            text(c,"THE SKY IS FALLING. KEEP FLYING.",65,360,19,MUTED,false);
            button(c,62,389,212,64,"DEPLOY",CYAN);
            button(c,288,389,223,64,"GARAGE",GOLD);
            text(c,"CAMPAIGN: "+(unlocked+1)+"/5      BEST: "+record+" m",64,467,16,MUTED,false);
            text(c,"CREATED BY AMIR ALI",638,511,21,WHITE,true);
        }
        void drawSelect(Canvas c){
            panel(c,28,27,904,486);
            text(c,"CAMPAIGN // SELECT A SECTOR",56,79,34,CYAN,false);
            text(c,"FINISH THE RIFT TITAN TO UNLOCK THE NEXT WORLD",57,112,18,MUTED,false);
            for(int i=0;i<5;i++){
                float x=48+i*177,y=149;
                int[] pal=PALETTES[i];
                boolean available=i<=unlocked;
                box(c,x,y,x+163,y+275,14,available?0xff14233c:0xff0c1728);
                outline(c,x,y,x+163,y+275,14,available?tint(pal[2],200):0xff334050,available?2:1);
                box(c,x+9,y+9,x+154,y+125,8,pal[0]);
                for(int q=0;q<5;q++){
                    float m=q*17f;
                    line(c,x+13+m,y+113,x+81,y+15,tint(pal[2],25+q*18),2);
                }
                ellipse(c,x+82,y+73,31,tint(pal[3],160));
                ellipse(c,x+82,y+73,15,0xff091127);
                text(c,"0"+(i+1),x+16,y+157,22,available?GOLD:MUTED,false);
                String[] top={"SKYLINE","CRIMSON","FROST","BIOFORGE","BLACK SUN"};
                String[] low={"ZERO","DUNES","ARRAY","DELTA","RIFT"};
                text(c,top[i],x+15,y+189,23,available?WHITE:MUTED,false);
                text(c,low[i],x+15,y+216,22,available?pal[2]:MUTED,false);
                text(c,available?(i==sector?"SELECTED":"TAP TO SELECT"):"LOCKED",x+15,y+255,15,available?GREEN:MUTED,false);
            }
            button(c,50,444,190,51,"BACK",MUTED);
            button(c,667,444,245,51,"START MISSION",CYAN);
        }
        void drawGarage(Canvas c){
            panel(c,27,20,906,502);
            text(c,"THE HANGAR  //  SHIP UPGRADES",56,69,34,CYAN,false);
            text(c,"CREDITS: "+bank,758,70,24,GOLD,false);
            text(c,"ENGINEERING BAY • PERMANENT UPGRADES",56,101,16,MUTED,false);
            String[] name={"ION ENGINES","REINFORCED HULL","CHRONO CORE","CREDIT MAGNET"};
            String[] desc={"+ SPEED AND BOOST TIME","+ ONE ARMOR HEART","+ SLOW-MOTION DURATION","+ WIDER PICKUP RADIUS"};
            int[] lvl={engineLv,armorLv,chronoLv,magnetLv};
            for(int i=0;i<4;i++){
                float x=51+(i%2)*458,y=119+(i/2)*154;
                int accent=i==0?CYAN:i==1?PINK:i==2?0xffba88ff:GOLD;
                box(c,x,y,x+438,y+138,15,0xff14243d);
                outline(c,x,y,x+438,y+138,15,tint(accent,140),2);
                ellipse(c,x+38,y+48,24,tint(accent,110));
                text(c,new String[]{"»","✚","◷","◇"}[i],x+37,y+57,32,WHITE,true);
                text(c,name[i],x+79,y+41,24,WHITE,false);
                text(c,desc[i],x+79,y+69,16,MUTED,false);
                for(int j=0;j<4;j++)box(c,x+81+j*33,y+85,x+109+j*33,y+94,2,j<lvl[i]?accent:0xff35465b);
                int cost=cost(i);
                text(c,lvl[i]>=4?"MAX LEVEL":cost+" CREDITS",x+265,y+96,18,lvl[i]>=4?GREEN:GOLD,false);
                text(c,"TAP TO BUY",x+265,y+119,15,CYAN,false);
            }
            button(c,54,446,219,55,"BACK",MUTED);
            text(c,"FINISH MISSIONS, COLLECT ORBS, GET STRONGER.",455,485,18,WHITE,false);
        }
        int level(int i){return i==0?engineLv:i==1?armorLv:i==2?chronoLv:magnetLv;}
        int cost(int i){return (i==0?160:i==1?260:i==2?210:175)+level(i)*260;}
        void purchase(int i){
            if(level(i)>=4)return;
            int price=cost(i);
            if(bank<price){shake=3;sound(2);return;}
            bank-=price;
            if(i==0)engineLv++;
            else if(i==1)armorLv++;
            else if(i==2)chronoLv++;
            else magnetLv++;
            persist();sound(1);
        }
        void drawHud(Canvas c){
            box(c,15,10,303,73,13,0xd8091529);
            text(c,"SECTOR 0"+(sector+1)+"   /   "+SECTORS[sector],30,33,17,CYAN,false);
            text(c,""+(int)meters+"m   /   "+(int)goal()+"m",30,60,23,WHITE,false);
            box(c,312,17,647,31,6,0xff172d41);
            box(c,313,18,313+333*Math.min(1,meters/goal()),30,5,GOLD);
            for(int i=0;i<maxLives();i++){
                text(c,"◆",322+i*26,57,24,i<lives?PINK:0xff394458,false);
            }
            text(c,"COMBO  x"+Math.max(1,combo),495,56,18,GREEN,false);
            box(c,663,9,941,73,14,0xe90b172c);
            text(c,"CREDITS +"+runCoins,682,33,19,GOLD,false);
            text(c,"BOOST "+(int)overdriveMeter+"%",682,58,18,WHITE,false);
            text(c,"Ⅱ",917,39,29,CYAN,true);
            if(state!=PLAY)return;
            // Clearly labeled touch targets
            button(c,18,428,133,89,"◀ LEFT",CYAN);
            button(c,169,428,133,89,"RIGHT ▶",CYAN);
            button(c,660,434,123,76,warp>0?"WARPING":warpCooldown>0?((int)warpCooldown)+"s":"TIME",0xffac9dff);
            button(c,805,434,138,76,overdrive>0?"BOOSTING":"BOOST",GOLD);
            if(intro>0){
                box(c,265,118,696,191,16,0xdd10223a);
                outline(c,265,118,696,191,16,CYAN,1);
                text(c,"SWIPE LEFT / RIGHT TO DODGE",480,147,23,WHITE,true);
                text(c,"COLLECT COINS  •  TIME SHIFT  •  BOOST",480,176,18,GOLD,true);
            }
            if(bossActive&&!bossDead){
                box(c,327,84,634,103,9,0xee102238);
                box(c,332,89,332+297*bossHealth/100,98,3,PINK);
                text(c,"RIFT TITAN",480,122,16,WHITE,true);
            }
        }
        void drawPause(Canvas c){
            box(c,0,0,960,540,0,0xd6000716);
            panel(c,276,87,408,353);
            text(c,"MISSION PAUSED",480,160,46,WHITE,true);
            text(c,SECTORS[sector]+"  /  "+(int)meters+"m",480,198,19,GOLD,true);
            button(c,318,242,324,63,"RESUME",CYAN);
            button(c,318,321,324,63,"ABORT MISSION",PINK);
        }
        void drawResult(Canvas c){
            box(c,0,0,960,540,0,0xe4050c1d);
            panel(c,213,62,534,427);
            text(c,won?"RIFT TITAN DEFEATED":"SIGNAL LOST",480,147,48,won?CYAN:PINK,true);
            text(c,won?"SECTOR CLEARED!":"SHIP DESTROYED",480,184,23,won?GREEN:WHITE,true);
            text(c,"DISTANCE     "+(int)meters+" m",480,231,25,WHITE,true);
            text(c,"CREDITS EARNED     +"+earned,480,271,25,GOLD,true);
            text(c,"BEST DISTANCE     "+record+" m",480,309,18,MUTED,true);
            button(c,257,340,215,62,"RETRY",CYAN);
            button(c,492,340,215,62,won?"NEXT SECTOR":"GARAGE",won?GREEN:GOLD);
            text(c,"AMIR ALI  //  VOIDRIDER",480,462,15,MUTED,true);
        }
        void swipe(int delta){
            if(delta>0)lane=Math.min(1,lane+1);
            else lane=Math.max(-1,lane-1);
            sound(0);
        }
        void handleTap(float x,float y){
            if(state==MENU){
                if(y>=379&&y<=470){
                    if(x>=53&&x<284)state=SELECT;
                    else if(x>=288&&x<535)state=GARAGE;
                }
            }else if(state==SELECT){
                if(y>=144&&y<429){
                    int i=(int)((x-48)/177);
                    if(x>=48&&i>=0&&i<5&&i<=unlocked){sector=i;persist();sound(0);}
                }else if(y>=440&&y<=506){
                    if(x<260)state=MENU;
                    else if(x>655)begin();
                }
            }else if(state==GARAGE){
                if(y>=119&&y<415){
                    int col=x<510?0:1,row=y<273?0:1;
                    purchase(row*2+col);
                }else if(y>=438&&x<304)state=MENU;
            }else if(state==PLAY){
                if(y<84&&x>880){state=PAUSE;sound(0);}
                else if(y>402){
                    if(x<165)swipe(-1);
                    else if(x<313)swipe(1);
                    else if(x>645&&x<798)shift();
                    else if(x>799)boost();
                }
            }else if(state==PAUSE){
                if(y>233&&y<312){state=PLAY;frameNano=System.nanoTime();}
                else if(y>315&&y<403){state=MENU;}
            }else if(state==RESULT){
                if(y>=331&&y<=416){
                    if(x<484)begin();
                    else if(won){sector=Math.min(unlocked,sector+1);persist();begin();}
                    else state=GARAGE;
                }
            }
        }
        @Override public boolean onTouchEvent(MotionEvent e){
            int action=e.getActionMasked();
            int idx=e.getActionIndex();
            float x=e.getX(idx)*960/Math.max(1,getWidth());
            float y=e.getY(idx)*540/Math.max(1,getHeight());
            if(action==MotionEvent.ACTION_DOWN || action==MotionEvent.ACTION_POINTER_DOWN){
                if(downPointer==-1){downPointer=e.getPointerId(idx);touchDownX=x;touchDownY=y;}
                return true;
            }
            if(action==MotionEvent.ACTION_UP || action==MotionEvent.ACTION_POINTER_UP){
                int pointer=e.getPointerId(idx);
                if(pointer==downPointer){
                    float dx=x-touchDownX,dy=y-touchDownY;
                    if(state==PLAY&&Math.abs(dx)>72&&Math.abs(dx)>Math.abs(dy)*.70f)swipe(dx>0?1:-1);
                    else if(state==PLAY&&dy<-70)boost();
                    else if(state==PLAY&&dy>100)shift();
                    else handleTap(x,y);
                    downPointer=-1;
                }
                return true;
            }
            if(action==MotionEvent.ACTION_CANCEL){downPointer=-1;return true;}
            return true;
        }
    }
}
