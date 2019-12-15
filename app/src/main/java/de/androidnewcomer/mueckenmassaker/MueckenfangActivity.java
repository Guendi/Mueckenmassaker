package de.androidnewcomer.mueckenmassaker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;


public class MueckenfangActivity extends Activity implements OnClickListener, Html.ImageGetter {


    private Animation animationEinblenden;
    private Animation animationWackeln;
    Button startButton;
    private Handler handler = new Handler();
    private Runnable wackelnRunnable = new WackleButton();
    private LinearLayout namenseingabe;
    private Button speichern;
    private Button stufeEinfach;
    private Button stufeMittel;
    private Button stufeSchwer;
    private static final String HIGHSCORE_SERVER_BASE_URL = "https://www.guendi.ch/game/app.php";
    private static final String HIGHSCORESERVER_GAME_ID = "mueckenfang-";
    private String SCHWIERIGKEITSGRAD = "";
    private String highscoresHtml;
    private ListView listView;
    private ToplistAdapter adapter;
    private List<String> highscoreList = new ArrayList<String>();
    private Spinner schwierigkeitsgrad;
    private ArrayAdapter<String> schwierigkeitsgradAdapter;
    private int erreichtePunkte;
    private int s;
    private TextView zahlTest;
    TextView highscoreText;

    class ToplistAdapter extends ArrayAdapter<String> {

        public ToplistAdapter(Context context, int resource) {
            super(context, resource);
        }

        @Override
        public int getCount() {
            return highscoreList.size();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView==null) {
                convertView = getLayoutInflater().inflate(R.layout.toplist_element,null);
            }
            TextView tvPlatz = convertView.findViewById(R.id.platz);
            tvPlatz.setText(Integer.toString(position + 1) + ".");
            TextUtils.SimpleStringSplitter sss = new TextUtils.SimpleStringSplitter(',');
            sss.setString(highscoreList.get(position));
            TextView tvName =  convertView.findViewById(R.id.name);
            tvName.setText(sss.next());
            TextView tvPunkte = convertView.findViewById(R.id.punkte);
            tvPunkte.setText(sss.next());


            return  convertView;
        }
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        Button button =  findViewById(R.id.btnStart);
        button.setOnClickListener(this);
        animationEinblenden = AnimationUtils.loadAnimation(this,R.anim.einblenden);
        animationWackeln = AnimationUtils.loadAnimation(this,R.anim.wackeln);
        startButton = findViewById(R.id.btnStart);
        namenseingabe = findViewById(R.id.namenseingabe);
        namenseingabe.setVisibility(View.GONE);
        stufeEinfach = findViewById(R.id.gradeEasy);
        stufeEinfach.setOnClickListener(this);
        stufeMittel =  findViewById(R.id.gradeMiddle);
        stufeMittel.setOnClickListener(this);
        stufeSchwer =  findViewById(R.id.gradeHard);
        stufeSchwer.setOnClickListener(this);
        speichern = findViewById(R.id.speichern);
        speichern.setOnClickListener(this);
        listView = findViewById(R.id.listView);
        highscoreText = findViewById(R.id.highscoreText);
        adapter = new ToplistAdapter(this,0);
        listView.setAdapter(adapter);
        schwierigkeitsgrad =  findViewById(R.id.schwierigkeitsgrad);
        schwierigkeitsgradAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, new String[] {"leicht","mittel","schwer"});
        schwierigkeitsgradAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        schwierigkeitsgrad.setAdapter(schwierigkeitsgradAdapter);;
    }




    private class WackleButton implements Runnable {
        // Wiggling button animation
        @Override
        public void run() {
            startButton.startAnimation(animationWackeln);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        View v = findViewById(R.id.wurzel);
        v.startAnimation(animationEinblenden);
        handler.postDelayed(wackelnRunnable, 1000 * 10);

      highscoreAnzeigen();
        internetHighscores("",0);
    }

    private void highscoreAnzeigen() {
        TextView tv = (TextView) findViewById(R.id.highscore);
        if(SCHWIERIGKEITSGRAD == ""){
            SCHWIERIGKEITSGRAD = "LEICHT";
        }
        int highscore = leseHighscore(SCHWIERIGKEITSGRAD);
        if(highscore > 0) {
            tv.setText(Integer.toString(highscore) + " von " + leseHighscoreName(SCHWIERIGKEITSGRAD));
        } else {
            tv.setText("kein HIghscore vorhanden");
        }
    }

    private int leseHighscore(String SCHWIERIGKEITSGRAD) {
        SharedPreferences pref = getSharedPreferences("GAME", 0);
        if(SCHWIERIGKEITSGRAD == "LEICHT"){
            return pref.getInt("HIGHSCORE_LEICHT", 0);
        }else if (SCHWIERIGKEITSGRAD == "MITTEL"){
            return pref.getInt("HIGHSCORE_MITTEL",0);
        }else if(SCHWIERIGKEITSGRAD == "SCHWER"){
            return pref.getInt("HIGHSCORE_SCHWER",0);
        }
        return 0;

    }

    private void schreibeHighscoreName(String SCHWIERIGKEITSGRAD) {
        TextView tv = (TextView) findViewById(R.id.spielername);
        String name = tv.getText().toString().trim();
        SharedPreferences pref = getSharedPreferences("GAME", 0);
        SharedPreferences.Editor editor = pref.edit();
        if(SCHWIERIGKEITSGRAD == "LEICHT"){
            editor.putString("HIGHSCORE_NAME_LEICHT", name);
        }else if (SCHWIERIGKEITSGRAD == "MITTEL"){
            editor.putString("HIGHSCORE_NAME_MITTEL", name);
        }else if(SCHWIERIGKEITSGRAD == "SCHWER"){
            editor.putString("HIGHSCORE_NAME_SCHWER", name);
        }

        editor.commit();
    }

    private String leseHighscoreName(String SCHWIERIGKEITSGRAD) {
        SharedPreferences pref = getSharedPreferences("GAME", 0);
        if(SCHWIERIGKEITSGRAD == "LEICHT"){
            return pref.getString("HIGHSCORE_NAME_LEICHT", "");
        }else if (SCHWIERIGKEITSGRAD == "MITTEL"){
            return pref.getString("HIGHSCORE_NAME_MITTEL","");
        }else if(SCHWIERIGKEITSGRAD == "SCHWER"){
            return pref.getString("HIGHSCORE_NAME_SCHWER","");
        }

        return pref.getString("HIGHSCORE_NAME_", "");
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(wackelnRunnable);
    }

    @Override
    public void onClick(View view) {
        if(view.getId() == R.id.btnStart) {
            s = schwierigkeitsgrad.getSelectedItemPosition();
            Intent intent = new Intent(this, GameActivity.class);
            intent.putExtra("schwierigkeitsgrad", s);
            startActivityForResult(intent,1);
        } else if(view.getId() == R.id.speichern) {
            schreibeHighscoreName(SCHWIERIGKEITSGRAD);
            schreibeHighscore(erreichtePunkte, SCHWIERIGKEITSGRAD);
            highscoreAnzeigen();
            namenseingabe.setVisibility(View.INVISIBLE);
            internetHighscores(leseHighscoreName(SCHWIERIGKEITSGRAD), leseHighscore(SCHWIERIGKEITSGRAD));
        }else if (view.getId() == R.id.gradeEasy){
            SCHWIERIGKEITSGRAD = "LEICHT";
            highscoreText.setText("Highscores-Leicht");
            internetHighscores("",0);
            highscoreAnzeigen();

        }else if (view.getId() == R.id.gradeMiddle){
            SCHWIERIGKEITSGRAD = "MITTEL";
            highscoreText.setText("Highscores-Mittel");
            internetHighscores("",0);
            highscoreAnzeigen();

        }else if (view.getId() == R.id.gradeHard){
            SCHWIERIGKEITSGRAD = "SCHWER";
            highscoreText.setText("Highscores-Schwer");
            internetHighscores("",0);
            highscoreAnzeigen();

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode==1) {
        s = schwierigkeitsgrad.getSelectedItemPosition();
                 if(s == 0){
                     SCHWIERIGKEITSGRAD = "LEICHT";
                     if(resultCode > leseHighscore(SCHWIERIGKEITSGRAD)) {
                         erreichtePunkte = resultCode;
                         namenseingabe.setVisibility(View.VISIBLE);
                     }
                 }else if (s == 1){
                     SCHWIERIGKEITSGRAD = "MITTEL";
                     if(resultCode > leseHighscore(SCHWIERIGKEITSGRAD)) {
                         erreichtePunkte = resultCode;
                         namenseingabe.setVisibility(View.VISIBLE);
                     }
                 }else if(s == 2) {
                     SCHWIERIGKEITSGRAD = "SCHWER";
                     if(resultCode > leseHighscore(SCHWIERIGKEITSGRAD)) {
                         erreichtePunkte = resultCode;
                         namenseingabe.setVisibility(View.VISIBLE);
                     }
                 }


        }

    }

    private void schreibeHighscore(int highscore, String SCHWIERIGKEITSGRAD) {
        SharedPreferences pref = getSharedPreferences("GAME", 0);
        SharedPreferences.Editor editor = pref.edit();
        if(SCHWIERIGKEITSGRAD == "LEICHT"){
            editor.putInt("HIGHSCORE_LEICHT", highscore);

        }else if (SCHWIERIGKEITSGRAD == "MITTEL"){
            editor.putInt("HIGHSCORE_MITTEL", highscore);

        }else if (SCHWIERIGKEITSGRAD == "SCHWER"){
            editor.putInt("HIGHSCORE_SCHWER", highscore);
        }

        editor.commit();
    }



    @Override
    public Drawable getDrawable(String name) {
        int id = getResources().getIdentifier(name, "drawable", this.getPackageName());
        Drawable d = getResources().getDrawable(id);
        d.setBounds(0, 0, 30, 30);
        return d;
    }

    private void internetHighscores(final String name, final int points) {
        (new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(HIGHSCORE_SERVER_BASE_URL
                            + "?game=" + HIGHSCORESERVER_GAME_ID + SCHWIERIGKEITSGRAD
                            + "&name=" + URLEncoder.encode(name, "utf-8")
                            + "&points=" + Integer.toString(points));
                          //  + "&max=100"
                    Log.d("link","link "+url);

                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                    InputStreamReader input = new InputStreamReader(conn.getInputStream(), "utf-8");

                    BufferedReader reader = new BufferedReader(input,2000);
                    highscoreList.clear();
                    String line = reader.readLine();
                    while (line != null) {
                        highscoreList.add(line);
                        line = reader.readLine();
                    }

                } catch(IOException e) {

                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.notifyDataSetInvalidated();
                    }
                });
            }
        })).start();

    }
}

