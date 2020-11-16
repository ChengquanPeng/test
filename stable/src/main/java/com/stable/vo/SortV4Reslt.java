package com.stable.vo;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.stable.service.realtime.ModelSortV4;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SortV4Reslt {
	private Map<Integer, List<ModelSortV4>> ln = new ConcurrentHashMap<Integer, List<ModelSortV4>>();
//	private List<List<ModelSortV4>> ln = new ArrayList<List<ModelSortV4>>();
//	private List<ModelSortV4> l1 = Collections.synchronizedList(new LinkedList<ModelSortV4>());
//	private List<ModelSortV4> l2 = Collections.synchronizedList(new LinkedList<ModelSortV4>());
//	private List<ModelSortV4> l3 = Collections.synchronizedList(new LinkedList<ModelSortV4>());
//	private List<ModelSortV4> l4 = Collections.synchronizedList(new LinkedList<ModelSortV4>());

	public SortV4Reslt() {
	}

	public synchronized void add(ModelSortV4 v) {
		List<ModelSortV4> lx = ln.get(v.getLevel());
		if (lx == null) {
			lx = Collections.synchronizedList(new LinkedList<ModelSortV4>());
			ln.put(v.getLevel(), lx);
		}
		lx.add(v);
	}

	public int size() {
		int i = 0;
		for (List<ModelSortV4> z : ln.values()) {
			i += z.size();
		}
		return i;
	}

	public void sort() {
		for (List<ModelSortV4> z : ln.values()) {
			Collections.sort(z, new Comparator<ModelSortV4>() {
				@Override
				public int compare(ModelSortV4 o1, ModelSortV4 o2) {
					return o2.getScore() - o1.getScore();
				}
			});
		}
	}
}
