/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.exoplayer;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.EventLogger;
import com.google.android.exoplayer2.util.Log;
import com.google.android.exoplayer2.util.Util;

import java.time.Duration;
import java.time.Instant;

import static java.time.Duration.between;


/**
 * A fullscreen activity to play audio or video streams.
 */
public class PlayerActivity extends AppCompatActivity {

  private PlayerView playerView;
  private SimpleExoPlayer player;
  private boolean playWhenReady = true;
  private int currentWindow = 0;
  private long playbackPosition = 0;
  private PlaybackStateListener playbackStateListener;
  private DefaultTrackSelector trackSelector;
  private DefaultTrackSelector.Parameters trackSelectorParameters;
  private static final String TAG = PlayerActivity.class.getName();
  private TextView titleTextView;
  private TextView analyticTextView;
  private AnalyticsTextViewHelper analyticsViewHelper;
  private Instant time1;
  private Instant time2;


  private void releasePlayer() {
    if (player != null) {
      playWhenReady = player.getPlayWhenReady();
      playbackPosition = player.getCurrentPosition();
      currentWindow = player.getCurrentWindowIndex();
      analyticsViewHelper.stop();
      player.removeListener(playbackStateListener);
      player.release();
      player = null;
    }
  }



  @SuppressLint("NewApi")
  @RequiresApi(api = Build.VERSION_CODES.O)
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_player);
    playbackStateListener = new PlaybackStateListener();
    playerView = findViewById(R.id.video_view);
    titleTextView = findViewById(R.id.title_text_view);
    analyticTextView = findViewById(R.id.debug_text_view);
    time1=Instant.now();
  }

  @SuppressLint("InlinedApi")
  private void hideSystemUi() {
    playerView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
  }

  @Override
  protected void onResume() {
    super.onResume();
    hideSystemUi();
    if ((Util.SDK_INT < 24 || player == null)) {
      initializePlayer();
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    if (Util.SDK_INT < 24) {
      releasePlayer();
    }
  }

  @Override
  protected void onStop() {
    super.onStop();
    if (Util.SDK_INT >= 24) {
      releasePlayer();
    }
  }

  @Override
  protected void onStart() {
    super.onStart();
    if (Util.SDK_INT >= 24) {
      initializePlayer();
    }
  }

  @SuppressLint("NewApi")
  private void initializePlayer() {
    if (player == null) {
      TrackSelection.Factory trackSelectionFactory = new AdaptiveTrackSelection.Factory();
      trackSelector = new DefaultTrackSelector(trackSelectionFactory );
      trackSelectorParameters = trackSelector.getParameters();
      trackSelector.setParameters(trackSelectorParameters);
      player = ExoPlayerFactory.newSimpleInstance(this, trackSelector);
      playerView.setPlayer(player);

      Uri uri = Uri.parse(getString(R.string.media_url_dash));
      Uri uri1 = Uri.parse(getString(R.string.media_url_dash));
      MediaSource mediaSource = buildMediaSource(uri,uri1);
      player.setPlayWhenReady(playWhenReady);
      player.seekTo(currentWindow, playbackPosition);
      player.addListener(playbackStateListener);
      player.addAnalyticsListener(new EventLogger(trackSelector));
      player.addAnalyticsListener(new AnalyticsEvents(player, titleTextView));
      analyticsViewHelper = new AnalyticsTextViewHelper(player, analyticTextView);
      analyticsViewHelper.start();
      player.prepare(mediaSource, false, false);
      time2=Instant.now();
      titleTextView.setText("startup time: " +startuptime(time2,time1));
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.O)
  private Duration startuptime(Instant time2, Instant time1) {
    Duration startuptime= between(time1,time2);
    return startuptime;
  }



  private MediaSource buildMediaSource(Uri uri, Uri uri1) {
    DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this, "exoplayer-codelab");
    DashMediaSource.Factory mediaSourceFactory = new DashMediaSource.Factory(dataSourceFactory);
    MediaSource mediaSource1 = mediaSourceFactory.createMediaSource(uri);
    MediaSource mediaSource2 = mediaSourceFactory.createMediaSource(uri1);
    return new ConcatenatingMediaSource(mediaSource1, mediaSource2);
  }


  private class PlaybackStateListener implements Player.EventListener
  {
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState)
    {
      String stateString;
      switch (playbackState) {
        case ExoPlayer.STATE_IDLE:
          stateString = "ExoPlayer.STATE_IDLE      -";
          break;
        case ExoPlayer.STATE_BUFFERING:
          stateString = "ExoPlayer.STATE_BUFFERING -";
          break;
        case ExoPlayer.STATE_READY:
          stateString = "ExoPlayer.STATE_READY     -";
          break;
        case ExoPlayer.STATE_ENDED:
          stateString = "ExoPlayer.STATE_ENDED     -";
          break;
        default:
          stateString = "UNKNOWN_STATE             -";
          break;
      }
      Log.d(TAG, "changed state to " + stateString + " playWhenReady: " + playWhenReady);
    }
  }
}