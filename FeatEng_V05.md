

# Introduction #
For a high-level overview of the feature engineering methods we've developed, refer to our paper: Garla, V and Brandt, C. [Ontology-Guided Feature Engineering for Clinical Text Classification](http://www.sciencedirect.com/science/article/pii/S1532046412000639) J Biomed Inform. 2012 May 9.

We developed a feature ranking method that utilizes the taxonomical structure of the UMLS to improve upon univariate feature ranking methods.  We developed a context-dependent semantic similarity measure and implemented a semantic kernel.  We improved the performance of the top-ranked machine learning-based system from the [I2B2 2008 challenge](https://www.i2b2.org/NLP/Obesity/) with these methods.

These feature engineering tools are included in YTEX, but do not depend on the YTEX NLP Pipeline: they can be used in conjunction with any text mining or NLP system.

# Tool Overview #
The main tools are:
  * ConceptDaoImpl: This tool generates a concept graph from the UMLS.  You can declaratively define which UMLS source vocabularies and relationships to use to generate this concept graph.
  * InfoContentEvaluatorImpl: This computes the information content of each concept in a corpus.
  * ImputedFeatureEvaluator: This computes the propagated and imputed information gain of all concepts in a corpus.
  * SemanticSimilarityService: This computes the semantic similarity of a pair of concepts.  We currently implement the Leacock-Chodorow, Lin, and Supervised Lin measures.
  * CorpusKernelEvaluator: Evaluate a user-defined kernel on all pairs of documents in a corpus.  The kernel matrices can be exported to data mining tools such as Matlab, R, or Libsvm for machine learning.  We have implemented a semantic kernel that uses the SemanticSimilarityService.

All tools are pure java and hence platform independent.  The tools currently require a MySQL 5.x database and the UMLS installed in your database.  All tools retrieve and store data from the MySQL database.  The YTEX NLP Pipeline stores data in the MySQL Database, thereby facilitating the use of these tools.  You are however not confined to the YTEX tables - you can configure the tools to retrieve the required data from any table.

These tools are plain old java programs; the I2B2 example shows how to call these programs.  We are currently developing tools and scripts to simplify the application of these methods to arbitrary user-defined corpora.

In our discussion, we refer to the I2B2 2008 challenge dataset; you may want to reproduce our results on this dataset before getting into details.  Reproducing our results requires a tiny bit of configuration, and will just run overnight.

**Note: we have released YTEX V0.7 beta, and provide an updated i2b2 2008 example for the v0.7 release.**

## `ConceptDaoImpl` ##
We generate a graph that represents the umls taxonomy, and store this concept graph for subsequent use.  The YTEX `ConceptDaoImpl` takes as input a properties file that specifies a query that retrieves all edges that will be used in the taxonomy, generates a graph, and removes cycles.  For the i2b2 corpus, we select edges using the following query:
```
select cui1, cui2 
from umls.MRREL 
where sab in ('RXNORM', 'SNOMEDCT', 'SRC')
and rel in ('PAR', 'RB')
```
As you can see, you have a large degree of flexibility in choosing which concepts and edges to use in your concept graph - you can filter by source vocabulary (sab), relation type (rel), relation attribute (rela) and more.

## `InfoContentEvaluatorImpl` ##
We use infocontent to compute semantic similarity via the lin measure.  We evaluate the information content of each concept in a corpus using the `InfoContentEvaluatorImpl` and store this in the `feature_rank` and `feature_eval` tables.  The `InfoContentEvaluatorImpl` is parameterized by a query that retrieves the raw frequency of each concept in a corpus.  For the i2b2 corpus, we do the following:
```
select code, count(*)
from anno_ontology_concept o
inner join anno_base b on b.anno_base_id = o.anno_base_id
inner join document d on d.document_id = b.document_id
where d.analysis_batch in ('i2b2.2008')
group by code
```
As you can see, you have a great deal of flexibility in how to retrieve concept frequencies: if it's in your database, you can get it.

## `ImputedFeatureEvaluator` ##
The `ImputedFeatureEvaluator` computes the propagated and imputed information gain of every concept in a corpus.  This is parameterized by queries that retrieve the document - class and document - concept relationships.  For example, for the I2B2 corpus
  * instanceClassQuery:  This query retrieves rows with the following fields:
    * document id: numeric (long) document id
    * document class: string document class
    * train/test flag: boolean (0/1) - is the instance from the training (1) or test (0) set.  Only documents from the training set are used to compute the infogain.
    * document label: string document label.  Optional, used only for a multi-label classification task)
For the i2b2 dataset, we use the following query:
```
select d.instance_id, a.class, 1, a.label
from corpus_doc d
inner join corpus_label a 
on a.instance_id = d.instance_id 
and a.corpus_name = d.corpus_name
where d.doc_group = 'train'
and d.corpus_name = 'i2b2.2008'
```
  * ytex.conceptInstanceQuery: This query retrieves document-concept relationships; the following fields are required:
    * label: Corresponds to document label from instanceClassQuery.  If this is not a multilabel task, simply retrieve the empty string.
    * document id: numeric document id
    * concept id: string concept id
For the i2b2 dataset, we use the following query:
```
select *, 1
from
(
    select distinct c.code, d.instance_id
    from corpus_doc d
    inner join corpus_label a 
        on a.instance_id = d.instance_id 
        and a.corpus_name = d.corpus_name    
    inner join ytex.document yd 
        on yd.uid = d.instance_id 
        and yd.analysis_batch = d.corpus_name
    inner join ytex.anno_base ac 
        on ac.document_id = yd.document_id
    inner join ytex.anno_ontology_concept c 
        on ac.anno_base_id = c.anno_base_id
    where d.doc_group = 'train'
    and d.corpus_name = 'i2b2.2008'
    and a.label = :label
) s
```
As you can see, you have a wide degree of flexibility in defining the document-concept relationship and document-class relationship.

The propagated and imputed infogain are stored in the `feature_rank` and `feature_eval` tables.  For the I2B2 corpus, to view the propagated infogain for the hypertension label:
```
select r.feature_name, min(str), truncate(r.evaluation, 3), r.rank
from feature_eval fe 
inner join feature_rank r 
    on r.feature_eval_id = fe.feature_eval_id
left join umls.MRCONSO c 
    on c.cui = r.feature_name 
    and c.tty in ('PT', 'PN', 'BN', 'OCD') and lat = 'ENG'
where fe.type = 'infogain-propagated' 
    and fe.label = 'Hypertension'
    and r.rank < 50
group by r.feature_name, r.evaluation, r.rank
order by r.rank ;
```

And to view the imputed infogain:
```
select r.feature_name, min(str), truncate(r.evaluation, 3), r.rank
from feature_eval fe 
inner join feature_rank r 
    on r.feature_eval_id = fe.feature_eval_id
left join umls.MRCONSO c 
    on c.cui = r.feature_name 
    and c.tty in ('PT', 'PN', 'BN', 'OCD') and lat = 'ENG'
where fe.type = 'infogain-imputed' 
    and fe.label = 'Hypertension'
    and r.rank < 100
group by r.feature_name, r.evaluation, r.rank
order by r.rank ;
```

## `SemanticSimilarityService` ##
This currently does not have a command-line interface; it is used by the CorpusKernelEvaluator.  In a future release, we will implement a web-services interface to retrieve semantic similarity measures.

## `CorpusKernelEvaluator` ##
This tool evaluates a kernel on a corpus.  Kernels can be viewed as similarity measures on arbitrary objects (not necessarily vectors).  We export the objects to be classified (documents) as hierarchical data structures (trees), and apply [convolution kernels](http://books.nips.cc/papers/files/nips14/AA58.pdf) to these trees.  As you can surmise from this mouthful, this subject is a bit complex; in this section we provide a short overview of how this works.  We will provide a more detailed description of our kernel implementation shortly.

### Exporting Trees ###
The first step in applying YTEX kernels is exporting documents as trees.  The YTEX `KernelLauncher` takes a property file that contains queries that retrieve the documents and their hierarchical attributes, and puts these attributes in a hierarchical tree structure, and exports these trees to the file system (creating a forest).  Refer to the I2B2 example below.

### Defining Kernels ###
We support the declarative definition of kernels using [Spring Dependency Injection](http://martinfowler.com/articles/injection.html).  Kernels are basically collaborations of objects that implement the YTEX Kernel interface.  We have implemented a semantic kernel that calls the `SemanticSimilarityService`.

### Evaluating Kernels ###
The `CorpusKernelEvaluator` takes as input the document trees of a corpus and a kernel definition, and evaluates the kernel on all document pairs.  The number of evaluations is quadratic in n; to speed things up, this can be parallelized across multiple processes (we actually parallelize kernel evaluations across an HPC cluster, making this very fast).  Kernel evaluations are stored in the `kernel_eval` and `kernel_instance_eval` tables.

### Exporting Kernel Matrices ###
Kernel machines such as SVMs can be trained on the matrix of pairwise kernel evaluations.  We have implemented tools to export these matrices to the libsvm format (`LibSVMGramMatrixExporterImpl`), and to a generic matrix format that can be used in tools such as Matlab and R (`RGramMatrixExporterImpl`).

### Training and Testing SVMs ###
We use LibSVM to train SVMs on kernel matrices.  We have implemented scripts that train and test LibSVM models, and store the prediction results in the database.  Prediction results are stored in the `classifier_eval*` tables.

# i2b2 2008 #
This section describes how to reproduce our results on the I2B2 2008 challenge dataset.

## Prerequisites ##
  * Quad Core workstation at the minimum with at least 6 GB memory; 8 cores would be better
  * [Install umls 2010AB](http://code.google.com/p/ytex/wiki/InstallationV05) or higher in your MySQL
  * [Install YTEX v0.5](http://code.google.com/p/ytex/wiki/InstallationV05) with MySQL (configured to use the UMLS)
  * [Install libsvm 3.1](http://www.csie.ntu.edu.tw/~cjlin/libsvm/) or higher
  * Download [I2B2 2008 Challenge](https://www.i2b2.org/NLP/Obesity/) data and extract all I2B2 2008 data files to some directory, referred to as `i2b2.dir`.  The following files are required:
    * obesity\_patient\_records\_training2.xml
    * obesity\_patient\_records\_test.xml
    * obesity\_standoff\_annotations\_training.xml
    * obesity\_standoff\_annotations\_test.xml
    * obesity\_standoff\_annotations\_training\_addendum.xml
    * obesity\_standoff\_annotations\_training\_addendum2.xml
    * obesity\_standoff\_annotations\_training\_addendum3.xml
  * Download i2b2-2008-v05.zip from this site, extract to your ytex home directory: you should have a folder `YTEX_HOME/i2b2.2008` when you are finished.
  * Install ytex data mining tables: open a command prompt and run the following commands (Unix users - you should be able to figure out the equivalent commands):
```
cd YTEX_HOME
setenv.cmd
cd data
ant -Dytex.home=%YTEX_HOME% kernel.create
```

## Configure ytex.properties ##
Add the following to your `ytex.properties` file:
```
# the IP address/hostname of your mysql server
db.host=localhost
# where challenge data was extracted
i2b2.dir=C:/downloads/text mining/i2b2 2008 challenge
# where libsvm binaries are located
libsvm.bin=C:/java/libsvm-3.1/windows
# where ytex is installed  
ytex.home=C:/clinicalnlp/ytex
# we generate a file that contains the umls concept graph 
# this is where we store it, adjust to match your environment
ytex.conceptGraphDir=C:/clinicalnlp/ytex/conceptGraph
# how many cores does your system have? 
# should have atleast 4, or this will take more than a weekend
kernel.threads=4
# the slices should go from 1 ... kernel.threads
kernel.slices=1,2,3,4
# where to place temporary files
kernel.eval.tmpdir=C:/temp
# copy the rest as is
parallel.folds=yes
kernel.name=i2b2.2008
ytex.conceptGraphName=rbpar
ytex.corpusName=i2b2.2008
ytex.conceptSetName=ctakes

# by default maxMemory is 1500m
# with a 64-bit jvm you may have to up this to 2g
# maxMemory=2g
```

## Reproduce results ##
This is completely automated by an ant build script.  We have split this up unto several high-level steps:
  * Setup: load and annotate the corpus, compute information-theoretic measures for all features (words, concepts) in the corpus.
  * Cross-Validation: perform cross-validation on the i2b2 training set to find the optimal parameters.  We have obviously already done this, so you can skip this step if you prefer.
  * Test: train models on the training set using optimal parameters, apply to test set.

Summary results (F1 scores) are stored in the `YTEX_HOME/i2b2.2008/<experiment>-test/results.txt` file, where `<experiment>` is one of word, imputed, cui, lin, superlin.  The individual document classifications are stored in the `classifier_instance_eval` table.

## Configure setenv.bat/ytex.profile ##
You need to specify the directory that contains the `mysql` executable used to execute `.sql` scripts.

For linux, add the following to `ytex.profile` (adjust the path to match your environment).
```
MYSQL_HOME=/usr/bin
export MYSQL_HOME
```

For windows, add the following (adjust the path to match your environment - `mysql.exe` will be in that directory).
```
set MYSQL_HOME=C:\Program Files\MySQL\MySQL Server 5.1\bin
```


### Setup ###
To load and annotate the corpus, compute hotspots, open a command prompt, and run the following from the commands.  (This will take ~4 hours w/ 4 cores):
```
cd YTEX_HOME
setenv.cmd
cd i2b2.2008
ant -Dytex.home=%YTEX_HOME% setup.all 
```

### Cross-Validation and Test ###
To run the cross validation and test, run the following (will take ~30 hours w/ 4 cores):
```
ant -Dytex.home=%YTEX_HOME% cv.all test.all
```

### Test Only ###
To run just the test using the optimal parameters we found (will take ~6 hours w/ 4 cores):
```
ant -Dytex.home=%YTEX_HOME% test.skip.cv test.all
```

## Setup ##
We did the following in the setup:
  * load i2b2: Load i2b2 data - the notes and class labels - into the database
  * setup v\_i2b2\_fword\_lookup:  Created a dictionary lookup table from the umls
  * run cpe: annotated i2b2.2008 corpus with the YTEX Pipeline
  * setup tfidf: compute tf-idf statistics on corpus
  * evaluate infogain: compute infogain of each word for each label (disease) in the i2b2 corpus
  * Generate Concept Graph: generate an acyclic directed graph representing the taxonomical relationships from the UMLS
  * evaluate infocontent: compute information content of each concept in the i2b2 corpus
  * evaluate imputed infogain: compute imputed infogain of each concept for each label in the i2b2 corpus
  * generate folds: generate stratified cross-validation folds

### Load I2B2 Data ###
We load the i2b2 data into the `corpus_doc` and `corpus_label` tables, which hold the documents and their labels respectively.

### Setup v\_i2b2\_fword\_lookup ###
We create a dictionary lookup table that uses the following UMLS Source Vocabularies:
  * SRC
  * MTH
  * RXNORM
  * SNOMEDCT
  * MSH
  * CSP
  * MEDLINEPLUS
  * MEDCIN
Refer to `YTEX_HOME\i2b2.2008\data\v_i2b2_fword_lookup.sql`

### Run CPE ###
We annotate the corpus with the YTEX pipeline.  We launch several CPE processes (as many cores as you have) in parallel to speed up processing.  We also only store the sentence, word, number, and named entity annotations to speed up processing.  Refer to `YTEX_HOME\i2b2.2008\desc\cpe.template.xml`.

### Setup tfidf ###
We compute tf-idf statistics on CUIs and store them in the `feature_eval` and `feature_rank` tables; refer to `YTEX_HOME\i2b2.2008\data\tfidf-cui.sql`.  Later during classification, we perform frequency thresholding on CUIs.

### Evaluate infogain ###
We evaluate the infogain of all words wrt all i2b2 labels, using only the i2b2 training data.  This is done by the `WekaAttributeEvaluatorImpl` which calls the WEKA [`InfoGainAttributeEval`](http://weka.sourceforge.net/doc.dev/weka/attributeSelection/InfoGainAttributeEval.html).  We store the evaluations in the `feature_eval` and `feature_rank` tables.  To view e.g. the top ranked words for hypertension, run the following query:
```
select r.feature_name, r.evaluation, r.rank
from feature_eval fe 
inner join feature_rank r 
    on r.feature_eval_id = fe.feature_eval_id
where fe.type = 'InfoGainAttributeEval' 
    and fe.label = 'Hypertension'
    and fe.featureset_name = 'usword'
    and r.rank < 11
order by rank;
```

### Generate Concept Graph ###
We call the `ConceptDaoImpl` (see above).

### Evaluate infocontent ###
We evaluate the information content of each concept in the i2b2 corpus using the `InfoContentEvaluatorImpl` and store this in the `feature_rank` and `feature_eval` tables  (see above).

### Evaluate imputed infogain ###
We call the `ImputedFeatureEvaluator` computes the propagated and imputed information gain of every concept in the i2b2 corpus  (see above).

### Generate Folds ###
We generate stratified cross-validation folds and store them in the `cv_fold` and `cv_fold_instance` tables using the `FoldGeneratorImpl`.  There is only one document in the training set for some label/class combinations; such documents are duplicated in the cv train/test sets.

## Cross-Validation ##
We performed several experiments in which we trained SVMs using different feature representations.  We identified the optimal parameters by performing a 5x2 fold cross-validation on the training set.  The `YTEX_HOME\i2b2.2008\<experiment>` directory contains files related to each experiment.  We ran the following experiments:
  * Bag-of-Words (word): This experiment represents a baseline with which we attempted to reproduce Ambert’s results.
  * Bag-of-Words + Imputed Hotspots (imputed): This experiment was designed to measure the contribution of imputed feature ranking.
  * Bag-of-Words + Imputed Hotspots + CUIs (cui): This experiment was designed to measure the contribution of enriching the feature set with UMLS Concept Unique Identifiers (CUIs); previous experiments used only words as features.
  * Semantic Kernel (lin): This experiment was designed to measure the contribution of unsupervised semantic similarity measures.
  * Supervised Semantic Kernel (superlin): This experiment was designed to measure the contribution of supervised semantic similarity measures.

In addition to the SVM Cost parameter, the parameters we need to optimize are:
  * Word hotspot cutoff (for all experiments)
  * Imputed cui hotspot cutoff (for all experiments beside word)
  * Supervised Semantic Similarity imputed cui cutoff (for superlin)
It is impractical to search a 4-dimensional hypercube for the optimal parameter.  Therefore, we optimize the parameters sequentially:
  * Word experiment: identify optimal word hotspot cutoff
  * Cui experiment: use optimal word hotspot cutoff, identify optimal imputed cui hotspot cutoff
  * Superlin experiment:  use optimal word and imputed cui hotspot cutoff, identify optimal supervised semantic similarity imputed cui cutoff.

The results of each cross-validation run are stored in the `classifier_eval` tables.  After each cross-validation experiment, we identify the optimal parameters for each label, and store them in the `cv_best_svm` table.

## Test ##
For each experiment, we train and evaluate SVMs on the feature representation obtained using the optimal parameters identified via the cross-validation.  We store the classification results in the `classifier_eval` tables and generate a report with aggregate results (macro-f1 scores).