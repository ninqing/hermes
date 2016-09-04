package com.taobao.tddl.sample;

import org.springframework.jdbc.core.JdbcTemplate;

import com.taobao.tddl.group.jdbc.TGroupDataSource;

public class GroupSample {
	public static void main(String[] args) throws Throwable {
		TGroupDataSource tg0 = new TGroupDataSource();
		tg0.setAppName("test_app");
		tg0.setDbGroupKey("test_group_key1");
		tg0.init();
		JdbcTemplate jt0 = new JdbcTemplate(tg0);
		jt0.update("delete from t1");
		System.out.println(jt0.update("insert into t1 values(123,'name1')"));
		tg0.destroyDataSource();

		TGroupDataSource tg1 = new TGroupDataSource();
		tg1.setAppName("test_app");
		tg1.setDbGroupKey("test_group_key1");
		tg1.init();
		JdbcTemplate jt1 = new JdbcTemplate(tg1);
		jt1.update("delete from t1");
		System.out.println(jt1.update("insert into t1 values(123,'name1')"));
		tg1.destroyDataSource();
	}
}
