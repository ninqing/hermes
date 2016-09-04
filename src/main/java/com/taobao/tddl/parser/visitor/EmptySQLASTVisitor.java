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
 * (created at 2011-11-9)
 */
package com.taobao.tddl.parser.visitor;

import java.util.Collection;

import com.taobao.tddl.parser.ast.ASTNode;
import com.taobao.tddl.parser.ast.expression.BinaryOperatorExpression;
import com.taobao.tddl.parser.ast.expression.PolyadicOperatorExpression;
import com.taobao.tddl.parser.ast.expression.UnaryOperatorExpression;
import com.taobao.tddl.parser.ast.expression.comparison.BetweenAndExpression;
import com.taobao.tddl.parser.ast.expression.comparison.ComparisionEqualsExpression;
import com.taobao.tddl.parser.ast.expression.comparison.ComparisionIsExpression;
import com.taobao.tddl.parser.ast.expression.comparison.ComparisionNullSafeEqualsExpression;
import com.taobao.tddl.parser.ast.expression.comparison.InExpression;
import com.taobao.tddl.parser.ast.expression.logical.LogicalAndExpression;
import com.taobao.tddl.parser.ast.expression.logical.LogicalOrExpression;
import com.taobao.tddl.parser.ast.expression.misc.InExpressionList;
import com.taobao.tddl.parser.ast.expression.misc.UserExpression;
import com.taobao.tddl.parser.ast.expression.primary.CaseWhenOperatorExpression;
import com.taobao.tddl.parser.ast.expression.primary.DefaultValue;
import com.taobao.tddl.parser.ast.expression.primary.ExistsPrimary;
import com.taobao.tddl.parser.ast.expression.primary.Identifier;
import com.taobao.tddl.parser.ast.expression.primary.MatchExpression;
import com.taobao.tddl.parser.ast.expression.primary.ParamMarker;
import com.taobao.tddl.parser.ast.expression.primary.PlaceHolder;
import com.taobao.tddl.parser.ast.expression.primary.RowExpression;
import com.taobao.tddl.parser.ast.expression.primary.SysVarPrimary;
import com.taobao.tddl.parser.ast.expression.primary.UsrDefVarPrimary;
import com.taobao.tddl.parser.ast.expression.primary.function.FunctionExpression;
import com.taobao.tddl.parser.ast.expression.primary.function.cast.Cast;
import com.taobao.tddl.parser.ast.expression.primary.function.cast.Convert;
import com.taobao.tddl.parser.ast.expression.primary.function.datetime.Extract;
import com.taobao.tddl.parser.ast.expression.primary.function.datetime.GetFormat;
import com.taobao.tddl.parser.ast.expression.primary.function.datetime.Timestampadd;
import com.taobao.tddl.parser.ast.expression.primary.function.datetime.Timestampdiff;
import com.taobao.tddl.parser.ast.expression.primary.function.groupby.Avg;
import com.taobao.tddl.parser.ast.expression.primary.function.groupby.Count;
import com.taobao.tddl.parser.ast.expression.primary.function.groupby.GroupConcat;
import com.taobao.tddl.parser.ast.expression.primary.function.groupby.Max;
import com.taobao.tddl.parser.ast.expression.primary.function.groupby.Min;
import com.taobao.tddl.parser.ast.expression.primary.function.groupby.Sum;
import com.taobao.tddl.parser.ast.expression.primary.function.string.Char;
import com.taobao.tddl.parser.ast.expression.primary.function.string.Trim;
import com.taobao.tddl.parser.ast.expression.primary.literal.IntervalPrimary;
import com.taobao.tddl.parser.ast.expression.primary.literal.LiteralBitField;
import com.taobao.tddl.parser.ast.expression.primary.literal.LiteralBoolean;
import com.taobao.tddl.parser.ast.expression.primary.literal.LiteralHexadecimal;
import com.taobao.tddl.parser.ast.expression.primary.literal.LiteralNull;
import com.taobao.tddl.parser.ast.expression.primary.literal.LiteralNumber;
import com.taobao.tddl.parser.ast.expression.primary.literal.LiteralString;
import com.taobao.tddl.parser.ast.expression.string.LikeExpression;
import com.taobao.tddl.parser.ast.expression.type.CollateExpression;
import com.taobao.tddl.parser.ast.fragment.GroupBy;
import com.taobao.tddl.parser.ast.fragment.Limit;
import com.taobao.tddl.parser.ast.fragment.OrderBy;
import com.taobao.tddl.parser.ast.fragment.ddl.ColumnDefinition;
import com.taobao.tddl.parser.ast.fragment.ddl.TableOptions;
import com.taobao.tddl.parser.ast.fragment.ddl.datatype.DataType;
import com.taobao.tddl.parser.ast.fragment.ddl.index.IndexColumnName;
import com.taobao.tddl.parser.ast.fragment.ddl.index.IndexOption;
import com.taobao.tddl.parser.ast.fragment.tableref.Dual;
import com.taobao.tddl.parser.ast.fragment.tableref.IndexHint;
import com.taobao.tddl.parser.ast.fragment.tableref.InnerJoin;
import com.taobao.tddl.parser.ast.fragment.tableref.NaturalJoin;
import com.taobao.tddl.parser.ast.fragment.tableref.OuterJoin;
import com.taobao.tddl.parser.ast.fragment.tableref.StraightJoin;
import com.taobao.tddl.parser.ast.fragment.tableref.SubqueryFactor;
import com.taobao.tddl.parser.ast.fragment.tableref.TableRefFactor;
import com.taobao.tddl.parser.ast.fragment.tableref.TableReferences;
import com.taobao.tddl.parser.ast.stmt.dal.DALSetCharacterSetStatement;
import com.taobao.tddl.parser.ast.stmt.dal.DALSetNamesStatement;
import com.taobao.tddl.parser.ast.stmt.dal.DALSetStatement;
import com.taobao.tddl.parser.ast.stmt.dal.ShowAuthors;
import com.taobao.tddl.parser.ast.stmt.dal.ShowBinLogEvent;
import com.taobao.tddl.parser.ast.stmt.dal.ShowBinaryLog;
import com.taobao.tddl.parser.ast.stmt.dal.ShowCharaterSet;
import com.taobao.tddl.parser.ast.stmt.dal.ShowCollation;
import com.taobao.tddl.parser.ast.stmt.dal.ShowColumns;
import com.taobao.tddl.parser.ast.stmt.dal.ShowContributors;
import com.taobao.tddl.parser.ast.stmt.dal.ShowCreate;
import com.taobao.tddl.parser.ast.stmt.dal.ShowDatabases;
import com.taobao.tddl.parser.ast.stmt.dal.ShowEngine;
import com.taobao.tddl.parser.ast.stmt.dal.ShowEngines;
import com.taobao.tddl.parser.ast.stmt.dal.ShowErrors;
import com.taobao.tddl.parser.ast.stmt.dal.ShowEvents;
import com.taobao.tddl.parser.ast.stmt.dal.ShowFunctionCode;
import com.taobao.tddl.parser.ast.stmt.dal.ShowFunctionStatus;
import com.taobao.tddl.parser.ast.stmt.dal.ShowGrants;
import com.taobao.tddl.parser.ast.stmt.dal.ShowIndex;
import com.taobao.tddl.parser.ast.stmt.dal.ShowMasterStatus;
import com.taobao.tddl.parser.ast.stmt.dal.ShowOpenTables;
import com.taobao.tddl.parser.ast.stmt.dal.ShowPlugins;
import com.taobao.tddl.parser.ast.stmt.dal.ShowPrivileges;
import com.taobao.tddl.parser.ast.stmt.dal.ShowProcedureCode;
import com.taobao.tddl.parser.ast.stmt.dal.ShowProcedureStatus;
import com.taobao.tddl.parser.ast.stmt.dal.ShowProcesslist;
import com.taobao.tddl.parser.ast.stmt.dal.ShowProfile;
import com.taobao.tddl.parser.ast.stmt.dal.ShowProfiles;
import com.taobao.tddl.parser.ast.stmt.dal.ShowSlaveHosts;
import com.taobao.tddl.parser.ast.stmt.dal.ShowSlaveStatus;
import com.taobao.tddl.parser.ast.stmt.dal.ShowStatus;
import com.taobao.tddl.parser.ast.stmt.dal.ShowTableStatus;
import com.taobao.tddl.parser.ast.stmt.dal.ShowTables;
import com.taobao.tddl.parser.ast.stmt.dal.ShowTriggers;
import com.taobao.tddl.parser.ast.stmt.dal.ShowVariables;
import com.taobao.tddl.parser.ast.stmt.dal.ShowWarnings;
import com.taobao.tddl.parser.ast.stmt.ddl.DDLAlterTableStatement;
import com.taobao.tddl.parser.ast.stmt.ddl.DDLCreateIndexStatement;
import com.taobao.tddl.parser.ast.stmt.ddl.DDLCreateTableStatement;
import com.taobao.tddl.parser.ast.stmt.ddl.DDLDropIndexStatement;
import com.taobao.tddl.parser.ast.stmt.ddl.DDLDropTableStatement;
import com.taobao.tddl.parser.ast.stmt.ddl.DDLRenameTableStatement;
import com.taobao.tddl.parser.ast.stmt.ddl.DDLTruncateStatement;
import com.taobao.tddl.parser.ast.stmt.ddl.DescTableStatement;
import com.taobao.tddl.parser.ast.stmt.ddl.DDLAlterTableStatement.AlterSpecification;
import com.taobao.tddl.parser.ast.stmt.dml.DMLCallStatement;
import com.taobao.tddl.parser.ast.stmt.dml.DMLDeleteStatement;
import com.taobao.tddl.parser.ast.stmt.dml.DMLInsertStatement;
import com.taobao.tddl.parser.ast.stmt.dml.DMLReplaceStatement;
import com.taobao.tddl.parser.ast.stmt.dml.DMLSelectStatement;
import com.taobao.tddl.parser.ast.stmt.dml.DMLSelectUnionStatement;
import com.taobao.tddl.parser.ast.stmt.dml.DMLUpdateStatement;
import com.taobao.tddl.parser.ast.stmt.extension.ExtDDLCreatePolicy;
import com.taobao.tddl.parser.ast.stmt.extension.ExtDDLDropPolicy;
import com.taobao.tddl.parser.ast.stmt.mts.MTSReleaseStatement;
import com.taobao.tddl.parser.ast.stmt.mts.MTSRollbackStatement;
import com.taobao.tddl.parser.ast.stmt.mts.MTSSavepointStatement;
import com.taobao.tddl.parser.ast.stmt.mts.MTSSetTransactionStatement;
import com.taobao.tddl.parser.util.Pair;

/**
 * @author <a href="mailto:shuo.qius@alibaba-inc.com">QIU Shuo</a>
 */
public class EmptySQLASTVisitor implements SQLASTVisitor {

    @SuppressWarnings( { "rawtypes" })
    private void visitInternal(Object obj) {
        if (obj == null) return;
        if (obj instanceof ASTNode) {
            ((ASTNode) obj).accept(this);
        } else if (obj instanceof Collection) {
            for (Object o : (Collection) obj) {
                visitInternal(o);
            }
        } else if (obj instanceof Pair) {
            visitInternal(((Pair) obj).getKey());
            visitInternal(((Pair) obj).getValue());
        }
    }

    @Override
    public void visit(BetweenAndExpression node) {
        visitInternal(node.getFirst());
        visitInternal(node.getSecond());
        visitInternal(node.getThird());
    }

    @Override
    public void visit(ComparisionIsExpression node) {
        visitInternal(node.getOperand());
    }

    @Override
    public void visit(InExpressionList node) {
        visitInternal(node.getList());
    }

    @Override
    public void visit(LikeExpression node) {
        visitInternal(node.getFirst());
        visitInternal(node.getSecond());
        visitInternal(node.getThird());
    }

    @Override
    public void visit(CollateExpression node) {
        visitInternal(node.getString());
    }

    @Override
    public void visit(UserExpression node) {
    }

    @Override
    public void visit(UnaryOperatorExpression node) {
        visitInternal(node.getOperand());
    }

    @Override
    public void visit(BinaryOperatorExpression node) {
        visitInternal(node.getLeftOprand());
        visitInternal(node.getRightOprand());
    }

    @Override
    public void visit(PolyadicOperatorExpression node) {
        for (int i = 0, len = node.getArity(); i < len; ++i) {
            visitInternal(node.getOperand(i));
        }
    }

    @Override
    public void visit(LogicalAndExpression node) {
        visit((PolyadicOperatorExpression) node);
    }

    @Override
    public void visit(LogicalOrExpression node) {
        visit((PolyadicOperatorExpression) node);
    }

    @Override
    public void visit(ComparisionEqualsExpression node) {
        visit((BinaryOperatorExpression) node);
    }

    @Override
    public void visit(ComparisionNullSafeEqualsExpression node) {
        visit((BinaryOperatorExpression) node);
    }

    @Override
    public void visit(InExpression node) {
        visit((BinaryOperatorExpression) node);
    }

    @Override
    public void visit(FunctionExpression node) {
        visitInternal(node.getArguments());
    }

    @Override
    public void visit(Char node) {
        visit((FunctionExpression) node);
    }

    @Override
    public void visit(Convert node) {
        visit((FunctionExpression) node);
    }

    @Override
    public void visit(Trim node) {
        visit((FunctionExpression) node);
        visitInternal(node.getRemainString());
        visitInternal(node.getString());
    }

    @Override
    public void visit(Cast node) {
        visit((FunctionExpression) node);
        visitInternal(node.getExpr());
        visitInternal(node.getTypeInfo1());
        visitInternal(node.getTypeInfo2());

    }

    @Override
    public void visit(Avg node) {
        visit((FunctionExpression) node);
    }

    @Override
    public void visit(Max node) {
        visit((FunctionExpression) node);
    }

    @Override
    public void visit(Min node) {
        visit((FunctionExpression) node);
    }

    @Override
    public void visit(Sum node) {
        visit((FunctionExpression) node);
    }

    @Override
    public void visit(Count node) {
        visit((FunctionExpression) node);
    }

    @Override
    public void visit(GroupConcat node) {
        visit((FunctionExpression) node);
        visitInternal(node.getAppendedColumnNames());
        visitInternal(node.getOrderBy());
    }

    @Override
    public void visit(Timestampdiff node) {
    }

    @Override
    public void visit(Timestampadd node) {
    }

    @Override
    public void visit(Extract node) {
    }

    @Override
    public void visit(GetFormat node) {
    }

    @Override
    public void visit(IntervalPrimary node) {
        visitInternal(node.getQuantity());
    }

    @Override
    public void visit(LiteralBitField node) {
    }

    @Override
    public void visit(LiteralBoolean node) {
    }

    @Override
    public void visit(LiteralHexadecimal node) {
    }

    @Override
    public void visit(LiteralNull node) {
    }

    @Override
    public void visit(LiteralNumber node) {
    }

    @Override
    public void visit(LiteralString node) {
    }

    @Override
    public void visit(CaseWhenOperatorExpression node) {
        visitInternal(node.getComparee());
        visitInternal(node.getElseResult());
        visitInternal(node.getWhenList());
    }

    @Override
    public void visit(DefaultValue node) {
    }

    @Override
    public void visit(ExistsPrimary node) {
        visitInternal(node.getSubquery());
    }

    @Override
    public void visit(PlaceHolder node) {
    }

    @Override
    public void visit(Identifier node) {
    }

    @Override
    public void visit(MatchExpression node) {
        visitInternal(node.getColumns());
        visitInternal(node.getPattern());
    }

    @Override
    public void visit(ParamMarker node) {
    }

    @Override
    public void visit(RowExpression node) {
        visitInternal(node.getRowExprList());
    }

    @Override
    public void visit(SysVarPrimary node) {
    }

    @Override
    public void visit(UsrDefVarPrimary node) {
    }

    @Override
    public void visit(IndexHint node) {
    }

    @Override
    public void visit(InnerJoin node) {
        visitInternal(node.getLeftTableRef());
        visitInternal(node.getOnCond());
        visitInternal(node.getRightTableRef());
    }

    @Override
    public void visit(NaturalJoin node) {
        visitInternal(node.getLeftTableRef());
        visitInternal(node.getRightTableRef());
    }

    @Override
    public void visit(OuterJoin node) {
        visitInternal(node.getLeftTableRef());
        visitInternal(node.getOnCond());
        visitInternal(node.getRightTableRef());
    }

    @Override
    public void visit(StraightJoin node) {
        visitInternal(node.getLeftTableRef());
        visitInternal(node.getOnCond());
        visitInternal(node.getRightTableRef());
    }

    @Override
    public void visit(SubqueryFactor node) {
        visitInternal(node.getSubquery());
    }

    @Override
    public void visit(TableReferences node) {
        visitInternal(node.getTableReferenceList());
    }

    @Override
    public void visit(TableRefFactor node) {
        visitInternal(node.getHintList());
        visitInternal(node.getTable());
    }

    @Override
    public void visit(Dual dual) {
    }

    @Override
    public void visit(GroupBy node) {
        visitInternal(node.getOrderByList());
    }

    @Override
    public void visit(Limit node) {
        visitInternal(node.getOffset());
        visitInternal(node.getSize());
    }

    @Override
    public void visit(OrderBy node) {
        visitInternal(node.getOrderByList());
    }

    @Override
    public void visit(ColumnDefinition columnDefinition) {
    }

    @Override
    public void visit(IndexOption indexOption) {
    }

    @Override
    public void visit(IndexColumnName indexColumnName) {
    }

    @Override
    public void visit(TableOptions node) {
    }

    @Override
    public void visit(AlterSpecification node) {
    }

    @Override
    public void visit(DataType node) {
    }

    @Override
    public void visit(ShowAuthors node) {
    }

    @Override
    public void visit(ShowBinaryLog node) {
    }

    @Override
    public void visit(ShowBinLogEvent node) {
        visitInternal(node.getLimit());
        visitInternal(node.getPos());
    }

    @Override
    public void visit(ShowCharaterSet node) {
        visitInternal(node.getWhere());
    }

    @Override
    public void visit(ShowCollation node) {
        visitInternal(node.getWhere());
    }

    @Override
    public void visit(ShowColumns node) {
        visitInternal(node.getTable());
        visitInternal(node.getWhere());
    }

    @Override
    public void visit(ShowContributors node) {
    }

    @Override
    public void visit(ShowCreate node) {
        visitInternal(node.getId());
    }

    @Override
    public void visit(ShowDatabases node) {
        visitInternal(node.getWhere());
    }

    @Override
    public void visit(ShowEngine node) {
    }

    @Override
    public void visit(ShowEngines node) {
    }

    @Override
    public void visit(ShowErrors node) {
        visitInternal(node.getLimit());
    }

    @Override
    public void visit(ShowEvents node) {
        visitInternal(node.getSchema());
        visitInternal(node.getWhere());
    }

    @Override
    public void visit(ShowFunctionCode node) {
        visitInternal(node.getFunctionName());
    }

    @Override
    public void visit(ShowFunctionStatus node) {
        visitInternal(node.getWhere());
    }

    @Override
    public void visit(ShowGrants node) {
        visitInternal(node.getUser());
    }

    @Override
    public void visit(ShowIndex node) {
        visitInternal(node.getTable());
    }

    @Override
    public void visit(ShowMasterStatus node) {
    }

    @Override
    public void visit(ShowOpenTables node) {
        visitInternal(node.getSchema());
        visitInternal(node.getWhere());
    }

    @Override
    public void visit(ShowPlugins node) {
    }

    @Override
    public void visit(ShowPrivileges node) {
    }

    @Override
    public void visit(ShowProcedureCode node) {
        visitInternal(node.getProcedureName());
    }

    @Override
    public void visit(ShowProcedureStatus node) {
        visitInternal(node.getWhere());
    }

    @Override
    public void visit(ShowProcesslist node) {
    }

    @Override
    public void visit(ShowProfile node) {
        visitInternal(node.getForQuery());
        visitInternal(node.getLimit());
    }

    @Override
    public void visit(ShowProfiles node) {
    }

    @Override
    public void visit(ShowSlaveHosts node) {
    }

    @Override
    public void visit(ShowSlaveStatus node) {
    }

    @Override
    public void visit(ShowStatus node) {
        visitInternal(node.getWhere());
    }

    @Override
    public void visit(ShowTables node) {
        visitInternal(node.getSchema());
        visitInternal(node.getWhere());
    }

    @Override
    public void visit(ShowTableStatus node) {
        visitInternal(node.getDatabase());
        visitInternal(node.getWhere());
    }

    @Override
    public void visit(ShowTriggers node) {
        visitInternal(node.getSchema());
        visitInternal(node.getWhere());
    }

    @Override
    public void visit(ShowVariables node) {
        visitInternal(node.getWhere());
    }

    @Override
    public void visit(ShowWarnings node) {
        visitInternal(node.getLimit());
    }

    @Override
    public void visit(DescTableStatement node) {
        visitInternal(node.getTable());
    }

    @Override
    public void visit(DALSetStatement node) {
        visitInternal(node.getAssignmentList());
    }

    @Override
    public void visit(DALSetNamesStatement node) {
    }

    @Override
    public void visit(DALSetCharacterSetStatement node) {
    }

    @Override
    public void visit(DMLCallStatement node) {
        visitInternal(node.getArguments());
        visitInternal(node.getProcedure());
    }

    @Override
    public void visit(DMLDeleteStatement node) {
        visitInternal(node.getLimit());
        visitInternal(node.getOrderBy());
        visitInternal(node.getTableNames());
        visitInternal(node.getTableRefs());
        visitInternal(node.getWhereCondition());
    }

    @Override
    public void visit(DMLInsertStatement node) {
        visitInternal(node.getColumnNameList());
        visitInternal(node.getDuplicateUpdate());
        visitInternal(node.getRowList());
        visitInternal(node.getSelect());
        visitInternal(node.getTable());
    }

    @Override
    public void visit(DMLReplaceStatement node) {
        visitInternal(node.getColumnNameList());
        visitInternal(node.getRowList());
        visitInternal(node.getSelect());
        visitInternal(node.getTable());
    }

    @Override
    public void visit(DMLSelectStatement node) {
        visitInternal(node.getGroup());
        visitInternal(node.getHaving());
        visitInternal(node.getLimit());
        visitInternal(node.getOrder());
        visitInternal(node.getSelectExprList());
        visitInternal(node.getTables());
        visitInternal(node.getWhere());
    }

    @Override
    public void visit(DMLSelectUnionStatement node) {
        visitInternal(node.getLimit());
        visitInternal(node.getOrderBy());
        visitInternal(node.getSelectStmtList());
    }

    @Override
    public void visit(DMLUpdateStatement node) {
        visitInternal(node.getLimit());
        visitInternal(node.getOrderBy());
        visitInternal(node.getTableRefs());
        visitInternal(node.getValues());
        visitInternal(node.getWhere());
    }

    @Override
    public void visit(MTSSetTransactionStatement node) {
    }

    @Override
    public void visit(MTSSavepointStatement node) {
        visitInternal(node.getSavepoint());
    }

    @Override
    public void visit(MTSReleaseStatement node) {
        visitInternal(node.getSavepoint());
    }

    @Override
    public void visit(MTSRollbackStatement node) {
        visitInternal(node.getSavepoint());
    }

    @Override
    public void visit(DDLTruncateStatement node) {
        visitInternal(node.getTable());
    }

    @Override
    public void visit(DDLAlterTableStatement node) {
        visitInternal(node.getTable());
    }

    @Override
    public void visit(DDLCreateIndexStatement node) {
        visitInternal(node.getIndexName());
        visitInternal(node.getTable());
    }

    @Override
    public void visit(DDLCreateTableStatement node) {
        visitInternal(node.getTable());
    }

    @Override
    public void visit(DDLRenameTableStatement node) {
        visitInternal(node.getList());
    }

    @Override
    public void visit(DDLDropIndexStatement node) {
        visitInternal(node.getIndexName());
        visitInternal(node.getTable());
    }

    @Override
    public void visit(DDLDropTableStatement node) {
        visitInternal(node.getTableNames());
    }

    @Override
    public void visit(ExtDDLCreatePolicy node) {
    }

    @Override
    public void visit(ExtDDLDropPolicy node) {
    }

}
