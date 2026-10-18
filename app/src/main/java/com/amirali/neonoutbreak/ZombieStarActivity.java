package com.amirali.neonoutbreak;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import java.util.ArrayList;
import java.util.Random;

public class ZombieStarActivity extends Activity {
    private Arena game;
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(1024, 1024);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        immersive();
        game = new Arena(this);
        setContentView(game);
    }
    void immersive() {
        getWindow().getDecorView().setSystemUiVisibility(5894 | 1024 | 512);
    }
    @Override public void onWindowFocusChanged(boolean f) { super.onWindowFocusChanged(f); if(f) immersive(); }
    @Override public void onBackPressed() { if (game != null) game.back(); else super.onBackPressed(); }

    static class Bullet {
        float x,y,vx,vy,r,damage,life;
        boolean hostile,plasma;
        Bullet(float X,float Y,float VX,float VY,float R,float D,boolean H,boolean P) {
            x=X;y=Y;vx=VX;vy=VY;r=R;damage=D;hostile=H;plasma=P;life=2.5f;
        }
    }
    static class Mob {
        float x,y,r,hp,maxHp,speed,hitTimer,shotTimer,phase;
        int kind;
        Mob(float X,float Y,int K,int wave) {
            x=X;y=Y;kind=K;
            r = K==4?42:K==2?24:K==3?19:K==1?15:19;
            maxHp = (K==4?75:K==2?11:K==3?5:K==1?3:5) * (1 + wave*.14f);
            hp=maxHp;
            speed = (K==4?53:K==2?49:K==3?66:K==1?143:83) + wave*1.1f;
            shotTimer=1.2f;
        }
    }
    static class Spark {
        float x,y,vx,vy,life,maxLife,r; int c;
        Spark(float X,float Y,float VX,float VY,float L,float R,int C) {
            x=X;y=Y;vx=VX;vy=VY;life=maxLife=L;r=R;c=C;
        }
    }
    static class Arena extends View {
        static final int MENU=0,PLAY=1,SHOP=2,END=3;
        final int cyan=Color.rgb(27,234,249),pink=Color.rgb(255,57,135),gold=Color.rgb(255,209,81);
        final int pale=Color.rgb(222,234,252),green=Color.rgb(85,238,151),muted=Color.rgb(120,142,175);
        final Paint p=new Paint(3);
        final Random rng=new Random();
        final SharedPreferences pref;
        final ArrayList<Mob> mobs=new ArrayList<>();
        final ArrayList<Bullet> bullets=new ArrayList<>();
        final ArrayList<Spark> sparks=new ArrayList<>();
        long last;
        float screenW=960,screenH=540,t=0,px=480,py=290,angle=0;
        float joyX,joyY,joyBaseX=110,joyBaseY=435;
        float targetX,targetY,shootTimer,dashTimer,dashTravel,invuln,flash,shake;
        float spawnTimer,regenDelay,shield,shopPulse,announcement;
        int left=-1,right=-1,state=MENU,shopOrigin=MENU,wave=1,spawned,toSpawn,kills,score,high,coins;
        int weapon=0,armorTier,damageTier,shieldTier,speedTier;
        boolean[] unlocked={true,false,false,false};
        boolean victory,hasAim;
        String toast="";
        float toastTimer;
        Arena(Context c) {
            super(c);
            setLayerType(View.LAYER_TYPE_SOFTWARE,null);
            pref=c.getSharedPreferences("zombie_star_v2",0);
            coins=pref.getInt("coins",0);
            high=pref.getInt("high",0);
            weapon=pref.getInt("weapon",0);
            armorTier=pref.getInt("armor",0); damageTier=pref.getInt("damage",0);
            shieldTier=pref.getInt("shield",0); speedTier=pref.getInt("speed",0);
            for(int i=1;i<4;i++) unlocked[i]=pref.getBoolean("gun"+i,false);
            if(weapon>3||!unlocked[weapon]) weapon=0;
            p.setTypeface(Typeface.create("sans-serif-condensed",Typeface.BOLD));
            setFocusable(true);
        }
        void save() {
            SharedPreferences.Editor e=pref.edit();
            e.putInt("coins",coins).putInt("high",high).putInt("weapon",weapon);
            e.putInt("armor",armorTier).putInt("damage",damageTier)
             .putInt("shield",shieldTier).putInt("speed",speedTier);
            for(int i=1;i<4;i++) e.putBoolean("gun"+i,unlocked[i]);
            e.apply();
        }
        float maxHp(){return 100+armorTier*25;}
        float maxShield(){return shieldTier*35f;}
        void newRun(){
            mobs.clear();bullets.clear();sparks.clear();
            px=480;py=300;joyX=joyY=0;left=right=-1;hasAim=false;
            wave=1;kills=0;score=0;victory=false;
            health=maxHp();shield=maxShield();
            dashTimer=shootTimer=invuln=flash=shake=0;
            state=PLAY;startWave();
            last=System.nanoTime();
        }
        float health=100;
        void startWave(){
            mobs.clear();bullets.clear();
            spawned=0;toSpawn=5+wave*2+(wave%5==0?1:0);
            spawnTimer=.7f;announcement=2.3f;
            health=Math.min(maxHp(),health+10);
            shield=Math.min(maxShield(),shield+12);
        }
        @Override protected void onDraw(Canvas real){
            long n=System.nanoTime();
            if(last==0)last=n;
            float dt=Math.min(.04f,(n-last)/1000000000f);last=n;t+=dt;
            float sx=getWidth()/960f,sy=getHeight()/540f;
            real.save();real.scale(sx,sy);
            if(state==PLAY)update(dt);
            if(toastTimer>0)toastTimer-=dt;
            shopPulse+=dt;
            real.drawColor(Color.rgb(5,9,23));
            backdrop(real);
            if(state==MENU)menu(real);
            else if(state==SHOP)shop(real);
            else {
                real.save();
                if(shake>0)real.translate((rng.nextFloat()-.5f)*shake,(rng.nextFloat()-.5f)*shake);
                world(real);
                real.restore();
                hud(real);
                if(state==END)end(real);
            }
            if(toastTimer>0){
                rect(real,329,486,631,525,12,0xdd111c32);
                txt(real,toast,480,513,20,pale,true);
            }
            real.restore();
            postInvalidateOnAnimation();
        }
        void update(float dt){
            shootTimer=Math.max(0,shootTimer-dt);dashTimer=Math.max(0,dashTimer-dt);
            invuln=Math.max(0,invuln-dt);flash=Math.max(0,flash-dt*3);shake=Math.max(0,shake-dt*35);
            regenDelay=Math.max(0,regenDelay-dt);announcement=Math.max(0,announcement-dt);
            float speed=215+speedTier*22f;
            if(dashTravel>0){
                dashTravel-=dt;px+=Math.cos(angle)*640*dt;py+=Math.sin(angle)*640*dt;
            }else{px+=joyX*speed*dt;py+=joyY*speed*dt;}
            px=clamp(px,33,927);py=clamp(py,81,503);
            Mob near=closest();
            if(right<0&&near!=null)angle=(float)Math.atan2(near.y-py,near.x-px);
            else if(hasAim)angle=(float)Math.atan2(targetY-py,targetX-px);
            if(near!=null&&shootTimer<=0){
                fire();shootTimer=weapon==1?.115f:weapon==2?.63f:weapon==3?.29f:.34f;
            }
            if(regenDelay<=0&&shield<maxShield())shield=Math.min(maxShield(),shield+7*dt);
            spawnTimer-=dt;
            if(spawned<toSpawn&&spawnTimer<=0){
                spawnMob();spawned++;
                spawnTimer=Math.max(.23f,.72f-wave*.027f);
            }
            for(int i=mobs.size()-1;i>=0;i--){
                Mob m=mobs.get(i);m.phase+=dt;m.hitTimer-=dt;m.shotTimer-=dt;
                float dx=px-m.x,dy=py-m.y,dist=(float)Math.hypot(dx,dy);
                if(dist>1){
                    float vx=dx/dist,vy=dy/dist;
                    if(m.kind==3&&dist<260){vx=-vx;vy=-vy;}
                    m.x+=vx*m.speed*dt;m.y+=vy*m.speed*dt;
                }
                if(dist<m.r+20&&m.hitTimer<=0){
                    hurt(m.kind==4?28:m.kind==2?21:12);
                    m.hitTimer=.85f;
                    if(dist>1){m.x-=dx/dist*18;m.y-=dy/dist*18;}
                }
                if((m.kind==3||m.kind==4)&&m.shotTimer<=0&&dist>40){
                    int shots=m.kind==4?8:1;
                    for(int q=0;q<shots;q++){
                        float a=m.kind==4?(float)(Math.PI*2*q/shots+t*.4f):(float)Math.atan2(dy,dx);
                        bullets.add(new Bullet(m.x,m.y,(float)Math.cos(a)*210,(float)Math.sin(a)*210,5,
                                m.kind==4?14:10,true,false));
                    }
                    m.shotTimer=m.kind==4?2.1f:1.8f;
                    emit(m.x,m.y,pink,5);
                }
            }
            for(int i=bullets.size()-1;i>=0;i--){
                Bullet b=bullets.get(i);
                b.x+=b.vx*dt;b.y+=b.vy*dt;b.life-=dt;
                if(b.life<=0||b.x<-50||b.x>1010||b.y<-50||b.y>590){bullets.remove(i);continue;}
                if(b.hostile){
                    if(Math.hypot(b.x-px,b.y-py)<b.r+15){
                        hurt(b.damage);bullets.remove(i);emit(b.x,b.y,pink,6);
                    }
                } else{
                    boolean hit=false;
                    for(int j=mobs.size()-1;j>=0;j--){
                        Mob m=mobs.get(j);
                        if(Math.hypot(b.x-m.x,b.y-m.y)<b.r+m.r){
                            m.hp-=b.damage;hit=true;
                            emit(b.x,b.y,b.plasma?cyan:gold,b.plasma?9:4);
                            if(b.plasma){
                                for(Mob other:mobs){
                                    if(other!=m&&Math.hypot(other.x-m.x,other.y-m.y)<95)other.hp-=b.damage*.42f;
                                }
                                shake=Math.max(shake,6);
                            }
                            break;
                        }
                    }
                    if(hit)bullets.remove(i);
                }
            }
            for(int i=mobs.size()-1;i>=0;i--){
                Mob m=mobs.get(i);
                if(m.hp<=0){
                    emit(m.x,m.y,m.kind==4?pink:green,m.kind==4?35:12);
                    if(m.kind==4){coins+=500;score+=2000;shake=19;}
                    else{coins+=m.kind==2?45:m.kind==1?24:30;score+=m.kind==2?200:100;}
                    kills++;if(score>high)high=score;
                    mobs.remove(i);
                    save();
                }
            }
            for(int i=sparks.size()-1;i>=0;i--){
                Spark s=sparks.get(i);
                s.x+=s.vx*dt;s.y+=s.vy*dt;s.vx*=.985f;s.vy*=.985f;s.life-=dt;
                if(s.life<=0)sparks.remove(i);
            }
            if(health<=0){health=0;victory=false;state=END;save();}
            else if(spawned>=toSpawn&&mobs.isEmpty()){
                if(wave==15){victory=true;state=END;save();}
                else{shopOrigin=2;state=SHOP;toast="WAVE CLEAR + BONUS";toastTimer=2;coins+=85+wave*15;save();}
            }
        }
        Mob closest(){
            Mob best=null;float d=Float.MAX_VALUE;
            for(Mob m:mobs){float z=(m.x-px)*(m.x-px)+(m.y-py)*(m.y-py);
                if(z<d){best=m;d=z;}}
            return best;
        }
        void fire(){
            float a=angle,damage=(weapon==0?3:weapon==1?1.65f:weapon==2?2.45f:5.5f)*(1+damageTier*.25f);
            int count=weapon==2?6:weapon==3?2:1;
            for(int i=0;i<count;i++){
                float aa=a+(count==1?0:(i-(count-1)/2f)*(weapon==2?.14f:.08f));
                float sp=weapon==3?460:670;
                bullets.add(new Bullet(px+(float)Math.cos(aa)*18,py+(float)Math.sin(aa)*18,
                        (float)Math.cos(aa)*sp,(float)Math.sin(aa)*sp,weapon==3?9:weapon==2?4:5,
                        damage,false,weapon==3));
            }
            emit(px+(float)Math.cos(a)*27,py+(float)Math.sin(a)*27,weapon==3?cyan:gold,4);
            shake=Math.max(shake,weapon==2?3.2f:1.3f);
        }
        void spawnMob(){
            int kind;
            if(wave%5==0&&spawned==0)kind=4;
            else{
                int v=rng.nextInt(100);
                kind=v<22&&wave>1?1:v<38&&wave>3?2:v<57&&wave>4?3:0;
            }
            int edge=rng.nextInt(4);
            float x=edge==0?-50:edge==1?1010:rng.nextInt(960);
            float y=edge==2?-50:edge==3?590:75+rng.nextInt(430);
            mobs.add(new Mob(x,y,kind,wave));
        }
        void hurt(float dmg){
            if(invuln>0)return;
            float effective=Math.max(3,dmg-armorTier*1.6f);
            float block=Math.min(shield,effective);
            shield-=block;health-=effective-block;
            invuln=.30f;regenDelay=4;flash=.8f;shake=12;
            emit(px,py,pink,12);
        }
        void emit(float x,float y,int c,int count){
            for(int i=0;i<count&&sparks.size()<170;i++){
                float a=rng.nextFloat()*6.283f,sp=25+rng.nextFloat()*150;
                sparks.add(new Spark(x,y,(float)Math.cos(a)*sp,(float)Math.sin(a)*sp,
                        .20f+rng.nextFloat()*.40f,2+rng.nextFloat()*4,c));
            }
        }
        void backdrop(Canvas c){
            p.setStyle(Paint.Style.FILL);
            for(int y=0;y<540;y+=40){
                int v=(int)(12+y*.013f);
                p.setColor(Color.rgb(5,9+v/3,22+v/2));
                c.drawRect(0,y,960,y+40,p);
            }
            float drift=(t*9)%55;
            p.setStrokeWidth(1);
            for(int x=0;x<=960;x+=55){
                p.setColor(0x2241d9fa);c.drawLine(x+drift,0,x+drift,540,p);
            }
            for(int y=20;y<=540;y+=55){
                p.setColor(0x2241d9fa);c.drawLine(0,y,960,y,p);
            }
            for(int i=0;i<27;i++){
                int x=(i*193+31)%962,y=(i*131+91)%530;
                float glow=(float)(.55+.4*Math.sin(t*1.5+i));
                p.setColor(i%4==0?0x5524f5fc:0x33f72e93);
                c.drawCircle(x,y,1.8f+glow*2,p);
            }
            for(int i=0;i<5;i++){
                float x=82+i*203,y=157+(i%3)*117;
                p.setColor(0x44152a4a);c.drawRoundRect(x,y,x+60,y+28,4,4,p);
                p.setColor(0x8833ddee);c.drawRect(x+5,y+5,x+40,y+7,p);
                p.setColor(0x447c62ff);c.drawRect(x+5,y+12,x+35,y+14,p);
            }
        }
        void world(Canvas c){
            for(Spark s:sparks){
                p.setStyle(Paint.Style.FILL);
                int alpha=(int)(255*clamp(s.life/s.maxLife,0,1));
                p.setColor((s.c&0x00ffffff)|(alpha<<24));
                c.drawCircle(s.x,s.y,s.r,p);
            }
            for(Bullet b:bullets){
                p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(b.r*2);
                p.setColor(b.hostile?0x55ff4480:b.plasma?0x5554ffff:0x66ffcf60);
                c.drawLine(b.x-b.vx*.023f,b.y-b.vy*.023f,b.x,b.y,p);
                p.setStyle(Paint.Style.FILL);
                p.setColor(b.hostile?pink:b.plasma?cyan:gold);
                c.drawCircle(b.x,b.y,b.r,p);
            }
            for(Mob m:mobs)drawMob(c,m);
            c.save();c.translate(px,py);c.rotate((float)Math.toDegrees(angle));
            p.setStyle(Paint.Style.FILL);
            p.setColor(0x4422ffff);c.drawCircle(0,0,31+2*(float)Math.sin(t*5),p);
            p.setColor(0xaa193c59);c.drawCircle(0,0,21,p);
            p.setColor(invuln>0&&((int)(t*20)%2==0)?Color.WHITE:cyan);
            c.drawRoundRect(-13,-17,11,17,8,8,p);
            p.setColor(0xff071328);c.drawRoundRect(-7,-12,8,12,5,5,p);
            p.setColor(gold);c.drawRoundRect(-3,-10,10,-5,2,2,p);
            p.setColor(0xffbceeff);c.drawRoundRect(8,-5,29,5,3,3,p);
            p.setColor(pink);c.drawCircle(-13,-13,5,p);c.drawCircle(-13,13,5,p);
            c.restore();
            if(flash>0){
                p.setColor(Color.argb((int)(flash*52),255,30,80));
                c.drawRect(0,0,960,540,p);
            }
        }
        void drawMob(Canvas c,Mob m){
            int col=m.kind==4?pink:m.kind==2?0xffffa954:m.kind==3?0xffad72fa:m.kind==1?0xffa6ff58:green;
            c.save();c.translate(m.x,m.y);
            float a=(float)Math.atan2(py-m.y,px-m.x);
            c.rotate((float)Math.toDegrees(a));
            p.setColor((col&0x00ffffff)|0x33000000);c.drawCircle(0,0,m.r+11,p);
            p.setColor(0xff102a30);c.drawCircle(0,0,m.r,p);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(m.kind==4?6:3);
            p.setColor(col);c.drawCircle(0,0,m.r-3,p);
            p.setStyle(Paint.Style.FILL);
            p.setColor(col);
            c.drawRoundRect(-m.r*.6f,-m.r*.47f,m.r*.55f,m.r*.47f,5,5,p);
            p.setColor(0xff071823);
            c.drawRect(m.r*.18f,-m.r*.31f,m.r*.64f,m.r*.31f,p);
            p.setColor(pink);
            c.drawCircle(m.r*.53f,-m.r*.17f,m.kind==4?5:3,p);
            c.drawCircle(m.r*.53f,m.r*.17f,m.kind==4?5:3,p);
            if(m.kind==4){
                p.setColor(gold);
                c.drawCircle(-19,-26,7,p);c.drawCircle(-19,26,7,p);
            }
            c.restore();
            if(m.hp<m.maxHp||m.kind==4){
                p.setColor(0xff26303f);c.drawRoundRect(m.x-m.r,m.y-m.r-13,m.x+m.r,m.y-m.r-8,2,2,p);
                p.setColor(col);c.drawRoundRect(m.x-m.r,m.y-m.r-13,m.x-m.r+2*m.r*Math.max(0,m.hp/m.maxHp),m.y-m.r-8,2,2,p);
            }
        }
        void hud(Canvas c){
            rect(c,14,12,282,69,13,0xcc111b36);
            txt(c,"HP",28,33,15,pale,false);
            bar(c,61,22,105,11,health/maxHp(),pink);
            txt(c,"SH",28,55,15,pale,false);
            bar(c,61,45,105,11,shield/Math.max(1,maxShield()),cyan);
            txt(c,weaponName(),181,45,15,gold,false);
            rect(c,296,12,681,64,12,0xcc111b36);
            txt(c,"WAVE "+wave+" / 15",315,34,20,cyan,false);
            txt(c,"KILLS "+kills,458,34,18,pale,false);
            txt(c,"SCORE "+score,554,53,15,muted,false);
            txt(c,"COINS",702,35,17,gold,false);
            txt(c,String.valueOf(coins),775,35,20,pale,false);
            rect(c,830,13,944,65,12,0xff162e40);
            txt(c,"STORE",887,45,22,cyan,true);
            if(announcement>0){
                txt(c,wave%5==0?"BOSS ALERT!":"WAVE "+wave+" - SURVIVE",480,125,32,wave%5==0?pink:gold,true);
            }
            // Joystick
            strokeCircle(c,left<0?105:joyBaseX,left<0?439:joyBaseY,70,0x7749effc,4);
            p.setColor(0x6649effc);
            c.drawCircle((left<0?105:joyBaseX)+joyX*49,(left<0?439:joyBaseY)+joyY*49,26,p);
            // Dash
            strokeCircle(c,864,437,61,dashTimer>0?0x996b7484:pink,6);
            txt(c,dashTimer>0?String.format(java.util.Locale.US,"%.1f",dashTimer):"DASH",864,443,19,pale,true);
            txt(c,"AUTO FIRE",747,509,15,green,true);
        }
        void menu(Canvas c){
            rect(c,178,63,782,496,26,0xee0b162c);
            stroke(c,178,63,782,496,26,0x7755ecff,2);
            txt(c,"ZOMBIE",480,183,84,cyan,true);
            txt(c,"STAR",480,255,84,pink,true);
            txt(c,"NIGHT RAID",480,297,21,gold,true);
            txt(c,"CREATED BY AMIR ALI",480,333,18,muted,true);
            button(c,308,354,652,417,"START SURVIVAL",cyan);
            button(c,308,430,652,483,"WEAPON STORE  -  "+coins+" C",gold);
            txt(c,"15 WAVES  /  4 WEAPONS  /  5 UPGRADES",480,519,14,muted,true);
        }
        void end(Canvas c){
            rect(c,0,0,960,540,0,0xdd030813);
            rect(c,213,90,747,455,25,0xff101a2c);
            stroke(c,213,90,747,455,25,victory?cyan:pink,3);
            txt(c,victory?"NIGHT RAID CLEARED":"YOU WERE OVERRUN",480,172,victory?38:34,victory?cyan:pink,true);
            txt(c,"WAVE "+wave+"       SCORE "+score,480,227,26,pale,true);
            txt(c,"HIGH SCORE "+high+"    /    COINS "+coins,480,265,20,gold,true);
            txt(c,"PERMANENT UPGRADES ARE SAVED",480,308,16,muted,true);
            button(c,276,337,684,388,"TRY AGAIN",cyan);
            button(c,276,400,684,448,"OPEN STORE",gold);
        }
        void shop(Canvas c){
            rect(c,0,0,960,540,0,0xf0050918);
            rect(c,14,10,946,528,18,0xff0b172b);
            stroke(c,14,10,946,528,18,0x6653edfc,2);
            txt(c,"ARMORY  /  BLACK MARKET",31,47,32,cyan,false);
            txt(c,"COINS  "+coins,733,45,24,gold,false);
            txt(c,shopOrigin==2?"WAVE "+wave+" CLEARED - PREPARE FOR NEXT RAID":
                    "BUY ONCE, KEEP YOUR UPGRADES FOREVER",34,95,20,muted,false);
            String[] names={"PISTOL","SMG RAPID","SHOTGUN","PLASMA","ARMOR","DAMAGE","SHIELD","SPEED"};
            String[] notes={"Starter","Fast fire","Wide blast","Energy splash",
                    "+25 MAX HP","+25% POWER","+35 SHIELD","+22 MOVE"};
            int[] colors={cyan,green,gold,pink,0xffa18dff,pink,cyan,green};
            for(int i=0;i<8;i++){
                int col=i%4,row=i/4;
                float x=30+col*231,y=113+row*169;
                rect(c,x,y,x+211,y+158,13,0xff16233a);
                stroke(c,x,y,x+211,y+158,13,i==weapon&&i<4?gold:0x88466b87,i==weapon&&i<4?3:1);
                txt(c,names[i],x+13,y+29,24,colors[i],false);
                txt(c,notes[i],x+13,y+54,15,pale,false);
                if(i<4){
                    txt(c,i==0?"FREE":i==1?"650 C":i==2?"1400 C":"2700 C",x+13,y+86,21,gold,false);
                    txt(c,i==weapon?"EQUIPPED":unlocked[i]?"TAP TO EQUIP":"TAP TO BUY",
                            x+13,y+132,17,i==weapon?green:cyan,false);
                } else{
                    int level=tier(i);
                    txt(c,"LEVEL "+level+" / 5",x+13,y+86,21,gold,false);
                    txt(c,level>=5?"MAXED OUT":cost(i)+" C  -  UPGRADE",x+13,y+132,17,level>=5?green:cyan,false);
                }
            }
            button(c,30,462,309,512,"MEDKIT  +60 HP  /  180 C",pink);
            button(c,633,462,932,512,shopOrigin==2?"NEXT WAVE >>":"BACK TO GAME",cyan);
            txt(c,shopOrigin==MENU?"SHOP BEFORE YOUR FIRST RUN":shopOrigin==2?"BONUS COINS ALREADY ADDED":"PAUSED",469,494,16,muted,true);
        }
        int tier(int item){
            if(item==4)return armorTier;
            if(item==5)return damageTier;
            if(item==6)return shieldTier;
            return speedTier;
        }
        int cost(int item){
            int l=tier(item);
            return (item==4?430:item==5?550:item==6?490:420)+l*260;
        }
        void buy(int id){
            if(id<4){
                int price=id==0?0:id==1?650:id==2?1400:2700;
                if(unlocked[id]){weapon=id;message("EQUIPPED "+new String[]{"PISTOL","SMG","SHOTGUN","PLASMA"}[id]);}
                else if(coins>=price){coins-=price;unlocked[id]=true;weapon=id;message("NEW WEAPON UNLOCKED!");}
                else message("NOT ENOUGH COINS");
            } else{
                int lv=tier(id),price=cost(id);
                if(lv>=5){message("ALREADY MAX LEVEL");return;}
                if(coins<price){message("NOT ENOUGH COINS");return;}
                coins-=price;
                if(id==4){armorTier++;health=Math.min(maxHp(),health+25);}
                else if(id==5)damageTier++;
                else if(id==6){shieldTier++;shield+=35;}
                else speedTier++;
                message("UPGRADE PURCHASED");
            }
            save();
        }
        void message(String text){toast=text;toastTimer=1.65f;}
        String weaponName(){return new String[]{"PISTOL","SMG","SHOTGUN","PLASMA"}[weapon];}
        void rect(Canvas c,float a,float b,float d,float e,float r,int color){
            p.setStyle(Paint.Style.FILL);p.setColor(color);c.drawRoundRect(a,b,d,e,r,r,p);
        }
        void stroke(Canvas c,float a,float b,float d,float e,float r,int color,int w){
            p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(w);c.drawRoundRect(a,b,d,e,r,r,p);p.setStyle(Paint.Style.FILL);
        }
        void strokeCircle(Canvas c,float x,float y,float r,int col,int w){
            p.setColor(col);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(w);c.drawCircle(x,y,r,p);p.setStyle(Paint.Style.FILL);
        }
        void txt(Canvas c,String s,float x,float y,int size,int color,boolean center){
            p.setStyle(Paint.Style.FILL);p.setColor(color);
            p.setTypeface(Typeface.create("sans-serif-condensed",Typeface.BOLD));
            p.setTextAlign(center?Paint.Align.CENTER:Paint.Align.LEFT);
            p.setTextSize(size);c.drawText(s,x,y,p);
        }
        void button(Canvas c,float x,float y,float x2,float y2,String s,int col){
            rect(c,x,y,x2,y2,13,0xff19334c);
            stroke(c,x,y,x2,y2,13,col,2);
            txt(c,s,(x+x2)/2,(y+y2)/2+8,22,col,true);
        }
        void bar(Canvas c,float x,float y,float w,float h,float f,int color){
            rect(c,x,y,x+w,y+h,4,0xff394355);
            if(f>0)rect(c,x,y,x+w*clamp(f,0,1),y+h,4,color);
        }
        float clamp(float x,float mn,float mx){return Math.max(mn,Math.min(mx,x));}
        void dash(){
            if(dashTimer>0||state!=PLAY)return;
            float d=(float)Math.hypot(joyX,joyY);
            if(d>.1)angle=(float)Math.atan2(joyY,joyX);
            dashTravel=.22f;dashTimer=2.35f;invuln=.28f;
            emit(px,py,cyan,18);shake=8;
        }
        void back(){
            if(state==PLAY){shopOrigin=PLAY;state=SHOP;}
            else if(state==SHOP){closeShop();}
            else if(state==END)state=MENU;
            else if(state==MENU) ((Activity)getContext()).finish();
        }
        void closeShop(){
            if(shopOrigin==2){wave++;startWave();state=PLAY;}
            else if(shopOrigin==MENU)state=MENU;
            else state=PLAY;
            last=System.nanoTime();
        }
        void tap(float x,float y){
            if(state==MENU){
                if(y>=352&&y<=422)newRun();
                else if(y>=426&&y<=498){shopOrigin=MENU;state=SHOP;}
            }else if(state==SHOP){
                if(y>=113&&y<440){
                    int col=(int)((x-30)/231),row=(int)((y-113)/169);
                    if(x>=30&&col>=0&&col<4&&row>=0&&row<2&&
                       x<=30+col*231+211&&y<=113+row*169+158)
                        buy(row*4+col);
                } else if(y>=458&&y<=518){
                    if(x<313){
                        if(shopOrigin!=MENU&&health<maxHp()&&coins>=180){
                            coins-=180;health=Math.min(maxHp(),health+60);save();message("HEALTH RESTORED");
                        }else message("MEDKIT NOT AVAILABLE");
                    }else if(x>630)closeShop();
                }
            }else if(state==END){
                if(y>=330&&y<=394)newRun();
                else if(y>=396&&y<=464){shopOrigin=MENU;state=SHOP;}
            }else if(state==PLAY){
                if(x>=824&&y<75){shopOrigin=PLAY;state=SHOP;left=right=-1;joyX=joyY=0;}
                else if(Math.hypot(x-864,y-437)<75)dash();
            }
        }
        @Override public boolean onTouchEvent(MotionEvent e){
            int action=e.getActionMasked(),index=e.getActionIndex();
            if(action==MotionEvent.ACTION_DOWN||action==MotionEvent.ACTION_POINTER_DOWN){
                int id=e.getPointerId(index);
                float x=e.getX(index)*960/Math.max(1,getWidth());
                float y=e.getY(index)*540/Math.max(1,getHeight());
                if(state!=PLAY){tap(x,y);return true;}
                if((x>824&&y<75)||Math.hypot(x-864,y-437)<75){tap(x,y);return true;}
                if(x<390&&y>185&&left==-1){
                    left=id;joyBaseX=x;joyBaseY=y;joyX=joyY=0;
                }else if(right==-1){
                    right=id;targetX=x;targetY=y;hasAim=true;
                }
            } else if(action==MotionEvent.ACTION_MOVE){
                for(int i=0;i<e.getPointerCount();i++){
                    int id=e.getPointerId(i);
                    float x=e.getX(i)*960/Math.max(1,getWidth());
                    float y=e.getY(i)*540/Math.max(1,getHeight());
                    if(id==left){
                        float dx=x-joyBaseX,dy=y-joyBaseY;
                        float len=(float)Math.hypot(dx,dy);
                        if(len>75){dx*=75/len;dy*=75/len;}
                        joyX=dx/75;joyY=dy/75;
                    }else if(id==right){targetX=x;targetY=y;hasAim=true;}
                }
            } else if(action==MotionEvent.ACTION_UP||
                    action==MotionEvent.ACTION_POINTER_UP||action==MotionEvent.ACTION_CANCEL){
                int id=e.getPointerId(index);
                if(id==left){left=-1;joyX=joyY=0;}
                if(id==right){right=-1;hasAim=false;}
                if(action==MotionEvent.ACTION_CANCEL){left=right=-1;joyX=joyY=0;hasAim=false;}
            }
            return true;
        }
    }
}
