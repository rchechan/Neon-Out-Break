package com.amirali.neonoutbreak;

import android.app.Activity;
import android.content.Context;
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
import java.util.Iterator;
import java.util.Random;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        hideSystemUi();
        setContentView(new GameView(this));
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemUi();
    }

    private void hideSystemUi() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    static class Bullet {
        float x, y, vx, vy;
        Bullet(float x, float y, float vx, float vy) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy;
        }
    }

    static class Enemy {
        float x, y, speed, radius, hitCooldown;
        int hp;
        boolean boss;
        Enemy(float x, float y, float speed, float radius, int hp, boolean boss) {
            this.x = x; this.y = y; this.speed = speed; this.radius = radius;
            this.hp = hp; this.boss = boss;
        }
    }

    static class GameView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Random random = new Random();
        private final ArrayList<Bullet> bullets = new ArrayList<>();
        private final ArrayList<Enemy> enemies = new ArrayList<>();

        private long lastNanos;
        private float width, height;
        private float playerX, playerY;
        private float joyBaseX, joyBaseY, joyX, joyY;
        private float aimX, aimY;
        private float dashVX, dashVY;
        private float fireCooldown, dashCooldown, dashTime, spawnTimer, waveBanner;
        private int leftPointer = -1, rightPointer = -1;
        private int health = 100, score = 0, wave = 1;
        private int toSpawn, spawned;
        private boolean firing, started, gameOver;

        private final int CYAN = Color.rgb(0, 238, 255);
        private final int PINK = Color.rgb(255, 40, 160);
        private final int PURPLE = Color.rgb(160, 70, 255);

        GameView(Context context) {
            super(context);
            setFocusable(true);
            paint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            width = w; height = h;
            playerX = width * 0.5f;
            playerY = height * 0.5f;
            aimX = width;
            aimY = height * 0.5f;
            joyBaseX = 130;
            joyBaseY = height - 130;
        }

        private void resetGame() {
            bullets.clear();
            enemies.clear();
            health = 100;
            score = 0;
            wave = 1;
            playerX = width * 0.5f;
            playerY = height * 0.5f;
            fireCooldown = 0;
            dashCooldown = 0;
            dashTime = 0;
            started = true;
            gameOver = false;
            leftPointer = rightPointer = -1;
            joyX = joyY = 0;
            firing = false;
            setupWave();
            lastNanos = System.nanoTime();
        }

        private void setupWave() {
            spawned = 0;
            toSpawn = (wave % 5 == 0) ? 1 : 5 + wave * 2;
            spawnTimer = 0.45f;
            waveBanner = 1.5f;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            long now = System.nanoTime();
            if (lastNanos == 0) lastNanos = now;
            float dt = Math.min(0.033f, (now - lastNanos) / 1_000_000_000f);
            lastNanos = now;

            if (started && !gameOver) update(dt);
            drawScene(canvas);
            postInvalidateOnAnimation();
        }

        private void update(float dt) {
            fireCooldown = Math.max(0, fireCooldown - dt);
            dashCooldown = Math.max(0, dashCooldown - dt);
            waveBanner = Math.max(0, waveBanner - dt);

            if (dashTime > 0) {
                dashTime -= dt;
                playerX += dashVX * dt;
                playerY += dashVY * dt;
            } else {
                float speed = 280f;
                playerX += joyX * speed * dt;
                playerY += joyY * speed * dt;
            }
            playerX = clamp(playerX, 28, width - 28);
            playerY = clamp(playerY, 28, height - 28);

            if (firing && fireCooldown <= 0) {
                shoot();
                fireCooldown = 0.14f;
            }

            spawnTimer -= dt;
            if (spawned < toSpawn && spawnTimer <= 0) {
                spawnEnemy();
                spawned++;
                spawnTimer = (wave % 5 == 0) ? 1.2f : Math.max(0.20f, 0.65f - wave * 0.025f);
            }

            Iterator<Bullet> bit = bullets.iterator();
            while (bit.hasNext()) {
                Bullet b = bit.next();
                b.x += b.vx * dt;
                b.y += b.vy * dt;
                if (b.x < -30 || b.x > width + 30 || b.y < -30 || b.y > height + 30) {
                    bit.remove();
                    continue;
                }
                boolean hit = false;
                Iterator<Enemy> eit = enemies.iterator();
                while (eit.hasNext()) {
                    Enemy e = eit.next();
                    float dx = b.x - e.x, dy = b.y - e.y;
                    if (dx * dx + dy * dy < (e.radius + 7) * (e.radius + 7)) {
                        e.hp--;
                        hit = true;
                        if (e.hp <= 0) {
                            score += e.boss ? 1000 : 100;
                            eit.remove();
                        }
                        break;
                    }
                }
                if (hit) bit.remove();
            }

            Iterator<Enemy> eit = enemies.iterator();
            while (eit.hasNext()) {
                Enemy e = eit.next();
                float dx = playerX - e.x, dy = playerY - e.y;
                float len = (float)Math.sqrt(dx * dx + dy * dy);
                if (len > 0.001f) {
                    e.x += dx / len * e.speed * dt;
                    e.y += dy / len * e.speed * dt;
                }
                e.hitCooldown = Math.max(0, e.hitCooldown - dt);
                float touch = e.radius + 24;
                if (len < touch && e.hitCooldown <= 0) {
                    if (dashTime <= 0) {
                        health -= e.boss ? 24 : 14;
                        e.hitCooldown = 0.65f;
                    }
                    float nx = len > 0 ? dx / len : 1;
                    float ny = len > 0 ? dy / len : 0;
                    e.x -= nx * 38;
                    e.y -= ny * 38;
                }
            }

            if (health <= 0) {
                health = 0;
                gameOver = true;
                firing = false;
            }

            if (spawned >= toSpawn && enemies.isEmpty()) {
                wave++;
                setupWave();
            }
        }

        private void shoot() {
            float dx = aimX - playerX, dy = aimY - playerY;
            float len = (float)Math.sqrt(dx * dx + dy * dy);
            if (len < 1) { dx = 1; dy = 0; len = 1; }
            float speed = 720f;
            bullets.add(new Bullet(playerX, playerY, dx / len * speed, dy / len * speed));
        }

        private void spawnEnemy() {
            boolean boss = wave % 5 == 0;
            float x, y;
            int edge = random.nextInt(4);
            if (edge == 0) { x = -60; y = random.nextFloat() * height; }
            else if (edge == 1) { x = width + 60; y = random.nextFloat() * height; }
            else if (edge == 2) { x = random.nextFloat() * width; y = -60; }
            else { x = random.nextFloat() * width; y = height + 60; }

            if (boss) {
                enemies.add(new Enemy(x, y, 72 + wave * 2, 58, 30 + wave * 2, true));
            } else {
                enemies.add(new Enemy(x, y, 92 + wave * 5, 20 + random.nextFloat() * 7,
                        1 + wave / 4, false));
            }
        }

        private void triggerDash() {
            if (dashCooldown > 0 || !started || gameOver) return;
            float dx = joyX, dy = joyY;
            float len = (float)Math.sqrt(dx * dx + dy * dy);
            if (len < 0.15f) {
                dx = aimX - playerX;
                dy = aimY - playerY;
                len = (float)Math.sqrt(dx * dx + dy * dy);
            }
            if (len < 0.01f) { dx = 1; dy = 0; len = 1; }
            dashVX = dx / len * 1050f;
            dashVY = dy / len * 1050f;
            dashTime = 0.18f;
            dashCooldown = 2.0f;
        }

        private void drawScene(Canvas c) {
            c.drawColor(Color.rgb(3, 5, 14));
            drawGrid(c);

            if (!started) {
                drawStart(c);
                return;
            }

            for (Bullet b : bullets) {
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.argb(65, 0, 238, 255));
                c.drawCircle(b.x, b.y, 14, paint);
                paint.setColor(CYAN);
                c.drawCircle(b.x, b.y, 5, paint);
            }

            for (Enemy e : enemies) {
                paint.setStyle(Paint.Style.FILL);
                int color = e.boss ? PINK : PURPLE;
                paint.setColor(Color.argb(55, Color.red(color), Color.green(color), Color.blue(color)));
                c.drawCircle(e.x, e.y, e.radius + 13, paint);
                paint.setColor(color);
                c.drawCircle(e.x, e.y, e.radius, paint);
                paint.setColor(Color.rgb(10, 8, 22));
                c.drawCircle(e.x, e.y, e.radius * 0.58f, paint);
                if (e.boss) {
                    paint.setColor(Color.WHITE);
                    paint.setTextAlign(Paint.Align.CENTER);
                    paint.setTextSize(20);
                    c.drawText("BOSS " + e.hp, e.x, e.y + 7, paint);
                }
            }

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(55, 0, 238, 255));
            c.drawCircle(playerX, playerY, 38, paint);
            paint.setColor(dashTime > 0 ? Color.WHITE : CYAN);
            c.drawCircle(playerX, playerY, 23, paint);
            paint.setColor(PINK);
            c.drawCircle(playerX, playerY, 9, paint);

            drawHud(c);
            drawControls(c);

            if (waveBanner > 0) {
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setTextSize(wave % 5 == 0 ? 48 : 42);
                paint.setColor(wave % 5 == 0 ? PINK : CYAN);
                c.drawText(wave % 5 == 0 ? "BOSS WAVE " + wave : "WAVE " + wave,
                        width * 0.5f, 90, paint);
            }

            if (gameOver) drawGameOver(c);
        }

        private void drawGrid(Canvas c) {
            paint.setStrokeWidth(1);
            paint.setColor(Color.rgb(12, 30, 48));
            float step = 64;
            for (float x = 0; x < width; x += step) c.drawLine(x, 0, x, height, paint);
            for (float y = 0; y < height; y += step) c.drawLine(0, y, width, y, paint);
        }

        private void drawHud(Canvas c) {
            paint.setTextAlign(Paint.Align.LEFT);
            paint.setTextSize(26);
            paint.setColor(Color.WHITE);
            c.drawText("HP " + health, 28, 40, paint);
            c.drawText("SCORE " + score, 28, 74, paint);
            c.drawText("WAVE " + wave, 28, 108, paint);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(32, 38, 55));
            c.drawRoundRect(28, 122, 248, 138, 8, 8, paint);
            paint.setColor(health > 35 ? CYAN : PINK);
            c.drawRoundRect(28, 122, 28 + 220 * (health / 100f), 138, 8, 8, paint);
        }

        private void drawControls(Canvas c) {
            float bx = (leftPointer >= 0) ? joyBaseX : 130;
            float by = (leftPointer >= 0) ? joyBaseY : height - 130;
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4);
            paint.setColor(Color.argb(120, 0, 238, 255));
            c.drawCircle(bx, by, 75, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(115, 0, 238, 255));
            c.drawCircle(bx + joyX * 55, by + joyY * 55, 27, paint);

            float dx = width - 115, dy = height - 115;
            float ready = 1f - Math.min(1f, dashCooldown / 2f);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(7);
            paint.setColor(dashCooldown <= 0 ? PINK : Color.rgb(90, 60, 90));
            c.drawCircle(dx, dy, 56, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(dashCooldown <= 0 ? Color.argb(70, 255, 40, 160) : Color.argb(60, 100, 80, 100));
            c.drawCircle(dx, dy, 48 * Math.max(0.15f, ready), paint);
            paint.setColor(Color.WHITE);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(20);
            c.drawText("DASH", dx, dy + 7, paint);

            paint.setTextAlign(Paint.Align.RIGHT);
            paint.setTextSize(18);
            paint.setColor(Color.argb(150, 255, 255, 255));
            c.drawText("Touch right side to FIRE", width - 24, 34, paint);
        }

        private void drawStart(Canvas c) {
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(CYAN);
            paint.setTextSize(Math.min(82, width * 0.075f));
            c.drawText("NEON OUTBREAK", width * 0.5f, height * 0.42f, paint);
            paint.setColor(PINK);
            paint.setTextSize(Math.min(44, width * 0.045f));
            c.drawText("AMIR ALI", width * 0.5f, height * 0.53f, paint);
            paint.setColor(Color.WHITE);
            paint.setTextSize(24);
            c.drawText("TAP TO START", width * 0.5f, height * 0.68f, paint);
            paint.setTextSize(17);
            paint.setColor(Color.LTGRAY);
            c.drawText("LEFT: MOVE   •   RIGHT: AIM & FIRE   •   DASH BUTTON", width * 0.5f, height * 0.76f, paint);
        }

        private void drawGameOver(Canvas c) {
            paint.setColor(Color.argb(205, 2, 3, 12));
            c.drawRect(0, 0, width, height, paint);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setColor(PINK);
            paint.setTextSize(64);
            c.drawText("OUTBREAK OVER", width * 0.5f, height * 0.42f, paint);
            paint.setColor(Color.WHITE);
            paint.setTextSize(30);
            c.drawText("SCORE " + score + "   •   WAVE " + wave, width * 0.5f, height * 0.54f, paint);
            paint.setColor(CYAN);
            paint.setTextSize(22);
            c.drawText("TAP TO RESTART", width * 0.5f, height * 0.66f, paint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            int action = event.getActionMasked();
            int index = event.getActionIndex();

            if ((action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN)
                    && (!started || gameOver)) {
                resetGame();
                return true;
            }

            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
                int id = event.getPointerId(index);
                float x = event.getX(index), y = event.getY(index);
                float ddx = x - (width - 115), ddy = y - (height - 115);
                if (ddx * ddx + ddy * ddy < 80 * 80) {
                    triggerDash();
                    return true;
                }
                if (x < width * 0.46f && leftPointer == -1) {
                    leftPointer = id;
                    joyBaseX = x;
                    joyBaseY = y;
                    joyX = joyY = 0;
                } else if (rightPointer == -1) {
                    rightPointer = id;
                    aimX = x;
                    aimY = y;
                    firing = true;
                }
            } else if (action == MotionEvent.ACTION_MOVE) {
                for (int i = 0; i < event.getPointerCount(); i++) {
                    int id = event.getPointerId(i);
                    float x = event.getX(i), y = event.getY(i);
                    if (id == leftPointer) {
                        float dx = x - joyBaseX, dy = y - joyBaseY;
                        float len = (float)Math.sqrt(dx * dx + dy * dy);
                        if (len > 75) { dx *= 75 / len; dy *= 75 / len; }
                        joyX = dx / 75f;
                        joyY = dy / 75f;
                    }
                    if (id == rightPointer) {
                        aimX = x;
                        aimY = y;
                    }
                }
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_CANCEL) {
                int id = event.getPointerId(index);
                if (id == leftPointer) {
                    leftPointer = -1;
                    joyX = joyY = 0;
                }
                if (id == rightPointer) {
                    rightPointer = -1;
                    firing = false;
                }
                if (action == MotionEvent.ACTION_CANCEL) {
                    leftPointer = rightPointer = -1;
                    joyX = joyY = 0;
                    firing = false;
                }
            }
            return true;
        }

        private float clamp(float value, float min, float max) {
            return Math.max(min, Math.min(max, value));
        }
    }
}
