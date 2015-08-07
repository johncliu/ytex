# Introduction #
NLM does not provide scripts/utilities for importing tables into SQL Server. To import tables into SQL Server, we have adapted the database table creation scripts from other platforms to SQL Server.  MetamorphoSys creates RRF files, which are pipe (|) delimited UTF-8 files. We load the RRF files into SQL Server Tables using jdbc.

# Running the Script #

This is how it works:
  1. You must run MetamophoSys to create a subset of the UMLS.
> See http://www.ncbi.nlm.nih.gov/books/NBK9683/.
> Metamorphosys will create RRF files with the table data.
> Metamorphosys will create directories of the form 2010AB/META and 2010AB/NET
> that contain RRF files.
  1. In `CTAKES_HOME\resources\org\apache\ctakes\ytexytex.properties`, define the following properties:
    * `umls.catalog` the catalog (database) where UMLS should be installed
    * `umls.schema` the schema where umls should be installed.
    * `rrf.home` the path to the RRF files, e.g. `c:/temp/umls2011AB/subset/2011AB`.  This folder should contain the `META` and `NET` directories that contain the RRF files for the Metathesaurus and semantic network respectively.
  1. Run the ant script to load the UMLS tables
Open a command prompt, change to the CTAKES\_HOME\bin\ctakes-ytex\scripts\data directory, and execute the following command:
```
..\..\..\ant umls.mssql.setup > umls.out 2>&1
```
View `umls.out` to see the progress of the umls setup.

This does the following:
  * create umls database tables
  * load RRF files into umls database tables
  * create indices for newly created tables

Configuration: all settings are taken from ytex.properties.  The following properties control the import:
  * meta.tables: comma-delimited list of UMLS metathesaurus tables to import.
> Defaults to: MRDOC,MRSTY,MRCONSO,MRREL
  * net.tables: comma-delimited list of UMLS semantic network tables to import.
> Defaults to: SRDEF,SRFIL,SRFLD,SRSTR,SRSTRE1,SRSTRE2