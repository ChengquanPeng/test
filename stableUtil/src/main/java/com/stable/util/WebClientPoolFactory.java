package com.stable.util;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.ScriptException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.javascript.JavaScriptErrorListener;

public class WebClientPoolFactory {
	/**
	 * 对象池
	 */
	private final static GenericObjectPool<WebClient> pool;
	/**
	 * 对象池的参数设置
	 */
	private final static GenericObjectPoolConfig<WebClient> config;

	/**
	 * 对象池每个key最大实例化对象数
	 */
	private final static int TOTAL_PERKEY = 10;
	/**
	 * 对象池每个key最大的闲置对象数
	 */
	private final static int IDLE_PERKEY = 1;

	static {
		config = new GenericObjectPoolConfig<WebClient>();
		config.setMaxTotal(TOTAL_PERKEY);
		config.setMaxIdle(IDLE_PERKEY);
		/** 支持jmx管理扩展 */
		config.setJmxEnabled(true);
		config.setJmxNamePrefix("myPoolProtocol");
		/** 保证获取有效的池对象 */
		config.setTestOnBorrow(true);
		config.setTestOnReturn(true);
		pool = new GenericObjectPool<WebClient>(new MyBeanPooledFactory(), config);
	}

	/**
	 * 从对象池中获取对象
	 */
	public static WebClient getWebClient() {
		try {
			return pool.borrowObject();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 归还对象
	 */
	public static void returnBean(WebClient webclient) {
		pool.returnObject(webclient);
	}

	/**
	 * 关闭对象池
	 * 
	 * public synchronized void close() { if (pool != null && !pool.isClosed()) {
	 * pool.close(); pool = null; } }
	 */

}

/**
 * 对象工厂
 */
class MyBeanPooledFactory extends BasePooledObjectFactory<WebClient> {
	/**
	 * 创建对象
	 */
	@Override
	public WebClient create() throws Exception {
		WebClient webClient = new WebClient();
		webClient = new WebClient(BrowserVersion.CHROME);// 新建一个模拟谷歌Chrome浏览器的浏览器客户端对象
		webClient.getOptions().setThrowExceptionOnScriptError(false);// 当JS执行出错的时候是否抛出异常, 这里选择不需要
		webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);// 当HTTP的状态非200时是否抛出异常, 这里选择不需要
		webClient.getOptions().setActiveXNative(false);
		webClient.getOptions().setCssEnabled(false);// 是否启用CSS, 因为不需要展现页面, 所以不需要启用
		webClient.getOptions().setJavaScriptEnabled(true); // 很重要，启用JS。有些网站要开启！
		webClient.setAjaxController(new NicelyResynchronizingAjaxController());// 很重要，设置支持AJAX
		webClient.getOptions().setTimeout(30000);
		webClient.waitForBackgroundJavaScript(3000);
		webClient.setJavaScriptErrorListener(new JavaScriptErrorListener() {
			@Override
			public void timeoutError(HtmlPage page, long allowedTime, long executionTime) {
			}
			
			@Override
			public void scriptException(HtmlPage page, ScriptException scriptException) {
			}
			
			@Override
			public void malformedScriptURL(HtmlPage page, String url, MalformedURLException malformedURLException) {
			}
			
			@Override
			public void loadScriptError(HtmlPage page, URL scriptUrl, Exception exception) {
			}

			@Override
			public void warn(String message, String sourceName, int line, String lineSource, int lineOffset) {
				// TODO Auto-generated method stub
				
			}
		});
		return webClient;
	}

	@Override
	public PooledObject<WebClient> wrap(WebClient value) {
		return new DefaultPooledObject<WebClient>(value);
	}

	/**
	 * 销毁
	 */
	@Override
	public void destroyObject(PooledObject<WebClient> p) throws Exception {
		/** 杀死他 */
		WebClient webClient = p.getObject();
		webClient.getCurrentWindow().getJobManager().removeAllJobs();
		webClient.close();
		System.gc();
	}

}
