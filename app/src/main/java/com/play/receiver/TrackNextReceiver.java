package com.play.receiver;

import com.play.entity.MyConstant;
import com.play.service.MusicService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class TrackNextReceiver extends BroadcastReceiver {
	
	private int position;
	@Override
	public void onReceive(Context context, Intent intent) {
		
		if (MusicService.position == MusicService.medias.size() - 1) {
			MusicService.position = 0;
		} else {
			position = MusicService.position++;
		}
		
		Intent newIntent = new Intent();
		newIntent.setAction(MyConstant.ACTION_NEXT);
		newIntent.getIntExtra("index", position);
		context.sendBroadcast(newIntent);
		
	}

}
