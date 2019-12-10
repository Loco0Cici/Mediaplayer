package com.play.receiver;

import com.play.entity.MyConstant;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class TrackPlayReceiver extends BroadcastReceiver{

	@Override
	public void onReceive(Context context, Intent intent) {
		Intent newIntent = new Intent();
        newIntent.setAction(MyConstant.ACTION_PLAY);
        context.sendBroadcast(newIntent);
	}

}
