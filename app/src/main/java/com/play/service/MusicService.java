package com.play.service;

import java.util.List;

import com.play.R;
import com.play.entity.Media;
import com.play.entity.MyConstant;
import com.play.receiver.TrackNextReceiver;
import com.play.receiver.TrackPlayReceiver;
import com.play.uitl.GlobalVariables;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.RemoteViews;
/**
 *
 * 后台服务
 */
public class MusicService extends Service implements OnPreparedListener, OnCompletionListener {

	/**
	 *
	 * listener 电话监听对象
	 */
	private MyPhoneStateListener listener;

	/**
	 * tm 电话管理器对象
	 */
	private TelephonyManager tm;

	/**
	 * mPlayer 媒体播放器对象
	 */
	private MediaPlayer mPlayer;

	/**
	 * mbr 自定义的广播接收者
	 */
	private MyBroadcastReceiver mbr;

	/**
	 * intentFilter 意图过滤器对象
	 */
	private IntentFilter intentFilter;

	/**
	 * isFirst_play ：默认true 如果是第一次就会随机播放一首歌曲，否则就执行暂停或者播放逻辑 restart：
	 * 判断当电话响铃之前是否在播放音乐的标志，如果true，当电话挂断后执行继续播放，否则不播放
	 */
	private boolean isFirst_play = true, restart = false;

	private boolean isPlay_No = true;
	/**
	 * medias 所有媒体文件的集合对象
	 */
	public static List<Media> medias;

	/**
	 *
	 * position 当前歌曲路径在资源集合中的下标
	 */
	public static int position;

	/**
	 * MILLISECONDS 线程暂停时间
	 */
	private static final int MILLISECONDS = 500;
	private static int time = 0;

	/**
	 * 启动广播通知栏的判断值
	 */
	private Intent playIntent, nextIntent;

	private PendingIntent playPendingIntent, nextPendingIntent;

	private RemoteViews mRemoteView;

	/**
	 * notification
	 */
	private NotificationManager mNotificationManager;
	private Notification mNotification;
	private int NOTI_ID = 1;

	@Override
	public void onCreate() {
		// 服务创建时
		super.onCreate();

		tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

		// 电话监听器
		listener = new MyPhoneStateListener();

		// 获取电话监听状态
		tm.listen(listener, MyPhoneStateListener.LISTEN_CALL_STATE);

		// 创建广播过滤器
		intentFilter = new IntentFilter();

		// 将所有的状态加入到过滤器中
		intentFilter.addAction(MyConstant.ACTION_PLAY);// 播放
		intentFilter.addAction(MyConstant.ACTION_PAUSE);// 暂停
		intentFilter.addAction(MyConstant.ACTION_NEXT);// 下一首
		intentFilter.addAction(MyConstant.ACTION_LAST);// 上一首
		intentFilter.addAction(MyConstant.ACTION_LIST);// 点击listView
		intentFilter.addAction(MyConstant.ACTION_LIST_SEARCH);// 单机搜索界面的listView条目
		intentFilter.addAction(MyConstant.ACTION_PLAN_CURRENT);// 用来发送进度条位置给服务的意图

		mbr = new MyBroadcastReceiver();// 创建一个广播接收者
		registerReceiver(mbr, intentFilter);// 注册广播接收者并添加过滤

	}

	/*********************************************************************
	 *
	 * 服务被启动
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		mPlayer = new MediaPlayer();
		medias = GlobalVariables.medias;
		initNotification();
		return super.onStartCommand(intent, flags, startId);

	}

	/********************************************************************
	 *
	 * 服务销毁
	 */
	@Override
	public void onDestroy() {
		isPlay_No = false;
		tm.listen(listener, MyPhoneStateListener.LISTEN_NONE);
		mPlayer.release();
		mPlayer = null;
		cancelNoti();
		super.onDestroy();
	}

	/*****************************************************************
	 *
	 * 服务被绑定
	 */
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	/*********************************************************************
	 *
	 * 自定义的电话监听器 用来监听电话状态，并作出相应动作
	 */
	private class MyPhoneStateListener extends PhoneStateListener {
		@Override
		public void onCallStateChanged(int state, String incomingNumber) {
			switch (state) {
				case TelephonyManager.CALL_STATE_IDLE:
					// 电话闲置或者挂断时
					if (restart) {
						mPlayer.start();
						updateNotification();
						sendPlayingWord(position);
					}
					break;
				case TelephonyManager.CALL_STATE_OFFHOOK:
					// 电话接通后
					updateNotification();
					break;
				case TelephonyManager.CALL_STATE_RINGING:
					// 电话响铃时
					if (mPlayer.isPlaying()) {
						mPlayer.pause();
						updateNotification();
						sendPause();
						restart = true;
					}
					break;
			}
		}
	}

	/****************************************************************
	 *
	 * 自定义广播接收者。用来接收相对应的广播并执行相对动作
	 */
	private class MyBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (MyConstant.ACTION_PLAY.equals(intent.getAction())) {

				// 如果播放器当前没有播放音乐
				if (!mPlayer.isPlaying()) {
					if (isFirst_play) {
						position = intent.getIntExtra("index", 0);
						prepareMusic(position);
						isFirst_play = false;
					} else {
						mPlayer.start();
						sendCurrentPosition();
						updateNotification();
						sendPlayingWord(position);// 发送播放消息
					}
				} else {
					mPlayer.pause();
					updateNotification();
					sendPause();
				}
			} else if (MyConstant.ACTION_PAUSE.equals(intent.getAction())) {
				// 暂停
				if (mPlayer.isPlaying()) {
					mPlayer.pause();
					updateNotification();
					sendPause();
				}
			} else if (MyConstant.ACTION_NEXT.equals(intent.getAction())) {
				// 下一首
				if (isFirst_play) {
					position = intent.getIntExtra("index", 0);
					updateNotification();
					prepareMusic(position);
					isFirst_play = false;
				} else {
					position = intent.getIntExtra("index", position);
					updateNotification();
					prepareMusic(position);
				}
			} else if (MyConstant.ACTION_LAST.equals(intent.getAction())) {
				// 上一首
				if (isFirst_play) {
					position = intent.getIntExtra("index", 0);
					updateNotification();
					prepareMusic(position);
					isFirst_play = false;
				} else {
					position = intent.getIntExtra("index", position);
					updateNotification();
					prepareMusic(position);
				}
			} else if (MyConstant.ACTION_LIST.equals(intent.getAction())) {
				// 单击条目
				if (isFirst_play) {
					position = intent.getIntExtra("index", 0);
					updateNotification();
					prepareMusic(position);
					isFirst_play = false;
				} else {
					position = intent.getIntExtra("index", position);
					updateNotification();
					prepareMusic(position);
				}
			} else if (MyConstant.ACTION_LIST_SEARCH.equals(intent.getAction())) {
				// 单击搜索界面的条目
				String id = intent.getStringExtra("id");
				for (int i = 0; i < medias.size(); i++) {
					if (id.equals(medias.get(i).getId())) {
						position = i;
						prepareMusic(position);
						break;
					}
				}

			} else if (MyConstant.ACTION_PLAN_CURRENT.equals(intent.getAction())) {
				// 拖动进度条
				mPlayer.seekTo(intent.getIntExtra("index", 0));
			}
			System.out.println("我接受到了什么：" + intent.getAction().toString());
			System.out.println("我的发送状态：" + playIntent + "//" + nextIntent + "====");
		}
	}

	/******************************************************************
	 *
	 * 准备音乐 index 需要准备的文件下标
	 */
	private void prepareMusic(int index) {
		mPlayer.reset();
		String music_uri = medias.get(index).getUri();
		Uri songUri = Uri.parse(music_uri);
		mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		try {
			mPlayer.setDataSource(getApplicationContext(), songUri);
		} catch (Exception e) {
			e.printStackTrace();
		}
		mPlayer.setOnPreparedListener(this);
		mPlayer.setOnCompletionListener(this);
		mPlayer.prepareAsync();
		updateNotification();
	}

	/******************************************************************
	 *
	 * 音乐准备完成的回调函数
	 */
	@Override
	public void onPrepared(MediaPlayer mp) {
		mPlayer.start();
		isFirst_play = false;
		sendPlayingWord(position);
		sendCurrentPosition();
		updateNotification();
	}

	/***********************************************************************
	 *
	 * 当前歌曲播放完毕的监听回调
	 */
	@Override
	public void onCompletion(MediaPlayer mp) {
		if (mp != null) {
			if (position == medias.size() - 1) {
				position = 0;
			} else {
				position = position + 1;
			}
			prepareMusic(position);
		}
	}

	/*****************************************************************************
	 *
	 * position
	 *
	 * 当前歌曲的位置； 每当服务开始播放音乐的时候就将当前播放的歌曲传给activity， 用于设置播放歌曲的相关信息
	 */
	public void sendPlayingWord(int position) {
		Intent intent = new Intent();
		intent.setAction(MyConstant.ACTION_PlAYING_STATE);
		intent.putExtra("media", position);
		sendBroadcast(intent);
	}

	/***************************************************************************
	 *
	 * 发送广播给activity歌曲已经暂停
	 */
	public void sendPause() {
		Intent intent = new Intent();
		intent.setAction(MyConstant.ACTION_SERVICR_PUASE);
		sendBroadcast(intent);
		updateNotification();
	}

	/*****************************************************************************
	 *
	 * 循环获取当前歌曲播放的进度并通过广播发送给activity
	 */
	public void sendCurrentPosition() {
		new Thread() {
			public void run() {
				Intent intent = new Intent();
				while (isPlay_No) {
					int playerPosition = mPlayer.getCurrentPosition();
					intent.setAction(MyConstant.ACTION_MUSIC_PLAN);
					intent.putExtra("playerPosition", playerPosition);
					sendBroadcast(intent);
					Log.v("jindu", "发到界面的进度条值1:" + playerPosition);
					try {
						sleep(MILLISECONDS);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}.start();

	}

	/***********************************************************************
	 *
	 * 自定义通知栏
	 */
	public void initNotification() {
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotification = new Notification();
		mNotification.icon = R.drawable.login1;
		mNotification.tickerText = "欢迎使用雅趣音乐！";
		// 创建自定义通知视图
		mRemoteView = new RemoteViews(getPackageName(), R.layout.layout_notification);
		mRemoteView.setImageViewResource(R.id.iv_art_noti, R.drawable.stat_notify);
		mRemoteView.setTextViewText(R.id.noti_title, medias.get(position).getName());
		mRemoteView.setTextViewText(R.id.noti_small_title, medias.get(position).getSinger());
		mRemoteView.setImageViewResource(R.id.btn_noti_pause, R.drawable.notification_play);
		mRemoteView.setImageViewResource(R.id.btn_noti_next, R.drawable.notification_next);
		mNotification.contentView = mRemoteView;

		if (playIntent == null) {
			playIntent = new Intent(this, TrackPlayReceiver.class);
		}
		if (nextIntent == null) {
			nextIntent = new Intent(this, TrackNextReceiver.class);
		}
		if (playPendingIntent == null) {
			playPendingIntent = PendingIntent.getBroadcast(this, 0, playIntent, 0);
		}
		if (nextPendingIntent == null) {
			nextPendingIntent = PendingIntent.getBroadcast(this, 0, nextIntent, 0);
		}

		// 单击事件
		mRemoteView.setOnClickPendingIntent(R.id.btn_noti_next, nextPendingIntent);
		mRemoteView.setOnClickPendingIntent(R.id.btn_noti_pause, playPendingIntent);

		// 点击跳转进应用程序
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);
		intent.setComponent(new ComponentName(getPackageName(), "com.lanren.music.MainActivity"));
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 100, intent, 0);
		// 点击的事件
		mNotification.contentIntent = contentIntent;
		// 点击通知之后不消失
		mNotification.flags = Notification.FLAG_NO_CLEAR;
		mNotificationManager.notify(NOTI_ID, mNotification);
	}

	/********************************************************************
	 *
	 * 更新通知内容
	 */
	public void updateNotification() {
		Log.i("TAG", "update notification");
		mRemoteView.setTextViewText(R.id.noti_title, medias.get(position).getName());

		System.out.println("isPlay:" + mPlayer.isPlaying());

		if (mPlayer.isPlaying()) {
			mRemoteView.setImageViewResource(R.id.btn_noti_pause, R.drawable.notification_pause);

		} else {

			mRemoteView.setImageViewResource(R.id.btn_noti_pause, R.drawable.notification_play);

		}

		// 获取专辑
		int Album = medias.get(position).getAlbum_id();
		String img = getAlbumArt(Album);
		Bitmap bm = null;
		System.out.println("Album:" + img);
		if (img != null) {
			bm = BitmapFactory.decodeFile(img);
			if (bm != null) {
				// 设置图片格式
				BitmapDrawable bmpDraw = new BitmapDrawable(bm);
				Log.v("TAG", "bmpDraw有没有:" + bmpDraw);
				// 设置专辑图片
				mRemoteView.setImageViewBitmap(R.id.iv_art_noti, bm);
			} else {
				mRemoteView.setImageViewResource(R.id.iv_art_noti, R.drawable.me);
			}
		} else {
			mRemoteView.setImageViewResource(R.id.iv_art_noti, R.drawable.me);
		}
		mNotificationManager.notify(NOTI_ID, mNotification);
	}

	public void cancelNoti() {
		mNotificationManager.cancel(NOTI_ID);
	}

	/**********************************************************************
	 *
	 * 拿到专辑图片的路径
	 */
	private String getAlbumArt(int album_id) {
		String mUriAlbums = "content://media/external/audio/albums";
		String[] projection = new String[] { "album_art" };
		Cursor cur = this.getContentResolver().query(Uri.parse(mUriAlbums + "/" + Integer.toString(album_id)),
				projection, null, null, null);
		String album_art = null;
		if (cur.getCount() > 0 && cur.getColumnCount() > 0) {
			cur.moveToNext();
			album_art = cur.getString(0);
		}
		cur.close();
		cur = null;
		return album_art;
	}

}