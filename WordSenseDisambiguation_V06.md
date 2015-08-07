# Introduction #

YTEX includes a word sense disambiguation (WSD) annotator. Terms in a natural language may be ambiguous, i.e. can be mapped to multiple distinct concepts. For example, the word ‘cold’ can refer to the viral infection ‘common cold’ or the ‘sensation of cold’. YTEX implements the 'adapted lesk' method that uses semantic similarity measures to quantify how well a concept ‘fits’ in a given context.  This page describes the WSD algorithm, the configuration for the `SenseDisambiguatorAnnotator`, and describes how to reproduce the results of our evaluation on the NLM WSD and MSH WSD data sets.

The adapted Lesk algorithm works as follows: for each term to be disambiguated (target term) it finds all possible senses; it selects all words within a window surrounding the target term and maps them to concepts (context concepts); and it scores senses by summing the semantic relatedness between each sense and all context concepts; and finally it selects the sense with the highest score.  In its original formulation, the adapted Lesk calculates the relatedness of concepts using the cosine of their profile vectors; however, this can be replaced with a [semantic similarity measure](SemanticSim_V06.md).

For a high-level overview of the WSD method we've implemented, refer to our paper: Garla, V and Brandt, C. Semantic similarity in the biomedical domain: an evaluation across ontologies and application to word sense disambiguation (submitted).

# SenseDisambiguatorAnnotator #

The `SenseDisambiguatorAnnotator` is a UIMA annotator integrated with YTEX.  YTEX identifed named entities (NamedEntityAnnotation), which in turn can contain multiple concepts (OntologyConcept).  The `SenseDisambiguatorAnnotator` disambiguates each ambiguous term (i.e. NamedEntity with multiple candidate concepts) in a document as follows:
  * Takes all NamedEntities within a window around the ambiguous term
  * Scores candidate concepts using the semantic similarity with context concepts; the score is stored in the `score` attribute of the `OntologyConcept`.
  * Picks the candidate concept with the highest score: sets the `OntologyConcept.disambiguated` attribute to true for the best concept, and false for others.

The `SenseDisambiguatorAnnotator` is configured via `YTEX_HOME/config/desc/ytex.properties`:
  * ytex.sense.windowSize - context window size. concepts from named entities +- windowSize around the target named entity are used for disambiguation. defaults to 10
  * ytex.sense.metric - measure to use. defaults to INTRINSIC\_PATH.  See [SemanticSim\_V06](SemanticSim_V06.md) for valid values.
  * ytex.conceptGraph - concept graph to use.  Defaults to sct-msh-csp-aod (SNOMED-CT, MeSH, CRISP thesaurus, Alcohold & Other Drug Thesaurus).

The optimal measure and concept graph depends on the application.  These defaults achieved the best score on the MSH WSD data set; you might want to experiment with the `INTRINSIC_LCH` measure and `umls` concept graph: this configuration achieved the best performance on the NLM WSD data set.

# Reproducing results on benchmarks #
## Prerequisites ##
  * MySQL Database Server
  * CRAN R Used for statistical analysis.  Install the Plyr package
  * UMLS installed in MySQL: We used 2011AB with SNOMED-CT + all Level 0 source vocabularies
  * [YTEX installation](Installation_V06.md): configured to use the UMLS
  * [YTEX UMLS Archive](Installation_V06.md): this contains the concept graphs used by similarity measures.  If this file was present during the YTEX installation, there will be a directory `YTEX_HOME/conceptGraph` with the needed files.  You can also generate these concept graphs from scracth (see  [SemanticSim\_V06](SemanticSim_V06.md))
  * [nlm.wsd-v0.6.zip](http://code.google.com/p/ytex/downloads/list) benchmark code and scripts.
  * NLM WSD Dataset: Download the [Basic Test Collection](http://wsd.nlm.nih.gov/Restricted/downloads/basic_reviewed_results.tar.gz) and [WSD Choices](http://wsd.nlm.nih.gov/Collaborations/NLM-WSD.target_word.choices_v0.3.tar.gz)
  * MSH WSD Dataset: Download the [Full MSH WSD Dataset](http://wsd.nlm.nih.gov/Collaborations/MSHCorpus.zip)


## Configuration ##
Unpack WSD Datasets, specify paths in `YTEX_HOME/config/desc/ytex.properties`:
  * nlm.wsd.home: path to the `Basic_Reviewed_Results` directory from the unpacked `basic_reviewed_results.tar.gz`.  This directory contains a subdirectory for each word (e.g. adjustment ...).
  * choices.home: path to the `2007` directory from the unpacked `NLM-WSD.target_word.choices_v0.3.tar.gz`.  This directory contains a file for each word (e.g. adjustment.choices)
  * msh.wsd.home: path to the `MSHCorpus` directory from the unpacked `MSHCorpus.zip` file; contains a file for each word (e.g. `AA_pmids_tagged.arff`).
  * set R.bin to directory that contains R executable
  * Optional: configure threads.  Annotating the corpora is the most time consuming step, and can be parallelized.  Specify the number of threads to use (n), and provide a comma separated list from 1-n (see example below).  Each thread actually starts a java process, each of which will require about 1GB of memory.

For example:
```
nlm.wsd.home=c:/temp/Basic_Reviewed_Results
msh.wsd.home=c:/temp/MSHCorpus
choices.home=c:/temp/NLM-WSD.target_word.choices_v0.3/choices/2007
R.bin=C:/Program Files/R/R-2.13.1/bin/x64
# use 8 threads to annotate corpus
kernel.threads=8
kernel.slices=1,2,3,4,5,6,7,8
```

## Run Ant Script ##
Unpack nlm.wsd-vX.Y.zip in your `<YTEX_HOME>` and run the ant script
Linux:
```
. ${HOME}/ytex.profile
cd ${YTEX_HOME}
unzip nlm.wsd-v0.6.zip
cd nlm.wsd
nohup ant -Dytex.home=${YTEX_HOME} all > wsd.out 2>&1 &  
```
Windows:
```
cd ${YTEX_HOME}
setenv.cmd
cd nlm.wsd
ant -Dytex.home=${YTEX_HOME} all > wsd.out 2>&1 
```

What this does:
  * compiles source
  * creates tables, loads NLM WSD and MSH WSD data sets into `nlm_wsd` and `msh_wsd` tables
  * annotates all abstracts with ytex
  * disambiguates concepts, outputs results to `eval/<concept graph>-10` and  `msh/eval/<concept graph>-10/`
  * computes accuracy and p-values with R, stores results in the `eval` directory



