# Introduction #

In YTEX v0.7 we've implemented the following features:
  * Upgraded to cTAKES 2.5.0
> Previous versions of YTEX were based on the cTAKES distributed with Maveric Arc 1.0.  With this version, we use cTAKES 2.5.0, and removed all dependencies on ARC.

  * Tested with MetaMap 2011
> We have tested YTEX with MetaMap 2011 and the MetaMap UIMA annotator.  We have configured a pipeline with MetaMap as a drop-in replacement for the cTAKES/YTEX dictionary lookup algorithm.

  * Ability to map any UIMA annotation to the database
> Previous versions of YTEX required coding to map new UIMA annotations to the database.  With this version of YTEX, mapping of UIMA annotations is done entirely via configuration.  We have preconfigured mappings for many of the cTAKES and MetaMap annotations.  You can configure the mapping of additional cTAKES annotations and annotations from other UIMA analysis engines.

In YTEX v0.8 we've added some bug fixes and enhancements for the MetaMap integration.

## Prerequisites ##
  * JDK 1.6 or higher [download](http://java.sun.com/javase/downloads/widget/jdk6.jsp)
  * Atleast 4 GB of free disk space
  * [we suggest that you install UMLS in your database](http://www.nlm.nih.gov/research/umls/implementation_resources/scripts/README_RRF_MySQL_Output_Stream.html).  NLM provides scripts for installing UMLS in MySQL and Oracle.  Refer to [UMLS SQL Server Installation](UMLS_SQL_SERVER_V06.md) for instructions on how to install UMLS in MS SQL Server.
  * Tested on Linux and Windows.  We don't test YTEX on mac; however, users have successfully installed this on mac following linux installation instructions.

## Database Prerequisites ##
YTEX supports MS SQL Server 2008, MySQL version 5.x, and Oracle versions 10gR2 and above.  Create a database user (and schema) for use with ytex.  See platform specific notes below.

### Oracle ###
As documented [here](http://www.nlm.nih.gov/research/umls/implementation_resources/scripts/index.html) your database must use the UTF-8 charset.

Make sure you use a tablespace with enough room; e.g. create the ytex user and schema like this:
```
create tablespace TBS_YTEX datafile 'C:/oracle/oradata/orcl/TBS_YTEX.dbf' size 1000M autoextend on online;
create user ytex identified by ytex default tablespace TBS_YTEX;
grant connect, resource to ytex;
grant create materialized view to ytex;
grant create view to ytex;
```

If you have installed the UMLS locally, you must also grant ytex select permissions on umls tables; e.g. assuming that umls tables are in the umls schema:
```
grant select on umls.MRCONSO to ytex;
grant select on umls.MRSTY to ytex;
grant select on umls.MRREL to ytex;
```

### MySQL ###
To create the mysql user and database, login to mysql as root and run the following commands (change as necessary):
```
CREATE DATABASE ytex CHARACTER SET utf8;
CREATE USER 'ytex'@'localhost' IDENTIFIED BY 'ytex';
GRANT ALL PRIVILEGES ON ytex.* TO 'ytex'@'localhost';
```
On mac you should use the 127.0.0.1 instead of localhost.  Note that if ytex connects to the mysql server from a different machine, you should replace localhost with the host name or ip address of the machine you will connect from, or use the wildcard ('%'):
```
CREATE USER 'ytex'@'%' IDENTIFIED BY 'ytex';
GRANT ALL PRIVILEGES ON ytex.* TO 'ytex'@'%';
```
If you have installed UMLS in your database, you must give the ytex user select permission on these tables:
```
GRANT SELECT on umls.* to 'ytex'@'%';
```

The `document` table uses the `text` and `blob` datatypes for the doc\_text column that holds the document text.  If you are processing large documents, you may need to use the `longtext` datatype instead.  Furthermore, you may have to increase the [maximum packet size](http://dev.mysql.com/doc/refman/5.5/en/packet-too-large.html).

### SQL Server ###
We have only tested YTEX with integrated authentication.  You must have the permission to create database objects in the YTEX database and schema.  If you don't have these permissions, ask your DBA to add you to the `db_ddladmin` & `db_datawriter` roles for the YTEX database.

If you want to install the UMLS in your SQL Server, you may want to use a different database/schema from the YTEX database.  If that is the case, you need permissions on the UMLS database/schema as well.

## Installation ##
These installation instructions apply to both YTEX v0.7 and v0.8; simply replace references to `0.7` with `0.8` for the v0.8 installation.

  1. Create a directory where ytex will be installed (hereafter referred to as the 'installation directory'), e.g

Linux:
```
mkdir ${HOME}/clinicalnlp
```

Windows:
```
c:\clinicalnlp
```

  1. Download the following archives to the installation directory:

  * YTEX Archive: `ytex-with-dependencies-v0.7.zip`
> Download from this site

  * YTEX UMLS Archive: [umls.zip](http://www.ytex-nlp.org/umls.download/secure/0.7/umls.zip) (optional, UTS login required)
> This archive contains 'concept graphs' used to compute semantic similarity measures, and UMLS 2011AB data for named entity recognition.  If you don't download this archive, WSD will be disabled.  If you neither have the UMLS installed nor download this archive, the installation will load a tiny subset of the UMLS for demonstration purposes.  For production use, [we suggest that you install UMLS in your database](http://www.nlm.nih.gov/research/umls/implementation_resources/scripts/README_RRF_MySQL_Output_Stream.html).  If you cannot/don't want to install UMLS, you can use the subset of the UMLS that we provide in this archive: the 2011AB SNOMED-CT and RXNORM source vocabularies.
    * If you have not done so already, obtain a UMLS License and create a UMLS Technology Services (UTS) Account, available free of charge: https://uts.nlm.nih.gov/home.html
    * Download the YTEX UMLS Archive: [umls.zip](http://www.ytex-nlp.org/umls.download/secure/0.7/umls.zip).  A valid UTS Login is required. and place it in the installation directory.
  * LVG 2008 (optional) and cTAKES 2.5.0: If the machine you are installing on does not have an internet connection, you will have to download these files manually.  If they are not found in the installation directory, the YTEX installation script will attempt to download these files.  If you already have cTAKES 2.5.0 installed, configure YTEX to use your installation (see below); in this case YTEX will not download and install cTAKES.
    * LVG 2008 Archive: [lvg2008lite.tgz](http://lexsrv3.nlm.nih.gov/LexSysGroup/Projects/lvg/2008/release/lvg2008lite.tgz)(optional)
> > Make sure you save this file as `lvg2008.tgz` (your browser may decompress or expand this file). The lvg is required for stemming; if you do not download this file, stemming will not be performed.
    * cTAKES 2.5.0: Download [cTAKES-2.5.0.zip](http://sourceforge.net/projects/ohnlp/files/cTAKES/v2.5/cTAKES-2.5.0.zip/download)

  1. Extract `ytex-with-dependencies-0.7.zip` to the installation directory; this will create a ytex subdirectory, e.g. windows `c:\java\clinicalnlp\ytex-0.7` / linux `${HOME}/clinicalnlp/ytex-0.7`, referred to subsequently as YTEX\_HOME.  This will also extract ant and tomcat; if you already have these installed, you may want to extract just the `ytex-0.7` directory from the archive, and configure ytex to use your existing ant & tomcat installation (set in `setenv.bat`/`ytex.profile`).
  1. Edit environment batch/shell script, and Fix the path references to match your environment.  If you have cTAKES 2.5.0 installed already, set the <tt>CTAKES_HOME</tt> variable in this script.  Note to linux users: there is a small bug in `ytex.profile` - You need to add `:${CTAKES_HOME}/cTAKES.jar` to the end of the `CLASSPATH` variable.
    * windows edit <tt>YTEX_HOME\setenv.cmd</tt>
    * linux move <tt>YTEX_HOME/ytex.profile</tt> to <tt>${HOME}/ytex.profile</tt>

  1. Create `YTEX_HOME\config\desc\ytex.properties`: In this file, you specify the database connection parameters. Use `YTEX_HOME\config\desc\ytex.properties.<db type>.example` as a template.  If you have UMLS installed on your database, specify the `umls.schema` and `umls.catalog` properties (see the properties file for an explanation of what these are).
  1. Execute the setup script.
    * **windows**: Open a command prompt, navigate to YTEX\_HOME, and execute setup script:
```
cd /d c:\clinicalnlp\ytex-0.7
setup all
```
    * **linux**: From a shell, cd to the YTEX\_HOME directory, set the environment, make sure necessary scripts are executable, and execute the ant script:
```
chmod u+x ${HOME}/ytex.profile
cd ${HOME}/clinicalnlp/ytex-0.7
chmod u+x ../apache-ant-1.8.0/bin/ant
chmod u+x *.sh
. ${HOME}/ytex.profile
nohup ant -buildfile build-setup.xml -Dytex.home=${YTEX_HOME} all > setup.out 2>&1 &
tail -f setup.out
```

This will call the ant script `build-setup.xml`, which does the following:
  * Download and install cTAKES 2.5.0 and LVG 2008
  * Generates configuration files from templates
  * Sets up Semantic Search Web Application

> The semantic search web application will be deployed to a tomcat server under `YTEX_HOME\web\catalina`.
  * Sets up YTEX Database Objects
> The installation executes SQL scripts located in the `YTEX_HOME\data` directory, and imports umls tables (if they are not already in the database). All YTEX database objects will be dropped and recreated. If this is the initial installation, ignore the errors about objects not existing when they are being dropped.  If you have installed the UMLS in your database and configured YTEX to use it, YTEX will create a dictionary lookup table with all concepts from the UMLS.  The setup speed is dependent on the latency between the machine you are installing on and the database server.  This can take several hours.