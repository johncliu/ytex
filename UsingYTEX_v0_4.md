

# Using YTEX v0.4 #
This document presumes familiarity with [UIMA](http://uima.apache.org/index.html) and [cTAKES](http://ohnlp.sourceforge.net/cTAKES/).

# Annotating Documents #
YTEX is based on UIMA, and standard UIMA Tools can be used to annotate documents with YTEX: you can annotate documents using the standard UIMA Document Analyzer and Collection Processing Engine.  YTEX is distributed with the `YTEXPipeline.xml` analysis engine configuration based on the cTAKES/ARC pipeline with the following modifications:
  * `SentenceAnnotator`: modified to not break sentences on newlines, and to break sentences at (certain) periods.
  * `SegmentRegexAnnotator`: annotate sections based on regular expressions.
  * `DictionaryLookup`: Configured to use the SNOMED-CT sub-vocabulary of the UMLS.
  * `NamedEntityRegexAnnotator`: annotate named entities based on regular expressions.
  * `NegexAnnotator`: this replaces the cTAKES NegationAnnotator.  The NegexAnnotator is based on the Negex negation detection algorithm.
  * `DBConsumer`: Stores annotatations and the XML CAS representation in the database
You are of course welcome to modify/add/remove annotators to the analysis engine and modify configuration parameters.

## UIMA Document Analyzer ##
  * start the documentAnalyzer.  windows: double-click/execute `YTEX_HOME\ytexDocumentAnalyzer.bat`, linux: from a shell run the following commands:
```
. ${HOME}/ytex.profile
cd ${YTEX_HOME}
./ytexTools.sh DocumentAnalyzer
```
  * Set the following options:
    * Input Directory: `YTEX_HOME\examples\pubmed\abstracts`
    * Output Directory: `YTEX_HOME\examples\pubmed\ytexoutput` (create this directory)
    * Analysis Engine: `YTEX_HOME\config\desc\ytex\uima\YTEXPipelineNoDBConsumer.xml` - this Pipeline will not store annotations in the database.
  * Click Run
  * You can then view the anaysis results by double-clicking individual documents.

## UIMA CPE - loading document from the database ##
Clinical Documents from an EMR are typically stored in a database.  We developed an UIMA CollectionReader that retrieves these documents for annotation (thereby avoiding exporting the documents to the file system).  The DBCollection reader works as follows:
  1. read all document unique ids into an internal list
  1. iterate through each id in the list, and retrieve the document for annotation.

Documents in a database typically are given a Unique ID, and are cross-referenced with other information (e.g. patient id, document type, document date, etc.).  This unique id can be stored in the database along with other document annotations.  This allows you to cross-reference annotation data with other document data.

By default, YTEX supports an integer document unique identifier; YTEX can be configured to support additional/different document identifiers (see [Document Key Example](DocKey_v0_4.md)).

The YTEX DBCollection reader is parameterized by 2 queries: a key query and a document query.  The key query  loads document unique ids from a database, and the document query loads a document for a given unique id.  For more information, refer to the example below.

### CPE DBCollectionReader Example ###
To illustrate this, we can simply retrieve sample documents stored in the `fracture_demo` table.  To configure and execute the CPE, do the following:
  * Start CPE Configuration Tool
> Windows: run `YTEX_HOME\ytexCPE.bat`, Linux: from YTEX\_HOME run `./ytexTools.sh CPE`
  * Clear the CPE Configuration: File->Clear
  * Configure the Collection Reader: In the Collection Reader section click the Browse Button.  For the Descriptor, specify `YTEX_HOME\config\desc\ytex\uima\DBCollectionReader.xml`.
  * Configure Queries: The 'Query Document Keys' and 'Query Get Document' parameters will appear.  Specify queries to retrieve keys and documents.  The key query returns a integer key, named `uid`.  The document query has a `uid` parameter.  The `uid` is stored in the ytex document table, allowing you to link annotations to the source document.  Here are the queries for the fracture demo:
    * MS SQL Server
Document Key Query (replace schema to match your configuration)
```
    select note_id uid from <schema>.fracture_demo
```
Document Query
```
    select note_text from <schema>.fracture_demo where note_id = :uid
```
    * MySQL
Document Key Query
```
    select note_id uid from  fracture_demo
```
Document Query
```
    select note_text from fracture_demo where note_id = :uid
```
    * Oracle
Document Key Query
```
    select note_id "uid" from fracture_demo
```
Document Query
```
    select note_text from fracture_demo where note_id = :uid
```
  * Configure Analysis Engine
> Click on the 'Add' button in the 'Analysis Engine' section, and select `YTEX_HOME\config\desc\ytex\uima\YTEXPipeline.xml`
    * analysis\_batch: Documents can be assigned a 'group' or analysis\_batch.  specify 'test2' here.
  * Run the CPE
> Click the 'Play' button
  * View results
```
  select * from 
  v_document where analysis_batch = 'test2'
```
> Notice that the uid column is set, and refers to the `fracture_demo.note_id`.

### Example - Custom Key Mapping ###
This example demonstrates how to use custom document keys.  Let's assume that clinical documents have a unique identifier that comprises 2 fields - `note_id` (integer) and `site_id` (character) - and that these documents are stored in the `fracture_demo` table.  We would like to link our annotations to the original document, so we need to store both the `note_id` and `site_id` in the ytex `document` table.  In this example, we map these columns to the `uid` and `site_id` columns in the `document` table.

To run this example, do the following:
  * modify the ytex document table and add a site\_id column. e.g. for MS SQL Server:
```
alter table document add site_id varchar(20)
```

  * Start the collection processing engine
> Windows: Run YTEX\_HOME/ytexCPE.cmd
> Unix: from a shell run the following commands
```
. ${HOME}/ytex.profile
cd ${YTEX_HOME}
./ytexTools.sh CPE 
```
  * Open the CPE Descriptor
> Go to File->Open, and select `YTEX_HOME/examples/cpe-fracture/fracture-demo.[platform].cpe.xml`.  The query to get the keys looks like this (notice how note\_id was renamed to uid):
```
select note_id uid, site_id from fracture_demo
```
> The query to get the document looks like this:
```
select note_text from fracture_demo where note_id = :uid and site_id = :site_id
```
> The CPE config "Store Doc Text" checkbox is unchecked: we will note store the document text in the document.doc\_text column, because it is already in the database - we can join the document and fracture\_demo tables on the uid/site\_id columns to get the corresponding text.

  * Run the CPE
> Press the play button

  * Verify that the uid and site\_id fields have been set
```
select * from document where analysis_batch = 'cpe-fracture'
```


## UIMA CPE - loading document from the file system ##
Instead of using the DBCollectionReader to load documents from the database, you can use the FileSystemCollectionReader to load documents from the file system.

### CPE File System Example ###
This example demonstrates annotating documents with the Collection Processing Engine (CPE).  We annotate the contents of the `YTEX_HOME\examples\pubmed\abstracts` directory that contains a handful of pubmed abstracts.

To run this example, do the following:
  * open a command prompt / shell
  * change to the `YTEX_HOME\examples\pubmed` directory
  * start the UIMA CPE configurator.  Windows:
```
..\..\ytexCPE.cmd
```
Unix:
```
. ${HOME}/ytex.profile
${YTEX_HOME}/ytexTools.sh CPE
```
  * click the 'Play' button

YTEX will annotate the documents and store them in the database.
To take a quick look at the concept annotations, run the following query against your database:
```
select * from v_document_cui_sent where analysis_batch = 'pubmed'
```

## YTEX Pipeline Configuration Parameters ##
The UIMA CPE Configurator (yes it is ugly) allows you to override parameters defined in the UIMA Pipeline.  Some of these properties are:

  * Patterns
> Regex used by the paragraph annotator to split paragraphs.

  * analysis\_batch
The `analysis_batch` is a way to identify document annotation runs or groups of documents.  It is stored in the `document.analysis_batch` column.

  * Store CAS
> Should the UIMA XML representation of document annotations be stored in the database?  The gzipped uima xml is stored in the `document.cas` column.

  * Store Doc Text
> Should the document text be stored in the database?  The document text is stored in the `document.doc_text` column.

  * XMI Output Directory
> Directory where UIMA XML representations of document annotations should be stored; if empty they will not be stored in the file system.

  * Types to Ignore
> UIMA annotations that should not be stored in the database.
Take a look at the `ref_uima_type` table for a list of types stored in the database. The class name should give you an idea of what each annotation represents.

## DBCollectionReader Configuration Parameters ##
  * Query Document Keys
> This query retrieves the document keys - the way to uniquely identify each document.

  * Query Get Document
> This query retrieves the document using a key.
> Key parameters are specified using `:`.
> The column names returned from the document key query must match the parameter names exactly (case sensitive).  Oracle users: oracle will return all column names as uppercase, unless you rename them using a quoted string (as in the uid column in the example above).
  * Key to document column mapping
> We match key values to columns in the `document` table by column name (case-insensitive) and data type.  We have tested this with non-decimal numeric types (short, int, bigint ...) and character types (char, varchar, ...).  Problems may arise with illegal column names that require escaping.

# Retrieving Documents #

## Using YTEX Views ##
Some simple queries (replace schema to match your configuration):
  * View all documents
```
select top 1000 document_id, doc_text
from <schema>.v_document
```
  * View all sentences that contain a negated UMLS Concept
```
select * 
from <schema>.v_document_cui_sent
where code = 'C0024228'
and certainty = -1
```

## Using YTEX DBAnnotationViewer ##
For a graphical representation of document annotations, use the DBAnnotationViewer.  This modified viewer retrieves the document CAS from the database (as opposed to the plain-vanilla AnnotationViewer which retrieves the CAS from the file system).
  * Execute `YTEX_HOME\ytexDBAnnotationViewer.bat`
  * Select `YTEX_HOME\config\desc\ytex\uima\YTEXPipeline.xml` as the annotation engine
  * Specify document id (e.g. from one of the views)

# Semantic Search Engine #
The semantic search engine allows searching by UMLS Concept id, negation status, patient id, and document date.  It is preconfigured to work with the patient id and document date from VACS Annotations, but can be reconfigured to retrieve these annotations from other tables.  The semantic search engine can also be configured to execute full-text searches (the SQL Server Full Text Index must be enabled for this).

To start the semantic search web app, execute `YTEX_HOME\ytexWeb.cmd`

To use the semantic search web app,
  1. Open a browser and navigate to http://localhost:9080/ytex.web
  1. login with username 'scott' and password 'wombat'

# Configuring YTEX #
## Dictionary Lookup ##
By default, YTEX configures the cTAKES DictionaryLookup algorithm to annotate documents with concepts from the SNOMED and RXNORM vocabularies.  These vocabularies are quite expansive; nevertheless, they may lack certain concepts relevant to your project.  Refer to [Dictionary Lookup Configuration](DictionaryLookup_V04.md) for information on how to configure the dictionary lookup algorithm.

## Named Entity Recognition Regexs ##
The Dictionary Lookup Algorithm may not be flexible enough to identify all variants of a concept; YTEX can also identify concepts using regular expressions.  Simply add a row to the `ref_named_entity_regex` table:
```
insert into <schema>.ref_named_entity_regex (regex, coding_scheme, code)
values ('(?i)\bREFER\s+TO\s+.*#{0,1}+\s*\d+','ESLD','DOCREF')
;
```

| column | description |
|:-------|:------------|
| regex  | regular expression  (see java [pattern](http://download.oracle.com/docs/cd/E17476_01/javase/1.5.0/docs/api/java/util/regex/Pattern.html?is-external=true)) |
| coding\_scheme | similar to sourcetype in umls lookup table (see above) |
| code   | concept id/code |
| context | the document section to which the regular expression search should be limited (see sections below) |

## Negation Detection ##
YTEX uses NegEx, which relies on a list of negation triggers.  To add or modify negation triggers, update `YTEX_HOME\config\desc\ytex\uima\annotators\negex_triggers.txt`

## Segments (Sections) Regexs ##
YTEX can identify sections within a document using regular expressions.  Update the `ref_segment_regex` table, e.g.:
```
insert into <schema>.ref_segment_regex (segment_id, regex) values('FINDINGS', '\nFINDINGS:|\nTECHNIQUE AND FINDINGS|\nPROCEDURE AND FINDINGS:|\nFindings:');
```

| column | description |
|:-------|:------------|
| segment\_id | the segment identifier |
| regex  | the regular expression that finds the section heading, or finds the entire section |
| limit\_to\_regex | 0 - false - the regular expression only identifies the section heading. In this case, the section will span the text from the heading to the next section. 1 - true - the regular expression identifies the entire section.  The section spans from the beginning to the end of the text covered by the regular expression |

# Data Mining #
For data mining purposes, text can be represented as a 'Bag-of-Words': a matrix with 'words' as columns and documents as rows.  The value of the column represents the (weighted) word frequency, or is an indicator representing the presence of the word in the document.  The words can be the raw natural language word, the stemmed word, or concept identifiers.  This is typically a very high-dimensional feature space, i.e. the number of columns (distinct words) can be very large.  This space is typically sparse: most of the words assume the value 0.

To deal with sparse, high-dimensional spaces efficiently, data mining packages support a sparse file format.  YTEX currently supports exporting sparse matrices in the following formats:
  * Arff (for use with Weka)
  * Sparse Matrix format  (for use with R/Matlab)
  * Libsvm format.

YTEX provides great flexibility in choosing which features or class of features to export.  Before we get into details, you should run either the WEKA or R example.

## Data Mining Example ##
In this example, we develop a classifier that identifies documents that assert the presence of a fracture.  The documents and their class labels are stored in the `fracture_demo` table.  We decided to use 2/3 of the notes as a training set, and 1/3 as a test set, and store this assignment in the `fracture_demo` table as well.

Before going through the following steps, please annotate the fracture documents using the YTEX collection processing engine as documented above (Example - Custom Key Mapping).

In this example, we annotate documents, export two different representations of these documents, and train classifiers using either R or Weka.  We export a bag-of-cuis representation where each variable represents the frequency of an affirmed UMLS concept within a document; we also export a bag-of-words representation where each variable represents the frequency of a stemmed word.


The SparseDataExporter takes a Java property file as input; the property file specifies the SQL queries used to obtain instances (i.e. documents), their class labels, and their 'attributes'.

### 1. Export Bag-of-Words ###
Start a command prompt/shell, change to the `YTEX_HOME\examples\fracture\cui` or `YTEX_HOME\examples\fracture\word` directory (for the bag-of-cuis vs. bag-of-words example respectively), and run the following commands.
Windows:
```
..\..\..\setenv.cmd
..\..\..\ytexTools.cmd SparseDataExporter -Dprop=export.[platform].xml -Dtype=[type]
```
Linux:
```
${HOME}/ytex.profile
../../../ytexTools.sh SparseDataExporter -Dprop=export.[platform].xml -Dtype=[type]
```
`export.[platform].xml` contains the queries for the respective datatabase platform.
`[type]` is either `weka` or `sparsematrix` (for R or matlab)

#### Weka ####
After executing this you should see 2 arff files in this directory corresponding to the training and test sets.

  * start the WEKA Gui Chooser: call startWeka.bat
```
startWeka.bat
```
  * start Explorer: click on `Explorer`
  * open an arff file: click on 'open file' and select e.g. `fracture-word.arff`
  * you should see a list of all the attributes

The rest is standard WEKA usage.
  * Here is one suggestion: filter for the top 50 attributes ranked by mutual information
  * Train the J48 decision tree on train.arff, evaluate on test.arff
  * If you are new to WEKA, refer to the [Explorer Guide](http://iweb.dl.sourceforge.net/project/weka/documentation/3.5.x/ExplorerGuide-3-5-8.pdf)

#### R ####
You should see 3 files:
  * attributes.txt contains attribute names, e.g. cui
  * instance.txt contains the instance\_id (i.e. fracture\_demo.note\_id), the class of the note (fracture/no fracture), and an indicator if this belongs to the training or test set.
  * data.txt contains a sparse representation of the data matrix 3 columns: row index, column index, and cell value

What all this is should become clear if you look at `YTEX_HOME\examples\fracture\classify.R`

To train and test a classifier, do the following
  * start R, change to the directory where you exported data
  * run the following:
```
source("../classify.R")
```

This will train a decision tree on the training data, print out the decision tree, run the decision tree on the test data, and print the results.

## Details ##
The `SparseDataExporter` is parameterized by java properties that contain queries that determine how data will be exported.  Refer to `export.[platform].xml` for sample queries.

The queries return data in instance-attribute-value triples.

### instanceClassQuery ###
Retrieves instance ids (i.e. document ids) and their class labels.  This query must return the following columns:
  * instance id: Long
  * class label: String
  * train/test indicator (optional): does the document belong to the training or test set?

### numericWordQuery ###
Retrieves numeric instance attributes for all attribute-instance combinations.  Must return 3 columns:
  1. instance\_id (Long)
  1. attribute name (string)
  1. attribute value (double)

### nominalWordQuery ###
Either a numericWordQuery, a nominalWordQuery, or both must be specified. Retrieves nominal instance attributes for all attribute-instance combinations.  Must return 3 columns:
  1. instance\_id (Long)
  1. attribute name (string)
  1. attribute value (string)
For weka, nominal attributes are created in the ARFF file.  For the sparsematrix format, each nominal attribute level is turned into a numeric attribute with a binary indicator.

### arffRelation ###
Applicable only to Weka. This is the internal 'name' of the dataset