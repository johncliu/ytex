# YTEX Data Model #
This page discusses the YTEX data model.  The figure below provides an overview of the YTEX data model:

![http://ytex.googlecode.com/svn/trunk/workspace/doc/images/dbdiagram_v07.jpg](http://ytex.googlecode.com/svn/trunk/workspace/doc/images/dbdiagram_v07.jpg)

YTEX maps UIMA annotations to a relational database using a table per annotation class.  Basically, a table exists for each UIMA annotation class.  Primitive annotation attributes are mapped directly to table columns.  Our strategy for mapping annotations to the database was to perform a 1-to-1 mapping: what you see in the database should correspond exactly to what you see in the UIMA CAS viewer.

## `document` ##
The `document` table represents a single note/document.  The columns are
  * `document_id` - unique generated id
  * `instance_id` - user defined id - i.e. a numeric reference to the document in your system.
  * `instance_key` - user defined id - a string reference to a document in your system.  When using a CollectionReader that retrieves files from the file system, this will correspond to the file name.
  * `analysis_batch` - a user-defined 'document group'
  * `cas` - the xml representation of the cas, gzipped
  * `doc_text` - the text of the document
Refer to the [UserGuide](UsingYTEX_V05#YTEX_Pipeline_Configuration_Parameters.md) for details on how to configure the YTEX Pipeline to update the document table.For each document processed, a `document` row is created.


## `anno_base` ##
An `anno_base` record represents an UIMA `Annotation`; there is a one-to-many relationship between `document` and `anno_base`.  The columns are:
  * `anno_base_id` - unique generated id
  * `document_id` - foreign key to `document` table
  * `span_begin` - corresponds to [Annotation.begin](http://uima.apache.org/downloads/releaseDocs/2.3.0-incubating/docs/api/org/apache/uima/jcas/tcas/Annotation.html#getBegin%28%29) attribute
  * `span_end` - `Annotation.end` attribute
  * `uima_type_id` - Foreign key to `ref_uima_type`, which contains the fully class name of the `Annotation` to which this record is mapped.

## `anno_[subclass]` ##
Annotation subclasses may have additional attributes; these attributes are stored in additional tables prefixed with `anno_`.  E.g. additional attributes of the `Sentence` annotation are stored in the `anno_sentence` table.  The primary key of these annotation subclass tables corresponds to the primary key of the `anno_base` table (i.e. it is also a foreign key).


### `anno_token` ###
This is mapped to the `edu.mayo.bmi.uima.core.type.NumToken`, `edu.mayo.bmi.uima.core.type.WordToken`, and `ytex.uima.types.WordToken` annotations.
  * `anno_base_id` - foreign key to `anno_base`, also primary key for this table
  * `tokenNumber` - from `BaseToken`
  * `normalizedForm` - from `BaseToken`
  * `partOfSpeech` - from `BaseToken`
  * `coveredText` - the text spanned by this token
  * `capitalization` - 0 - no caps, 1 - 1 cap letter in word, 2 - 2 cap letters in word, 3 - 3 or more cap letters in word
  * `numPosition` - 1st position of number within word
  * `canonicalForm` - uninflected lower case word form, set by LVGAnnotator
  * `negated` - 1 - word is negated, 0 - word is not negated  (based on negex)
  * `possible` - 1 - possible (from negex)

### `anno_med_event` ###
This is mapped to the cTAKES `Medicationevent` annotation.

## Feature Structures ##
In addition to Annotations, UIMA defines `FeatureStruct`s; these are typically not 'free standing' annotations - they usually are 'inside'  an Annotation.  e.g. the `Medicationevent` and `EntityMention` annotations have arrays of `OntologyConcept`s.  `FeatureStruct`s are also mapped to `anno_[subclass]` tables, e.g. `OntologyConcept`s are mapped to the `anno_ontology_concept` table, and have a foreign key to the annotation 'within which' they reside (one-to-many relationship).

### `anno_ontology_concept` ###
This is mapped to the cTAKES `OntologyConceptArr` of the `Medicationevent` or `EntityMention` annotation; these are the concepts (CUIs) of a Named Entity:
  * `anno_ontology_concept_id` - unique system generated id
  * `anno_base_id` - foreign key to `named_entity`
  * `code` - CUI
  * `disambiguated` - used by SenseDisambiguatorAnnotator.  Set to 1 if this concept is the best sense or only sense for the given named entity.  Set to 0 (default) otherwise, or if the annotator is not used.

### anno\_mm\_candidate ###
Metamap `Candidate` annotations are mapped to this table.

## Annotation Links ##
UIMA annotations can also have references to other UIMA annotations, e.g. the `TreeBankNode` annotation represents a node in a parse tree.  This annotation has reference to a parent and children `TreeBankNode` annotations.  Rows in the `anno_link` represent Annotation links

  * `anno_link_id`: Synthetic primary key
  * `parent_anno_base_id`: foreign key to the anno\_base table.  This is the parent (source) of the link
  * `child_anno_base_id`: foreign key to the anno\_base table.  This is the child (target) of the link
  * `feature`: the attribute on the parent object that corresponds to this link

## `anno_contain` ##
This table represents containment relationships between annotations, e.g. that a word/named entity is contained in a sentence.  This has no direct equivalent in any UIMA object; these relationships can be inferred from the begin/end of UIMA annotations, but 'precomputing' these relationships has many practical applications; e.g. it simplfies writing queries of the sort 'give me all named entities in the Impression section'.
  * `parent_anno_base_id` - foreign key to `anno_base` table.  Represents the parent or containing concept.
  * `parent_uima_type_id` - foreign key to `ref_uima_type`, the class of the parent annotation.
  * `child_anno_base_id` - foreign key to `anno_base` table.  Represents the child or contained concept.
  * `child_uima_type_id` - foreign key to `ref_uima_type`, the class of the child annotation.

# Configuration #
Mapping of Annotations is purely configurative.  To map a new annotation do the following:
  1. Create a table in your database to store the annotation's attributes.
  1. Tell YTEX to map the annotation class to your table (i.e. add a row to the `ref_uima_type` table).

To illustrate this, say for example we would like to map your annotation named Foo that has a 'period' has the float attribute `period`.  We would create a table for this annotation, e.g. for mysql:
```
create table anno_foo (
  anno_base_id int not null primary key, /* foreign key to anno_base */
  period double
) engine=myisam;
```
Note: the column names must match the UIMA annotation's attribute names (case insensitive).

And we need to tell YTEX to map `Foo`s to this table:
```
insert into ref_uima_type (uima_type_id, uima_type_name, table_name)
values (201, 'org.acme.Foo', 'anno_foo');
```

## `ref_uima_type` ##
This table tells YTEX what annotations to map, and the tables to map them to:
  * `uima_type_id`: unique, manually assigned id
  * `uima_type_name`: the fully qualified class name for the annotation
  * `table_name`: the table to which the annotation should be mapped.  If null, only a row in the `anno_base` table will be created for this annotation (many annotations do not have any additional properties).

## `config/desc/ytex/beans-uima-mapper.xml` ##
This is a spring bean configuration file that allows more mapping customization, e.g. mapping attributes to columns with different names.