/*
 * Copyright (C) 2010 ZXing authors
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

package com.syscan.example;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.util.Log;

import java.io.IOException;

public class BeepManager {

  private static final String TAG = BeepManager.class.getSimpleName();

  private static final float BEEP_VOLUME = 1.0f;

  private final Activity activity;
  private MediaPlayer mediaPlayer;
  private static int mID;

  public BeepManager(Activity activity, int id) {
    this.activity = activity;
    this.mediaPlayer = null;
    mID = id;
    updatePrefs();
  }

  public void playBeepSound() {
    if (mediaPlayer != null) {
      mediaPlayer.start();
    }
  }
  
  private void updatePrefs() {
	    if (mediaPlayer == null) {
	      // The volume on STREAM_SYSTEM is not adjustable, and users found it too loud,
	      // so we now play on the music stream.
	      activity.setVolumeControlStream(AudioManager.STREAM_MUSIC);
	      mediaPlayer = buildMediaPlayer(activity);
	    }
	  }

  private static MediaPlayer buildMediaPlayer(Context activity) {
    MediaPlayer mediaPlayer = new MediaPlayer();
    mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
    // When the beep has finished playing, rewind to queue up another one.
    mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
      @Override
      public void onCompletion(MediaPlayer player) {
        player.seekTo(0);
      }
    });
    mediaPlayer.setOnErrorListener(new OnErrorListener() {
        public boolean onError(MediaPlayer mp, int what, int extra) {
        	Log.e(TAG, "Error occurred while playing audio.");
            return true;
        }
    });
    AssetFileDescriptor file = activity.getResources().openRawResourceFd(mID);
    try {
      mediaPlayer.setDataSource(file.getFileDescriptor(), file.getStartOffset(), file.getLength());
      file.close();
      mediaPlayer.setVolume(BEEP_VOLUME, BEEP_VOLUME);
      mediaPlayer.prepare();
    } catch (IOException ioe) {
      Log.w(TAG, ioe);
      mediaPlayer = null;
    }
    return mediaPlayer;
  }

}
