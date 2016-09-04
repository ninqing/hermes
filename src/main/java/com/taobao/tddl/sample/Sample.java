package com.taobao.tddl.sample;

import java.sql.SQLException;

import org.springframework.jdbc.core.JdbcTemplate;

import com.taobao.tddl.common.exception.TddlException;
import com.taobao.tddl.matrix.jdbc.TDataSource;

public class Sample {

    public static void main(String[] args) throws TddlException, SQLException {

        TDataSource ds = new TDataSource();

        ds.setAppName("test_app");
        ds.setRuleFile("rule.xml");
        ds.setDynamicRule(true);
        ds.init();

        System.out.println("init done");

        JdbcTemplate jt0 = new JdbcTemplate(ds);
		jt0.update("delete from test_table");
		System.out.println(jt0.update("insert into test_table values(1,'name1')"));
		System.out.println(jt0.update("insert into test_table values(2,'name1')"));
		System.out.println(jt0.update("insert into test_table values(3,'name1')"));
		System.out.println(jt0.update("insert into test_table values(4,'name1')"));
		
    }

}
