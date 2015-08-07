# Introduction #

This page describes the cTAKES dictionary lookup algorithm and its configuration in YTEX.  The dictionary lookup is very simple; this simplicity is both a blessing and a curse.  It's simplicity makes it very easy to configure and troubleshoot; however this also limits its functionality.

The cTAKES dictionary lookup algorithm relies on a table that contains the first word of a term, the full text of a term, and the term's identifier.  The dictionary lookup takes text in a lookup window; for each word in the lookup window it searches the dictionary for a match to first word column.  For each hit, it tries to match subsequent words from the lookup window to subsequent words in the term.  For example, if the lookup window contains the text 'congestive heart failure', cTAKES will search for 'congestive', 'heart', and 'failure', and try to match all sequences of words to terms in the dictionary.  Depending on your dictionary, this will result in separate annotations for 'heart', 'heart failure', and 'congestive heart failure'.

# Configuration #

## Lookup Window ##
The Dictionary Lookup algorithm searches text that lies within a configured annotation type.  By default, YTEX is configured to use the sentence as a lookup window.  cTAKES also has a LookupWindowAnnotation; you can configure the dictionary lookup to look in LookupWindowAnnotation to increase precision at the expense of recall.  This is configured in `YTEX_HOME/config/desc/ytex/uima/annotators/LookupDesc_SNOMED.xml`

## Default YTEX Dictionary Lookup Table ##
During installation YTEX creates a table `umls_aui_fword` with the first word of every term from the UMLS `MRCONSO` table.  It creates a view `v_snomed_fword_lookup` (in mysql a table) that joins the `umls_aui_fword` and `MRCONSO` tables.  The SQL for this view is:
```
select c.fword, mrc.cui, mrc.str text
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
	'T021','T022','T023','T024','T025','T026','T029','T030','T031',
	'T059','T060','T061',
	'T019','T020','T037','T046','T047','T048','T049','T050','T190','T191',
	'T033','T034','T040','T041','T042','T043','T044','T045','T046','T056','T057','T184'
	)
)
```

This view includes only the SNOMED and RXNORM vocabularies, and limits the semantic types (T0XX...).  You are free to modify this view to include other UMLS source vocabularies, and other semantic types.

## Using your own dictionary ##
You can create your own dictionary: you just need to create a table with the 3 fields (first word, concept text, and concept id) and point the dictionary lookup annotator at it.  The table/view that is queried is configured in `YTEX_HOME/config/desc/ytex/uima/annotators/LookupDesc_SNOMED.xml`

# Debugging Dictionary Lookup #
In case the concepts you expect aren't being annotated, perform the following steps.  For this example, we'll assume that the term 'Taxol' is not being annotated.

  * Check tokenization
If the text contains a punctuation symbol (e.g. - or '), then named entity recognition may fail in v0.4.  cTAKES' dictionary lookup algorithm requires a space-delimited tokenized string for matching.  cTAKES splits some hyphenated words into different tokens, and splits most other punctuation marks into different tokens.  We have resolved this in version 0.5.

  * Check MRCONSO:
The simplest explanation would be that the term you are using is missing from the MRCONSO table; check this with the following query:
`select * from umls.MRCONSO where str = 'Taxol'`

  * Check umls\_aui\_fword:
YTEX sets up a table 'umls\_aui\_fword' that has the first word of every term from MRCONSO; there might have been a problem setting this table up.  To check this table run this following query:
`select * from umls_aui_fword where fword = 'taxol'`

  * Check v\_snomed\_fword\_lookup:
By default the DictionaryLookupAnnotator is configured to use a view called 'v\_snomed\_fword\_lookup' (in mysql this is a table).  This view joins the umls\_aui\_fword and MRCONSO tables.  This view might exclude the vocabulary or semantic type that Taxol belongs to.  To check this, simply try:
`select * from v_snomed_fword_lookup where fword = 'taxol'`

  * Check Lookup Window
If you are sure that the word is in your dictionary (i.e. in `v_snomed_fword_lookup`), perform this step.  The cTAKES dictionary lookup algorithm tries to match word(s) that lie within a configured Lookup Window annotation.  By default, YTEX uses the Sentence as a lookup Window.  There might be a problem in sentence splitting that causes the word to fall outside a sentence.  Open the document that contains taxol in the DBAnnotationViewer and verify that it is in a sentence.