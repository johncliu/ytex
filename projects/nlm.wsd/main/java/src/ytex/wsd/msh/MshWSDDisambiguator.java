package ytex.wsd.msh;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowCallbackHandler;

import ytex.kernel.SimSvcContextHolder;
import ytex.kernel.metric.ConceptSimilarityService;
import ytex.kernel.metric.ConceptSimilarityService.SimilarityMetricEnum;
import ytex.kernel.wsd.WordSenseDisambiguator;

public class MshWSDDisambiguator {
	private static final Log log = LogFactory.getLog(MshWSDDisambiguator.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		String[] metrics = args[0].split(",");
		int windowSize = Integer.parseInt(args[1]);
		String analysisBatch = args.length > 2 ? args[2] : "msh-wsd-ctakes";
		MshWSDDisambiguator wsd = new MshWSDDisambiguator();
		wsd.load();
		SortedSet<SimilarityMetricEnum> metricSet = new TreeSet<SimilarityMetricEnum>();
		for (String metricName : metrics) {
			metricSet.add(SimilarityMetricEnum.valueOf(metricName));
		}
		wsd.disambiguate(metricSet, windowSize, analysisBatch);
	}

	/**
	 * The abstract, represented as a list of named entities, which in turn are
	 * a list of cuis
	 * 
	 * @author vijay
	 * 
	 */
	public static class Sentence {
		long instanceId = -1;
		int index = -1;
		List<Set<String>> concepts = new ArrayList<Set<String>>();

		public Sentence(long instanceId) {
			super();
			this.instanceId = instanceId;
		}

		public int getIndex() {
			return index;
		}

		public long getInstanceId() {
			return instanceId;
		}

		public void setInstanceId(long instanceId) {
			this.instanceId = instanceId;
		}

		public void setIndex(int index) {
			this.index = index;
		}

		public List<Set<String>> getConcepts() {
			return concepts;
		}

		public void setConcepts(List<Set<String>> concepts) {
			this.concepts = concepts;
		}
	}

	/**
	 * the word that maps to multiple cuis that needs to be disambiguated.
	 * 
	 * @author vijay
	 * 
	 */
	public static class Word {
		public long getInstanceId() {
			return instanceId;
		}

		public void setInstanceId(int instanceId) {
			this.instanceId = instanceId;
		}

		public String getWord() {
			return word;
		}

		public void setWord(String word) {
			this.word = word;
		}

		public String getCui() {
			return cui;
		}

		public void setCui(String cui) {
			this.cui = cui;
		}

		public int getSpanBegin() {
			return spanBegin;
		}

		public void setSpanBegin(int spanBegin) {
			this.spanBegin = spanBegin;
		}

		public int getSpanEnd() {
			return spanEnd;
		}

		public void setSpanEnd(int spanEnd) {
			this.spanEnd = spanEnd;
		}

		public Word(long instanceId, int pmid, String word, String cui,
				int spanBegin, int spanEnd) {
			super();
			this.instanceId = instanceId;
			this.pmid = pmid;
			this.word = word;
			this.cui = cui;
			this.spanBegin = spanBegin;
			this.spanEnd = spanEnd;
		}

		public int getPmid() {
			return pmid;
		}

		public void setPmid(int pmid) {
			this.pmid = pmid;
		}

		long instanceId;
		int pmid;
		String word;
		String cui;
		int spanBegin;
		int spanEnd;
	}

	/**
	 * map of word to the cuis for the word
	 */
	Map<String, Set<String>> wordCuis;
	/**
	 * map of abstract id to word
	 */
	Map<Long, Word> words;

	JdbcTemplate jdbcTemplate;
	/**
	 * the abstracts
	 */
	SortedMap<Long, Sentence> sentences;
	WordSenseDisambiguator wordSenseDisambiguator;

	public MshWSDDisambiguator() {
		DataSource ds = (DataSource) SimSvcContextHolder
				.getApplicationContext().getBean("dataSource");
		this.jdbcTemplate = new JdbcTemplate(ds);
		this.wordSenseDisambiguator = SimSvcContextHolder
				.getApplicationContext().getBean(WordSenseDisambiguator.class);
	}

	public Map<String, Set<String>> loadWordCuis() {
		wordCuis = new HashMap<String, Set<String>>();
		jdbcTemplate.query(
				"select distinct word, cui from msh_wsd order by word",
				new RowCallbackHandler() {
					String wordCurrent = null;
					Set<String> cuisCurrent = null;

					@Override
					public void processRow(ResultSet rs) throws SQLException {
						String wordNew = rs.getString(1);
						String cui = rs.getString(2);
						if (wordCurrent == null || !wordCurrent.equals(wordNew)) {
							cuisCurrent = new HashSet<String>();
							wordCurrent = wordNew;
							wordCuis.put(wordCurrent, cuisCurrent);
						}
						cuisCurrent.add(cui);
					}
				});
		return wordCuis;
	}

	public Map<Long, Word> loadWords() {
		words = new HashMap<Long, Word>();
		jdbcTemplate
				.query("select w.instance_id, w.pmid, w.word, w.cui, w.abs_ambiguity_start, w.abs_ambiguity_end from msh_wsd w",
						new RowCallbackHandler() {
							@Override
							public void processRow(ResultSet rs)
									throws SQLException {
								Word w = new Word(rs.getLong(1), rs.getInt(2),
										rs.getString(3), rs.getString(4), rs
												.getInt(5), rs.getInt(6));
								words.put(w.getInstanceId(), w);
							}
						});
		return words;
	}

	private class WSDRowCallbackHandler implements RowCallbackHandler {
		int currentSpanBegin = -1;
		int currentSpanEnd = -1;
		Sentence currentSentence = null;
		Set<String> currentConcepts = null;
		int currentConceptIndex = -1;
		boolean bFirstRow = true;

		public WSDRowCallbackHandler(Set<SimilarityMetricEnum> metrics,
				int windowSize, PrintStream ps) {
			super();
			this.metrics = metrics;
			this.windowSize = windowSize;
			this.ps = ps;
		}

		private Set<ConceptSimilarityService.SimilarityMetricEnum> metrics;
		private int windowSize;
		private PrintStream ps;

		@Override
		public void processRow(ResultSet rs) throws SQLException {
			if(bFirstRow) {
				log.info("disambiguate start: " + (new Date()));
				bFirstRow = false;
			}
			long instanceId = rs.getLong(1);
			int spanBegin = rs.getInt(2);
			int spanEnd = rs.getInt(3);
			String cui = rs.getString(4);
			if (log.isDebugEnabled())
				log.debug("processing instance " + instanceId);
			if (currentSentence == null
					|| currentSentence.getInstanceId() != instanceId) {
				// new word
				reset(instanceId);
			}
			if (currentConcepts == null || currentSpanBegin != spanBegin
					|| currentSpanEnd != spanEnd) {
				// new concept
				resetCurrentConcepts(spanBegin, spanEnd);
			}
			// don't touch the concept that's supposed to
			// be disambiguated
			if (currentConceptIndex != currentSentence.getIndex()) {
				currentConcepts.add(cui);
			}
			// if (rs.isLast())
			// checkSentenceTargetIndex();
		}

		private void resetCurrentConcepts(int spanBegin, int spanEnd) {
			// increment index
			currentConceptIndex++;
			// allocate new set for concepts
			currentConcepts = new HashSet<String>();
			// add the set to the sentence
			if (currentSentence != null) {
				currentSentence.getConcepts().add(currentConcepts);
				Word w = words.get(currentSentence.getInstanceId());
				if (currentSentence.getIndex() < 0 && w.spanBegin < spanBegin) {
					// we didn't have the target concept
					// annotated as a named entity, and we've
					// passed the target concept.
					// insert the target concept into the
					// sentence
					currentSentence.setIndex(currentConceptIndex);
					currentConcepts.addAll(wordCuis.get(w.getWord()));
					// reset again
					resetCurrentConcepts(spanBegin, spanEnd);
				}

				if (w.getSpanBegin() == spanBegin && w.spanEnd == spanEnd) {
					// this concept is the target for
					// disambiguation
					currentSentence.setIndex(currentConceptIndex);
					currentConcepts.addAll(wordCuis.get(w.getWord()));
				}
			}
		}

		private void reset(long instanceId) {
			checkSentenceTargetIndex();
			currentSpanBegin = -1;
			currentSpanEnd = -1;
			currentSentence = new Sentence(instanceId);
			currentConcepts = null;
			currentConceptIndex = -1;
		}

		private void checkSentenceTargetIndex() {
			if (currentSentence != null && currentSentence.getIndex() == -1) {
				Word w = words.get(currentSentence.getInstanceId());
				// we didn't have the target concept
				// annotated as a named entity, and we've
				// come to the end of the sentence.
				// insert the target concept into the
				// sentence.
				// this would be a problem if
				currentSentence.setIndex(currentConceptIndex + 1);
				currentConcepts = new HashSet<String>();
				currentConcepts.addAll(wordCuis.get(w.getWord()));
				currentSentence.getConcepts().add(currentConcepts);
			}
			if (this.currentSentence != null) {
				try {
					disambiguateSentence(metrics, windowSize, currentSentence,
							ps);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	/**
	 * process sentences as we read them from the database - there are 5.2
	 * million cuis and this fills small heaps
	 * 
	 * @param metric
	 * @param windowSize
	 * @param ps
	 */
	public void processSentences(
			final Set<ConceptSimilarityService.SimilarityMetricEnum> metrics,
			final int windowSize, final PrintStream ps, final String analysisBatch) {
		WSDRowCallbackHandler ch = new WSDRowCallbackHandler(metrics,
				windowSize, ps);
		PreparedStatement s = null;
		Connection conn = null;
		ResultSet rs = null;
		try {
			conn = this.jdbcTemplate.getDataSource().getConnection();
			s = conn.prepareStatement(
					"select d.instance_id, b.span_begin, b.span_end, c.code from document d inner join anno_base b on d.document_id = b.document_id inner join anno_ontology_concept c on c.anno_base_id = b.anno_base_id where d.analysis_batch = '"+analysisBatch+"' order by d.instance_id, span_begin, span_end",
					java.sql.ResultSet.TYPE_FORWARD_ONLY,
					java.sql.ResultSet.CONCUR_READ_ONLY);
			s.setFetchSize(Integer.MIN_VALUE);
			rs = s.executeQuery();
			while (rs.next()) {
				ch.processRow(rs);
			}
			ch.checkSentenceTargetIndex();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
				}
			}
			if (s != null) {
				try {
					s.close();
				} catch (SQLException e) {
				}
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
				}
			}
		}
//		jdbcTemplate.query(new PreparedStatementCreator() {
//
//			public PreparedStatement createPreparedStatement(Connection conn)
//					throws SQLException {
//				// do this so that the mysql driver doesn't read the entire
//				// resultset into memory
//				PreparedStatement s = conn.prepareStatement(
//						"select d.uid instance_id, b.span_begin, b.span_end, c.code from document d inner join anno_base b on d.document_id = b.document_id inner join anno_ontology_concept c on c.anno_base_id = b.anno_base_id where d.analysis_batch = 'msh.wsd' order by d.uid, span_begin, span_end",
//						java.sql.ResultSet.TYPE_FORWARD_ONLY,
//						java.sql.ResultSet.CONCUR_READ_ONLY);
//				s.setFetchSize(Integer.MIN_VALUE);
//				return s;
//			}
//		}, ch);
	}

	public void disambiguate(
			Set<ConceptSimilarityService.SimilarityMetricEnum> metrics,
			int windowSize,
			String analysisBatch) throws IOException {
		PrintStream ps = null;
		try {
			ps = new PrintStream(new BufferedOutputStream(new FileOutputStream(
					"msh-wsd.txt")));
			this.processSentences(metrics, windowSize, ps, analysisBatch);
			log.info("disambiguate end: " + (new Date()));
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception e) {
				}
			}
		}
	}

	public void disambiguateSentence(
			Set<ConceptSimilarityService.SimilarityMetricEnum> metrics,
			int windowSize, Sentence s, PrintStream ps) throws IOException {
		long instanceId = s.getInstanceId();
		Word w = words.get(instanceId);
		for (ConceptSimilarityService.SimilarityMetricEnum metric : metrics) {
			ps.print(instanceId);
			ps.print("\t");
			ps.print(metric.toString());
			ps.print("\t");
			ps.print(w.getWord());
			ps.print("\t");
			ps.print(w.getPmid());
			ps.print("\t");
			ps.print(w.getCui());
			ps.print("\t");
			Map<String, Double> scoreMap = new HashMap<String, Double>();
			String cui = this.wordSenseDisambiguator.disambiguate(
					s.getConcepts(), s.getIndex(), null, windowSize, metric,
					scoreMap, true);
			ps.print(cui);
			ps.print("\t");
			ps.println(scoreMap);
		}
	}

	private void load() {
		this.loadWordCuis();
		this.loadWords();
	}
}
