package com.taobao.tddl.optimizer.costbased;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;

import com.alibaba.cobar.parser.ast.stmt.SQLStatement;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.taobao.tddl.common.TddlConstants;
import com.taobao.tddl.common.exception.NotSupportException;
import com.taobao.tddl.common.exception.TddlException;
import com.taobao.tddl.common.exception.TddlRuntimeException;
import com.taobao.tddl.common.jdbc.ParameterContext;
import com.taobao.tddl.common.jdbc.SqlTypeParser;
import com.taobao.tddl.common.model.ExtraCmd;
import com.taobao.tddl.common.model.SqlType;
import com.taobao.tddl.common.model.lifecycle.AbstractLifecycle;
import com.taobao.tddl.common.utils.logger.Logger;
import com.taobao.tddl.common.utils.logger.LoggerFactory;
import com.taobao.tddl.monitor.Monitor;
import com.taobao.tddl.optimizer.Optimizer;
import com.taobao.tddl.optimizer.OptimizerContext;
import com.taobao.tddl.optimizer.core.ASTNodeFactory;
import com.taobao.tddl.optimizer.core.ast.ASTNode;
import com.taobao.tddl.optimizer.core.ast.DMLNode;
import com.taobao.tddl.optimizer.core.ast.QueryTreeNode;
import com.taobao.tddl.optimizer.core.ast.dml.DeleteNode;
import com.taobao.tddl.optimizer.core.ast.dml.InsertNode;
import com.taobao.tddl.optimizer.core.ast.dml.PutNode;
import com.taobao.tddl.optimizer.core.ast.dml.UpdateNode;
import com.taobao.tddl.optimizer.core.ast.query.JoinNode;
import com.taobao.tddl.optimizer.core.ast.query.MergeNode;
import com.taobao.tddl.optimizer.core.ast.query.QueryNode;
import com.taobao.tddl.optimizer.core.ast.query.TableNode;
import com.taobao.tddl.optimizer.core.plan.IDataNodeExecutor;
import com.taobao.tddl.optimizer.core.plan.query.IMerge;
import com.taobao.tddl.optimizer.costbased.after.ChooseTreadOptimizer;
import com.taobao.tddl.optimizer.costbased.after.FillRequestIDAndSubRequestID;
import com.taobao.tddl.optimizer.costbased.after.FuckAvgOptimizer;
import com.taobao.tddl.optimizer.costbased.after.LimitOptimizer;
import com.taobao.tddl.optimizer.costbased.after.MergeConcurrentOptimizer;
import com.taobao.tddl.optimizer.costbased.after.MergeJoinMergeOptimizer;
import com.taobao.tddl.optimizer.costbased.after.QueryPlanOptimizer;
import com.taobao.tddl.optimizer.costbased.after.StreamingOptimizer;
import com.taobao.tddl.optimizer.costbased.chooser.DataNodeChooser;
import com.taobao.tddl.optimizer.costbased.chooser.JoinChooser;
import com.taobao.tddl.optimizer.costbased.pusher.FilterPusher;
import com.taobao.tddl.optimizer.costbased.pusher.OrderByPusher;
import com.taobao.tddl.optimizer.exceptions.QueryException;
import com.taobao.tddl.optimizer.exceptions.SqlParserException;
import com.taobao.tddl.optimizer.parse.SqlAnalysisResult;
import com.taobao.tddl.optimizer.parse.SqlParseManager;
import com.taobao.tddl.optimizer.parse.cobar.CobarSqlAnalysisResult;
import com.taobao.tddl.optimizer.parse.cobar.CobarSqlParseManager;
import com.taobao.tddl.optimizer.parse.cobar.visitor.MysqlOutputVisitor;
import com.taobao.tddl.optimizer.parse.hint.DirectlyRouteCondition;
import com.taobao.tddl.optimizer.parse.hint.RouteCondition;
import com.taobao.tddl.optimizer.parse.hint.RuleRouteCondition;
import com.taobao.tddl.optimizer.parse.hint.SimpleHintParser;
import com.taobao.tddl.rule.model.TargetDB;

/**
 * <pre>
 * 此优化器是根据开销进行优化的，主要优化流程在public IQueryCommon optimize(QueryTreeNode qn)中 
 * 分为两部分进行
 * a. 第一部分，对关系查询树的优化，包含以下几个步骤： 
 *  s1.将SELECT提前，放到叶子节点进行 SELECT列提前进行可以减少数据量
 *      由于一些列是作为连接列的，他们不在最后的SELECT中
 *          (比如SELECT table1.id from table1 join table2 on table1.name=table2.name table1.name和table2.name作为连接列)
 *      在对table1与table2的查询中应该保存，同时在执行执行结束后，需要将table1.name与table2.name去除.
 *      所以在执行这一步的时候，需要保存中间需要的临时列. 在生成执行计划后，需要将这些列从最后的节点中删除。 
 *  效果是：
 *      原SQL：table1.join(table2).addJoinColumns("id","id").select("table1.id table2.id")
 *      转换为：table1.select("table1.id").join(tabl2.select("table2.id")).addJoinColumns("id","id")
 *              
 *  s2.将Join中连接列上的约束条件复制到另一边 
 *      比如SELECT * from table1 join table2 on table1.id=table2.id where table1.id = 1
 *      因为Join是在table1.id与table2.id上的，所以table2.id上同样存在约束table2.id=1,此步就是需要发现这些条件，并将它复制。
 *  效果是：
 *      原SQL: table1.query("id=1").join(table2).addJoinColumns("id","id")
 *      转换为：table1.query("id=1").join(table2.query("id=2")).addJoinColumns("id","id")
 * 
 * s3.将约束条件提前，约束条件提前进行可以减少结果集的行数，并且可以合并QueryNode 
 *   效果是：
 *       原SQL:  table1.join(table2).addJoinColumns("id","id").query("table1.name=1")
 *       转换为: table1.query("table1.name=1").join(table2).addJoinColumns("id","id")
 * 
 * s4.找到并遍历每种个子查询，调整其Join顺序，并为其选择Join策略 
 * 
 * s5.所有子查询优化之后，再调整这个查询树的Join顺序
 *      对Join顺序调整的依据是通过计算开销，开销主要包括两种: 
 *          1. 磁盘IO与网络传输 详细计算方式请参见CostEstimater实现类的相关注释
 *          2. 对Join顺序的遍历使用的是最左树 在此步中，还会对同一列的约束条件进行合并等操作
 *      选取策略见JoinChooser的注释 
 * 
 * s6.将s1中生成的临时列删除
 * 
 * s7.将查询树转换为原始的执行计划树 
 * 
 * 第二部分，对执行计划树的优化，包含以下几个步骤： 
 * s8.为执行计划的每个节点选择执行的GroupNode
 *      这一步是根据TDDL的规则进行分库 在Join，Merge的执行节点选择上，遵循的原则是尽量减少网络传输 
 *  
 * s9.调整分库后的Join节点
 *      由于分库后，一个Query节点可能会变成一个Merge节点，需要对包含这样子节点的Join节点进行调整，详细见splitJoinAfterChooseDataNode的注释
 * </pre>
 * 
 * @since 5.0.0
 */
public class CostBasedOptimizer extends AbstractLifecycle implements Optimizer {

    private static final String            _DIRECT         = "_DIRECT_";
    private static final Logger            logger          = LoggerFactory.getLogger(CostBasedOptimizer.class);
    private int                            cacheSize       = 1000;
    private long                           expireTime      = TddlConstants.DEFAULT_OPTIMIZER_EXPIRE_TIME;
    private SqlParseManager                sqlParseManager;
    private Cache<String, OptimizeResult>  optimizedResults;
    private final List<QueryPlanOptimizer> afterOptimizers = new ArrayList<QueryPlanOptimizer>();

    @Override
    protected void doInit() throws TddlException {
        // after处理
        afterOptimizers.add(new FuckAvgOptimizer());
        afterOptimizers.add(new ChooseTreadOptimizer());
        afterOptimizers.add(new FillRequestIDAndSubRequestID());
        afterOptimizers.add(new LimitOptimizer());
        afterOptimizers.add(new MergeJoinMergeOptimizer());
        afterOptimizers.add(new MergeConcurrentOptimizer());
        afterOptimizers.add(new StreamingOptimizer());

        if (this.sqlParseManager == null) {
            CobarSqlParseManager sqlParseManager = new CobarSqlParseManager();
            sqlParseManager.setCacheSize(cacheSize);
            sqlParseManager.setExpireTime(expireTime);
            this.sqlParseManager = sqlParseManager;
        }

        if (!sqlParseManager.isInited()) {
            sqlParseManager.init(); // 启动
        }

        optimizedResults = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(expireTime, TimeUnit.MILLISECONDS)
            .build();
    }

    @Override
    protected void dodestroy() throws TddlException {
        optimizedResults.invalidateAll();
        sqlParseManager.destroy();
    }

    private class OptimizeResult {

        public ASTNode        optimized = null;
        public QueryException ex        = null;
    }

    @Override
    public IDataNodeExecutor optimizeAndAssignment(ASTNode node, Map<Integer, ParameterContext> parameterSettings,
                                                   Map<String, Object> extraCmd) throws QueryException {
        return optimizeAndAssignment(node, parameterSettings, extraCmd, null, false);
    }

    @Override
    public IDataNodeExecutor optimizeAndAssignment(String sql, Map<Integer, ParameterContext> parameterSettings,
                                                   Map<String, Object> extraCmd, boolean cached) throws QueryException,
                                                                                                SqlParserException {
        // 处理sql hint
        RouteCondition routeCondition = SimpleHintParser.convertHint2RouteCondition(sql, parameterSettings);
        if (routeCondition != null && !routeCondition.getExtraCmds().isEmpty()) {
            // 合并sql中的extra cmd参数
            if (extraCmd == null) {
                extraCmd = new HashMap<String, Object>();
            }

            extraCmd.putAll(routeCondition.getExtraCmds());
        }

        if (routeCondition != null
            && (routeCondition instanceof DirectlyRouteCondition || routeCondition instanceof RuleRouteCondition)) {
            sql = SimpleHintParser.removeHint(sql, parameterSettings);
            return optimizerHint(sql, cached, routeCondition, parameterSettings, extraCmd);
        } else {
            return optimizeAndAssignment(null, parameterSettings, extraCmd, sql, cached);
        }
    }

    private IDataNodeExecutor optimizerHint(String sql, boolean cached, RouteCondition routeCondition,
                                            Map<Integer, ParameterContext> parameterSettings,
                                            Map<String, Object> extraCmd) {
        long time = System.currentTimeMillis();
        IDataNodeExecutor qc;
        String groupHint = SimpleHintParser.extractTDDLGroupHintString(sql);
        // 基于hint直接构造执行计划
        if (routeCondition instanceof DirectlyRouteCondition) {
            DirectlyRouteCondition drc = (DirectlyRouteCondition) routeCondition;
            if (!drc.getTables().isEmpty()) {
                SqlAnalysisResult result = sqlParseManager.parse(sql, cached);
                Map<String, String> sqls = buildDirectSqls(result,
                    drc.getVirtualTableName(),
                    drc.getTables(),
                    groupHint);
                qc = buildDirectPlan(result.getSqlType(), drc.getDbId(), sqls);
            } else {
                // 直接下推sql时，不做任何sql解析
                Map<String, String> sqls = new HashMap<String, String>();
                sqls.put(_DIRECT, sql);
                qc = buildDirectPlan(SqlTypeParser.getSqlType(sql), drc.getDbId(), sqls);
            }
        } else if (routeCondition instanceof RuleRouteCondition) {
            RuleRouteCondition rrc = (RuleRouteCondition) routeCondition;
            SqlAnalysisResult result = sqlParseManager.parse(sql, cached);
            boolean isWrite = (result.getSqlType() != SqlType.SELECT && result.getSqlType() != SqlType.SELECT_FOR_UPDATE);
            List<TargetDB> targetDBs = OptimizerContext.getContext()
                .getRule()
                .shard(rrc.getVirtualTableName(), rrc.getCompMapChoicer(), isWrite);
            // 考虑表名可能有重复
            Set<String> tables = new HashSet<String>();
            for (TargetDB target : targetDBs) {
                tables.addAll(target.getTableNames());
            }
            Map<String, String> sqls = buildDirectSqls(result, rrc.getVirtualTableName(), tables, groupHint);
            qc = buildRulePlain(result.getSqlType(), targetDBs, sqls);
        } else {
            throw new NotSupportException("RouteCondition : " + routeCondition.toString());
        }

        // 进行一些自定义的额外处理
        for (QueryPlanOptimizer after : afterOptimizers) {
            qc = after.optimize(qc, parameterSettings, extraCmd);
        }
        if (logger.isDebugEnabled()) {
            logger.warn(qc.toString());
        }

        time = Monitor.monitorAndRenewTime(Monitor.KEY1,
            Monitor.AndOrExecutorOptimize,
            Monitor.Key3Success,
            System.currentTimeMillis() - time);
        return qc;
    }

    private IDataNodeExecutor optimizeAndAssignment(final ASTNode node,
                                                    final Map<Integer, ParameterContext> parameterSettings,
                                                    final Map<String, Object> extraCmd, final String sql,
                                                    final boolean cached) throws QueryException {
        long time = System.currentTimeMillis();
        ASTNode optimized = null;
        if (cached && sql != null && !sql.isEmpty()) {
            OptimizeResult or;
            try {
                or = optimizedResults.get(sql, new Callable<OptimizeResult>() {

                    @Override
                    public OptimizeResult call() throws Exception {
                        OptimizeResult or = new OptimizeResult();
                        try {
                            SqlAnalysisResult result = sqlParseManager.parse(sql, true);
                            or.optimized = optimize(result.getAstNode(parameterSettings), parameterSettings, extraCmd);
                        } catch (Exception e) {
                            if (e instanceof QueryException) {
                                or.ex = (QueryException) e;
                            } else {
                                or.ex = new QueryException(e);
                            }
                        }
                        return or;
                    }
                });
            } catch (ExecutionException e1) {
                throw new QueryException("Optimizer future task interrupted,the sql is:" + sql, e1);
            }

            if (or.ex != null) {
                throw or.ex;
            }
            optimized = or.optimized.deepCopy();
            optimized.build();
        } else {
            if (node == null) {
                SqlAnalysisResult result = sqlParseManager.parse(sql, cached);
                optimized = this.optimize(result.getAstNode(parameterSettings), parameterSettings, extraCmd);
            } else {
                optimized = this.optimize(node, parameterSettings, extraCmd);
            }
        }

        if (parameterSettings != null) {
            optimized.assignment(parameterSettings);
            // 绑定变量后，再做一次
            if (optimized instanceof DMLNode) {
                ((DMLNode) optimized).setNode((TableNode) FilterPreProcessor.optimize(((DMLNode) optimized).getNode(),
                    false));
            } else {
                optimized = FilterPreProcessor.optimize(((QueryTreeNode) optimized), false);
            }
        }

        // 分库，选择执行节点
        try {
            optimized = DataNodeChooser.shard(optimized, parameterSettings, extraCmd);
        } catch (Exception e) {
            if (e instanceof QueryException) {
                throw (QueryException) e;
            } else {
                throw new QueryException(e);
            }
        }

        optimized = this.createMergeForJoin(optimized, extraCmd);
        if (optimized instanceof QueryTreeNode) {
            OrderByPusher.optimize((QueryTreeNode) optimized);
        }

        IDataNodeExecutor qc = optimized.toDataNodeExecutor();
        // 进行一些自定义的额外处理
        for (QueryPlanOptimizer after : afterOptimizers) {
            qc = after.optimize(qc, parameterSettings, extraCmd);
        }
        if (logger.isDebugEnabled()) {
            logger.warn(qc.toString());
        }

        time = Monitor.monitorAndRenewTime(Monitor.KEY1,
            Monitor.AndOrExecutorOptimize,
            Monitor.Key3Success,
            System.currentTimeMillis() - time);
        return qc;
    }

    public ASTNode optimize(ASTNode node, Map<Integer, ParameterContext> parameterSettings, Map<String, Object> extraCmd)
                                                                                                                         throws QueryException {
        // 先调用一次build，完成select字段信息的推导
        node.build();
        ASTNode optimized = null;
        if (node instanceof QueryTreeNode) {
            optimized = this.optimizeQuery((QueryTreeNode) node, extraCmd);
        }

        if (node instanceof InsertNode) {
            optimized = this.optimizeInsert((InsertNode) node, extraCmd);
        }

        else if (node instanceof DeleteNode) {
            optimized = this.optimizeDelete((DeleteNode) node, extraCmd);
        }

        else if (node instanceof UpdateNode) {
            optimized = this.optimizeUpdate((UpdateNode) node, extraCmd);
        }

        else if (node instanceof PutNode) {
            optimized = this.optimizePut((PutNode) node, extraCmd);
        }

        return optimized;
    }

    private QueryTreeNode optimizeQuery(QueryTreeNode qn, Map<String, Object> extraCmd) throws QueryException {

        // / 预先处理子查询
        qn = SubQueryPreProcessor.optimize(qn);

        qn = JoinPreProcessor.optimize(qn);

        // 预处理filter，比如过滤永假式/永真式
        qn = FilterPreProcessor.optimize(qn, true);

        // 将约束条件推向叶节点
        qn = FilterPusher.optimize(qn);

        // 找到每一个子查询，并进行优化
        qn = JoinChooser.optimize(qn, extraCmd);

        // 完成之前build
        qn.build();
        return qn;
    }

    private ASTNode optimizeUpdate(UpdateNode update, Map<String, Object> extraCmd) throws QueryException {
        update.build();
        if (extraCmd == null) {
            extraCmd = new HashMap();
        }
        // update暂不允许使用索引
        extraCmd.put(ExtraCmd.CHOOSE_INDEX, "FALSE");
        QueryTreeNode queryCommon = this.optimizeQuery(update.getNode(), extraCmd);
        queryCommon.build();
        update.setNode((TableNode) queryCommon);
        return update;

    }

    private ASTNode optimizeInsert(InsertNode insert, Map<String, Object> extraCmd) throws QueryException {
        insert.setNode((TableNode) insert.getNode().convertToJoinIfNeed());
        return insert;
    }

    private ASTNode optimizeDelete(DeleteNode delete, Map<String, Object> extraCmd) throws QueryException {
        QueryTreeNode queryCommon = this.optimizeQuery(delete.getNode(), extraCmd);
        delete.setNode((TableNode) queryCommon);
        return delete;
    }

    private ASTNode optimizePut(PutNode put, Map<String, Object> extraCmd) throws QueryException {
        return put;
    }

    // ============= helper method =============

    /**
     * 通过visitor替换表名生成sql
     */
    private Map<String, /* table name */String/* sql */> buildDirectSqls(SqlAnalysisResult sqlAnalysisResult,
                                                                         String vtab, Collection<String> tables,
                                                                         String groupHint) {
        if (groupHint == null) {
            groupHint = "";
        }
        Map<String, String> sqls = new HashMap<String, String>();
        // 指定分库分表，直接下推sql
        // 目前先考虑只有一张表的表名需要替换
        SQLStatement statement = ((CobarSqlAnalysisResult) sqlAnalysisResult).getStatement();
        boolean singleNode = (tables.size() > 1);
        if (StringUtils.isNotEmpty(vtab) && !tables.isEmpty()) {
            String[] vtabs = StringUtils.split(vtab, ',');
            Map<String, String> logicTable2RealTable = new HashMap<String, String>();
            for (String realTable : tables) {
                String[] rtabs = StringUtils.split(realTable, ',');
                if (rtabs.length != vtabs.length) {
                    throw new TddlRuntimeException("hint中逻辑表和真实表数量不匹配");
                }
                int i = 0;
                for (String v : vtabs) {
                    logicTable2RealTable.put(v, rtabs[i++]);
                }
                MysqlOutputVisitor sqlVisitor = new MysqlOutputVisitor(new StringBuilder(groupHint),
                    singleNode,
                    logicTable2RealTable);
                statement.accept(sqlVisitor);
                sqls.put(realTable, sqlVisitor.getSql());
            }
        } else {
            // 没有执行表设置，直接下推sql，不需要做表名替换
            sqls.put(_DIRECT, sqlAnalysisResult.getSql());
        }
        return sqls;
    }

    /**
     * 根据规则生成对应的执行计划
     */
    private IDataNodeExecutor buildRulePlain(SqlType sqlType, List<TargetDB> targetDBs, Map<String, String> sqls) {
        List<IDataNodeExecutor> subs = new ArrayList<IDataNodeExecutor>();
        for (TargetDB target : targetDBs) {
            for (String table : target.getTableNames()) {
                subs.add(buildOneDirectPlan(sqlType, target.getDbIndex(), sqls.get(table)));
            }
        }

        if (subs.size() > 1) {
            IMerge merge = ASTNodeFactory.getInstance().createMerge();
            for (IDataNodeExecutor sub : subs) {
                merge.addSubNode(sub);
            }
            merge.executeOn(subs.get(0).getDataNode());// 选择第一个
            return merge;
        } else {
            return subs.get(0);
        }
    }

    /**
     * 根据指定的库和表生成执行计划
     */
    private IDataNodeExecutor buildDirectPlan(SqlType sqlType, String dbId, Map<String, String> sqls) {
        if (sqls.size() > 1) {
            IMerge merge = ASTNodeFactory.getInstance().createMerge();
            for (String sql : sqls.values()) {
                merge.addSubNode(buildOneDirectPlan(sqlType, dbId, sql));
            }
            merge.executeOn(dbId);
            return merge;
        } else {
            return buildOneDirectPlan(sqlType, dbId, sqls.values().iterator().next());
        }
    }

    private IDataNodeExecutor buildOneDirectPlan(SqlType sqlType, String dbId, String sql) {
        IDataNodeExecutor executor = null;
        switch (sqlType) {
            case SELECT:
                executor = ASTNodeFactory.getInstance().createQuery();
                break;
            case UPDATE:
                executor = ASTNodeFactory.getInstance().createUpdate();
                break;
            case DELETE:
                executor = ASTNodeFactory.getInstance().createDelete();
                break;
            case INSERT:
                executor = ASTNodeFactory.getInstance().createInsert();
                break;
            case REPLACE:
                executor = ASTNodeFactory.getInstance().createReplace();
                break;
            default:
                break;
        }

        if (executor != null) {
            executor.setSql(sql);
            executor.executeOn(dbId);
        }

        return executor;
    }

    private ASTNode createMergeForJoin(ASTNode dne, Map<String, Object> extraCmd) {
        if (dne instanceof MergeNode) {
            for (ASTNode sub : ((MergeNode) dne).getChildren()) {
                this.createMergeForJoin(sub, extraCmd);
            }
        }

        if (dne instanceof JoinNode) {
            this.createMergeForJoin(((JoinNode) dne).getLeftNode(), extraCmd);
            this.createMergeForJoin(((JoinNode) dne).getRightNode(), extraCmd);
            // 特殊处理子查询
            if (((JoinNode) dne).getRightNode() instanceof QueryNode) {
                QueryNode right = (QueryNode) ((JoinNode) dne).getRightNode();
                if (right.getDataNode() != null) {
                    // right和join节点跨机，则需要右边生成Merge来做mget
                    if (!right.getDataNode().equals(dne.getDataNode())) {
                        MergeNode merge = new MergeNode();
                        merge.merge(right);
                        merge.setSharded(false);
                        merge.executeOn(right.getDataNode());
                        merge.build();
                        ((JoinNode) dne).setRightNode(merge);
                    }
                }
            }
        }

        if (dne instanceof QueryNode) {
            if (((QueryNode) dne).getChild() != null) {
                this.createMergeForJoin(((QueryNode) dne).getChild(), extraCmd);
            }
        }

        return dne;
    }

    public void setCacheSize(int cacheSize) {
        this.cacheSize = cacheSize;
    }

    public void setExpireTime(long expireTime) {
        this.expireTime = expireTime;
    }

    public void setSqlParseManager(SqlParseManager sqlParseManager) {
        this.sqlParseManager = sqlParseManager;
    }

}
