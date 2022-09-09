package com.stable.service.datamv;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.stable.es.dao.base.EsConceptDao;
import com.stable.es.dao.base.UserDao;
import com.stable.utils.SpringUtil;
import com.stable.vo.bus.Concept;
import com.stable.vo.bus.UserInfo;

@Service
@SuppressWarnings("rawtypes")
public class DataMovingNewer implements InitializingBean {

	public Map<String, ElasticsearchRepository> tableMap = new HashMap<String, ElasticsearchRepository>();
	public Map<String, Class> tableClz = new HashMap<String, Class>();

	@Override
	public void afterPropertiesSet() throws Exception {
		tableMap.put("UserInfo", SpringUtil.getBean(UserDao.class));
		tableClz.put("UserInfo", UserInfo.class);

		tableMap.put("Concept", SpringUtil.getBean(EsConceptDao.class));
		tableClz.put("Concept", Concept.class);
	}

	@Autowired
	private DataMovingProvider dataMovingProvider; // TODO

	private Dw fetchData(String tableName, int pageNum, int pageSize) {
		System.err.println("=fetchData=");
		return dataMovingProvider.getData(tableName, pageNum, pageSize);// TODO
	}

//	@javax.annotation.PostConstruct
	public void test() throws Exception {
		afterPropertiesSet();
		String tableName = "Concept";
		int pageSize = 100;

		Dw dw = fetchData(tableName, 1, pageSize);
		if (dw.tableSize > 0) {
			long tot = dw.tableSize;
			int i = 2;
			while (true) {
				insert(tableName, dw);
				tot = tot - dw.batchSize;
				if (tot <= 0) {
					break;
				}
				dw = fetchData(tableName, i, pageSize);
				i++;
			}
		}

		System.err.println("=end=");
//		System.exit(0);
	}

	// TODO
	@SuppressWarnings("unchecked")
	public void insert(String tableName, Dw dw) {
		String json = JSON.toJSONString(dw);
		System.err.println(json);

		Dw<?> res = JSON.toJavaObject(JSON.parseObject(json), Dw.class);
		for (Object obj : res.getTableData()) {
			JSONObject jo = (JSONObject) obj;
			System.err.println(jo.toJavaObject(tableClz.get(tableName)));
		}
	}
}
