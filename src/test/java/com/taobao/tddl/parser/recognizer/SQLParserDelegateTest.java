/*
 * Copyright 1999-2012 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * (created at 2012-3-13)
 */
package com.taobao.tddl.parser.recognizer;

import java.sql.SQLSyntaxErrorException;

import org.junit.Assert;

import com.taobao.tddl.parser.ast.stmt.SQLStatement;
import com.taobao.tddl.parser.ast.stmt.dml.DMLSelectStatement;
import com.taobao.tddl.parser.recognizer.SQLParserDelegate;
import com.taobao.tddl.parser.recognizer.mysql.syntax.AbstractSyntaxTest;

/**
 * @author <a href="mailto:shuo.qius@alibaba-inc.com">QIU Shuo</a>
 */
public class SQLParserDelegateTest extends AbstractSyntaxTest {

    public void testProperlyEnd() throws SQLSyntaxErrorException {
        String sql = "select * from tb1;";
        SQLStatement stmt = SQLParserDelegate.parse(sql);
        Assert.assertEquals(DMLSelectStatement.class, stmt.getClass());

        sql = "select * from tb1 ;;;  ";
        stmt = SQLParserDelegate.parse(sql);
        Assert.assertEquals(DMLSelectStatement.class, stmt.getClass());

        sql = "select * from tb1 /***/  ";
        stmt = SQLParserDelegate.parse(sql);
        Assert.assertEquals(DMLSelectStatement.class, stmt.getClass());

        sql = "select * from tb1 ,  ";
        try {
            stmt = SQLParserDelegate.parse(sql);
            Assert.fail("should detect inproperly end");
        } catch (SQLSyntaxErrorException e) {
        }

        sql = "select * from tb1 ;,  ";
        try {
            stmt = SQLParserDelegate.parse(sql);
            Assert.fail("should detect inproperly end");
        } catch (SQLSyntaxErrorException e) {
        }

    }
}
