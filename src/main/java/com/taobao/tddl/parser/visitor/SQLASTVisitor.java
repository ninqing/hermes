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
 * (created at 2011-5-30)
 */
package com.taobao.tddl.parser.visitor;

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

/**
 * @author <a href="mailto:shuo.qius@alibaba-inc.com">QIU Shuo</a>
 */
public interface SQLASTVisitor {

    void visit(BetweenAndExpression node);

    void visit(ComparisionIsExpression node);

    void visit(InExpressionList node);

    void visit(LikeExpression node);

    void visit(CollateExpression node);

    void visit(UserExpression node);

    void visit(UnaryOperatorExpression node);

    void visit(BinaryOperatorExpression node);

    void visit(PolyadicOperatorExpression node);

    void visit(LogicalAndExpression node);

    void visit(LogicalOrExpression node);

    void visit(ComparisionEqualsExpression node);

    void visit(ComparisionNullSafeEqualsExpression node);

    void visit(InExpression node);

    //-------------------------------------------------------
    void visit(FunctionExpression node);

    void visit(Char node);

    void visit(Convert node);

    void visit(Trim node);

    void visit(Cast node);

    void visit(Avg node);

    void visit(Max node);

    void visit(Min node);

    void visit(Sum node);

    void visit(Count node);

    void visit(GroupConcat node);

    void visit(Extract node);

    void visit(Timestampdiff node);

    void visit(Timestampadd node);

    void visit(GetFormat node);

    //-------------------------------------------------------
    void visit(IntervalPrimary node);

    void visit(LiteralBitField node);

    void visit(LiteralBoolean node);

    void visit(LiteralHexadecimal node);

    void visit(LiteralNull node);

    void visit(LiteralNumber node);

    void visit(LiteralString node);

    void visit(CaseWhenOperatorExpression node);

    void visit(DefaultValue node);

    void visit(ExistsPrimary node);

    void visit(PlaceHolder node);

    void visit(Identifier node);

    void visit(MatchExpression node);

    void visit(ParamMarker node);

    void visit(RowExpression node);

    void visit(SysVarPrimary node);

    void visit(UsrDefVarPrimary node);

    //-------------------------------------------------------
    void visit(IndexHint node);

    void visit(InnerJoin node);

    void visit(NaturalJoin node);

    void visit(OuterJoin node);

    void visit(StraightJoin node);

    void visit(SubqueryFactor node);

    void visit(TableReferences node);

    void visit(TableRefFactor node);

    void visit(Dual dual);

    void visit(GroupBy node);

    void visit(Limit node);

    void visit(OrderBy node);

    void visit(ColumnDefinition node);

    void visit(IndexOption node);

    void visit(IndexColumnName node);

    void visit(TableOptions node);

    void visit(AlterSpecification node);

    void visit(DataType node);

    //-------------------------------------------------------
    void visit(ShowAuthors node);

    void visit(ShowBinaryLog node);

    void visit(ShowBinLogEvent node);

    void visit(ShowCharaterSet node);

    void visit(ShowCollation node);

    void visit(ShowColumns node);

    void visit(ShowContributors node);

    void visit(ShowCreate node);

    void visit(ShowDatabases node);

    void visit(ShowEngine node);

    void visit(ShowEngines node);

    void visit(ShowErrors node);

    void visit(ShowEvents node);

    void visit(ShowFunctionCode node);

    void visit(ShowFunctionStatus node);

    void visit(ShowGrants node);

    void visit(ShowIndex node);

    void visit(ShowMasterStatus node);

    void visit(ShowOpenTables node);

    void visit(ShowPlugins node);

    void visit(ShowPrivileges node);

    void visit(ShowProcedureCode node);

    void visit(ShowProcedureStatus node);

    void visit(ShowProcesslist node);

    void visit(ShowProfile node);

    void visit(ShowProfiles node);

    void visit(ShowSlaveHosts node);

    void visit(ShowSlaveStatus node);

    void visit(ShowStatus node);

    void visit(ShowTables node);

    void visit(ShowTableStatus node);

    void visit(ShowTriggers node);

    void visit(ShowVariables node);

    void visit(ShowWarnings node);

    void visit(DescTableStatement node);

    void visit(DALSetStatement node);

    void visit(DALSetNamesStatement node);

    void visit(DALSetCharacterSetStatement node);

    //-------------------------------------------------------
    void visit(DMLCallStatement node);

    void visit(DMLDeleteStatement node);

    void visit(DMLInsertStatement node);

    void visit(DMLReplaceStatement node);

    void visit(DMLSelectStatement node);

    void visit(DMLSelectUnionStatement node);

    void visit(DMLUpdateStatement node);

    void visit(MTSSetTransactionStatement node);

    void visit(MTSSavepointStatement node);

    void visit(MTSReleaseStatement node);

    void visit(MTSRollbackStatement node);

    void visit(DDLTruncateStatement node);

    void visit(DDLAlterTableStatement node);

    void visit(DDLCreateIndexStatement node);

    void visit(DDLCreateTableStatement node);

    void visit(DDLRenameTableStatement node);

    void visit(DDLDropIndexStatement node);

    void visit(DDLDropTableStatement node);

    void visit(ExtDDLCreatePolicy node);

    void visit(ExtDDLDropPolicy node);

}
