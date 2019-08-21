package de.androidnewcomer.mueckenmassaker;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Date;
import java.util.Random;


public class GameActivity extends Activity implements View.OnClickListener, Runnable{

    private static final long HOECHSTALTER_MS = 2000;
    public static final int DELAY_MILLIS = 100;
    public static final int ZEITSCHEIBEN =600;
    private static final String ELEFANT = "ELEFANT";
    private static final int MUECKEN_BILDER[][] = {
            {R.drawable.muecke_nw, R.drawable.muecke_n, R.drawable.muecke_no},
            {R.drawable.muecke_w,  R.drawable.muecke,   R.drawable.muecke_o},
            {R.drawable.muecke_sw, R.drawable.muecke_s, R.drawable.muecke_so}};
    private int punkte;
    private int runde;
    private int gefangeneMuecken;
    private int zeit;
    private float massstab;
    private int muecken;
    private Random zufallsgenerator = new Random();
    private ViewGroup spielbereich;
    private boolean spielLaeuft;
    private Handler handler = new Handler();
    private MediaPlayer mp;
    private MediaPlayer slapp;
    private int schwierigkeitsgrad;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game);
        massstab = getResources().getDisplayMetrics().density;
        spielbereich = findViewById(R.id.spielbereich);
        mp = MediaPlayer.create(this,R.raw.summen);
        slapp = MediaPlayer.create(this,R.raw.slap);
        schwierigkeitsgrad = getIntent().getIntExtra("schwierigkeitsgrad",0);
        spielStarten();
    }


    // Game start
    private void spielStarten() {
        spielLaeuft = true;
        runde = 0;
        punkte = 0;
        starteRunde();
    }

    private void bildschirmAktualisieren() {
        TextView tvPunkte = findViewById(R.id.points);
        tvPunkte.setText(Integer.toString(punkte));
        TextView tvRunde = findViewById(R.id.round);
        tvRunde.setText(Integer.toString(runde));
        TextView tvTreffer =  findViewById(R.id.hits);
        tvTreffer.setText(Integer.toString(gefangeneMuecken));
        TextView tvZeit =  findViewById(R.id.time);
        tvZeit.setText(Integer.toString(zeit/(1000/DELAY_MILLIS)));
        FrameLayout flTreffer = findViewById(R.id.bar_hits);
        FrameLayout flZeit = findViewById(R.id.bar_time);
        ViewGroup.LayoutParams lpTreffer = flTreffer.getLayoutParams();
        lpTreffer.width = Math.round( massstab * 300 * // 320 * 300 * (0,10)/10
                Math.min( gefangeneMuecken,muecken)/muecken);
        ViewGroup.LayoutParams lpZeit = flZeit.getLayoutParams();
        lpZeit.width = Math.round(massstab*zeit*300/ ZEITSCHEIBEN);

    }

    private void zeitHerunterzaehlen(){
        zeit = zeit-1;
        if(zeit % (1000/DELAY_MILLIS) ==0) {
            float zufallszahl = zufallsgenerator.nextFloat();
            double wahrscheinlichkeit = muecken * 1.5;
            if (wahrscheinlichkeit > 1) {
                eineMueckeAnzeigen();
                if (zufallszahl < wahrscheinlichkeit - 1) {
                    eineMueckeAnzeigen();
                }
            } else {
                if (zufallszahl < wahrscheinlichkeit) {
                    eineMueckeAnzeigen();
                }
            }
        }
        mueckenVerschwinden();
        mueckenBewegen();
        bildschirmAktualisieren();
        if(!pruefeSpielende()) {
            if(!pruefeRundenende()) {
                handler.postDelayed(this, DELAY_MILLIS);
            }
        }
    }

    private boolean pruefeRundenende() {
        if(gefangeneMuecken >= muecken) {
            starteRunde();
            return true;
        }
        return false;
    }

    /**
     * add round +1
     * runde * muecke um es jede runde schwieriger zu machen
     * Zeit
     * verzögerung für 1 sec
     */
    private void starteRunde() {
        runde = runde +1;
        muecken = runde * 10 + (schwierigkeitsgrad*10);
        gefangeneMuecken = 0;
        zeit = ZEITSCHEIBEN;
        bildschirmAktualisieren();
        handler.postDelayed(this, 1000);
       int id = getResources().getIdentifier("hintergrund"+ Integer.toString(runde),"drawable",this.getPackageName());
        if(id>0){
            LinearLayout l = findViewById(R.id.hintergrund);
            l.setBackgroundResource(id);
        }
    }


    private boolean pruefeSpielende() {

        if(zeit == 0 && gefangeneMuecken < muecken) {
            gameOver();
            return true;
        }
        return false;
    }

    private void gameOver() {
        setResult(punkte);

        final Dialog dialog = new Dialog(this, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);

        dialog.setContentView(R.layout.gameover);
        FrameLayout over = dialog.findViewById(R.id.over);
        over.setOnClickListener((v)->{
            dialog.dismiss();
            windowClose();
        });
        dialog.show();

        spielLaeuft = false;
    }

    private void windowClose() {
        this.finish();
    }

    private void mueckenBewegen() {
        int nummer=0;
        while(nummer < spielbereich.getChildCount()) {
            ImageView muecke = (ImageView) spielbereich.getChildAt(nummer);
            int vx = (Integer) muecke.getTag(R.id.vx);
            int vy = (Integer) muecke.getTag(R.id.vy);
            //Bewegung
            FrameLayout.LayoutParams params = (android.widget.FrameLayout.LayoutParams) muecke.getLayoutParams();
            params.leftMargin += vx*runde;
            params.topMargin += vy*runde;
            muecke.setLayoutParams(params);
            nummer++;
        }
    }

    private void mueckenVerschwinden() {
        int nummer=0;
        while(nummer < spielbereich.getChildCount()) {
            ImageView muecke = (ImageView) spielbereich.getChildAt(nummer);
            Date geburtsdatum = (Date) muecke.getTag(R.id.geburtsdatum);
            long alter = (new Date()).getTime() - geburtsdatum.getTime();
            if(alter > HOECHSTALTER_MS) {
                spielbereich.removeView(muecke);
            } else {
                nummer++;
            }
        }
    }



    private void eineMueckeAnzeigen() {
        int breite = spielbereich.getWidth();
        int hoehe  = spielbereich.getHeight();
        int muecke_breite = Math.round(massstab*50);
        int muecke_hoehe = Math.round(massstab*42);
        int links = zufallsgenerator.nextInt(breite - muecke_breite );
        int oben = zufallsgenerator.nextInt(hoehe - muecke_hoehe);
        ImageView muecke = new ImageView(this);

        // Mücke erzeugen
        muecke.setOnClickListener(this);
        muecke.setTag(R.id.geburtsdatum, new Date());

        // Bewegungsvektor erzeugen
        int vx;
        int vy;
        double elefantSpawnChance;
        do {
            vx = zufallsgenerator.nextInt(3)-1;
            vy = zufallsgenerator.nextInt(3)-1;
        } while(vx==0 && vy==0);

        if(schwierigkeitsgrad==0){
            elefantSpawnChance = 0.05;
        }else if(schwierigkeitsgrad ==1){
            elefantSpawnChance = 0.08;
        }else{
            elefantSpawnChance = 0.10;
        }
        muecke.setTag(R.id.vx, new Integer(vx));
        muecke.setTag(R.id.vy, new Integer(vy));

        if(zufallsgenerator.nextFloat()<elefantSpawnChance){
            muecke.setImageResource(R.drawable.elefant);
            muecke.setTag(R.id.tier,ELEFANT);
        }else {
            setzeBild(muecke, vx, vy);
        }

        // Geschwindigkeitskorrektur für schräge Mücken
        double faktor = 1.0;
        if(vx != 0 && vy != 0) {
            faktor = 0.70710678;
        }

    vx = (int) Math.round(massstab*faktor*vx);
        vy = (int) Math.round(massstab*faktor*vy);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(muecke_breite, muecke_hoehe);
        params.leftMargin = links;
        params.topMargin = oben;
        params.gravity = Gravity.TOP + Gravity.LEFT;

        spielbereich.addView(muecke, params);

        mp.seekTo(0);
        mp.start();
    }

    private void setzeBild(ImageView muecke, int vx, int vy) {
        muecke.setImageResource(MUECKEN_BILDER[vy+1][vx+1]);
    }

    @Override
    public void onClick(View muecke) {


        if(muecke.getTag(R.id.tier)== ELEFANT){
           if(schwierigkeitsgrad == 0){
               zeit -= 100;
           }else if(schwierigkeitsgrad == 1){
               zeit -= 200;
           }else {
               zeit -= 300;
           }
        }else {
            gefangeneMuecken++;
            if(schwierigkeitsgrad == 0){
                punkte += 100;
            }else if( schwierigkeitsgrad == 1){
                punkte += 200;
            }else{
                punkte += 300;
            }
        }

        bildschirmAktualisieren();
        mp.pause();
       if(muecke.getTag(R.id.tier)!= ELEFANT){
           if(slapp.isPlaying()){
               slapp.pause();
           }
           slapp.seekTo(0);
           slapp.start();
            ImageView tot = (ImageView) muecke;
            tot.setImageResource(R.drawable.muecke_killed);
           mueckeGetroffen(muecke);
       }else {
           mueckeGetroffen(muecke);
       }
    }

    public void mueckeGetroffen(View muecke){
        Animation animationTreffer = AnimationUtils.loadAnimation(this,R.anim.treffer);
        muecke.startAnimation(animationTreffer);
        animationTreffer.setAnimationListener(new MueckeAnimationListener(muecke));
        muecke.setOnClickListener(null);
    }

    @Override
    public void run() {
        zeitHerunterzaehlen();
    }

    @Override
    protected void onDestroy() {
        mp.release();
        slapp.release();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(this);
    }




    private class MueckeAnimationListener implements Animation.AnimationListener {
        private View muecke;

        public MueckeAnimationListener(View m) {
            muecke = m;
        }
        @Override
        public void onAnimationStart(Animation animation) {

        }

        @Override
        public void onAnimationEnd(Animation animation) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    spielbereich.removeView(muecke);
                }
            });
        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }
    }
}

