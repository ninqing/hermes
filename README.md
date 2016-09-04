Tddl(Taobao Distribute Data Layer)是整个淘宝数据库体系里面具有非常重要的一个中间件产品，在公司内部具有广泛的使用。

Tddl整个产品包括对**应用透明的分库分表层** 和 具有众多特性的**动态数据源**。

**动态数据源的主要特性有:** <br/>
1.数据库主备和动态切换<br/>
2.带权重的读写分离 <br/>
3.单线程读重试 <br/>
4.集中式数据源信息管理和动态变更 <br/>
5.剥离的稳定jboss数据源<br/>
6.支持mysql和oracle数据库<br/>
7.基于jdbc规范，很容易扩展支持实现jdbc规范的数据源<br/>
8.无server,client-jar形式存在，应用直连数据库<br/>
9.读写次数,并发度流控，动态变更<br/>
10.可分析的日志打印,日志流控，动态变更<br/>

这些特性解决了一些数据库使用的基本问题,并且降低了数据库运维的成本.


TDDL动态数据源使用示例说明
环境准备

因为tddl动态数据源强依赖diamond配置中心,该依赖项目已经开源.请到http://code.taobao.org/p/diamond/src/ checkout 代码,下载使用文档(http://code.taobao.org/p/diamond/wiki/index/)按步骤部署.
Jdk 1.6 安装.
Mysql 5.1.x或者5.5.x 安装
Maven2 settings.xml修改(见附录)
示例使用

Tddl 动态数据源的开源代码中,有一个tddl-sample 工程, 该工程演示了如何使用tddl动态数据源.但需要一些配置支持,以下说明需要做的配置.

AtomDataSource示例使用说明

1. 创建mysql库和表

(1).建库qatest_normal_0:

drop database if exists qatest_normal_0;

CREATE DATABASE qatest_normal_0

(2). 在库qatest_normal_0中建表normaltbl_0001

USE qatest_normal_0;

CREATE TABLE normaltbl_0001 (

pk int(11) NOT NULL,

id int(11) DEFAULT NULL,

gmt_create date DEFAULT ‘2010-12-17’,

name varchar(30) DEFAULT NULL,

floatCol float(9,3) DEFAULT ‘0.000’,

PRIMARY KEY (pk)

) ENGINE=InnoDB DEFAULT CHARSET=latin1;

(3). 在库qatest_normal_0中创建tddl用户,授予读写数据库权限

CREATE USER ‘tddl’@’%’ IDENTIFIED BY ‘tddl’;

GRANT Insert,Update,Select,Delete ON qatest_normal_0.* TO ‘tddl’@’%’;

2. 在diamond中配置Atom数据源

Global配置

dataId：com.taobao.tddl.atom.global.qatest_normal_0

group：DEFAULT_GROUP

content：

ip=127.0.0.1

port=3306

dbName=qatest_normal_0

dbType=mysql

dbStatus=RW

App配置

dataId：com.taobao.tddl.atom.app.tddl_sample.qatest_normal_0

group：DEFAULT_GROUP

content：

userName=tddl

minPoolSize=1

maxPoolSize=2

idleTimeout=10

blockingTimeout=5

preparedStatementCacheSize=15

connectionProperties=characterEncoding=gbk

User配置(dataId最后一段’.tddl’和用户名紧相关,如果数据库用户名不是tddl,请修改这个dataId)

dataId：com.taobao.tddl.atom.passwd.qatest_normal_0.mysql.tddl

group：DEFAULT_GROUP

content:

encPasswd=xxxxxxx(**密文,请用tddl-atom-datasource工程下的JbossPasswordDecode**加密下明文密码)

3. 添加TDDL依赖(建议使用maven,settings.xml文件绑定淘宝开源maven库,见附录)

<dependency>

<groupId>com.taobao.tddl</groupId>

<artifactId>tddl-group-datasource</artifactId>

<version>3.0.1.5.taobaocode-SNAPSHOT</version>

</dependency>

<dependency>

<groupId>com.taobao.tddl</groupId>

<artifactId>tddl-atom-datasource</artifactId>

<version>3.0.1.5.taobaocode-SNAPSHOT</version>

</dependency>

4. 在代码中调用AtomDataSource

TAtomDataSource tAtomDataSource = new TAtomDataSource();
tAtomDataSource.setAppName(appName);//appName是当前业务的名称
tAtomDataSource.setDbKey(dbKey);// dbKey是dba告知业务的当前数据库实例的名字（用于标志唯一的数据库）
TAtomDataSource.init();

GroupDataSource示例使用说明

1. 在mysql数据库中创建库和表(如果atom ds示例中已经创建的库表,则不需要再创建)

(1) 建库qatest_normal_0

drop database if exists qatest_normal_0;

CREATE DATABASE qatest_normal_0

(2) 在库qatest_normal_0中建表normaltbl_0001

USE qatest_normal_0;

CREATE TABLE normaltbl_0001 (

pk int(11) NOT NULL,

id int(11) DEFAULT NULL,

gmt_create date DEFAULT ‘2010-12-17’,

name varchar(30) DEFAULT NULL,

floatCol float(9,3) DEFAULT ‘0.000’,

PRIMARY KEY (pk)

) ENGINE=InnoDB DEFAULT CHARSET=latin1;

(3) 建库qatest_normal_0_bac

drop database if exists qatest_normal_0_bac;

CREATE DATABASE qatest_normal_0_bac;

(4) 在库qatest_normal_0_bac中建表normaltbl_0001

USE qatest_normal_0_bac;

CREATE TABLE normaltbl_0001 (

pk int(11) NOT NULL,

id int(11) DEFAULT NULL,

gmt_create date DEFAULT ‘2010-12-17’,

name varchar(30) DEFAULT NULL,

floatCol float(9,3) DEFAULT ‘0.000’,

PRIMARY KEY (pk)

) ENGINE=InnoDB DEFAULT CHARSET=latin1;

(5). 在以上两个数据库中创建tddl用户,授予读写数据库权限

CREATE USER ‘tddl’@’%’ IDENTIFIED BY ‘tddl’;

GRANT Insert,Update,Select,Delete ON qatest_normal_0.* TO ‘tddl’@’%’;

GRANT Insert,Update,Select,Delete ON qatest_normal_0_bac.* TO ‘tddl’@’%’;

2. 在diamond中配置Group数据源

(1)在diamond中配置Group一组对等的数据的读写权重

dataId：com.taobao.tddl.jdbc.group_V2.4.1_group_sample

group：DEFAULT_GROUP

content:

qatest_normal_0:r10w10,qatest_normal_0_bac:r10w0

(2)在diamond中配置Atom(qatest_normal_0)数据源

Global配置

dataId：com.taobao.tddl.atom.global.qatest_normal_0

group：DEFAULT_GROUP

content：

ip=127.0.0.1

port=3306

dbName=qatest_normal_0

dbType=mysql

dbStatus=RW

App配置

dataId：com.taobao.tddl.atom.app.tddl_sample.qatest_normal_0

group：DEFAULT_GROUP

content：

userName=tddl

minPoolSize=1

maxPoolSize=2

idleTimeout=10

blockingTimeout=5

preparedStatementCacheSize=15

connectionProperties=characterEncoding=gbk

User配置(dataId最后一段’.tddl’和用户名紧相关,如果数据库用户名不是tddl,请修改这个dataId)

dataId：com.taobao.tddl.atom.passwd.qatest_normal_0.mysql.tddl

group：DEFAULT_GROUP

content：

encPasswd=xxxxxxx(**密文,请用tddl-atom-datasource工程下的JbossPasswordDecode**加密下明文密码)

(3)在diamond中配置Atom(qatest_normal_0_bac)数据源

Global配置

dataId：com.taobao.tddl.atom.global.qatest_normal_0_bac

group：DEFAULT_GROUP

content：

ip=127.0.0.1

port=3306

dbName=qatest_normal_0_bac

dbType=mysql

dbStatus=WR

App配置

dataId：com.taobao.tddl.atom.app.tddl_sample.qatest_normal_0_bac

group：DEFAULT_GROUP

content：

userName=tddl

minPoolSize=1

maxPoolSize=2

idleTimeout=10

blockingTimeout=5

preparedStatementCacheSize=15

connectionProperties=characterEncoding=gbk

User配置(dataId最后一段’.tddl’和用户名紧相关,如果数据库用户名不是tddl,请修改这个dataId)

dataId：com.taobao.tddl.atom.passwd.qatest_normal_0_bac.mysql.tddl

group：DEFAULT_GROUP

content：

encPasswd=xxxxxxx(**密文,请用tddl-atom-datasource工程下的JbossPasswordDecode**加密下明文密码)

3. 添加TDDL依赖

<dependency>

<groupId>com.taobao.tddl</groupId>

<artifactId>tddl-group-datasource</artifactId>

<version>3.0.1.5.taobaocode-SNAPSHOT</version>

</dependency>

<dependency>

<groupId>com.taobao.tddl</groupId>

<artifactId>tddl-atom-datasource</artifactId>

<version>3.0.1.5.taobaocode-SNAPSHOT</version>

</dependency>

4. 在代码中调用Group DataSource

TGroupDataSource ds = new TGroupDataSource(dbGroupKey, appName);
ds.init();

附录

请修改MAVEN/conf/settings.xml文件，确保使用正确的Maven Repository。

目前我们将Tddl动态数据源发布在淘蝌蚪的maven repository中。
```xml

<?xml version=”1.0” encoding=”UTF-8”?>

<settings xmlns=”http://maven.apache.org/SETTINGS/1.0.0“

xmlns:xsi=”http://www.w3.org/2001/XMLSchema-instance“

xsi:schemaLocation=”

http://maven.apache.org/SETTINGS/1.0.0

http://maven.apache.org/xsd/settings-1.0.0.xsd"&gt;

……

<servers>

<server>

<id>taocodeReleases</id>

<username>admin</username>

<password>admintaocode321</password>

</server>

<server>

<id>taocodeSnapshots</id>

<username>admin</username>

<password>admintaocode321</password>

</server>

</servers>

…

<profiles>

……

<profile>

<id>opensource</id>

<repositories>

<repository>

<id>taocodeReleases</id>

<name>taocode nexus</name>

<url>http://mvnrepo.code.taobao.org/nexus/content/repositories/releases/</url&gt;

</repository>

<repository>

<id>taocodeSnapshots</id>

<name>taocode nexus</name>

<url>http://mvnrepo.code.taobao.org/nexus/content/repositories/snapshots/</url&gt;

</repository>

</repositories>

</profile>

</profiles>

……

<activeProfiles>

<activeProfile>opensource</activeProfile>

</activeProfiles>

</settings>```

