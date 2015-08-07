# Introduction #

This page describes the cTAKES dictionary lookup algorithm and its configuration in YTEX.  The dictionary lookup is very simple; this simplicity is both a blessing and a curse.  It's simplicity makes it very easy to configure and troubleshoot; however this also limits its functionality.

The cTAKES dictionary lookup algorithm relies on a table that contains the first word of a term, the full text of a term, and the term's identifier.  The dictionary lookup takes text in a lookup window; for each word in the lookup window it searches the dictionary for a match to first word column.  For each hit, it tries to match subsequent words from the lookup window to subsequent words in the term.  For example, if the lookup window contains the text 'congestive heart failure', cTAKES will search for 'congestive', 'heart', and 'failure', and try to match all sequences of words to terms in the dictionary.  Depending on your dictionary, this will result in separate annotations for 'heart', 'heart failure', and 'congestive heart failure'.

One potential drawback to the cTAKES dictionary lookup algorithm is that it requires an exact match to the text in the dictionary.  For example, the cTAKES DictionaryLookupAlgorithm will not match the text 'Transplant organ' to the concept 'Transplanted organ' (cui C0524930).  With YTEX v0.5 and onwards, we implemented an additional dictionary lookup algorithm that matches uninflected (stemmed) text to UMLS Concepts.

# Configuration #

## Lookup Window ##
The Dictionary Lookup algorithm searches text that lies within a configured annotation type.  By default, YTEX is configured to use the sentence as a lookup window.  cTAKES also has a LookupWindowAnnotation; you can configure the dictionary lookup to look in LookupWindowAnnotation to increase precision at the expense of recall.  This is configured in `YTEX_HOME/config/desc/ytex/uima/annotators/LookupDesc_SNOMED.xml`

## Default YTEX Dictionary Lookup Table ##
During installation YTEX creates a table `umls_aui_fword` with the first word of every term from the UMLS `MRCONSO` table, the tokenized text of the term, and the stemmed text of the term.  It creates a table `v_snomed_fword_lookup` that joins the `umls_aui_fword` and `MRCONSO` tables.  The SQL for this table is:
```
select mrc.cui, c.fword, c.fstem, c.tok_str, c.stem_str
from umls_aui_fword c
inner join umls.MRCONSO mrc on c.aui = mrc.aui
where mrc.SAB in ( 'SNOMEDCT','RXNORM' )
and exists
(
	select *
	from umls.MRSTY sty
	where mrc.cui = sty.cui
	and sty.tui in
	(
	'T017' /* Anatomical Structure */,
	'T021','T022','T023','T024','T025','T026','T029','T030','T031',
	'T059','T060','T061',
	'T019','T020','T037','T046','T047','T048','T049','T050','T190','T191',
	'T033','T034','T040','T041','T042','T043','T044','T045','T046','T056','T057','T184','T121'
	)
)
```

This tableincludes only the SNOMED and RXNORM vocabularies, and limits the semantic types (T0XX...).  You are free to modify this table to include other UMLS source vocabularies, and other semantic types.

## Using your own dictionary ##
You can create your own dictionary: you just need to create a table with the 3 fields (first word, tokenized concept text, and concept id) and point the dictionary lookup annotator at it.  The table that is queried is configured in `YTEX_HOME/config/desc/ytex/uima/annotators/LookupDesc_SNOMED.xml`.

Care must be taken with concepts that include punctuation characters: the tokens that comprise the concept must be separated according to the cTAKES tokenizer.  This splits words on all punctuation characters, with the exception of hyphens and apostrophes.  In a future release, we will provide a utility to generate dictionaries from user-defined concepts.

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