# YTEX Data Model #
This page discusses the YTEX data model.  The figure below provides an overview of the YTEX data model:

![http://ytex.googlecode.com/svn/trunk/workspace/doc/images/dbdiagram.jpg](http://ytex.googlecode.com/svn/trunk/workspace/doc/images/dbdiagram.jpg)

YTEX maps cTAKES annotations to a relational database using the Table-Per-Subclass object-relational mapping paradigm.  Basically, a table exists for each class in the annotation hierarchy.  All UIMA annotations are subclasses of the `Annotation` class, which is mapped to the `anno_base` table.  cTAKES annotations such as `WordToken` is a subclass of the `BaseToken` annotation, which in turn is a subclass of `Annotation`; the additional properties of `BaseToken` and `WordToken` are mapped to columns of the `anno_base_token` and `anno_word_token` tables respectively.  To get all the attributes of a `WordToken` annotation, simply join the `anno_base`, `anno_base_token`, and `anno_word_token` tables.

Our strategy for mapping annotations to the database was to perform a 1-to-1 mapping: what you see in the database should correspond exactly to what you see in the UIMA CAS viewer.

Here, we detail a few of the main annotation types / tables.

# `document` #
The `document` table represents a single note/document.  The columns are
  * `document_id` - unique generated id
  * `uid` - 'user' id - i.e. a numeric reference to the document in your system.
  * `analysis_batch` - a user-defined 'document group'
  * `cas` - the xml representation of the cas, gzipped
  * `doc_text` - the text of the document
Refer to the [UserGuide](UsingYTEX_V05#YTEX_Pipeline_Configuration_Parameters.md) for details on how to configure the YTEX Pipeline to update the document table.

# `anno_base` #
An `anno_base` record represents an UIMA `Annotation`; there is a one-to-many relationship between `document` and `anno_base`.  The columns are:
  * `anno_base_id` - unique generated id
  * `document_id` - foreign key to `document` table
  * `span_begin` - corresponds to [Annotation.begin](http://uima.apache.org/downloads/releaseDocs/2.3.0-incubating/docs/api/org/apache/uima/jcas/tcas/Annotation.html#getBegin%28%29) attribute
  * `span_end` - `Annotation.end` attribute
  * `uima_type_id` - Foreign key to `ref_uima_type`, which contains the fully class name of the `Annotation` to which this record is mapped.
  * `covered_text` - The text covered by this annotation (i.e. the text between begin and end).  This is only stored (and indexed) for certain annotations; this can be configured by the `DBConsumer.typesStoreCoveredText` configuration parameter.

# `anno_word_token` #
This is mapped to the `ytex.uima.types.WordToken`, which extends cTAKES' `edu.mayo.bmi.uima.core.ae.type.WordToken` annotation with attributes that indicate the negation status of the word.
  * `anno_base_id` - foreign key to `anno_base`, also primary key for this table
  * `capitalization` - 0 - no caps, 1 - 1 cap letter in word, 2 - 2 cap letters in word, 3 - 3 or more cap letters in word
  * `num_position` - 1st position of number within word
  * `canonical_form` - uninflected lower case word form, set by LVGAnnotator
  * `negated` - 1 - word is negated, 0 - word is not negated  (based on negex)
  * `possible` - 1 - possible (from negex)

# `anno_named_entity` #
This is mapped to the cTAKES `NamedEntity` annotation.  Most of the attributes of this annotation are always empty/set to the same value and are thus useless.  However, we map attributes from the annotation to the database, even if it doesn't make sense (we want to model as closely as possible, even if the data is meaningless).  The only useful attribute is the certainty which represents the negation status:
  * `anno_base_id` - foreign key to `anno_base`, also primary key for this table
  * `certainty` - -1 - negated (via negex), 0 - not negated
  * `discovery technique` - always 1
  * `status` - always 0
  * `type_id` - always 0
  * `segment_id` - always null

# `anno_ontology_concept` #
This is mapped to the cTAKES `OntologyConceptArr` of the `NamedEntity` annotation; these are the concepts (CUIs) of a Named Entity:
  * `anno_ontology_concept_id` - unique system generated id
  * `anno_base_id` - foreign key to `named_entity`
  * `code` - CUI
  * `coding_scheme` - always 'UMLS'
  * `oid` - always null
  * `disambiguated` - used by SenseDisambiguatorAnnotator.  Set to 1 if this concept is the best sense or only sense for the given named entity.  Set to 0 (default) otherwise, or if the annotator is not used.


# `anno_contain` #
This table represents containment relationships between annotations, i.e. that a word/named entity is contained in a sentence.  This has no direct equivalent in any UIMA object; these relationships can be inferred from the begin/end of UIMA annotations, but 'precomputing' these relationships has many practical applications; e.g. it simplfies writing queries of the sort give me all named entities in the Impression section.
  * `parent_anno_base_id` - foreign key to `anno_base` table.  Represents the parent or containing concept.
  * `parent_uima_type_id` - foreign key to `ref_uima_type`, the class of the parent annotation.
  * `child_anno_base_id` - foreign key to `anno_base` table.  Represents the child or contained concept.
  * `child_uima_type_id` - foreign key to `ref_uima_type`, the class of the child annotation.