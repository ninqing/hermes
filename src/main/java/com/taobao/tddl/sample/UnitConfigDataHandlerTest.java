package com.taobao.tddl.sample;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import com.taobao.tddl.config.ConfigDataListener;
import com.taobao.tddl.config.impl.UnitConfigDataHandler;

public class UnitConfigDataHandlerTest extends UnitConfigDataHandler {
	public static Map<String, String> meta = new HashMap<String, String>();
	static {
		meta.put("com.taobao.tddl.v1_test_app_dbgroups", "tddl_test_0,tddl_test_1");
		{   //group key
			meta.put("com.taobao.tddl.jdbc.group_V2.4.1_tddl_test_0", "Tddl_3306_0_tesT:R10W10,Tddl_3307_0_tesT:R0W0");
			{   //global key
				meta.put("com.taobao.tddl.atom.global.Tddl_3306_0_tesT", "ip=127.0.0.1\nport=3306\ndbName=tddl_test_0\ndbType=mysql\ndbStatus=RW");
				//app key
				meta.put("com.taobao.tddl.atom.app.test_app.Tddl_3306_0_tesT", "userName=tddl\nminPoolSize=10\ninitPoolSize=15\nmaxPoolSize=20\nidleTimeout=10\nblockingTimeout=5\npreparedStatementCacheSize=15\nconnectionProperties=rewriteBatchedStatements=true&characterEncoding=UTF8&connectTimeout=1000&autoReconnect=true&socketTimeout=12000");
				
				//global key
				meta.put("com.taobao.tddl.atom.global.Tddl_3307_0_tesT", "ip=127.0.0.1\nport=3307\ndbName=tddl_test_0\ndbType=mysql\ndbStatus=RW");
				//app key
				meta.put("com.taobao.tddl.atom.app.test_app.Tddl_3307_0_tesT", "userName=tddl\nminPoolSize=10\ninitPoolSize=15\nmaxPoolSize=20\nidleTimeout=10\nblockingTimeout=5\npreparedStatementCacheSize=15\nconnectionProperties=rewriteBatchedStatements=true&characterEncoding=UTF8&connectTimeout=1000&autoReconnect=true&socketTimeout=12000");
				
				//pass key
				meta.put("com.taobao.tddl.atom.passwd.tddl_test_0.mysql.tddl", "encPasswd=tddl");
			}
			
			//group key
			meta.put("com.taobao.tddl.jdbc.group_V2.4.1_tddl_test_1", "Tddl_3306_1_tesT:R10W10,Tddl_3307_1_tesT:R0W0");
			{   //global key
				meta.put("com.taobao.tddl.atom.global.Tddl_3306_1_tesT", "ip=127.0.0.1\nport=3306\ndbName=tddl_test_1\ndbType=mysql\ndbStatus=RW");
				//app key
				meta.put("com.taobao.tddl.atom.app.test_app.Tddl_3306_1_tesT", "userName=tddl\nminPoolSize=10\ninitPoolSize=15\nmaxPoolSize=20\nidleTimeout=10\nblockingTimeout=5\npreparedStatementCacheSize=15\nconnectionProperties=rewriteBatchedStatements=true&characterEncoding=UTF8&connectTimeout=1000&autoReconnect=true&socketTimeout=12000");
				
				//global key
				meta.put("com.taobao.tddl.atom.global.Tddl_3307_1_tesT", "ip=127.0.0.1\nport=3307\ndbName=tddl_test_1\ndbType=mysql\ndbStatus=RW");
				//app key
				meta.put("com.taobao.tddl.atom.app.test_app.Tddl_3307_1_tesT", "userName=tddl\nminPoolSize=10\ninitPoolSize=15\nmaxPoolSize=20\nidleTimeout=10\nblockingTimeout=5\npreparedStatementCacheSize=15\nconnectionProperties=rewriteBatchedStatements=true&characterEncoding=UTF8&connectTimeout=1000&autoReconnect=true&socketTimeout=12000");
				
				//pass key
				meta.put("com.taobao.tddl.atom.passwd.tddl_test_1.mysql.tddl", "encPasswd=tddl");
			}
		
		}
		
		
	}

	@Override
	public String getData(long timeout, String strategy) {
		System.out.println(this.dataId);
		String result = meta.get(this.dataId);
		System.out.println(result);
		return result;
	}

	@Override
	public String getNullableData(long timeout, String strategy) {
		System.out.println("getNullableData");
		return null;
	}

	@Override
	public void addListener(ConfigDataListener configDataListener, Executor executor) {
		System.out.println("addListener");

	}

	@Override
	public void addListeners(List<ConfigDataListener> configDataListenerList, Executor executor) {
		System.out.println("addListeners");

	}

}
