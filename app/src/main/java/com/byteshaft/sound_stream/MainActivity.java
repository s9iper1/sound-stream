package com.byteshaft.sound_stream;

import android.app.ProgressDialog;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.byteshaft.sound_stream.adapter.SongsAdapter;
import com.byteshaft.sound_stream.utils.AppGlobals;
import com.byteshaft.sound_stream.utils.Helpers;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;

import wseemann.media.FFmpegMediaPlayer;

public class MainActivity extends ActionBarActivity implements View.OnClickListener, FFmpegMediaPlayer.OnPreparedListener {

    private FFmpegMediaPlayer mMediaPlayer;
    private ProgressDialog mProgressDialog;
    private ListView mListView;
    // media controls
    private ImageView mPlayerControl;
    private ImageView buttonNext;
    private ImageView buttonPrevious;
    private LinearLayout controls_layout;
    private boolean controlsVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mListView = (ListView) findViewById(R.id.song_list);
        /// Media Controls
        mPlayerControl = (ImageView) findViewById(R.id.btnPlay);
        buttonNext = (ImageView) findViewById(R.id.btnNext);
        buttonPrevious = (ImageView) findViewById(R.id.btnPrevious);
        controls_layout = (LinearLayout) findViewById(R.id.controls_layout);
        AppGlobals.initializeAllDataSets();
        new GetSoundDetailsTask().execute();
        mMediaPlayer = new FFmpegMediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setOnPreparedListener(this);
//        mMediaPlayer.setOnPreparedListener(new FFmpegMediaPlayer.OnPreparedListener() {
//            @Override
//            public void onPrepared(FFmpegMediaPlayer fFmpegMediaPlayer) {
////                fFmpegMediaPlayer.start();
////                togglePlayPause();
//                animateBottomUp();
//            }
//        });
//        mMediaPlayer.setOnCompletionListener(new FFmpegMediaPlayer.OnCompletionListener() {
//            @Override
//            public void onCompletion(FFmpegMediaPlayer mp) {
//                mPlayerControl.setImageResource(android.R.drawable.ic_media_play);
//                animateBottomDown();
//
//            }
//        });

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String url = AppGlobals.getStreamUrlsHashMap().
                        get(Integer.valueOf(String.valueOf(parent.getItemAtPosition(position))));
                String formattedUrl = String.format("%s%s%s", url,
                        AppGlobals.ADD_CLIENT_ID, AppGlobals.CLIENT_KEY);
                new PlaySoundTask().execute(formattedUrl);
            }
        });
        mPlayerControl.setOnClickListener(this);
    }

    private void animateBottomDown() {
        Animation bottomDown = AnimationUtils.loadAnimation(MainActivity.this,
                R.anim.bottom_down);
        controls_layout.startAnimation(bottomDown);
        controls_layout.setVisibility(View.GONE);
        controlsVisible = false;

    }

    private void animateBottomUp() {
        if (!controlsVisible) {
            Animation bottomUp = AnimationUtils.loadAnimation(MainActivity.this,
                    R.anim.bottom_up);
            controls_layout.startAnimation(bottomUp);
            controls_layout.setVisibility(View.VISIBLE);
            controlsVisible = true;
        }

    }

    private void playSong(String formattedUrl) {
        Uri uri = Uri.parse(formattedUrl);
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.stop();
            mMediaPlayer.reset();
        }
        try {
            mMediaPlayer.setDataSource(getApplicationContext(), uri);
            mMediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void togglePlayPause() {
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
            mPlayerControl.setImageResource(android.R.drawable.ic_media_play);
        } else {
            mMediaPlayer.start();
            mPlayerControl.setImageResource(android.R.drawable.ic_media_pause);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
            }
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnPlay:
//                togglePlayPause();
                break;
        }
    }

    @Override
    public void onPrepared(FFmpegMediaPlayer fFmpegMediaPlayer) {
        fFmpegMediaPlayer.start();

    }

    class GetSoundDetailsTask extends AsyncTask<String, String, ArrayList<Integer>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog = new ProgressDialog(MainActivity.this);
            mProgressDialog.setMessage("loading ...");
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
        }

        @Override
        protected ArrayList<Integer> doInBackground(String... params) {
            int responseCode = 0;
            if (Helpers.isNetworkAvailable() && Helpers.isInternetWorking()) {
                try {
                    responseCode = Helpers.getRequest(AppGlobals.USER_URL + AppGlobals.CLIENT_KEY);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    JsonParser jsonParser = new JsonParser();
                    JsonArray jsonArray = jsonParser.parse(Helpers.getParsedString())
                            .getAsJsonArray();
                    System.out.println(jsonArray);
                    for (int i = 0; i < jsonArray.size(); i++) {
                        JsonObject jsonObject = jsonArray.get(i).getAsJsonObject();
                        if (!AppGlobals.getsSongsIdsArray().contains(jsonObject.get("id").getAsInt())) {
                            int currentSongId = jsonObject.get("id").getAsInt();
                            AppGlobals.addSongId(currentSongId);
                            if (!jsonObject.get("title").isJsonNull()) {
                                AppGlobals.addTitleToHashMap(currentSongId, jsonObject.get("title")
                                        .getAsString());
                            }
                            if (!jsonObject.get("stream_url").isJsonNull()) {
                                AppGlobals.addStreamUrlsToHashMap(currentSongId, jsonObject.get("stream_url")
                                        .getAsString());
                            }
                            if (!jsonObject.get("duration").isJsonNull()) {
                                AppGlobals.addDurationHashMap(currentSongId, jsonObject.get("duration")
                                        .getAsString());
                            }
                            if (!jsonObject.get("genre").isJsonNull()) {
                                AppGlobals.addGenreHashMap(currentSongId, jsonObject.get("genre")
                                        .getAsString());
                            }
                            if (!jsonObject.get("artwork_url").isJsonNull()) {
                                AppGlobals.addSongImageUrlHashMap(currentSongId,
                                        jsonObject.get("artwork_url").getAsString());
                            }
                            JsonObject jsonElements = jsonObject.get("user").getAsJsonObject();
                            System.out.println(jsonElements.get("username").getAsString());
                            if (!jsonElements.get("username").isJsonNull()) {
                                AppGlobals.addSongArtistHashMap(currentSongId,
                                        jsonElements.get("username").getAsString());
                            }
                        }

                    }

                }
            }
            return AppGlobals.getsSongsIdsArray();
        }

        @Override
        protected void onPostExecute(ArrayList<Integer> songIdsArray) {
            super.onPostExecute(songIdsArray);
            mProgressDialog.dismiss();
            SongsAdapter songsAdapter = new SongsAdapter(getApplicationContext(),R.layout.single_row,
                    songIdsArray, MainActivity.this);
            mListView.setAdapter(songsAdapter);
        }
    }

    class PlaySoundTask extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... params) {
            playSong(params[0]);
            return null;
        }
    }
}
