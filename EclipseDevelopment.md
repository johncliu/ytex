# Introduction #
This page describes checking YTEX out and setting up an eclipse development environment.  To understand what's going on, you need a solid grasp of UIMA.  YTEX also makes extensive use of [Spring](http://www.springsource.org/about) and [Hibernate](http://www.hibernate.org/).  Many tasks are automated with ant; we assume you know how to configure and execute ant tasks in eclipse.

## Prerequisites ##
  * Eclipse Galileo or higher (Helios)
  * [Eclipse SVN Plugins](http://www.eclipse.org/subversive/)
  * [Eclipse Maven Plugin](http://www.eclipse.org/m2e/)
  * [Apache Tomcat v6.x](http://tomcat.apache.org/download-60.cgi)
  * [Ant-Contrib](http://sourceforge.net/projects/ant-contrib/) (included with ytex-with-dependencies)
  * Jdbc Drivers (included with ytex-with-dependencies, see [Installation](Installation.md))
  * LVG 2008(see [Installation](Installation.md))
  * UMLS Database Export (see [Installation](Installation.md))
  * Maven
  * Ant 1.8 or higher
  * [ant-contrib](http://sourceforge.net/projects/ant-contrib/files/ant-contrib/1.0b3/ant-contrib-1.0b3-bin.zip/download) - unzip `ant-contrib-1.0b3.jar` to `ANT_HOME/lib`
  * Weka 3.6
  * cTAKES 2.5 (see [Installation](Installation.md))
  * MetaMap 2011 or higher (see [Installation](Installation.md))
  * SQL Server Users: add the SQL Server JDBC Driver DLLs to your path.
    * Control Panel -> Edit System Environment Variables -> Environment Variables.
    * Add/Edit the `PATH` variable in the user variable section
    * set it to e.g. `C:\java\sqljdbc_3.0\enu\auth\x86` (of course this should match your environment)

## Setup ##
  * Setup Eclipse Workspace
> Assume the workspace is e.g. `c:\projects\ytex`.  Open eclipse, and select this workspace.
  * Checkout
    * Setup Repository: https://ytex.googlecode.com/svn/
    * Checkout the latest stable version.  The trunk is used for active development.  For a stable version, look at the svn tags.  Right click on v0.3 -> find / checkout as -> find projects in the children of the selected resource
  * Download dependenices via maven.
    * Install weka in maven:
```
mvn install:install-file -Dfile=<WEKA Install Dir>\weka.jar -DgroupId=weka -DartifactId=weka -Dversion=3.6 -Dpackaging=jar
```
    * Copy dependencies to ytex manage dependencies using maven; you must download required libraries using maven.  Run the `process-sources` goal on `ytex.web/pom.xml` file.  There is a run configuration named `ytex.web process-sources` - run this.
  * Create config.local project
> We try to keep local configuration settings separate from checked-in code.  Create a java project named `config.local`, and create a `ytex.properties` file in the root of this project's source directory, i.e. `${workspace_loc}/config.local/src/ytex.properties`.  Use `${workspace_loc}/config/desc/ytex.properties.*.example` as a template.  Also add the following property:
```
kernel.cp=kernel.cp.dev
```
  * Setup Workspace Preferences:
> Go to Window -> Preferences
    * Add tomcat: Server -> Runtime Environment -> Add -> Apache Tomcat v6.0
    * Add ant-contrib: Ant -> Runtime -> Classpath -> Global Entries
    * Setup ant properties: Ant -> Runtime -> Properties -> Add Property, and add the following properties:
| name | value |
|:-----|:------|
| `config.local`| `${workspace_loc}/config.local/src` |
| `ytex.properties` | `${workspace_loc}/config.local/src/ytex.properties` |
| `ytex.home` | `${workspace_loc}` |
| `mysql.home` | necessary only for mysql users.  location of mysql.exe, e.g. C:\Program Files\MySQL\MySQL Server 5.1\bin |
-> Add Jar, and add `ant-contrib.jar`
  * Setup config files: execute `templateToConfig` in build/build-setup.xml`
  * Setup classpath variables: Wiindow -> Preferences -> Java Build Path -> Classpath Variables -> New
    * MM\_HOME - metamap install dir
    * CTAKES\_HOME - ctakes install dir
  * Setup lvg:
    * copy the `lvg2008.tgz` file to the directory that contains the workspace.  e.g. if your workspace is `c:\projects\ytex`, copy the lvg file to `c:\projects`.
    * execute `setup.lvg` in build/build-setup.xml`
  * Setup Database:
    * copy database archive (e.g. `umls-mysql.zip`) to the directory that contains the workspace.
    * execute `all` in `data/build.xml`
  * Disable validators in ytex.web project.  For some reason, eclipse doesn't like the syntax of the JSPs (they work fine).  Right-click on ytex.web -> Properties -> Validation -> Disable All -> Apply -> OK
  * Select all projects -> Right Click -> Refresh
> Now everything should compile.

## Tomcat setup ##
  * Open Servers tab, Add Tomcat Server, Add ytex.web to server
  * Open Server -> Open Launch Configuration
    * Environment: add PATH=${workspace\_loc}/libs.system/sqljdbc\_3.0/enu/auth/x86
    * Arguments: add
> > -Djava.util.logging.config.file=[file:///${workspace_loc}/config/desc/Logger.properties](file:///${workspace_loc}/config/desc/Logger.properties)
> > -Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager
> > -Dorg.apache.el.parser.COERCE\_TO\_ZERO=false
    * Classpath: Add libs.system to server classpath

Everything should work

## Testing ##
You can launch UIMA and YTEX tools directly from eclipse.  If you look in the Debug Configurations (the bug icon), you should see several run configurations including
  * `ytex CPE` - run the Collection Processing Engine
  * `ytex DBAnnotationViewer` - run the Annotation Viewer
  * `ExportBagOfWords` - to run the ytex bag-of-words exporter