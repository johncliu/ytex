

# Prerequisites #
  * Database Utilities
> YTEX supports MS SQL Server 2008, MySQL version 5.x, and Oracle versions 10gR2 and above.  To set up database objects, the installation requires database-specific utilities:
    * MS SQL Server: [SQL Server Tools](http://www.microsoft.com/downloads/details.aspx?familyid=08E52AC2-1D62-45F6-9A4A-4B76A8564A2B&displaylang=en) and [SQL Server Command-Line Utilities](http://www.microsoft.com/downloads/en/details.aspx?FamilyId=C6C3E9EF-BA29-4A43-8D69-A2BED18FE73C&displaylang=en)
    * Oracle: SQL\*Plus and SQL\*Loader
    * MySQL: mysql installation
  * Database User and Schema.
> Create a database user (and schema) for use with ytex.
  * JDK 1.6 [download](http://java.sun.com/javase/downloads/widget/jdk6.jsp)
  * Atleast 2 GB of free disk space

# Installation #
This assumes a Windows installation.  YTEX should work on unices (tests and instructions pending)
  1. Create a directory where ytex and its dependencies will be installed, e.g. `c:\java\clinicalnlp`
  1. Download the following archives to the installation directory:
    * YTEX Archive: `ytex-with-dependencies.zip`
> > Download from this site
    * LVG 2010 Archive: `lvg2010lite.tgz` (optional)
> > ([download](http://lexsrv3.nlm.nih.gov/LexSysGroup/Projects/lvg/2010/release/lvg2010lite.tgz)).  Make sure you save this file as `lvg2010lite.tgz` (Internet Explorer may rename this).  The lvg is required for stemming; if you do not download this file, stemming will not be performed.
    * UMLS Database Export
> > UMLS is used for Named Entity Recognition.  The YTEX distribution includes a small subset of the UMLS that contains only concepts for the examples. To obtain a larger subset of the UMLS for use with YTEX, do the following:
      * If you have not done so already, obtain a UMLS License and create a UMLS Technology Services (UTS) Account, available free of charge: https://uts.nlm.nih.gov/home.html
      * Download the YTEX UMLS Database Archive from http://www.ytex-nlp.org/umls.download/secure/index.html.  A valid UTS Login is required.
  1. Extract `ytex-with-dependencies.zip` to the installation directory; this will create a `ytex` subdirectory, e.g. `c:\java\clinicalnlp\ytex`, referred to subsequently as `YTEX_HOME`.
  1. Edit `YTEX_HOME\setenv.cmd`: Fix the path references to match your environment.
  1. Create `YTEX_HOME\config\desc\ytex.properties`: In this file, you specify the database connection parameters.  Use `YTEX_HOME\config\desc\ytex.properties.<db type>.example` as a template.
  1. Execute setup.cmd

> Open a command prompt, navigate to `YTEX_HOME`, and execute `setup.cmd all`.  This will call the ant script `build-setup.xml`, which does the following:
    * Generates configuration files from templates
    * Sets up YTEX Database Objects
> > The installation executes SQL scripts located in the `YTEX_HOME\data` directory, and imports umls tables.  All YTEX database objects will be dropped and recreated.  If this is the initial installation, ignore the errors about objects not existing when they are being dropped.
    * Unpacks lvg data files
    * Sets up Semantic Search Web Application
> > The semantic search web application will be deployed to a tomcat server under `YTEX_HOME\web\catalina`.

# YTEX Dependencies #
YTEX is distributed with the following software:
  * [MS SQL Server JDBC Driver v3.0](http://www.microsoft.com/downloads/details.aspx?displaylang=en&FamilyID=a737000d-68d0-4531-b65d-da0f2a735707) (`.\sqljdbc_3.0`)

> This is required for connectivity to the SQL Server Database.
  * [Oracle Database 11g Release 2 JDBC Drivers](http://www.oracle.com/technology/software/tech/java/sqlj_jdbc/htdocs/jdbc_112010.html) (`.\oracle11.2.0.1.0`)
  * [MySQL Connector/J](http://www.mysql.com/downloads/connector/j/) (`.\mysql-connector-java-5.1.9`)
  * [Maveric ARC Pipeline](http://research.maveric.org/mig/arc/arc_v1.0_release_notes.html) (`.\ytex\maveric`).
> YTEX uses the cTAKES version distributed with Maveric ARC.
  * [Apache Ant](http://ant.apache.org/bindownload.cgi) (`.\apache-ant-1.8.0`)
> The YTEX setup is automated via ant, a java-based scripting language.
  * [Ant Contrib](http://sourceforge.net/projects/ant-contrib/files/) (`.\apache-ant-1.8.0`)
  * [Apache Tomcat](http://tomcat.apache.org/download-60.cgi) (`.\apache-tomcat-6.0.26`)
> The YTEX semantic search engine is distributed as a java web application, and can be deployed on Tomcat.

If you have already installed this software, you can install the YTEX using `ytex.zip` archive, which only contains YTEX and Maveric ARC.