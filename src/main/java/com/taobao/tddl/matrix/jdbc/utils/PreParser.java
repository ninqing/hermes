package com.taobao.tddl.matrix.jdbc.utils;

import java.sql.SQLException;
import java.util.regex.Pattern;

import com.taobao.tddl.common.model.SqlType;
import com.taobao.tddl.common.utils.TStringUtil;

/**
 * 解析SQL语句，得到这条语句的类型
 * 
 * @author yangzhu
 */
public class PreParser {

    // private static final ParserCache globalCache = ParserCache.instance();
    /**
     * 用于判断是否是一个select ... for update的sql
     */
    private static final Pattern SELECT_FOR_UPDATE_PATTERN = Pattern.compile("^select\\s+.*\\s+for\\s+update.*$",
                                                               Pattern.CASE_INSENSITIVE);

    /**
     * 获得SQL语句种类
     * 
     * @param sql SQL语句
     * @throws SQLException 当SQL语句不是SELECT、INSERT、UPDATE、DELETE语句时，抛出异常。
     */
    public static SqlType getSqlType(String sql) throws SQLException {
        // #bug 2011-11-24,modify by junyu,先不走缓存，否则sql变化巨大，缓存换入换出太多，gc太明显
        // SqlType sqlType = globalCache.getSqlType(sql);
        // if (sqlType == null) {
        SqlType sqlType = null;
        // #bug 2011-12-8,modify by junyu ,this code use huge cpu resource,and
        // most
        // sql have no comment,so first simple look for there whether have the
        // comment
        String noCommentsSql = sql;
        if (sql.contains("/*")) {
            noCommentsSql = TStringUtil.stripComments(sql, "'\"", "'\"", true, false, true, true).trim();
        }

        if (TStringUtil.startsWithIgnoreCaseAndWs(noCommentsSql, "select")) {
            // #bug 2011-12-9,this select-for-update regex has low
            // performance,so
            // first judge this sql whether have ' for ' string.
            if (noCommentsSql.toLowerCase().contains(" for ")
                && SELECT_FOR_UPDATE_PATTERN.matcher(noCommentsSql).matches()) {
                sqlType = SqlType.SELECT_FOR_UPDATE;
            } else {
                sqlType = SqlType.SELECT;
            }
        } else if (TStringUtil.startsWithIgnoreCaseAndWs(noCommentsSql, "show")) {
            // ??show 不支持？
            // sqlType = SqlType.SHOW;
        } else if (TStringUtil.startsWithIgnoreCaseAndWs(noCommentsSql, "insert")) {
            sqlType = SqlType.INSERT;
        } else if (TStringUtil.startsWithIgnoreCaseAndWs(noCommentsSql, "update")) {
            sqlType = SqlType.UPDATE;
        } else if (TStringUtil.startsWithIgnoreCaseAndWs(noCommentsSql, "delete")) {
            sqlType = SqlType.DELETE;
        } else if (TStringUtil.startsWithIgnoreCaseAndWs(noCommentsSql, "replace")) {
            sqlType = SqlType.REPLACE;
        } else if (TStringUtil.startsWithIgnoreCaseAndWs(noCommentsSql, "truncate")) {
            sqlType = SqlType.TRUNCATE;
        } else if (TStringUtil.startsWithIgnoreCaseAndWs(noCommentsSql, "create")) {
            sqlType = SqlType.CREATE;
        } else if (TStringUtil.startsWithIgnoreCaseAndWs(noCommentsSql, "drop")) {
            sqlType = SqlType.DROP;
        } else if (TStringUtil.startsWithIgnoreCaseAndWs(noCommentsSql, "load")) {
            sqlType = SqlType.LOAD;
        } else if (TStringUtil.startsWithIgnoreCaseAndWs(noCommentsSql, "merge")) {
            sqlType = SqlType.MERGE;
        } else {
            throw new SQLException("only select, insert, update, delete,replace,truncate,create,drop,load,merge sql is supported");
        }
        // sqlType = globalCache.setSqlTypeIfAbsent(sql, sqlType);
        // }
        return sqlType;
    }
}
