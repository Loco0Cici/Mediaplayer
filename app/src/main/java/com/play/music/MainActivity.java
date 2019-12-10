package com.play.music;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.github.angads25.filepicker.controller.DialogSelectionListener;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;
import com.github.angads25.filepicker.view.FilePickerDialog;
import com.play.R;
import com.play.adapter.MyListViewAdapter;
import com.play.adapter.MyViewPagerAdapter;
import com.play.entity.BaseActivity;
import com.play.entity.Media;
import com.play.entity.MyConstant;
import com.play.service.MusicService;
import com.play.uitl.CharacterParser;
import com.play.uitl.GlobalVariables;
import com.play.uitl.MyApplication;
import com.play.uitl.PinyinComparator;
import com.play.view.ActionSheetDialog;
import com.play.view.SideBar;

import static android.os.Environment.getExternalStorageDirectory;

public class MainActivity extends BaseActivity implements OnSeekBarChangeListener, OnItemClickListener {
    /**
     * flag 暂停还是继续的标志
     */
    private boolean flag = true;

    /**
     * ViewPager的view集合
     */
    private List<View> views; //

    /**
     * 歌曲所在的列表
     */
    private ListView listView;

    /**
     * viewPager中的view
     */
    private View view1, view2, list_popup;

    /**
     * viewPager的对象
     */
    private ViewPager viewPager;

    /**
     * 布局填充器
     */
    private LayoutInflater mInflater;

    /**
     * service：服务 broadcast：广播意图对象
     */
    private Intent service, broadcast;

    /**
     * 播放/暂停，控制按钮
     */
    private ImageView control;

    /**
     * 搜索按钮
     */
    private ImageView sou;
    private ImageView add;

    /**
     * 歌曲标记
     */
    private int position;

    /**
     * 泡泡窗口宽高
     */
    private static final int POPUP_WIDHT = 600;
    private static final int POPUP_HEIGHT = 400;

    /**
     * 获取手机屏幕宽高
     */
    private int screenHeight, screenWidth;

    /**
     * play_Title歌曲名 play_Artist艺术家 text_select被选中的字母
     * music_current_time歌曲当前播放的时间 music_always_time歌曲总长度
     */
    private TextView play_Title, play_Artist, text_select, music_current_time, music_always_time;

    private MyBroadcastReceiver broadcastReceiver;// 广播
    private IntentFilter filter;// 广播过滤器

    /**
     * iv_special播放界面的专辑imageView对象 Iv_select_bg当alphaView被选中时弹出的底色
     */
    private ImageView iv_special, Iv_select_bg;
    private TextView dialog;

    /**
     * 字母索引回调
     */
    // private Map<String, Integer> map = new HashMap<String, Integer>();

    /**
     * 字母索引view对象
     */
    private SideBar sideBarView;
    private MyListViewAdapter adapter;
    private CharacterParser characterParser;
    private PinyinComparator pinyinComparator;

    /**
     * 音频文件对象
     */
    private Media media;// 音频文件对象

    /**
     * 播放进度条
     */
    private SeekBar sb_music;

    /**
     * time第一次按back时间；
     */
    private long time = 0; //
    private ImageView about;
    private ImageView iv_list;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.aty_main);
        init();

        sou.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Log.v("TAG", "点击了搜索");
                onSearchRequested();// 激活搜索对话框
                Intent intent = new Intent(MainActivity.this, SearchActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {

        super.onResume();
        if (GlobalVariables.back) {
            control.setImageResource(R.drawable.apollo_holo_dark_pause);
            flag = true;
            GlobalVariables.back = false;
        }
    }

    /*********************************************************
     *
     * 控件初始化
     */
    public void init() {
        viewPager = (ViewPager) findViewById(R.id.vp);
        views = new ArrayList<View>();
        mInflater = LayoutInflater.from(this);
        control = (ImageView) findViewById(R.id.control);
        sou = (ImageView) findViewById(R.id.search);
        add = (ImageView) findViewById(R.id.iv_add);
        about = (ImageView) findViewById(R.id.about);
        iv_list = (ImageView) findViewById(R.id.iv_list);
        about.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AboutActivity.class));
            }
        });
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addSongs();
            }
        });
        // 创建字母索引工具
        characterParser = CharacterParser.getInstance();
        pinyinComparator = new PinyinComparator();

        // 创建一个广播
        broadcastReceiver = new MyBroadcastReceiver();

        // 创建广播过滤器
        filter = new IntentFilter();
        filter.addAction(MyConstant.ACTION_PlAYING_STATE);
        filter.addAction(MyConstant.ACTION_SERVICR_PUASE);
        filter.addAction(MyConstant.ACTION_MUSIC_PLAN);
        filter.addAction(MyConstant.ACTION_PLAY);
        registerReceiver(broadcastReceiver, filter);

        load();
        addView();// 拿到所有媒体文件的路径并添加到资源集合中

        // 设置viewpager的适配器
        MyViewPagerAdapter mvp = new MyViewPagerAdapter(views);
        viewPager.setAdapter(mvp);

        viewPager.setOnPageChangeListener(new OnPageChangeListener() {

            @Override
            public void onPageSelected(int arg0) {

            }

            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {

            }

            @Override
            public void onPageScrollStateChanged(int arg0) {
                // 判断字母索引0显示和1隐藏
                MyConstant.viewPage = 1;
            }
        });

        /**********************************************************************
         *
         * 第一次进入，点击播放按钮，直接播放第一首歌曲 启动服务
         */
        Intent service = new Intent(this, MusicService.class);
        service.putParcelableArrayListExtra("medias", (ArrayList<? extends Parcelable>) GlobalVariables.medias);
        startService(service);
    }

    private void addSongs() {
        DialogProperties properties = new DialogProperties();
        properties.selection_mode = DialogConfigs.MULTI_MODE;
        properties.selection_type = DialogConfigs.FILE_SELECT;
        properties.root = new File(DialogConfigs.DEFAULT_DIR);
        properties.extensions = new String[]{"mp3", "aac", "wma"};
        FilePickerDialog dialog = new FilePickerDialog(MainActivity.this, properties);
        dialog.setTitle("添加歌曲");
        dialog.setDialogSelectionListener(new DialogSelectionListener() {
            @Override
            public void onSelectedFilePaths(String[] files) {
                if (files != null) {
                    for (String file : files) {
                        addSong(file);
                    }
                }
            }
        });
        dialog.show();
    }

    private void addSong(String file) {

        try {
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            mmr.setDataSource(file);
            String title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            String album = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
            String artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            String duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);// 播放时长单位为毫秒 
            int dua = Integer.parseInt(duration);
            // 汉字转换成拼音
            Media m = new Media();
            m.setId(System.currentTimeMillis()+"");
            m.setDuration(dua);
            m.setSinger(artist);
//            Uri uri = Uri.fromFile(new File(file));
            m.setUri(file);
            m.setAlbum_id(0);
            m.setName(title);
            GlobalVariables.medias.add(m);

            Log.v("TAG", "拿到的音乐信息：" + m.toString());

            String key = characterParser.getSelling(title);
            Log.v("TAG", "拼音：" + key);
            String sortString = key.substring(0, 1).toUpperCase();

            // 正则表达式，判断首字母是否是英文字母
            if (sortString.matches("[A-Z]")) {
                m.setKey(sortString.toUpperCase());
            } else {
                m.setKey("#");
            }
            adapter.notifyDataSetChanged();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

    }

    /****************************************************************
     *
     * 实例化ViewPager
     *
     * 实例化ListView
     */
    private void addView() {

        /*********************************************************
         * 字母索引
         */
        view1 = mInflater.inflate(R.layout.list_view, null);

        sideBarView = (SideBar) view1.findViewById(R.id.SideBarView);// 拿到alphaView所在的id
        dialog = (TextView) view1.findViewById(R.id.dialog);
        sideBarView.setTextView(dialog);

        sideBarView.setOnTouchingLetterChangedListener(new SideBar.OnTouchingLetterChangedListener() {

            @Override
            public void onTouchingLetterChanged(String s) {
                int position = adapter.getPositionForSection(s.charAt(0));
                if (position != -1) {
                    listView.setSelection(position);
                }
            }
        });

        listView = (ListView) view1.findViewById(R.id.lv);
        listView.setOnItemLongClickListener(new OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                // 这里要利用adapter.getItem(position)来获取当前position所对应的对象
                Toast.makeText(getApplication(), ((Media) adapter.getItem(position)).getName(), Toast.LENGTH_SHORT)
                        .show();
                return false;
            }
        });

        Collections.sort(GlobalVariables.medias, pinyinComparator);
        adapter = new MyListViewAdapter(this, GlobalVariables.medias);
        listView.setAdapter(adapter);
        views.add(view1);

        /*********************************************************
         * */

        listView.setOnItemClickListener(this);// 添加ListView单击触控事件
        itemOnLongClick();

//		views.add(view2);

        setPlayText(0);// 设置默认播放为第一首歌

    }

    private void itemOnLongClick() {
        listView.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {

                    public void onCreateContextMenu(ContextMenu menu, View v,
                                                    ContextMenu.ContextMenuInfo menuInfo) {
                        menu.add(0, 0, 0, "删除");
                    }
                });
    }

    // 长按菜单响应函数
    public boolean onContextItemSelected(MenuItem item) {

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
                .getMenuInfo();
        int id = (int) info.id;// 这里的info.id对应的就是数据库中_id的值

        switch (item.getItemId()) {
            case 0:
                GlobalVariables.medias.remove(id);
                adapter.notifyDataSetChanged();
                if(id == position++){
                    // 如果当前播放歌曲的位置是最后一首，就跳到第一首
                    if (position >= GlobalVariables.medias.size() - 1) {
                        position = 0;
                    } else {
                        position += 1;
                    }
                    music_play(position, MyConstant.ACTION_NEXT);
                }
                break;
            default:
                break;
        }

        return super.onContextItemSelected(item);

    }

    /**************************************************************
     *
     * 加载手机存储卡中媒体文件
     */
    private void load() {
        List<Media> mList = new ArrayList<Media>();

        ContentResolver resolver = this.getContentResolver();

        Cursor cursor = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null,
                MediaStore.Audio.Media.DEFAULT_SORT_ORDER);

        if (cursor != null && cursor.getCount() > 0) {
        }
        for (int i = 0; i < cursor.getCount(); i++) {
            cursor.moveToNext();

            // 如果当前媒体库的文件大于500Kb，则是音乐文件
            if (cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)) >= 500 * 1024
                    && cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.IS_MUSIC)) == 1) {


                // 设置歌曲编号
                String id = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
                // 设置得到歌曲时长
                int duration = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
                // 设置得到艺术家
                String singer = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                // 设置得到歌曲路径
                String uri = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                // 设置得到专辑图片ID
                int album_id = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID));
                // 设置得到歌曲标题
                String name = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));

                // 汉字转换成拼音
                Media m = new Media();
                m.setId(id);
                m.setDuration(duration);
                m.setSinger(singer);
                m.setUri(uri);
                m.setAlbum_id(album_id);
                m.setName(name);
                GlobalVariables.medias.add(m);

                Log.v("TAG", "拿到的音乐信息：" + m.toString());

                String key = characterParser.getSelling(name);
                Log.v("TAG", "拼音：" + key);
                String sortString = key.substring(0, 1).toUpperCase();

                // 正则表达式，判断首字母是否是英文字母
                if (sortString.matches("[A-Z]")) {
                    m.setKey(sortString.toUpperCase());
                } else {
                    m.setKey("#");
                }
                mList.add(m);
            }
        }

        // 拿到屏幕高宽
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);
        screenWidth = outMetrics.widthPixels;
        screenHeight = outMetrics.heightPixels;
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
        if(GlobalVariables.medias == null || GlobalVariables.medias.size() == 0){
            Toast.makeText(this,"请先添加歌曲",Toast.LENGTH_LONG).show();
            return;
        }
        broadcast = new Intent();
        broadcast.setAction(action);
        broadcast.putExtra("index", index);
        sendBroadcast(broadcast);
        if (flag) {
            flag = !flag;
        }
    }

    /*************************************************************
     *
     * ListView单击触控事件
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        this.position = position;
        music_play(position, MyConstant.ACTION_LIST);
        Intent intent = new Intent(this, PlayActivity.class);
        intent.putExtra("position", position);
        startActivity(intent);
    }


     /*
     * 设置播放界面歌曲的相关信息
     */
    public void setPlayText(int index) {
//		play_Title.setText(GlobalVariables.medias.get(index).getName());
//		play_Artist.setText(GlobalVariables.medias.get(index).getSinger());
    }

    /************************************************************
     *
     * 播放界面广播接收者
     */
    public class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (MyConstant.ACTION_PlAYING_STATE.equals(intent.getAction())) {

            } else if (MyConstant.ACTION_SERVICR_PUASE.equals(intent.getAction())) {

            } else if (MyConstant.ACTION_MUSIC_PLAN.equals(intent.getAction())) {

            } else if (MyConstant.ACTION_PLAY.equals(intent.getAction())) {
                Log.v("TAG", "播放有没有:" + intent.getAction());
                if (flag) {
                    control.setImageResource(R.drawable.apollo_holo_dark_pause);

                } else {
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
            String[] projection = new String[]{"album_art"};
            Cursor cur = MainActivity.this.getContentResolver()
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

    /******************************************************************
     *
     * 给服务发送广播方法
     */
    public void sendBroadcastToService(String action, int intExtra, String stringExtra) {
        broadcast = new Intent();
        broadcast.setAction(action);
        broadcast.putExtra("index", intExtra);
        broadcast.putExtra("date", stringExtra);
        sendBroadcast(broadcast);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**************************************************************
     *
     * 双击返回桌面方法和菜单监控
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if ((System.currentTimeMillis() - time > 1000)) {
                Toast.makeText(this, "再按一次返回桌面", Toast.LENGTH_SHORT).show();
                time = System.currentTimeMillis();
            } else {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_HOME);

                startActivity(intent);
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_MENU) {
            new ActionSheetDialog(MainActivity.this).builder().setTitle("菜单").setCancelable(false)
                    .setCanceledOnTouchOutside(false)
                    .addSheetItem("退出应用", ActionSheetDialog.SheetItemColor.Red, new ActionSheetDialog.OnSheetItemClickListener() {
                        @Override
                        public void onClick(int which) {
                            // MusicService.isPlay_No = false;
                            Intent intent = new Intent(MainActivity.this, MusicService.class);
                            stopService(intent);
                            MyApplication.getInstance().killActivity();
                            MainActivity.this.finish();
                        }
                    }).show();

            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }

    }
}
