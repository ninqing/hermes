package com.taobao.tddl.matrix.jdbc;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.sql.DataSource;

import com.taobao.tddl.common.TddlConstants;
import com.taobao.tddl.common.exception.TddlException;
import com.taobao.tddl.common.exception.TddlRuntimeException;
import com.taobao.tddl.common.model.ExtraCmd;
import com.taobao.tddl.common.model.lifecycle.AbstractLifecycle;
import com.taobao.tddl.common.utils.GeneralUtil;
import com.taobao.tddl.common.utils.thread.NamedThreadFactory;
import com.taobao.tddl.executor.MatrixExecutor;
import com.taobao.tddl.matrix.config.MatrixConfigHolder;

/**
 * matrix的jdbc datasource实现
 * 
 * @author mengshi.sunmengshi 2013-11-22 下午3:26:14
 * @since 5.0.0
 */
public class TDataSource extends AbstractLifecycle implements DataSource {

    private String                               ruleFilePath          = null;
    private String                               machineTopologyFile   = null;
    private String                               schemaFile            = null;
    private String                               appName               = null;
    private boolean                              dynamicRule           = true;          // 是否使用动态规则
    private MatrixExecutor                       executor              = null;
    private Map<String, Object>                  connectionProperties  = new HashMap(2);
    private MatrixConfigHolder                   configHolder;
    /**
     * 用于并行查询的线程池
     */
    private ExecutorService                      globalExecutorService = null;
    private LinkedBlockingQueue<ExecutorService> executorServiceQueue  = null;

    @Override
    public void doInit() throws TddlException {
        this.executor = new MatrixExecutor();
        executor.init();

        MatrixConfigHolder configHolder = new MatrixConfigHolder();
        configHolder.setAppName(appName);
        configHolder.setTopologyFilePath(this.machineTopologyFile);
        configHolder.setSchemaFilePath(this.schemaFile);
        configHolder.setRuleFilePath(this.ruleFilePath);
        configHolder.setConnectionProperties(this.connectionProperties);
        configHolder.setDynamicRule(dynamicRule);
        configHolder.init();
        this.configHolder = configHolder;

        /**
         * 如果不为每个连接都初始化，则为整个ds初始化一个线程池
         */
        boolean everyConnectionPool = GeneralUtil.getExtraCmdBoolean(this.getConnectionProperties(),
            ExtraCmd.INIT_CONCURRENT_POOL_EVERY_CONNECTION,
            true);
        if (everyConnectionPool) {
            executorServiceQueue = new LinkedBlockingQueue<ExecutorService>();
        } else {
            // 全局共享线程池
            int poolSize;
            Object poolSizeObj = GeneralUtil.getExtraCmdString(this.getConnectionProperties(),
                ExtraCmd.CONCURRENT_THREAD_SIZE);

            if (poolSizeObj == null) {
                throw new TddlRuntimeException("如果线程池为整个datasource共用，请使用CONCURRENT_THREAD_SIZE指定线程池大小");
            }

            poolSize = Integer.valueOf(poolSizeObj.toString());
            // 默认queue队列为poolSize的两倍，超过queue大小后使用当前线程
            globalExecutorService = createThreadPool(poolSize);
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        try {
            return new TConnection(this);
        } catch (Exception e) {
            throw new SQLException(e);

        }
    }

    public ExecutorService borrowExecutorService() {
        if (globalExecutorService != null) {
            return globalExecutorService;
        } else {
            ExecutorService executor = executorServiceQueue.poll();
            if (executor == null) {
                Object poolSizeObj = GeneralUtil.getExtraCmdString(this.getConnectionProperties(),
                    ExtraCmd.CONCURRENT_THREAD_SIZE);
                int poolSize = 0;
                if (poolSizeObj != null) {
                    poolSize = Integer.valueOf(poolSizeObj.toString());
                } else {
                    poolSize = TddlConstants.DEFAULT_CONCURRENT_THREAD_SIZE;
                }
                executor = createThreadPool(poolSize);
            }

            if (executor.isShutdown()) {
                return borrowExecutorService();
            } else {
                return executor;
            }
        }
    }

    public void releaseExecutorService(ExecutorService executor) {
        if (executor != null && executor != globalExecutorService) {
            executorServiceQueue.offer(executor);// 放回队列中
        }
    }

    private ThreadPoolExecutor createThreadPool(int poolSize) {
        return new ThreadPoolExecutor(poolSize,
            poolSize,
            0L,
            TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue(poolSize * 2),
            new NamedThreadFactory("tddl_concurrent_query_executor"),
            new ThreadPoolExecutor.CallerRunsPolicy());
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return this.getConnection();
    }

    @Override
    public void dodestroy() throws TddlException {
        if (globalExecutorService != null) {
            globalExecutorService.shutdownNow();
        }

        if (executorServiceQueue != null) {
            for (ExecutorService executor : executorServiceQueue) {
                executor.shutdownNow();
            }

            executorServiceQueue.clear();
        }

        if (configHolder != null) {
            configHolder.destroy();
        }

        if (executor.isInited()) {
            executor.destroy();
        }
    }

    public String getRuleFile() {
        return ruleFilePath;
    }

    public void setRuleFile(String ruleFilePath) {
        this.ruleFilePath = ruleFilePath;
    }

    public String getMachineTopologyFile() {
        return machineTopologyFile;
    }

    public void setTopologyFile(String machineTopologyFile) {
        this.machineTopologyFile = machineTopologyFile;
    }

    public String getSchemaFile() {
        return schemaFile;
    }

    public void setSchemaFile(String schemaFile) {
        this.schemaFile = schemaFile;
    }

    public MatrixExecutor getExecutor() {
        return this.executor;
    }

    public Map<String, Object> getConnectionProperties() {
        return this.connectionProperties;
    }

    public void setConnectionProperties(Map<String, Object> cp) {
        this.connectionProperties = cp;
    }

    public void setAppName(String appName) {
        this.appName = appName;

    }

    public MatrixConfigHolder getConfigHolder() {
        return this.configHolder;
    }

    public void setDynamicRule(boolean dynamicRule) {
        this.dynamicRule = dynamicRule;
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        throw new UnsupportedOperationException("getLogWriter");
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        throw new UnsupportedOperationException("getLoginTimeout");
    }

    @Override
    public void setLogWriter(PrintWriter arg0) throws SQLException {
        throw new UnsupportedOperationException("setLogWriter");

    }

    @Override
    public void setLoginTimeout(int arg0) throws SQLException {
        throw new UnsupportedOperationException("setLoginTimeout");

    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return this.getClass().isAssignableFrom(iface);
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        try {
            return (T) this;
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }

	@Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		// TODO Auto-generated method stub
		return null;
	}

}
