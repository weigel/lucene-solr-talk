package de.fhopf.lucenesolrtalk.web;

import com.google.common.base.Optional;
import com.yammer.metrics.annotation.Timed;
import de.fhopf.Result;
import de.fhopf.lucene.Searcher;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.store.FSDirectory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Path("/lucene")
@Produces(MediaType.TEXT_HTML)
public class LuceneSearchResource {

    private final Searcher searcher;

    public LuceneSearchResource(String indexDir) {
        try {
            this.searcher = new Searcher(FSDirectory.open(new File(indexDir)));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @GET
    @Timed
    public SearchResultView search(@QueryParam("query")Optional<String> query) throws ParseException {
        List<Result> results = Collections.emptyList();
        List<String> categories = searcher.getAllCategories();
        if (query.isPresent()) {
            results = searcher.search(query.get());
        }

        return new SearchResultView(query.or("-"), results, categories);
    }
}