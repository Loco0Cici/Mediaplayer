package com.play.uitl;

import com.play.entity.Media;

import java.util.ArrayList;
import java.util.List;

public class GlobalVariables {
	public final static String ROOT_PATH = android.os.Environment.getExternalStorageDirectory() + "/com.my/";// 根目录
	public final static String IMAGE_FLODER = "/image/";// 图片文件夹
	public final static String AUDIO_FOLDER = "/audio/";// 语音文件夹
	public final static String AVATAR_FOLDER = ROOT_PATH + "avatar/";// 联系人头像文件夹
	public final static String HTML_FOLDER = ROOT_PATH + "html/";// 网页文件夹
	public final static String LOG_FOLDER = ROOT_PATH + "log/";// 日志文件夹
	public final static String APK_PATH = ROOT_PATH + "apk/";// 系统安装包路径
	public final static String TEMP_PATH = ROOT_PATH + "temp/";// 临时目录
	public final static List<Media> medias = new ArrayList<>();// 临时目录
	public static boolean back = false;
	public static int mode = 0;
}
