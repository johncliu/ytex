

# Using YTEX v0.3 #
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
  * Execute `YTEX_HOME\ytexDocumentAnalyzer.bat` and set the following options:
    * Input Directory: `YTEX_HOME\examples\weka-fracture\collection`
    * Output Directory: `YTEX_HOME\examples\weka-fracture\ytexoutput` (create this directory)
    * Analysis Engine: `YTEX_HOME\config\desc\ytex\uima\YTEXPipeline.xml`
  * Click Run
  * You can then view the anaysis results by double-clicking individual documents.

## UIMA Collection Processing Engine w/ DBCollectionReader ##
Clinical Documents for the VACS project are stored in a database.  We developed an UIMA CollectionReader that retrieves these documents for annotation (thereby avoiding exporting the documents to the file system).  The DBCollection reader works as follows:
  1. read all document unique ids into an internal list
  1. iterate through each id in the list, and retrieve the document for annotation.

Documents in a database typically are given a Unique ID, and are cross-referenced with other information (e.g. patient id, document type, document date, etc.).  This document id can be stored as a document annotation, which in turn will be stored in the database, along with other document annotations.  This allows you to cross-reference annotation data with other document data.

YTEX comes with a `DocumentKey` annotation; the document primary key can be mapped to the `DocumentKey`.  If the document key query returns any of the following fields, they will be mapped to the corresponding fields in the `DocumentKey` annotation and stored in the `anno_dockey` database table:
  * `uid`: a numeric document identifier
  * `studyid`: numeric patient identifier
  * `document_type_id`: a numeric document type id (foreign key to `ref_document_type` table)
  * `site_id`: character site identifier

### Example ###
To illustrate this, we can simply retrieve documents we stored in the database when we ran the UIMA Document Analyzer above.  To configure and execute the CPE, do the following:
  * Start CPE
> Execute `YTEX_HOME\ytexCPE.bat`,
  * Clear Settings
> In case of any settings are specified File->Clear All
  * Configure DBCollectionReader
> For the Descriptor, specify `YTEX_HOME\config\desc\ytex\vacs\uima\DBCollectionReader.xml`.
> The 'Query Document Keys' and 'Query Get Document' parameters will appear.
    * Specify query to retrieve document keys.
```
    select document_id uid
		from document
```
    * Specify query to retrieve individual document:
```
		select doc_text 
		from document
		where document_id = :uid
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
> You will note that the uid column is set, and refers to the source document\_id.

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
## Named Entity Lookup Table ##
By default, YTEX annotates documents with SNOMED concepts.
The UMLS and SNOMED are quite expansive; nevertheless, these vocabularies may lack certain synonyms/lexical variants for clinical concepts, are lack concepts completely.

To annotate (new) concepts using new lexical variants, do the following:
  * Add concepts to the UMLS lookup table
YTEX uses cTAKES JDBC DictionaryLookup algorithm, which searches the UMLS lookup table using the first word of each phrase which can be mapped to a concept.  To add a lexical variant to the lookup table, simply insert a row, e.g.:
```
insert into <schema>.umls_ms_2009 (  cui, fword, text, code, sourcetype, tui)
values ('ESLD_MASS','hypodense','hypodense area','ESLD_MASS','ESLD','T060');
```

| column | description |
|:-------|:------------|
| cui    | Intended as the UMLS CUI, but can be used as an arbitrary concept id |
| fword  | the 1st word of the phrase to be mapped to a concept |
| text   | the full text of the (potentially multiword) phrase to be mapped |
| code   | Intended for SNOMED code, but not used by YTEX |
| sourcetype | Source vocabulary (e.g. SNOMED, NCI).  Set this to the a project-specific code (ESLD for end-stage liver disease) |
| tui    | umls semantic type.  used to filter concepts (see below regarding the view) |

  * Modify the view used by YTEX to search for concepts
YTEX retrieves concepts from the `v_snomed_fword_lookup` view.  Simply modify the view definition to include your concepts:
```
drop view <schema>.v_snomed_fword_lookup;

create view <schema>.v_snomed_fword_lookup
as
select fword, cui, text
from ytex.umls_ms_2009
where 
(
	tui in 
	(
	'T021','T022','T023','T024','T025','T026','T029','T030','T031',
	'T059','T060','T061',
	'T019','T020','T037','T046','T047','T048','T049','T050','T190','T191',
	'T033','T034','T040','T041','T042','T043','T044','T045','T046','T056','T057','T184'
	)
	and sourcetype = 'SNOMEDCT'
) 
or
sourcetype = 'ESLD'
```
In the example above, the view is modified to include all terms from the `ESLD` source, in addition to `SNOMED`.

## Named Entity Recognition Regexs ##
The UMLS lookup table may not be flexible enough to identify all variants of a concept; YTEX can also identify concepts using regular expressions.  Simply add a row to the `ref_named_entity_regex` table:
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