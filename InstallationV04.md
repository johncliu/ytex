# Introduction #

In YTEX v0.4 we've implemented the following features:

  * Support for Linux.  We have tested YTEX on Linux w/ MySQL.

  * Support for updated UMLS.  For previous versions, we sent users UMLS data for named entity recognition based on an older version of the UMLS, and using only the SNOMED, ICD, and NCI vocabularies.  For this version, we use the [UMLS tables](http://www.ncbi.nlm.nih.gov/books/NBK9685/#ch03.I31_Data_Files) directly.  We recommend that you install UMLS in your database.  We also provide a subset of the UMLS 2010AB that contains SNOMED and RXNORM for users that do not want to/are unable to install the UMLS locally.

  * Support for exporting data to R, Matlab, SAS, Libsvm, and SVMLight



## Prerequisites ##
  * Database Utilities
> YTEX supports MS SQL Server 2008, MySQL version 5.x, and Oracle versions 10gR2 and above.  To set up database objects, the installation requires database-specific utilities:
    * MS SQL Server: [SQL Server Tools](http://www.microsoft.com/downloads/details.aspx?familyid=08E52AC2-1D62-45F6-9A4A-4B76A8564A2B&displaylang=en) and [SQL Server Command-Line Utilities](http://www.microsoft.com/downloads/en/details.aspx?FamilyId=C6C3E9EF-BA29-4A43-8D69-A2BED18FE73C&displaylang=en)
    * Oracle: SQL\*Plus and SQL\*Loader
    * MySQL: mysql installation
  * Database User and Schema.
> Create a database user (and schema) for use with ytex.
  * JDK 1.6 [download](http://java.sun.com/javase/downloads/widget/jdk6.jsp)
  * Atleast 2 GB of free disk space

As mentioned above, [we suggest that you install UMLS in your database](http://www.nlm.nih.gov/research/umls/implementation_resources/scripts/README_RRF_MySQL_Output_Stream.html).  NLM provides scripts for installing UMLS in MySQL and Oracle.  Refer to [UMLS SQL Server Installation](UMLS_SQL_SERVER.md) for instructions on how to install UMLS in MS SQL Server.


## Installation ##
  1. Create a directory where ytex will be installed, e.g

Linux:
```
mkdir ${HOME}/clinicalnlp
```

Windows:
```
c:\java\clinicalnlp
```

  1. Download the following archives to the installation directory:
    * YTEX Archive: `ytex-with-dependencies-vX.Y.zip`
> > Download from this site
    * LVG 2011 Archive: `lvg2011lite.tgz` (optional)
> > ([download](http://lexsrv3.nlm.nih.gov/LexSysGroup/Projects/lvg/2011/release/lvg2011lite.tgz)). Make sure you save this file as lvg2011lite.tgz (Internet Explorer may rename this). The lvg is required for stemming; if you do not download this file, stemming will not be performed.
      * UMLS Database Export (Required if you do not have UMLS installed in your DB)
> > UMLS is used for Named Entity Recognition.  The YTEX distribution includes a tiny subset of the UMLS for demonstration purposes.  For production use, [we suggest that you install UMLS locally](http://www.nlm.nih.gov/research/umls/implementation_resources/scripts/README_RRF_MySQL_Output_Stream.html).  If you cannot/don't want to install UMLS, you can get a subset of the UMLS 2010AB that contains SNOMED-CT and RXNORM from us:
      * If you have not done so already, obtain a UMLS License and create a UMLS Technology Services (UTS) Account, available free of charge: https://uts.nlm.nih.gov/home.html
      * Download the YTEX UMLS Database Archive from http://www.ytex-nlp.org/umls.download/secure/index.html.  A valid UTS Login is required.
      * Place the archive (`umls-<platform>.zip`) in the installation directory as well
  1. Extract `ytex-with-dependencies-vX.Y.zip` to the installation directory; this will create a ytex subdirectory, e.g. windows `c:\java\clinicalnlp\ytex` / linux `${HOME}/clinicalnlp/ytex`, referred to subsequently as YTEX\_HOME.
  1. Edit environment batch/shell script, and Fix the path references to match your environment
    * windows edit <tt>YTEX_HOME\setenv.cmd</tt>
    * linux move <tt>YTEX_HOME/ytex.profile</tt> to <tt>${HOME}/ytex.profile</tt>
  1. Create `YTEX_HOME\config\desc\ytex.properties`: In this file, you specify the database connection parameters. Use `YTEX_HOME\config\desc\ytex.properties.<db type>.example` as a template.  If you have UMLS installed on your database, specify the `umls.schema` and `umls.catalog` properties (see the properties file for an explanation of what these are).  For oracle, set the change the `db.isolationLevel` property to `READ_COMMITTED` (to be fixed in a future version).
  1. Execute the setup script.
    * **windows**: Open a command prompt, navigate to YTEX\_HOME, and execute setup script:
```
cd /d c:\clinicalnlp\ytex
setup all
```
    * **linux**: From a shell, cd to the YTEX\_HOME directory, set the environment, make sure necessary scripts are executable, and execute the ant script:
```
chmod u+x ${HOME}/ytex.profile
cd ${HOME}/clinicalnlp/ytex
chmod u+x ../apache-ant-1.8.0/bin/ant
chmod u+x *.sh
. ${HOME}/ytex.profile
ant -buildfile build-setup.xml -Dytex.home=${YTEX_HOME} all > setup.out 2>&1 &
tail -f setup.out
```

This will call the ant script `build-setup.xml`, which does the following:
  * Unpacks lvg data files
  * Generates configuration files from templates
  * Sets up Semantic Search Web Application

> The semantic search web application will be deployed to a tomcat server under `YTEX_HOME\web\catalina`.
  * Sets up YTEX Database Objects
> The installation executes SQL scripts located in the `YTEX_HOME\data` directory, and imports umls tables (if they are not already in the database). All YTEX database objects will be dropped and recreated. If this is the initial installation, ignore the errors about objects not existing when they are being dropped.

## Notes on UMLS Installation ##
During setup, we try to see if the UMLS is installed in the database (we look for the MRCONSO table); if we don't find it, we look for a <tt>umls-(platform).zip</tt> file with the tables. If we don't find that we load the sample data files included in the YTEX distribution - this contains a tiny subset of the UMLS for use with the YTEX examples.

The cTAKES Database Lookup algorithm requires a table/view that contains the first word of a concept, a concept code, and the full text of the concept.  The <tt>MRCONSO</tt> table contains concept codes (CUI field) and the full text of the concept (STR field).  We generate a table <tt>umls_aui_fword</tt> that contains the first word of every concept in the <tt>MRCONSO</tt> table.  We then join the <tt>umls_aui_fword</tt> and <tt>MRCONSO</tt> tables, to create a view ([for mysql a table](MySQL.md)) that contains a subset of the UMLS (by default, the SNOMED-CT and RXNORM vocabularies).  You are free to replace this view with subsets of the UMLS of your liking; refer to <tt>YTEX_HOME/data/mysql/umls/create_view.template.sql</tt>.

### Differences between cTAKES/ARC/YTEX ###
You are free to use whatever dictionary you like with cTAKES/ARC/YTEX.  'By default' cTAKES/ARC are configured to use a database table for SNOMED-CT, and a lucene index for RXNORM.  In addition cTAKES/ARC store the SNOMED Codes for concepts.

In contrast, YTEX uses only a database table for lookup, and only stores the UMLS CUIs.  The reasons for this include:
  * Performance
> Using the RXNORM lucene index incurs some memory requirements (you need a 1gb heap at least to load the lucene index).  For each UMLS concept it finds, cTAKES executes another query to find the corresponding SNOMED codes.  Therefore, for each word that cTAKES tries to map, cTAKES is running 2 queries (snomed + rxnorm lookups) + n queries (cui to snomed lookup).  YTEX in contrast looks in a single database table.  Thus, the dictionary lookup in YTEX runs just 1 query (umls lookup).
  * Cleaner Data Model
> cTAKES/ARC store the SNOMED-CT code and the UMLS CUI.  YTEX in contrast stores only the UMLS CUI: YTEX is DB-oriented, and storing only the UMLS CUI corresponds to a normalized data model.  To map UMLS CUIs to codes from any source vocabulary (e.g. SNOMED-CT/RXNORM), you can simply join the <tt>ytex.anno_ontology_concept</tt> and <tt>umls.mrconso</tt> tables.

If prefer the way cTAKES is doing things, you can configure YTEX to get the same functionality.  Refer to [the cTAKES documentation](http://ohnlp.sourceforge.net/cTAKES/#boost_performance) for information on setting up the tables / lucene indices and configuring the Dictionary Lookup Annotator.

## Notes on running examples on linux ##
We've created .sh files for many of the .cmd files.  For the others, you should be able to figure out what to do by looking at the .cmd files.

## DB Platform Specific Notes ##
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

You must also grant ytex select permissions on umls tables; e.g. assuming that umls tables are in the umls schema:
```
grant select on umls.MRCONSO to ytex;
grant select on umls.MRSTY to ytex;
```

### MySQL ###
The `document` table uses the `text` and `blob` datatypes for the doc\_text column that holds the document text.  If you are processing large documents, you may need to use longtext the datatype instead.  Furthermore, you may have to increase the [maximum packet size](http://dev.mysql.com/doc/refman/5.5/en/packet-too-large.html).