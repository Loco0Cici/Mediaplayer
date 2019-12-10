package com.play.music;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.play.R;
import com.play.entity.MyConstant;
import com.play.music.base.ManageActivity;
import com.play.uitl.GlobalVariables;
import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.ViewInject;
import org.xutils.x;


import java.util.Random;

@ContentView(R.layout.aty_play)
public class PlayActivity extends ManageActivity implements SeekBar.OnSeekBarChangeListener {
	private TextView music_current_time;
	private TextView music_always_time;
	private SeekBar sb_music;
	private ImageView iv_special;
	private TextView play_Title;
	private TextView play_Artist;
	private TextView tvMode;
	private ImageView ivMode;
	private LinearLayout llMode;
	/**
	 * 歌曲标记
	 */
	private int position;
	private ImageView control;
	private MyBroadcastReceiver broadcastReceiver;// 广播
	private IntentFilter filter;// 广播过滤器
	/**
	 * flag 暂停还是继续的标志
	 */
	private boolean flag = true;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		x.view().inject(this);
		initView();
	}

	@Override
	protected void initView() {
		super.initView();
		/**
		 * 歌曲播放界面
		 */
		// 创建一个广播
		position = getIntent().getIntExtra("position",0);
		broadcastReceiver = new MyBroadcastReceiver();
		// 创建广播过滤器
		filter = new IntentFilter();
		filter.addAction(MyConstant.ACTION_PlAYING_STATE);
		filter.addAction(MyConstant.ACTION_SERVICR_PUASE);
		filter.addAction(MyConstant.ACTION_MUSIC_PLAN);
		filter.addAction(MyConstant.ACTION_PLAY);
		registerReceiver(broadcastReceiver, filter);
		tvMode = (TextView) findViewById(R.id.tv_mode);
		ivMode = (ImageView) findViewById(R.id.iv_mode);
		llMode = (LinearLayout) findViewById(R.id.ll_mode);
		music_current_time = (TextView) findViewById(R.id.music_current_time);// 歌曲的播放到当前的进度
		music_always_time = (TextView) findViewById(R.id.music_always_time);// 歌曲的总长度
		sb_music = (SeekBar) findViewById(R.id.sb_music);// 进度条
		sb_music.setOnSeekBarChangeListener(this);
		control = (ImageView) findViewById(R.id.control);
		/**
		 * 实例化播放界面控件
		 */
		iv_special = (ImageView) findViewById(R.id.infoOperating);// 专辑图片
		play_Title = (TextView) findViewById(R.id.play_Title);// 播放界面的歌曲标题
		play_Artist = (TextView) findViewById(R.id.play_Artist);// 播放界面的歌曲艺术家
		setPlayText(position);
		tvMode.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setMode();
			}
		});
		ivMode.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setMode();
			}
		});
		llMode.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setMode();
			}
		});
	}

	private void setMode() {
		if(GlobalVariables.mode == 0){
			GlobalVariables.mode = 1;
			tvMode.setText("随机播放");
			ivMode.setImageDrawable(getResources().getDrawable(R.drawable.random));
		}else{
			GlobalVariables.mode = 0;
			tvMode.setText("顺序循环");
			ivMode.setImageDrawable(getResources().getDrawable(R.drawable.circle));
		}
	}

	/*************************************************************
	 *
	 * 歌曲时间格式转换
	 */
	public String timeconvert(int time) {
		int min = 0, hour = 0;
		time /= 1000;
		min = time / 60;
		time %= 60;
		return min + ":" + time;
	}
	/*************************************************************
	 *
	 * 上一首
	 */
	public void click_last(View view) {
		// 如果当前播放歌曲是第一首，就跳到最后一首
		if (position == 0) {
			position = GlobalVariables.medias.size() - 1;

		} else {
			position -= 1;
		}
		music_play(position, MyConstant.ACTION_LAST);
	}

	/**************************************************************
	 *
	 * 播放/暂停
	 */
	public void click_pause(View view) {
		if (flag) {
			// 发送播放广播
			sendBroadcastToService(MyConstant.ACTION_PLAY, 0, null);
			control.setImageResource(R.drawable.apollo_holo_dark_pause);
		} else {
			// 发送暂停广播
			sendBroadcastToService(MyConstant.ACTION_PAUSE, 0, null);
			control.setImageResource(R.drawable.apollo_holo_dark_play);
		}
		flag = !flag;
	}

	/**************************************************************
	 *
	 * 下一首
	 */
	public void click_next(View view) {
		// 如果当前播放歌曲的位置是最后一首，就跳到第一首
		if(GlobalVariables.mode == 0){
			if (position == GlobalVariables.medias.size() - 1) {
				position = 0;
			} else {
				position += 1;
			}
		}else{
			if(GlobalVariables.medias.size() == 0){
				position = 0;
			}else{
				Random r = new Random(System.currentTimeMillis());
				position = r.nextInt(GlobalVariables.medias.size());
			}
		}
		music_play(position, MyConstant.ACTION_NEXT);
	}

	/*********************************************************************
	 *
	 * @param index
	 *            歌曲的索引下标
	 * @param action
	 *            按键动作
	 */
	public void music_play(int index, String action) {
		Intent broadcast = new Intent();
		broadcast.setAction(action);
		broadcast.putExtra("index", index);
		sendBroadcast(broadcast);
		if (flag) {
			flag = !flag;
		}
		finish();
		Intent intent = new Intent(this,PlayActivity.class);
		intent.putExtra("position",position);
		startActivity(intent);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		GlobalVariables.back = true;
	}

	/********************************************************
	 *
	 * 设置播放界面歌曲的相关信息
	 */
	public void setPlayText(int index) {
		play_Title.setText(GlobalVariables.medias.get(index).getName());
		play_Artist.setText(GlobalVariables.medias.get(index).getSinger());
	}
	/************************************************************
	 *
	 * 播放界面广播接收者
	 */
	public class MyBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (MyConstant.ACTION_PlAYING_STATE.equals(intent.getAction())) {

				// 收取服务开始播放的广播
				int index = intent.getIntExtra("media", 0);// 拿到服务发过来播放当前歌曲下标
				position = index;

				// 设置下标信息
				setPlayText(index);

				// 收到播放信息，改变按钮标识
				control.setImageResource(R.drawable.apollo_holo_dark_pause);

				// 设置当前音乐总时长
				music_always_time.setText(timeconvert(GlobalVariables.medias.get(index).getDuration()));

				// 设置当前歌曲进度条最大值
				sb_music.setMax(GlobalVariables.medias.get(index).getDuration());

				// 通过下标拿到当前服务播放歌曲的专辑图片所在资源中的id
				int album_id = GlobalVariables.medias.get(index).getAlbum_id();

				Log.v("TAG", "album_id有没有:" + album_id + "");

				// 再通过资源id拿到专辑的实际路径
				String albumArt = getAlbumArt(album_id);
				Log.v("TAG", "专辑实际路径有没有:" + albumArt);

				// 如果专辑路径是空，就设置默认图片
				if (albumArt == null) {
					iv_special.setImageResource(R.drawable.me);
				} else {
					// 用BitmapFactory.decodeFile拿到具体位图

					Bitmap btm = BitmapFactory.decodeFile(albumArt);
					Log.v("TAG", "bm有没有:" + btm);
					if (btm != null) {
						// 设置图片格式
						BitmapDrawable bmpDraw = new BitmapDrawable(btm);
						Log.v("TAG", "bmpDraw有没有:" + bmpDraw);
						// 设置专辑图片
						iv_special.setImageDrawable(bmpDraw);
					} else {
						iv_special.setImageResource(R.drawable.me);
					}

				}
				// 当服务开始播放音乐后，将专辑图片添加旋转动画效果
				Animation operatingAnim = AnimationUtils.loadAnimation(PlayActivity.this, R.anim.tip);
				LinearInterpolator lin = new LinearInterpolator();
				operatingAnim.setInterpolator(lin);

				if (operatingAnim != null) {
					iv_special.startAnimation(operatingAnim);
				}

			} else if (MyConstant.ACTION_SERVICR_PUASE.equals(intent.getAction())) {

				// 收歌曲暂停广播，移除专辑imageView的动画
				iv_special.clearAnimation();

			} else if (MyConstant.ACTION_MUSIC_PLAN.equals(intent.getAction())) {

				// 收到服务发送的播放歌曲进度的意图
				int playerPosition = intent.getIntExtra("playerPosition", 0);
				String playerTime = timeconvert(playerPosition);
				Log.v("jindu", "服务发来的进度条值:" + playerTime);
				sb_music.setProgress(playerPosition);// 设置进度条进度
				music_current_time.setText(playerTime);// 设置进度条进度时间
				sb_music.invalidate();// 自动刷屏（动起来）

			} else if (MyConstant.ACTION_PLAY.equals(intent.getAction())) {
				Log.v("TAG", "播放有没有:" + intent.getAction());
				if (flag) {
					control.setImageResource(R.drawable.apollo_holo_dark_pause);

				}else {
					control.setImageResource(R.drawable.apollo_holo_dark_play);
				}
				flag = !flag;

			}
		}
		/*********************************************************
		 *
		 * 获取专辑图片实际地址方法
		 */
		private String getAlbumArt(int album_id) {
			String mUriAlbums = "content://media/external/audio/albums";
			String[] projection = new String[] { "album_art" };
			Cursor cur = getContentResolver()
					.query(Uri.parse(mUriAlbums + "/" + Integer.toString(album_id)), projection, null, null, null);
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

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {

	}

	/******************************************************************
	 *
	 * 给服务发送广播方法
	 */
	public void sendBroadcastToService(String action, int intExtra, String stringExtra) {
		Intent broadcast = new Intent();
		broadcast.setAction(action);
		broadcast.putExtra("index", intExtra);
		broadcast.putExtra("date", stringExtra);
		sendBroadcast(broadcast);
	}

	/************************************************************
	 *
	 * 手动调节进度条
	 */
	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {

		int mediaPlayer = (seekBar.getProgress());
		sendBroadcastToService(MyConstant.ACTION_PLAN_CURRENT, mediaPlayer, null);
	}
}
