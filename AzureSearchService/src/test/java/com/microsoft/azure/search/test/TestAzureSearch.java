package com.microsoft.azure.search.test;

import com.microsoft.azure.search.samples.IndexBatchOperation;
import com.microsoft.azure.search.samples.IndexBatchOperationResult;
import com.microsoft.azure.search.samples.IndexBatchResult;
import com.microsoft.azure.search.samples.IndexDefinition;
import com.microsoft.azure.search.samples.IndexField;
import com.microsoft.azure.search.samples.IndexSearchOptions;
import com.microsoft.azure.search.samples.IndexSearchResult;
import com.microsoft.azure.search.samples.IndexSuggestOptions;
import com.microsoft.azure.search.samples.IndexSuggestResult;
import com.microsoft.azure.search.samples.SearchIndexClient;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.testng.Assert;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

/**
 *
 * @author chris vugrinec (chvugrin@microsoft.com)
 */
public class TestAzureSearch {

    private static final String SERVICE_NAME = "bplayer";
    private static final String INDEX_NAME = "sample";
    private static final String API_KEY = "YOUR API KEY";
    private static final Logger logger = Logger.getLogger("AzureSearchTest");

    SearchIndexClient indexClient = null;

    @BeforeSuite
    private void init() throws IOException, InterruptedException{
        indexClient = new SearchIndexClient(SERVICE_NAME, INDEX_NAME, API_KEY);
        createIndex(indexClient, true);
        indexData(indexClient);
        Thread.sleep(1000); //
    }

    private static void createIndex(SearchIndexClient indexClient, boolean deleteFirst) throws IOException {
        // Typical application initialization may create an index if it doesn't exist. Deleting an index
        // on initialization is a sample-only thing to do
        if (deleteFirst) {
            indexClient.delete();
        }

        // Indexes may be created via the management UI in portal.azure.com or via APIs. In addition to field
        // details index definitions include options for custom scoring, suggesters and more
        if (!indexClient.exists()) {
            IndexDefinition definition = new IndexDefinition();

            Collection<IndexField> fields = definition.getFields();
            fields.add(new IndexField("id", "Edm.String").setKey(true));
            fields.add(new IndexField("name", "Edm.String").setSearchable(true).setAnalyzer("en.lucene"));
            fields.add(new IndexField("category", "Collection(Edm.String)").setFilterable(true).setFacetable(true));
            fields.add(new IndexField("rating", "Edm.Int32").setFilterable(true).setFacetable(true));
            fields.add(new IndexField("created", "Edm.DateTimeOffset").setFilterable(true).setSortable(true).setFacetable(true));

            IndexDefinition.Suggester suggester = new IndexDefinition.Suggester();
            suggester.setName("sg");
            suggester.setSourceFields(new String[]{"name"});
            definition.getSuggesters().add(suggester);

            indexClient.create(definition);
        }
    }

    private static void indexData(SearchIndexClient indexClient) throws IOException {
        // In this case we create sample data in-memory. Typically this will come from another database, file or
        // API and will be turned into objects with the desired shape for indexing
        ArrayList<IndexBatchOperation> operations = new ArrayList<>();
        operations.add(IndexBatchOperation.upload(newDocument("1", "first name", 10, "aaa", "bbb")));
        operations.add(IndexBatchOperation.upload(newDocument("2", "second name", 11, "aaa", "ccc")));
        operations.add(IndexBatchOperation.upload(newDocument("3", "second second name", 12, "aaa", "eee")));
        operations.add(IndexBatchOperation.upload(newDocument("4", "third name", 13, "ddd", "eee")));
        operations.add(IndexBatchOperation.delete("id", "5"));

        // consider handling HttpRetryException and backoff (wait 30, 60, 90 seconds) and retry
        IndexBatchResult result = indexClient.indexBatch(operations);
        if (result.getHttpStatus() == 207) {
            // handle partial success, check individual operation status/error message
        }
        result.getOperationResults().forEach((r) -> {
            logger.log(Level.INFO, "Operation for id: {0}, success: {1}  ",new Object[]{r.getKey(), r.getStatus()});
        });
    }

    private static Map<String, Object> newDocument(String id, String name, int rating, String ... categories) {
        HashMap<String, Object> doc;
        doc = new HashMap<>();
        doc.put("id", id);
        doc.put("name", name);
        doc.put("rating", rating);
        doc.put("category", (String[])categories);
        doc.put("created", new Date());
        return doc;
    }

    
    @Test()
    public void testSearchSimple() throws IOException {
        logger.log(Level.INFO, "\n\ntestSearchSimple\n\n");

        IndexSearchOptions options = new IndexSearchOptions();
        options.setIncludeCount(true);
        IndexSearchResult result = indexClient.search("name", options);
        logger.log(Level.INFO, "Found {0} hits",result.getCount());
        result.getHits().forEach((hit) -> {
            logger.log(Level.INFO, "Id: {0} name {1}, score {2}",new Object[]{hit.getDocument().get("id"), hit.getDocument().get("name"), hit.getScore()});
        });
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getCount(), 4);

    }
  
    @Test()
    public void testSearchAllFeatures() throws IOException {
        logger.log(Level.INFO, "\n\ntestSearchAllFeatures\n\n");

        IndexSearchOptions options = new IndexSearchOptions();
        options.setIncludeCount(true);
        options.setFilter("rating lt 13 and category/any(c: c eq 'aaa')");
        options.setOrderby("created");
        options.setSelect("id,name,category,created");
        options.setSearchFields("name");
        options.setFacets(new String[] { "rating,values:11|13", "category", "created" });
        options.setHighlight("name");
        options.setHighlightPreTag("*pre*");
        options.setHighlightPostTag("*post*");
        options.setTop(10);
        options.setSkip(1);
        options.setRequireAllTerms(true);
        options.setMinimumCoverage(0.75);

        Assert.assertNotNull(indexClient);
        IndexSearchResult result = indexClient.search("second name", options);
        Assert.assertNotNull(result);
        
        
        // list search hits
        logger.log(Level.INFO, "Found {0} hits, coverage: {1}",new Object[]{result.getCount(), result.getCoverage() == null ? "-" : result.getCoverage()});
        result.getHits().forEach((hit) -> {
            logger.log(Level.INFO, "id {0}, name: {1}, score: {2} ",new Object[]{hit.getDocument().get("id"), hit.getDocument().get("name"), hit.getScore()});
        });

        // list facets
        
        logger.log(Level.INFO, "List Facets");
        result.getFacets().keySet().stream().map((field) -> {
            logger.log(Level.INFO, "Field : ");
            return field;
        }).forEachOrdered((field) -> {
            for (IndexSearchResult.FacetValue value: result.getFacets().get(field)) {
                if (value.getValue() != null) {
                    logger.log(Level.INFO, "{0}: {1} ", new Object[]{value.getValue(), value.getCount()});
                }
                else {
                    logger.log(Level.INFO, "{0}-{1}: {2} ", new Object[]{
                        value.getFrom() == null ? "min" : value.getFrom(),
                        value.getTo() == null ? "max" : value.getTo(),
                        value.getCount()});
                }
            }
        });
        
        //  TODO:   Add testCases
    }

    @Test()
    public void testLookup() throws IOException, URISyntaxException {
        logger.log(Level.INFO, "\n\ntestLookup\n\n");

        Map<String, Object> document = indexClient.lookup("2");
        logger.log(Level.INFO, "Document lookup, key='2'");
        logger.log(Level.INFO, "Name: {0}",document.get("name"));
        logger.log(Level.INFO, "Created: {0}",document.get("created"));
        logger.log(Level.INFO, "Rating: {0}",document.get("rating"));

        //  TODO:   Add testCases
    }

    @Test()
    public void testSuggestions() throws IOException {
        logger.log(Level.INFO, "\n\ntestSuggestions\n\n");

        IndexSuggestOptions options = new IndexSuggestOptions();
        options.setFuzzy(true);
        IndexSuggestResult result = indexClient.suggest("secp", "sg", options);
        logger.log(Level.INFO, "Suggest results, coverage: {0}",(result.getCoverage() == null ? "-" : result.getCoverage().toString()));
        result.getHits().forEach((hit) -> {
            logger.log(Level.INFO, "Text: {0} (id: {1}",new Object[]{hit.getText(), hit.getDocument().get("id")});
        }); 

        //  TODO:   Add testCases

    }
    
    
    
    
}
