package de.fhopf.lucene;

import de.fhopf.Talk;
import de.fhopf.TalkFromFileTest;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.ParallelReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static junit.framework.Assert.assertEquals;

/**
 */
public class IndexerTest {

    private Directory directory;
    private Indexer indexer;

    @Before
    public void initIndexer() {
        directory = new RAMDirectory();
        indexer = new Indexer(directory);
    }

    @Test
    public void twoTalksAreIndexed() throws IOException {
        Talk talk1 = new Talk("/path/to/talk1", "Title 1", Arrays.asList("Author 1"), new Date(), "Contents", new ArrayList<String>());
        Talk talk2 = new Talk("/path/to/talk2", "Title 2", Arrays.asList("Author 2"), new Date(), "More Contents", new ArrayList<String>());

        indexer.index(talk1, talk2);
        assertDocumentCount(2);
    }

    private void assertDocumentCount(int amount) throws IOException {
        IndexReader reader = IndexReader.open(directory);
        assertEquals(amount, reader.numDocs());
        reader.close();
    }

    @Test
    public void indexOneTalkFromDirectory() throws IOException {
        Path dir = Files.createTempDirectory("indexertest");
        Path file = Files.createTempFile(dir, "post", ".properties");
        List<String> lines = new ArrayList<String>();
        lines.add("speaker=Tester");
        lines.add("title=Test");
        lines.add("content=Content");
        lines.add("date=12.06.2012");
        lines.add("categories=Test1,Test2");
        Files.write(file, lines, Charset.forName("ISO-8859-1"));

        indexer.indexDirectory(dir.toAbsolutePath().toString());

        assertDocumentCount(1);
    }

}
