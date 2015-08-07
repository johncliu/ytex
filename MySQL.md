# Introduction #

View performance in MySQL is [very poor](http://www.mysqlperformanceblog.com/2007/08/12/mysql-view-as-performance-troublemaker/).  However, by default, the Named Entity Recognition annotator (DBLookup) is configured to use views.

If you plan on using MySQL with YTEX, you should replace the view with a table.  This has been addressed in later versions of YTEX (v0.4 and above).

# Creating the lookup table #
YTEX uses the <tt>V_SNOMED_FWORD_LOOKUP</tt> view for named entity recognition.  To replace this with a table, execute the statements below.  Keep in mind that changes to the underlying table <tt>umls_ms_2009</tt> will not be reflected in this new table.

```
drop view if exists v_snomed_fword_lookup;
drop table if exists v_snomed_fword_lookup;

create table v_snomed_fword_lookup (
  fword varchar(100) not null,
  cui varchar(10) not null,
  text text not null
) engine=myisam, comment 'umls lookup table, created from umls_ms_2009' ;

insert into v_snomed_fword_lookup
select fword, cui, text
from umls_ms_2009
where 
(
	tui in 
	(
	'T021','T022','T023','T024','T025','T026','T029','T030','T031',
	'T059','T060','T061',
	'T019','T020','T037','T046','T047','T048','T049','T050','T190','T191',
	'T033','T034','T040','T041','T042','T043','T044','T045','T046','T056','T057','T184'
	)
	and sourcetype = 'SNOMEDCT'
) 
;

create index idx_fword on v_snomed_fword_lookup (fword);
create index idx_cui on v_snomed_fword_lookup (cui);

```