/**
 * 
 */
package com.play.uitl;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Application;

import org.xutils.BuildConfig;
import org.xutils.x;

public class MyApplication extends Application {
	private List<Activity> lists = new ArrayList<Activity>();
	private static MyApplication instance;

	private MyApplication() {
		CrashHandler crashHandler = CrashHandler.getInstance();
		crashHandler.init(this);
		x.Ext.init(this);
		x.Ext.setDebug(BuildConfig.DEBUG); // 是否输出debug日志, 开启debug会影响性能.
	}

	public static MyApplication getInstance() {
		if (instance == null) {
			instance = new MyApplication();
		}
		return instance;
	}

	public void addActivity(Activity activity) {
		lists.add(activity);
	}

	public void killActivity() {
		for (Activity activity : lists) {
			activity.finish();
		}
	}
}
