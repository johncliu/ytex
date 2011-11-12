delete from tfidf_doclength where name = 'i2b2.2008-cui';
delete from tfidf_docfreq where name = 'i2b2.2008-cui';
delete from tfidf_termfreq where name = 'i2b2.2008-cui';

insert into tfidf_doclength (name, instance_id, length)
select 'i2b2.2008-cui', s.*
from
(
	select d.uid, count(*)
	from ytex.document d
	inner join ytex.anno_base an on an.document_id = d.document_id
	inner join ytex.anno_ontology_concept c on c.anno_base_id = an.anno_base_id
	where analysis_batch = 'i2b2.2008'
	group by d.uid
) s;


insert into tfidf_docfreq (name, term, numdocs)
select 'i2b2.2008-cui', s.*
from
(
	select code, count(*)
	from
	(
	  	select distinct c.code, d.uid
		from ytex.document d
		inner join ytex.anno_base an on an.document_id = d.document_id
		inner join ytex.anno_ontology_concept c on c.anno_base_id = an.anno_base_id
		where analysis_batch = 'i2b2.2008'
		group by c.code, d.uid
	) s
	group by code
) s
;


insert into tfidf_termfreq (name, instance_id, term, freq)
select 'i2b2.2008-cui', s.*
from
(
	select d.uid, c.code, count(*)
	from ytex.document d
	inner join ytex.anno_base an on an.document_id = d.document_id
	inner join ytex.anno_ontology_concept c on c.anno_base_id = an.anno_base_id
	where analysis_batch = 'i2b2.2008'
	group by d.uid, c.code
) s
;
