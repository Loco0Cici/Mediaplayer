package com.play.music;

import java.util.ArrayList;
import java.util.List;

import com.play.R;
import com.play.adapter.MySouListViewAdapter;
import com.play.entity.Media;
import com.play.entity.MyConstant;
import com.play.music.base.ManageActivity;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.ViewInject;
import org.xutils.x;

/**
 * 搜索类
 * */
@ContentView(R.layout.aty_search)
public class SearchActivity extends ManageActivity implements OnItemClickListener {
	@ViewInject(R.id.iv_back)
	public ImageView back;
	@ViewInject(R.id.searchView)
	private SearchView searchView;
	private List<Media> medias_search = new ArrayList<>();
	@ViewInject(R.id.search_list)
	private ListView lv_search;
	private MySouListViewAdapter listViewAdapter;
	private String query;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		x.view().inject(this);
		initView();
	}

	@Override
	protected void initView() {
		super.initView();
		back.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			// 当点击搜索按钮时触发该方法
			@Override
			public boolean onQueryTextSubmit(String query) {
				if(TextUtils.isEmpty(query)){
					Toast.makeText(SearchActivity.this,"请输入歌名",Toast.LENGTH_LONG).show();
					return false;
				}
				SearchActivity.this.query = query;
				doMySearch(query);
				return false;
			}

			// 当搜索内容改变时触发该方法
			@Override
			public boolean onQueryTextChange(String newText) {
				return false;
			}
		});
	}

	/***************************************************************
	 *
	 * 实际搜索是在这里实现的
	 * */
	private void doMySearch(String query) {
		medias_search.clear();
		if(listViewAdapter != null){
			listViewAdapter.notifyDataSetChanged();
		}
		CustomAsyncQueryHandler asyncQueryHandler = new CustomAsyncQueryHandler(getContentResolver());
		asyncQueryHandler.startQuery(0, lv_search, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, "title like ?",
				new String[] { "%" + query + "%" }, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
		lv_search.setOnItemClickListener(this);
	}

	/****
	 *
	 * 异步查询处理器
	 *
	 * */
	private class CustomAsyncQueryHandler extends AsyncQueryHandler {

		public CustomAsyncQueryHandler(ContentResolver cr) {
			super(cr);
		}

		@Override
		protected void onQueryComplete(int token, Object cookie, Cursor cursor) {

			for (int i = 0; i < cursor.getCount(); i++) {
				cursor.moveToNext();
				System.out.println(i);

				// 如果当前媒体库的文件大于500Kb，则是音乐文件

				if (cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.IS_MUSIC)) == 1) {

					// cursor.getColumnIndex:得到指定列名的索引号,就是说这个字段是第几列
					// cursor.getString(columnIndex) 可以得到当前行的第几列的值

					Media media = new Media();// 实例化媒体对象
					media.setId(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media._ID)));// 设置歌曲编号
					String name = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
					media.setName(name);// 设置得到歌曲标题

					media.setDuration(cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)));// 设置得到歌曲时长
					media.setSinger(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)));// 设置得到艺术家
					media.setUri(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA)));// 设置得到歌曲路径
					media.setAlbum_id(cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)));
					if(!TextUtils.isEmpty(name) && name.toLowerCase().contains(SearchActivity.this.query) && media.getDuration()>10){
						medias_search.add(media);
					}
				}
			}

			listViewAdapter = new MySouListViewAdapter(SearchActivity.this, medias_search);
			lv_search.setAdapter(listViewAdapter);
		}
	}

	/***********************************************************
	 *
	 * 单机搜索界面的listView条目
	 * */
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Intent broadcast = new Intent();
		broadcast.setAction(MyConstant.ACTION_LIST_SEARCH);
		broadcast.putExtra("id", medias_search.get(position).getId());
		sendBroadcast(broadcast);
	}
}