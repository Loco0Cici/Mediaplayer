package com.play.uitl;

import java.util.Comparator;

import com.play.entity.Media;


public class PinyinComparator implements Comparator<Media> {

	public int compare(Media o1, Media o2) {
		if (o1.getKey().equals("@")
				|| o2.getKey().equals("#")) {
			return -1;
		} else if (o1.getKey().equals("#")
				|| o2.getKey().equals("@")) {
			return 1;
		} else {
			return o1.getKey().compareTo(o2.getKey());
		}
	}

}
