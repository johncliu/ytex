# Introduction #

YTEX provides a generalizable framework for the computation of path finding, corpus & intrinsic information content based semantic similarity measures from any domain ontology.  This page describes the usage and configuration of the YTEX Semantic Similarity Tools.  For a high-level overview, refer to our paper: [Semantic similarity in the biomedical domain: an evaluation across knowledge sources](http://www.biomedcentral.com/1471-2105/13/261/abstract).

Semantic similarity measures include path finding measures based purely on path distances, and information-content based measures based on taxonomic relationships and information content (IC) of concepts, a measure of concept frequency.  Semantic similarity measures utilize a concept graph where vertices represent concepts and edges represent taxonomical relationships.  The similarity between concepts is computed from the length of the path between concepts and their nearest common ‘parent’.   Previous studies that took advantage of a large, annotated medical corpus to estimate concept frequencies showed that IC based measures of semantic similarity outperform path finding measures.  Unfortunately, large annotated corpora are not typically available for many applications.  To overcome this limitation, methods that estimate IC from the structure of the concept graph have been developed and their accuracy [shown to rival that of corpus-based measures](http://www.sciencedirect.com/science/article/pii/S1532046411000645).

# Usage #
YTEX provides a web application client, web services interface, RESTful interface, and command-line interface to compute similarity measures.  The demo similarity web app is available under http://informatics.med.yale.edu/ytex.web; if you plan to use this application extensively, please install ytex locally.  Please refer to [Sanchez & Batet](http://www.sciencedirect.com/science/article/pii/S1532046411000645) for an excellent overview of similarity measures in general, and intrinsic information content (IC) based measures in particular.  We scale all measures to the unit interval; see [YTEX Semantic Similarity Measures](http://ytex.googlecode.com/svn/trunk/projects/nlm.wsd/doc/YTEX%20Semantic%20Similarity%20Measures.pdf) for details.

YTEX allows the declarative definition of concept graphs in which nodes represent concepts and edges taxonomical relationships, and can compute the similarity between nodes in these graphs.  YTEX comes with two concept graphs derived from the UMLS (version 2011AB)
  * sct-msh-csp-aod: concepts from the SNOMED-CT, MeSH, CRISP, and Alcohol and Drug thesaurus
  * umls: concepts from all restriction free (level 0) UMLS source vocabularies and SNOMED-CT
You can configure additional concept graphs (see below).

The YTEX demo application (http://informatics.med.yale.edu/ytex.web/) has 3 concept graphs configured:
  * umls: see above
  * sct: Concept graph dervied from the SNOMED-CT 2011-07 international release
  * msh: Concept graph derived from the MeSH 2012

## Similarity Web App ##
The similarity web app allows you to select
  * select a concept graph against which measures should be computed
  * specify concept pair(s)
  * specify measures

The similarity web application has two pages:

### Similarity Single ###
Compute similarities for a single concept pair.  In addition to the similarity values, this page outputs the path between concepts.  You can enter the text of the concept, and the application will attempt to find the corresponding concept id (CUI).  Alternatively, you can simply enter the concept id.

### Similarity Multiple ###
Similarity Multiple: Compute the similarity between multiple pairs of concepts.  Enter each concept pair on a different line, and separate concepts by a comma or whitespace.  The output can be exported to a CSV file or Excel spreadsheet.

## Similarity Web/RESTful Services ##
As with the web application, you can specify the concept graph, concept pairs, and measures for which similarities should be computed.  Both methods accept a list of measures; these are:
  * Path-Finding Measures
    * WUPALMER: Wu & Palmer
    * LCH: Leacock & Chodorow
    * PATH: Path
    * RADA: Rada
  * Corpus IC Based Measures:
    * LIN: Lin
  * Intrinsic IC Based Measures:
    * INTRINSIC\_LIN: Intrinsic IC based Lin
    * INTRINSIC\_LCH: Intrinsic IC based Leacock & Chodorow
    * INTRINSIC\_PATH: Intrinsic IC based Path, identical to Jiang & Conrath
    * INTRINSIC\_RADA: Intrinsic IC based Rada
    * JACCARD: Intrinsic IC based Jaccard
    * SOKAL: Intrinsic IC based Sokal & Sneath

### RESTful interface ###
To get the similarity between a pair of concepts using the concept graph `sct-umls`, and the LCH and Intrinsic LCH measures:
http://informatics.med.yale.edu/ytex.web/services/rest/similarity?conceptGraph=umls&concept1=C0018787&concept2=C0024109&metrics=LCH,INTRINSIC_LCH&lcs=true

The parameters are:
  * concept1/concept2 the concept ids
  * metrics comma-separated list of metrics
  * conceptGraph (optional) concept graph to use; if not specified will use the default
  * lcs (optional) set to true to get the paths through the Least Common Subsumer.
Will return XML with a list of similarities corresponding to the list of metrics.  See the WSDL for the corresponding web service for the schema.

To get a list of concept graphs:
http://informatics.med.yale.edu/ytex.web/services/rest/getConceptGraphs

To get the 'default' concept graph: http://informatics.med.yale.edu/ytex.web/services/rest/getDefaultConceptGraph

### Web Services interface ###
The Web Services interface is analogous to the restful interface, but allows the computation of similarities fro multiple concept pairs.  See http://informatics.med.yale.edu/ytex.web/services/conceptSimilarityWebService?wsdl

## Command-Line Interface ##
The `ConceptSimilarityServiceImpl` java program accepts a list of concept pairs, and outputs their similarities in a tab-delimited format.  It accepts the following arguments:
  * `-metrics`: required, comma separated list of metrics (see above in for valid values)
  * `-out`: optional file to send output to.  if not specified will send output to standard out.
  * `-lcs`: should the least common subsumer and paths be output for each concept pair?
  * `-concepts`: a list of concept pairs, or a file with concept pairs.  For a file place each concept pair on a separate line, separate concepts by whitespace or commas.  For a list of concept pairs, separate each concept by a comma, each pair by a semicolon:
```
cd <YTEX_HOME>
java -Xmx1g -Dlog4j.configuration=log4j.properties -classpath libs.system\ytex.jar ytex.kernel.metric.ConceptSimilarityServiceImpl -concepts C0018787,C0024109;C0034069,C0242379 -metrics LCH,INTRINSIC_LCH
```

The concept graph that will be used is defined in `<YTEX_HOME>/config/desc/ytex.properties` with the `ytex.conceptGraphName` key (default is `sct-msh-csp-aod`); alternatively, you can specify the concept graph with the java `-Dytex.conceptGraphName=<concept graph>` option.  The amount of memory needed depends on the concept graph; SNOMED-CT fits comfortably in a 500 MB heap.  The large `umls` concept graphs need 1 GB (specify the following java option: `-Xmx1g`).

# Configuration #

## Creating a Concept Graph ##
To create a concept graph, you create a properties file that contains a query that retrieves all the edges from a taxonomy.  The `ConceptDaoImpl` does the following:
  * executes this query
  * builds a concept graph
  * removes edges that induce cycles
  * computes the depth and intrinsic information content of each node in the graph
  * writes the concept graph to the file system
Computing the intrinsic IC is very memory intensive - give this task all the memory that you have for large concept graphs.  Computing the intrinsic IC for the entire UMLS takes 1.5 hours with an 8GB java heap.

As an example, here is what you do to create a concept graph with just the SNOMED-CT vocabulary from the UMLS:

1) Create a properties file that defines the required parameters:
`<YTEX_HOME>\sct-umls.xml`:
```
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">
<properties>
	<entry key="ytex.conceptGraphName">sct-umls</entry>
	<entry key="ytex.conceptGraphQuery"><![CDATA[
	select distinct cui1, cui2 
	from umls.MRREL 
	where sab in ('SNOMEDCT')
	and rel in ('PAR')
	order by cui1, cui2
	]]></entry>
</properties>
```

2) Run the `ConceptDaoImpl`:
```
cd <YTEX_HOME>
java -Xmx500m -Dlog4j.configuration=log4j.properties -classpath libs.system\ytex.jar ytex.kernel.dao.ConceptDaoImpl -prop sct-umls.xml
```
You will get warnings about removing cycles.  The concept graph will be stored in the `<YTEX_HOME>/conceptGraph` directory.

## Corpus Information Content ##
We compute the **intrinsic** information content (intrinsic IC) when creating the concept graph.  The `InfoContentEvaluatorImpl` class computes the **corpus** information content (corpus IC) for a given concept graph and corpus.  This class takes as input a properties file that contains a query used to retrieve concept frequencies from the database; it then computes the information content of each node in the concept graph; finally it stores this in the `feature_eval` and `feature_rank` ytex database tables.

Concept frequencies may come from the YTEX annotation tables, but can come from any database table.  For example, to compute the corpus ic using all the concepts from all annotated documents in the ytex databases, we would create the following properties file - `corpusIC.props.xml`:
```
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">
<properties>
	<!-- the query to retireve concept counts -->
	<entry key="ytex.freqQuery"><![CDATA[
	select code, count(*)
	from anno_ontology_concept o
	inner join anno_base b on b.anno_base_id = o.anno_base_id
	inner join document d on d.document_id = b.document_id
	group by code
	]]></entry>
	<!-- corpusName is a required property -->
	<entry key="ytex.corpusName">wsd</entry>
	<!-- the name of the concept graph -->
	<entry key="ytex.conceptGraphName">umls</entry>
</properties>
```

To compute corpus information content, run the `InfoContentEvaluatorImpl`:
```
%YTEX_HOME%\setenv.cmd
java -Xmx1g ytex.kernel.InfoContentEvaluatorImpl -prop=corpusIC.props.xml > ic.out 2>&1
```

To use the corpus IC with the Lin similarity measure, specify the corpus name in `YTEX_HOME/config/desc/ytex.properties`, e.g.:
```
ytex.conceptGraphName=umls
ytex.corpusName=wsd
```

# Similarity Benchmark #
Running the similarity benchmark.
## Prerequisities ##
Tested on windows and linux using the 64-bit jdk.
  * MySQL Database Server:   We test with version 5.1.  The UMLS-only part of the benchmark scripts will work with Oracle & SQL Server.  We load SNOMED-CT and MeSH into the datbase; this has only been tested on MySQL
  * CRAN R: Used for statistical analysis. Install the Plyr package
  * UMLS installed in MySQL: We used 2011AB with SNOMED-CT + all Level 0 source vocabularies
  * [YTEX installation](Installation_V06.md): configured to use the UMLS
  * [YTEX UMLS Archive](Installation_V06.md) (Optional): this contains the `umls` and `sct-msh-csp-aod` concept graphs.  The YTEX installation unpacks these files to `YTEX_HOME/conceptGraph`.  If the concept graphs are not present, they will be created (but this is memory intensive).
  * SNOMED-CT download: Download and extract SNOMED-CT from http://www.nlm.nih.gov/research/umls/licensedcontent/snomedctfiles.html (we used 20110731, available from http://download.nlm.nih.gov/umls/kss/IHTSDO20110731/SnomedCT_RF2Release_INT_20110731.zip)
  * MeSH download:
> Download following files, and extract the zip files to ${mesh.home}:
    * [desc2012.zip](http://www.nlm.nih.gov/mesh/termscon.html)  (extract desc2012.xml from this file)
    * [supp2012.zip](http://www.nlm.nih.gov/mesh/termscon.html)  (extract supp2012.xml from this file)
    * [desc2012.dtd](http://www.nlm.nih.gov/mesh/2012/download/desc2012.dtd)
    * [supp2012.dtd](http://www.nlm.nih.gov/mesh/2012/download/supp2012.dtd)

## Running the Benchmark ##
Setup some properties in ytex, run a couple scripts, and you're done.

### Setup YTEX Properties ###
In YTEX\_HOME/config/desc/ytex.properties
  * set sct.home property to full path of the Snapshot/Terminology directory, and sct.version to the version date.
  * set mesh.home to directory containing desc2012.xml, desc2012.dtd, supp2012.xml, and supp2012.dtd.
  * set R.bin to directory that contains R executable
```
sct.home=E:/temp/SnomedCT_RF2Release_INT_20110731/Snapshot/Terminology
sct.version=20110731
mesh.home=c:/temp/mesh
R.bin=C:/Program Files/R/R-2.13.1/bin/x64
```

### Load SNOMED-CT into database ###
Linux:
```
. ${HOME}/ytex.profile
cd ${YTEX_HOME}/data
ant -Dytex.home=${YTEX_HOME} sct.all > sct.out 2>&1 &
```

Windows:
```
cd %YTEX_HOME%\data
..\setenv.cmd
ant -Dytex.home=%YTEX_HOME% sct.all > sct.out 2>&1 &
```


### Run the benchmark ###
Download and unpack [simbenchmark-v0.6.zip](http://code.google.com/p/ytex/downloads/detail?name=simbenchmark-v0.6.zip&can=2&q=) in your `<YTEX_HOME>` and run the ant script

Linux:
```
cd ${YTEX_HOME}
unzip simbenchmark-v0.6.zip
cd simbenchmark
nohup ant -DmaxMemory=8g -Dytex.home=${YTEX_HOME} all > simbenchmark.out 2>&1 &  
```
Windows:
```
ant -DmaxMemory=8g -Dytex.home=${YTEX_HOME} all > simbenchmark.out 2>&1 
```

If you did not install the precomputed concept graphs, set `-DmaxMemory` to as much memory as you can spare, as computing the intrinsic infocontent for the entire UMLS is very memory intensive.

What this does:
  * Parses MeSH XML files, loads into database
  * Generates concept graphs
  * Computes similarity using each benchmark, measure, and concept graph combination.  Benchmark data under `<YTEX_HOME>/simbenchmark/data`
  * Computes spearman correlation with R

Output:
Under `<YTEX_HOME>/simbenchmark/data`
  * simbenchmark-summary.csv: consolidated results with spearman correlation for each concept graph / benchmark / measure combination.
  * simbenchmark-cg-significance.csv: significance of differences between intrinsic lch measures on different concept graphs.
  * simbenchmark-spearman.csv: spearman correlations and p-values
  * sim.txt: similarity computations for each concept pair/measure/concept graph/benchmark