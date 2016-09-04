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
package com.taobao.tddl.parser.recognizer.mysql.syntax;

import com.taobao.tddl.parser.ast.ASTNode;
import com.taobao.tddl.parser.visitor.MySQLOutputASTVisitor;

import junit.framework.TestCase;

public abstract class AbstractSyntaxTest extends TestCase {

    private static final boolean debug = false;

    protected String output2MySQL(ASTNode node, String sql) {
        StringBuilder sb = new StringBuilder(sql.length());
        node.accept(new MySQLOutputASTVisitor(sb));
        if (debug) {
            System.out.println(getClass().getName() + "'s testcase: ");
            System.out.println("    " + sql);
            System.out.println("==> " + sb);
            System.out.println("--------------------------------------------------");
        }
        return sb.toString();
    }

}
