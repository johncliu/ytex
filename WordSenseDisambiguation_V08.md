# Introduction #

YTEX includes a word sense disambiguation (WSD) annotator. Terms in a natural language may be ambiguous, i.e. can be mapped to multiple distinct concepts. For example, the word ‘cold’ can refer to the viral infection ‘common cold’ or the ‘sensation of cold’. YTEX implements the 'adapted lesk' method that uses semantic similarity measures to quantify how well a concept ‘fits’ in a given context.  This page describes the WSD algorithm, the configuration for the `SenseDisambiguatorAnnotator`, and describes how to reproduce the results of our evaluation on the NLM WSD and MSH WSD data sets.

The adapted Lesk algorithm works as follows: for each term to be disambiguated (target term) it finds all possible senses; it selects all words within a window surrounding the target term and maps them to concepts (context concepts); and it scores senses by summing the semantic relatedness between each sense and all context concepts; and finally it selects the sense with the highest score.  In its original formulation, the adapted Lesk calculates the relatedness of concepts using the cosine of their profile vectors; however, this can be replaced with a [semantic similarity measure](SemanticSim_V06.md).

For a high-level overview of the WSD method we've implemented, refer to our paper: [Knowledge-based biomedical word sense disambiguation: an evaluation and application to clinical document classification](http://jamia.bmj.com/content/early/2012/10/15/amiajnl-2012-001350.long).

# SenseDisambiguatorAnnotator #

The `SenseDisambiguatorAnnotator` is an UIMA annotator integrated with YTEX.  YTEX identifed named entities (`EntityMention` Annotations), which in turn can contain multiple concepts (`OntologyConcept` Feature Structures).  The `SenseDisambiguatorAnnotator` disambiguates each ambiguous term (i.e. `EntityMention` with multiple `OntologyConcept`s) in a document as follows:
  * Takes all `EntityMention`s within a window around the ambiguous term
  * Scores candidate concepts using the semantic similarity with context concepts; the score is stored in the `score` attribute of the `OntologyConcept`.
  * Picks the candidate concept with the highest score: sets the `OntologyConcept.disambiguated` attribute to true for the best concept, and false for others.

The `SenseDisambiguatorAnnotator` is configured via `YTEX_HOME/config/desc/ytex.properties`:
  * ytex.sense.windowSize - context window size. concepts from named entities +- windowSize around the target named entity are used for disambiguation. defaults to 50
  * ytex.sense.metric - measure to use. defaults to INTRINSIC\_PATH.  See [SemanticSim\_V06](SemanticSim_V06.md) for valid values.
  * ytex.conceptGraph - concept graph to use.  Defaults to `sct-msh-csp-aod` (SNOMED-CT, MeSH, CRISP thesaurus, Alcohol & Other Drug Thesaurus).

The optimal measure and concept graph depends on the application.  These defaults achieved the best score on the MSH WSD data set; you might want to experiment with the `LCH` measure and `umls` concept graph: this configuration achieved the best performance on the NLM WSD data set.

# Reproducing results on WSD datasets #
This section describes the steps to reproduce our results on the NLM WSD and MSH WSD datasets.

## Prerequisites ##
  * MySQL Database Server
  * CRAN R Used for statistical analysis.  Install the Plyr package
  * UMLS installed in MySQL: We used 2011AB with SNOMED-CT + all Level 0 source vocabularies
  * [MetaMap](http://metamap.nlm.nih.gov/) Optional - needed only if you want to reproduce our results with MetaMap.  We tested MetaMap version 2011 with the USABase Strict model (the default).  You will also have to install the [MetaMap Java API](http://metamap.nlm.nih.gov/#MetaMapJavaApi) and the [MetaMap UIMA Annotator](http://metamap.nlm.nih.gov/#MetaMapUIMA).
  * [YTEX installation](Installation_V07.md): configured to use the UMLS
  * [YTEX UMLS Archive](Installation_V07.md): this contains the `UMLS` concept graphs used by similarity measures.  If this file was present during the YTEX installation, there will be a file `YTEX_HOME/conceptGraph/umls.gz`.  If the `UMLS` concept graph is not present, it will be created, but this is memory intensive and time consuming.
  * [nlm.wsd-v0.8.zip](http://ytex.googlecode.com/files/nlm.wsd-v0.8.zip) benchmark code and scripts.
  * NLM WSD Dataset: Download the [Basic Test Collection](http://wsd.nlm.nih.gov/Restricted/downloads/basic_reviewed_results.tar.gz) and [WSD Choices](http://wsd.nlm.nih.gov/Collaborations/NLM-WSD.target_word.choices_v0.3.tar.gz)
  * MSH WSD Dataset: Download the [Full MSH WSD Dataset](http://wsd.nlm.nih.gov/Collaborations/MSHCorpus.zip)


## Configuration ##
Unpack WSD Datasets, specify paths in `YTEX_HOME/config/desc/ytex.properties`:
  * nlm.wsd.home: path to the `Basic_Reviewed_Results` directory from the unpacked `basic_reviewed_results.tar.gz`.  This directory contains a subdirectory for each word (e.g. adjustment ...).
  * choices.home: path to the `2007` directory from the unpacked `NLM-WSD.target_word.choices_v0.3.tar.gz`.  This directory contains a file for each word (e.g. adjustment.choices)
  * msh.wsd.home: path to the `MSHCorpus` directory from the unpacked `MSHCorpus.zip` file; contains a file for each word (e.g. `AA_pmids_tagged.arff`).
  * set R.bin to directory that contains R executable
  * Optional: configure threads.  Annotating the corpora is the most time consuming step, and can be parallelized.  Specify the number of threads to use (n), and provide a comma separated list from 1-n (see example below).  Each thread actually starts a java process, each of which will require about 1GB of memory for cTAKES, 2GB for MetaMap.

For example:
```
nlm.wsd.home=c:/temp/Basic_Reviewed_Results
msh.wsd.home=c:/temp/MSHCorpus
choices.home=c:/temp/NLM-WSD.target_word.choices_v0.3/choices/2007
R.bin=C:/Program Files/R/R-2.13.1/bin/x64
# use 4 threads to annotate corpus
kernel.threads=4
kernel.slices=1,2,3,4
```

Annotating and disambiguating the NLM WSD and MSH WSD datasets with cTAKES on a 4-core machine will take about 5 hours; with MetaMap this will take about 24 hours.

## Run Ant Script ##

### cTAKES evaluation ###
Unpack nlm.wsd-v0.8.zip in your `<YTEX_HOME>` and run the ant script
Linux:
```
. ${HOME}/ytex.profile
cd ${YTEX_HOME}
unzip nlm.wsd-v0.8.zip
cd nlm.wsd
nohup ant setup.all eval.ctakes.all > wsd.out 2>&1 &  
```
Windows:
```
cd ${YTEX_HOME}
setenv.cmd
cd nlm.wsd
ant setup.all eval.ctakes.all > wsd.out 2>&1 
```

What this does:
  * compiles source
  * creates tables, loads NLM WSD and MSH WSD data sets into `nlm_wsd` and `msh_wsd` tables
  * generates tables for the cTAKES dictionary lookup algorithm
  * annotates all abstracts with cTAKES, stores annotations in database
  * disambiguates concepts, outputs results to `eval/nlm/<concept graph>/<window size>` and  `msh/eval/<concept graph>/<window size>` for the NLM WSD and MSH datasets respectively
  * computes accuracy and p-values with R, stores results in these directories as .csv files

If you did not install the `UMLS` concept graph (see above), then it will be created.  Creating this concept graph is very memory intensive; you should give the script all the memory you can spare by specifying the `maxMemory` option.  E.g. to give the concept graph setup 8gb:
```
ant -DmaxMemory=8g setup.all eval.ctakes.all > wsd.out 2>&1 
```

#### cTAKES lookup tables ####
cTAKES dictionary lookup is very 'permissive' anything that matches a term from the dictionary will be mapped to a concept.  In our experience, using the entire UMLS with cTAKES' dictionary lookup will result in many false positives.

We configured a dictionary table for each WSD dataset that is limited to the 'relevant' source vocabularies and semantic types.  The tables are `v_nlm_wsd_fword_lookup` and `v_msh_wsd_fword_lookup` for the NLM and MSH WSD datasets respectively.  The dictionary includes all terms (MRCONSO entries) from all source vocabularies and semantic types from the target terms for the dataset.

### MetaMap evaluation ###
In order to annotate the NLM WSD and MSH WSD with MetaMap in a reasonable amount of time, you must have atleast 4 cores and 10 GB of memory.  The MetaMap UIMA annotator relies on the MetaMap server which is single-threaded.  We start _n_ instances of the MetaMap server to enable multi-threaded processing, with _n_ set to the number of cores (see `ytex.properties` above).  Each MetaMap server instance will need at least 2GB of memory.

We run MetaMap with default options (i.e. no options); in particular this means that we do **not** use MetaMap's word sense disambiguation features.  Changes you may have made to your MetaMap scripts will be copied to the new scripts.

Run the following ant target to generate MetaMap scripts and configuration files:
```
ant mm.copy.all 
```

This will generate scripts 1\_mmserver to n\_mmserver in your `public_mm/bin` directory (n = kernel.threads from your ytex.properties) that start the MetaMap server on port 80m1 and the MetaMap tagger server on port 80m5 (m = 1...n).

Run the following ant target to start all _n_ MetaMap servers
Windows:
```
ant mm.start.all
```
Linux:
```
ant mm.start.all.linux
```

Run the following ant target to annotate and disambiguate the NLM WSD and MSH WSD datasets:
```
ant eval.metamap.all
```

### Producing Summary Results ###
The `summary.R` script does the following
  * consolidates results from all datasets and parameter combinations, outputs `all-wsd-results.csv`
  * fits a linear model to quantify the effect of each parameter; outputs model coefficients and their p-values to `nlm-model.csv` and `msh-model.csv`
```
ant summary.R
```

# Reproducing results on CMC 2007 challenge #
This section describes the steps to reproducing our results on the CMC 2007 challenge.  The prerequisites are the same as for the WSD datasets (see above).

## CMC 2007 challenge data ##
Download and extract the following files to a directory, which we refer to as `cmc.dir`
  * [2007 Challenge Training Data](http://computationalmedicine.org/catalog/downloads/2007-challenge-training-data)
  * [2007 Challenge Testing Data WITH Codes](http://computationalmedicine.org/catalog/downloads/2007-challenge-testing-data-codes)
  * [cmc.2007-v0.8.zip](http://ytex.googlecode.com/files/cmc.2007-v0.8.zip) benchmark code and scripts.

`cmc.dir` should have the following subdirectories:
  * `training`
  * `testing-with-codes`

Set the `cmc.dir` property in `ytex.properties`

### Concept Graph and lookup table setup ###
The CMC evaluation depends on the following from the WSD dataset evaluation:
  * requires the setup of the `sct-msh-csp-aod` concept graph.
  * The cTAKES evaluation requires the setup of a dictionary lookup table

To set these up, if you have not done so already, run the `setup.all` target from the WSD evaluation.

### cTAKES evaluation ###
Unpack cmc.2007-v0.8.zip in your `<YTEX_HOME>` and run the ant script
Linux:
```
. ${HOME}/ytex.profile
cd ${YTEX_HOME}
unzip cmc.2007-v0.8.zip
cd cmc.c2007
nohup ant setup.all ctakes.all > ctakes.out 2>&1 &  
```
Windows:
```
cd ${YTEX_HOME}
setenv.cmd
cd cmc.2007
ant setup.all ctakes.all > ctakes.out 2>&1 
```

`setup.all` does:
  * builds source
  * loads CMC 2007 corpus into the database
  * Sets up cross-validation folds in the `cv_fold` table of the database

`ctakes.all` does:
  * annotates and disambiguates CMC 2007 corpus, stores annotations in DB
  * exports training set in libsvm format for cross validation under the `libsvm/bow-ctakes` and `libsvm/bow-ctakes-wsd` directories
  * performs cross-validation with libsvm, identifies optimal parameters
  * exports training and test set in libsvm format for final evaluation in the `libsvm/bow-ctakes-test` and ```libsvm/bow-ctakes-wsd-test`` directories, evaluates libsvm, outputs results in the `results.csv` files in the respective directories

### MetaMap evaluation ###
The CMC 2007 corpus is fairly small, and we annotate it with a single thread.  Start the tagger server (skrmedpostctl) and MetaMap server, and run the following target:

```
ant metamap.all > metamap.out 2>&1 
```

The difference to `ctakes.all` is that we use MetaMap to annotate the CMC 2007 corpus (and directories will have `metamap` in place of `ctakes`).