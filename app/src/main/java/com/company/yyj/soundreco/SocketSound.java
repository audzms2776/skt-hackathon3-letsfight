package com.company.yyj.soundreco;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;


import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONObject;

import java.net.URISyntaxException;

/**
 * Created by LikeJust on 2017-08-25.
 */

class SocketSound {
    private MainActivity context;
    private MediaPlayer media;
    private Socket mSocket;

    {
        try {
            mSocket = IO.socket(MyConstant.SOCKETIO_ADDR);
            Log.e("ss", String.valueOf(mSocket));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    SocketSound(Context context) {
        this.context = (MainActivity) context;
        mSocket.on("type", onNewSoundMsg);
        mSocket.connect();
    }

    private int selectMusic(String type) {
        int musicId = 0;
        switch (type) {
            case "bitter":
                musicId = R.raw.bad;
                break;
            case "weird":
                musicId = R.raw.good;
                break;
            default:
                musicId = R.raw.effectsound;
                break;

        }

        return musicId;
    }

    private void soundPlay(String type) {
        if (media == null) {
            media = MediaPlayer.create(context, selectMusic(type));
            media.start();
        } else if (!media.isPlaying()) {
            media.release();
            media = MediaPlayer.create(context, selectMusic(type));
            media.start();
        }
    }

    private Emitter.Listener onNewSoundMsg = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            String type = (String) args[0];
            Log.e("Socket Sound -> ", type + "");
            soundPlay(type);
        }
    };
}
