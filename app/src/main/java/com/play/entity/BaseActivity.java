/**
 * 
 */
package com.play.entity;

import com.play.uitl.MyApplication;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.os.PersistableBundle;

/**
 * @author ������Activity������������Զ���ӵ�Activity�����У����ڰ�ȫ�˳�
 */
@SuppressLint("NewApi")
public class BaseActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState,
			PersistableBundle persistentState) {
		MyApplication.getInstance().addActivity(this);
		super.onCreate(savedInstanceState, persistentState);
	}
}
