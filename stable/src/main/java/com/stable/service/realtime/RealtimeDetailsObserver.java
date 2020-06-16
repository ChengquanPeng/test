package com.stable.service.realtime;

import java.util.Observable;
import java.util.Observer;

public class RealtimeDetailsObserver implements Observer {

	@Override
	public void update(Observable o, Object arg) {
		if (o instanceof RealtimeDetailsObservable) {
			System.out.println("购物者x观察到房价已调整为：" + arg);
		}

	}

}
