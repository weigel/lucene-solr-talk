package de.fhopf.lucene;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.analysis.de.GermanLightStemFilter;
import org.apache.lucene.analysis.de.GermanNormalizationFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.*;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class AnalyzerTest {

    private String text = "Die Stadt liegt in den Bergen. Vom Berg kann man die Stadt sehen.";

    private static final Analyzer ONLY_TOKENIZED = new Analyzer() {
        @Override
        public TokenStream tokenStream(String fieldName, Reader reader) {
            return new StandardTokenizer(Version.LUCENE_36, reader);
        }
    };

    private static final Analyzer TOKENIZED_AND_LOWERCASED = new ReusableAnalyzerBase() {
        @Override
        protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
            Tokenizer source = new StandardTokenizer(Version.LUCENE_36, reader);
            return new TokenStreamComponents(source, new LowerCaseFilter(Version.LUCENE_36, source));
        }
    };

    private static final Analyzer TOKENIZED_AND_LOWERCASED_AND_STEMMED = new ReusableAnalyzerBase() {
        @Override
        protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
            Tokenizer source = new StandardTokenizer(Version.LUCENE_36, reader);
            TokenStream result = new LowerCaseFilter(Version.LUCENE_36, source);
            result = new GermanNormalizationFilter(result);
            result = new GermanLightStemFilter(result);
            return new TokenStreamComponents(source, result);
        }
    };

    @Test
    public void tokenize() throws IOException {
        assertAnalyzed(text, ONLY_TOKENIZED, "Die", "Stadt", "liegt", "in", "den", "Bergen", "Vom", "Berg", "kann", "man", "die", "sehen");
    }

    @Test
    public void tokenizeAndLowercase() throws IOException {
        assertAnalyzed(text, TOKENIZED_AND_LOWERCASED, "die", "stadt", "liegt", "in", "den", "bergen", "vom", "berg", "kann", "man", "sehen");
    }

    @Test
    public void tokenizedAndLowercasedAndStemmed() throws IOException {
        assertAnalyzed(text, TOKENIZED_AND_LOWERCASED_AND_STEMMED, "die", "stadt", "liegt", "in", "den", "berg", "vom", "kann", "man", "seh");
    }

    @Test
    public void indexExampleTalks() throws IOException, ParseException {
        Document luceneSolr = new Document();
        luceneSolr.add(new Field("title", "Integration ganz einfach mit Apache Camel", Field.Store.YES, Field.Index.ANALYZED));
        luceneSolr.add(new Field("date", "20120404", Field.Store.NO, Field.Index.ANALYZED));
        luceneSolr.add(new Field("speaker", "Christian Schneider", Field.Store.YES, Field.Index.ANALYZED));

        Document karaf = new Document();
        karaf.add(new Field("title", "Apache Karaf", Field.Store.YES, Field.Index.ANALYZED));
        karaf.add(new Field("date", "20120424", Field.Store.NO, Field.Index.ANALYZED));
        karaf.add(new Field("speaker", "Christian Schneider", Field.Store.YES, Field.Index.ANALYZED));
        karaf.add(new Field("speaker", "Achim Nierbeck", Field.Store.YES, Field.Index.ANALYZED));

        Directory dir = FSDirectory.open(new File("/tmp/testindex"));
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_36, new GermanAnalyzer(Version.LUCENE_36));
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        IndexWriter writer = new IndexWriter(dir, config);

        writer.addDocument(luceneSolr);
        writer.addDocument(karaf);

        writer.commit();
        writer.close();

        IndexReader reader = IndexReader.open(dir);
        IndexSearcher searcher = new IndexSearcher(reader);
        QueryParser parser = new QueryParser(Version.LUCENE_36, "title", new GermanAnalyzer(Version.LUCENE_36));
        Query query = parser.parse("apache");

        TopDocs result = searcher.search(query, 10);
        assertEquals(2, result.totalHits);

        for(ScoreDoc scoreDoc: result.scoreDocs) {
            Document doc = searcher.doc(scoreDoc.doc);
            String title = doc.get("title");
            assertTrue(title.equals("Apache Karaf") || title.equals("Integration ganz einfach mit Apache Camel"));
        }


    }

    private void assertAnalyzed(String text, Analyzer analyzer, String ... expectedTokens) throws IOException {
        Directory dir = new RAMDirectory();
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_36, analyzer);
        IndexWriter writer = new IndexWriter(dir, config);

        Document doc = new Document();
        doc.add(new Field("name", text, Field.Store.NO, Field.Index.ANALYZED));

        writer.addDocument(doc);

        writer.close();

        IndexReader indexReader = IndexReader.open(dir);

        TermEnum terms = indexReader.terms();

        int count = 0;
        List<String> expectedTokenList = Arrays.asList(expectedTokens);

        while (terms.next()) {
            count++;
            Term term = terms.term();
            assertTrue(term.text(), expectedTokenList.contains(term.text()));
        }
        assertEquals(expectedTokens.length, count);
    }

    private List<String> getTokens(TokenStream tokenStream) throws IOException {
        CharTermAttribute termAttribute = tokenStream.addAttribute(CharTermAttribute.class);

        List<String> tokens = new ArrayList<String>();

        while (tokenStream.incrementToken()) {
            tokens.add(termAttribute.toString());
        }
        return tokens;
    }

    private String toString(TokenStream tokenStream) throws IOException {
        CharTermAttribute termAttribute = tokenStream.addAttribute(CharTermAttribute.class);

        StringBuilder result = new StringBuilder();

        while (tokenStream.incrementToken()) {
            result.append(termAttribute.toString());
            result.append(" ");
        }
        return result.toString();
    }

}
