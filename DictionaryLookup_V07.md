# Introduction #

This page describes the cTAKES dictionary lookup algorithm and its configuration in YTEX.  The dictionary lookup is very simple; this simplicity is both a blessing and a curse.  It's simplicity makes it very easy to configure and troubleshoot; however this also limits its functionality.  **If you don't like cTAKES' dictionary lookup algorithm, you may want to try the YTEX metamap pipeline.**  The YTES MetaMap pipeline uses the normal cTAKES pipeline, but replaces the cTAKES dictionary lookup with the Metamap uima annotator; see [user guide](UsingYTEX_V07.md) for details.

The cTAKES dictionary lookup algorithm relies on a table that contains the first word of a term, the full text of a term, and the term's identifier.  The dictionary lookup takes text in a lookup window; for each word in the lookup window it searches the dictionary for a match to first word column.  For each hit, it tries to match subsequent words from the lookup window to subsequent words in the term.  For example, if the lookup window contains the text 'congestive heart failure', cTAKES will search for 'congestive', 'heart', and 'failure', and try to match all sequences of words to terms in the dictionary.  Depending on your dictionary, this will result in separate annotations for 'heart', 'heart failure', and 'congestive heart failure'.

One potential drawback to the cTAKES dictionary lookup algorithm is that it requires an exact match to the text in the dictionary.  For example, the cTAKES DictionaryLookupAlgorithm will not match the text 'Transplant organ' to the concept 'Transplanted organ' (cui C0524930).  With YTEX v0.5 and onwards, we implemented an additional dictionary lookup algorithm that matches uninflected (stemmed) text to UMLS Concepts.

# Configuration #

## Lookup Window ##
The Dictionary Lookup algorithm searches text that lies within a configured annotation type.  By default, YTEX is configured to use the sentence as a lookup window.  cTAKES also has a LookupWindowAnnotation; you can configure the dictionary lookup to look in LookupWindowAnnotation to increase precision at the expense of recall.  This is configured in `YTEX_HOME/config/desc/ytex/uima/annotators/LookupDesc_SNOMED.xml`

## Default YTEX Dictionary Lookup Table ##
During installation YTEX creates a table `umls_aui_fword` with the first word of every term from the UMLS `MRCONSO` table, the tokenized text of the term, and the stemmed text of the term.  It creates a table `v_snomed_fword_lookup` that joins the `umls_aui_fword` and `MRCONSO` tables.  The SQL for this table is:
```
insert into v_snomed_fword_lookup (cui, tui, fword, fstem, tok_str, stem_str)
select mrc.cui, t.tui, c.fword, c.fstem, c.tok_str, c.stem_str
from umls_aui_fword c
inner join umls.MRCONSO mrc on c.aui = mrc.aui and mrc.SAB in ( 'SNOMEDCT', 'RXNORM')
inner join 
(
	select cui, min(tui) tui
	from umls.MRSTY sty
	where sty.tui in
	(
    /* diseasesAndDisordersTuis */
    'T019', 'T020', 'T037', 'T046', 'T047', 'T048', 'T049', 'T050', 
      'T190', 'T191', 'T033',
    /* signAndSymptomTuis */
    'T184',
    /* anatomicalSitesTuis */
    'T017', 'T029', 'T023', 'T030', 'T031', 'T022', 'T025', 'T026',
        'T018', 'T021', 'T024',
    /* medicationsAndDrugsTuis */
     'T116', 'T195', 'T123', 'T122', 'T118', 'T103', 'T120', 'T104',
        'T200', 'T111', 'T196', 'T126', 'T131', 'T125', 'T129', 'T130',
        'T197', 'T119', 'T124', 'T114', 'T109', 'T115', 'T121', 'T192',
        'T110', 'T127',
	/* proceduresTuis */
    'T060', 'T065', 'T058', 'T059', 'T063', 'T062', 'T061',
    /* deviceTuis */
    'T074', 'T075',
    /* laboratoryTuis */
    'T059'
	)
	group by cui
) t on t.cui = mrc.cui
```

This tableincludes only the SNOMED and RXNORM vocabularies, and limits the semantic types (T0XX...).  You are free to modify this table to include other UMLS source vocabularies, and other semantic types.

## Using your own dictionary ##
You can create your own dictionary: you just need to create a table with the 3 fields (first word, tokenized concept text, and concept id) and point the dictionary lookup annotator at it.  The table that is queried is configured in `YTEX_HOME/config/desc/ytex/uima/annotators/LookupDesc_SNOMED.xml`.

Care must be taken with concepts that include punctuation characters: the tokens that comprise the concept must be separated according to the cTAKES tokenizer.  This splits text according to Penn Treebank rules.

## Dictionary lookup with stemmed words ##
To perform dictionary lookup with stemmed words, replace `YTEX_HOME/config/desc/ytex/uima/annotators/LookupDesc_SNOMED.xml` with `YTEX_HOME/config/desc/ytex/uima/annotators/LookupDesc_stem_SNOMED.xml`.

# Debugging Dictionary Lookup #
In case the concepts you expect aren't being annotated, perform the following steps.  For this example, we'll assume that the term 'Taxol' is not being annotated.

  * Check MRCONSO:
The simplest explanation would be that the term you are using is missing from the MRCONSO table; check this with the following query:
`select * from umls.MRCONSO where str = 'Taxol'`

  * Check umls\_aui\_fword:
YTEX sets up a table 'umls\_aui\_fword' that has the first word of every term from MRCONSO; there might have been a problem setting this table up.  To check this table run this following query:
`select * from umls_aui_fword where fword = 'taxol'`

  * Check v\_snomed\_fword\_lookup:
By default the DictionaryLookupAnnotator is configured to use a tablecalled 'v\_snomed\_fword\_lookup'.  This table is created out of a join of the umls\_aui\_fword and MRCONSO tables.  This table might exclude the vocabulary or semantic type that Taxol belongs to.  To check this, simply try:
`select * from v_snomed_fword_lookup where fword = 'taxol'`

  * Check Lookup Window
If you are sure that the word is in your dictionary (i.e. in `v_snomed_fword_lookup`), perform this step.  The cTAKES dictionary lookup algorithm tries to match word(s) that lie within a configured Lookup Window annotation.  By default, YTEX uses the Sentence as a lookup Window.  There might be a problem in sentence splitting that causes the word to fall outside a sentence.  Open the document that contains taxol in the DBAnnotationViewer and verify that it is in a sentence.

# Notes on UMLS Installation #
During setup, we try to see if the UMLS is installed in the database (we look for the MRCONSO table); if we don't find it, we look for a <tt>umls.zip</tt> file with the tables. If we don't find that we load the sample data files included in the YTEX distribution - this contains a tiny subset of the UMLS for use with the YTEX examples.

The cTAKES Database Lookup algorithm requires a table/view that contains the first word of a concept, a concept code, and the full tokenized text of the concept.  The <tt>MRCONSO</tt> table contains concept codes (CUI field) and the text of the concept.  We generate a table <tt>umls_aui_fword</tt> that contains the first word and tokenized text of every concept in the <tt>MRCONSO</tt> table.  We then join the <tt>umls_aui_fword</tt> and <tt>MRCONSO</tt> tables, to create a table that contains a subset of the UMLS (by default, the SNOMED-CT and RXNORM vocabularies).  You are free to replace this with subsets of the UMLS of your liking (see above).

# Differences between cTAKES/YTEX #
You are free to use whatever dictionary you like with cTAKES/YTEX.  'By default' cTAKES is configured to use a database table for SNOMED-CT, and a lucene index for RXNORM.  In addition, cTAKES stores the SNOMED Codes for concepts.

In contrast, YTEX uses only a database table for lookup, and only stores the UMLS CUIs.  The reasons for this include:
  * Performance
> Using the RXNORM lucene index incurs some memory requirements (you need a 1gb heap at least to load the lucene index).  For each UMLS concept it finds, cTAKES executes another query to find the corresponding SNOMED codes.  Therefore, for each word that cTAKES tries to map, cTAKES is running 2 queries (snomed + rxnorm lookups) + n queries (cui to snomed lookup).  YTEX in contrast looks in a single database table.  Thus, the dictionary lookup in YTEX runs just 1 query (umls lookup).
  * Cleaner Data Model
> cTAKES store the SNOMED-CT code and the UMLS CUI.  YTEX in contrast stores only the UMLS CUI: YTEX is DB-oriented, and storing only the UMLS CUI corresponds to a normalized data model.  To map UMLS CUIs to codes from any source vocabulary (e.g. SNOMED-CT/RXNORM), you can simply join the <tt>ytex.anno_ontology_concept</tt> and <tt>umls.mrconso</tt> tables.

If you prefer the way cTAKES is doing things, you can configure YTEX to get the same functionality.  Refer to [the cTAKES documentation](http://ohnlp.sourceforge.net/cTAKES/#boost_performance) for information on setting up the tables / lucene indices and configuring the Dictionary Lookup Annotator.