# Introduction #
NLM does not provide scripts/utilities for importing tables into SQL Server.
To import tables into SQL Server, we have adapted the database table creation
scripts from other platforms to SQL Server.  We reformat the output
of [NLM's MetamorphoSys](http://www.ncbi.nlm.nih.gov/books/NBK9683/) to enable import using the SQL Server bcp utility.

MetamorphoSys creates RRF files, which are pipe (|) delimited UTF-8 files.
SQL Server does not support UTF-8, but does support UCS2 (unicode).
To simplify the import, we convert the RRF files into tab-delimited UCS2 files.

# Running the Script #

This is how it works:
  1. You must run MetamophoSys to create a subset of the UMLS.
> See http://www.ncbi.nlm.nih.gov/books/NBK9683/.
> Metamorphosys will create RRF files with the table data.
> Metamorphosys will create directories of the form 2010AB/META and 2010AB/NET
> that contain RRF files.
  1. In `ytex.properties`, define `umls.catalog` and `umls.schema`, and set them to the catalog (database) and schema where the umls should be installed.
  1. Run the ant script `build-mssql-umls.xml`, and specify the directory which contains the META and NET directories via the rrf.home option.  Open a command prompt, change to the YTEX\_HOME directory, and execute the following commands:
```
setenv.cmd
cd data
ant -Dytex.home=%YTEX_HOME% -Drrf.home=c:\temp\2010AB -f build-mssql-umls.xml all
```

This does the following:
  * create umls database tables
  * convert RRF files into tab-delimited UCS2 files
  * import the tab-delimited files using bcp
  * create indices for newly created tables

Configuration: all settings are taken from ytex.properties.  The following
properties control the import:
  * umls.catalog: the catalog (database) to install the umls in.
> This defaults to the ytex database (db.name)
  * umls.schema: the schema to install the umls in.
> Defaults to the ytex schema (db.schema)
  * meta.tables: comma-delimited list of UMLS metathesaurus tables to import.
> Defaults to: MRCUI,MRDOC,MRSAB,MRSTY,MRCONSO,MRREL
  * net.tables: comma-delimted list of UMLS semantic network tables to import.
> Defaults to: SRDEF,SRFIL,SRFLD,SRSTR,SRSTRE1,SRSTRE2