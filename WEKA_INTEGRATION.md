# Introduction #
WEKA is a powerful, open-source, java-based data mining toolkit.  It implements a wide variety of clustering, classification, and regression algorithms, and is very easy to use.  This article describes using YTEX with WEKA.

# Bag-of-Words #
For the purposes of document classification, text can be represented as a 'Bag-of-Words': a matrix with 'words' as columns and documents as rows.  The value of the column represents the (weighted) word frequency, or is an indicator representing the presence of the word in the document.  The words can be the raw natural language word, the stemmed word, or concept identifiers.  This is typically a very high-dimensional feature space, i.e. the number of columns (distinct words) can be very large.  This space is typically sparse: most of the words assume the value 0.

To deal with sparse, high-dimensional spaces efficiently, WEKA (as do other statistics packages) support a sparse file format.  WEKA exchanges data in the form of attribute relationship (ARFF) files, comma-delimited files with a header that specifies the data type of each column.

# Demo #
Before getting into the details, go through a demo that illustrates this.

## 1. Annotate Documents ##
  * Open a command prompt
  * Change to the YTEX\_HOME\examples\weka-fracture directory and execute the Collection Processing Engine:
```
cd C:\java\clinicalnlp\ytex\examples\weka-fracture
..\..\ytexCPE.cmd
```
  * Choose File->Open CPE Description and select `weka-fracture-cpe.xml`
  * Press the play button at the bottom

This will annotate all documents in the `weka-fracture\reference set` directory; it will assign them the `weka-demo` analysis batch.  Note that the paths are important - the cpe descriptor file uses paths relative to the directory from which the cpe is launched (the `weka-fracture` directory).

## 2. Export Bag-of-Words ##
The BagOfWords exporter takes a Java property file as input; the property file specifies the SQL queries used to obtain documents, their class labels, and their 'words'.  There are 2 parameter files per database platform:
  * `fracture-word.<db>.xml`
> Uses stemmed words as the 'words'.
  * `fracture-cui.<db>.xml`
> Uses only **affirmed** UMLS CUIs as the 'words'.

To run the BagOfWords exporter:
  * close the CPE
  * execute the following from the command prompt:
```
..\..\ytexExportBagOfWords.cmd fracture-cui.<db>.xml
..\..\ytexExportBagOfWords.cmd fracture-word.<db>.xml
```
  * you should see 2 arff files in this directory:
```
dir *.arff
```

## 3. Open ARFF File with WEKA Explorer ##
  * start the WEKA Gui Chooser: call startWeka.bat
```
startWeka.bat
```
  * start Explorer: click on `Explorer`
  * open an arff file: click on 'open file' and select e.g. `fracture-word.arff`
  * you should see a list of all the attributes

## 4. Try some classifiers out ##
The rest is standard WEKA usage.  Here is one suggestion: filter for the top 50 attributes ranked by mutual information, and try the J48 decision tree.  If you are new to WEKA, refer to the [Explorer Guide](http://iweb.dl.sourceforge.net/project/weka/documentation/3.5.x/ExplorerGuide-3-5-8.pdf)

# Details #
The `BagOfWordsExporter` is parameterized by java properties that contain queries that determine how data will be exported.  Refer to `fracture-cui.<db>.xml` and `fracture-word.<db>.xml` for sample queries.

## instanceClassQuery ##
> Retrieves instance ids (i.e. document ids) and their class labels.  This query must return 2 columns:
    * instance id: integer
    * class label: will be converted to string

In the example, documents whose file names are not divisible by 3 represent reports that assert the presence of fractures.  We used the following query, which retrieves the document id (instance id), and the class label, calculated from the file name which was stored in the anno\_source\_doc\_info table.
```
select
	d.document_id,
	case
		when cast(left(right(uri, 8), 4) as unsigned) % 3 = 0 then 'no fracture'
		else 'fracture'
	end class
from anno_source_doc_info s
inner join anno_base da on s.anno_base_id = da.anno_base_id
inner join document d on da.document_id = d.document_id
where d.analysis_batch = 'weka_demo' and
s.uri like '%/reference set/%'
```

## numericWordQuery ##
Retrieves numeric instance attributes for all attribute-instance combinations.  Must return 3 columns:
  1. instance\_id (integer)
  1. attribute name (string)
  1. attribute value (double)

In the example, we retrieved the number of times a document contains a CUI:
```
select o.document_id, code, COUNT(*)
from v_document_ontoanno o
inner join anno_base b on o.document_id = b.document_id
inner join anno_source_doc_info s on b.anno_base_id = s.anno_base_id
where certainty <> -1
and analysis_batch = 'weka_demo'
and s.uri like '%/reference set/%'
group by o.document_id, code
```

## nominalWordQuery ##
Either a numericWordQuery, a nominalWordQuery, or both must be specified. Retrieves nominal instance attributes for all attribute-instance combinations.  Must return 3 columns:
  1. instance\_id (integer)
  1. attribute name (string)
  1. attribute value (string)

## arffRelation ##
This is the internal 'name' of the dataset

## arffFile ##
This is the name of the file to be generated.