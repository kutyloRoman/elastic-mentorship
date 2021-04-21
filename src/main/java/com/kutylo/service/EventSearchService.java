package com.kutylo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kutylo.config.ConnectionConfig;
import com.kutylo.exceptions.EventNotFoundException;
import com.kutylo.model.Event;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.core.CountResponse;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.PutMappingRequest;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class EventSearchService {

  private RestHighLevelClient restHighLevelClient;
  private ObjectMapper objectMapper = new ObjectMapper();

  private static String INDEX = "events";

  private static final Logger log = LogManager.getLogger(EventSearchService.class.getName());

  public EventSearchService() {
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
    objectMapper.registerModule(new JavaTimeModule());
    this.restHighLevelClient = ConnectionConfig.getConnection();
  }

  public void createIndex(String index) {
    log.info("Create index: {}", index);
    CreateIndexRequest request = new CreateIndexRequest(index);
    try {
      CreateIndexResponse createIndexResponse =
          restHighLevelClient.indices().create(request, RequestOptions.DEFAULT);
      if (createIndexResponse.isAcknowledged()) {
        log.info("Index create successfully {}", index);
      } else {
        log.info("Index wan`t created {}", index);
      }
    } catch (IOException e) {
      log.info("Error: {}", e.getMessage());
    }
  }

  public boolean checkIfIndexExist(String index) throws IOException {
    log.info("Check if index exist: {}", index);
    GetIndexRequest request = new GetIndexRequest(index);
    try {
      boolean indexExist = restHighLevelClient.indices().exists(request, RequestOptions.DEFAULT);
      log.info("{} exist: {}", index, indexExist);
      return indexExist;
    } catch (IOException e) {
      log.info("Error: {}", e.getMessage());
      return false;
    }
  }

  public void updateIndexMapping(String index, XContentBuilder mapping) {
    log.info("Update index: {} mapping: {}", index, mapping);
    PutMappingRequest request = new PutMappingRequest("index");
    request.source(mapping);
    try {
      AcknowledgedResponse putMappingResponse =
          restHighLevelClient.indices().putMapping(request, RequestOptions.DEFAULT);
      log.info(
          "Index: {} mapping: {} updated: {}", index, mapping, putMappingResponse.isAcknowledged());
    } catch (IOException e) {
      log.info("Error: {}", e.getMessage());
    }
  }

  public Event getEventById(String id) {
    log.info("Get event by id:{}", id);
    GetRequest getRequest = new GetRequest(INDEX, id);
    try {
      GetResponse getResponse = restHighLevelClient.get(getRequest, RequestOptions.DEFAULT);

      if (getResponse.isExists()) {
        String event = getResponse.getSourceAsString();
        Map<String, Object> eventAsMap = getResponse.getSourceAsMap();
        return objectMapper.readValue(event, Event.class);
      } else {
        throw new EventNotFoundException("Not found event with id:" + id);
      }
    } catch (IOException | EventNotFoundException e) {
      log.info("Error: {}", e.getMessage());
      return null;
    }
  }

  public List<Event> getAllEvents() throws IOException {
    log.info("Get all events");
    SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
    sourceBuilder.sort(new FieldSortBuilder("id").order(SortOrder.DESC));
    SearchRequest searchRequest = new SearchRequest(INDEX);
    searchRequest.source(sourceBuilder);
    try {
      SearchResponse searchResponse =
          restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
      return mapSearchResponseToObjects(searchResponse);
    } catch (ElasticsearchException e) {
      e.getDetailedMessage();
      log.info("Error:{}", e.getDetailedMessage());
      return Collections.emptyList();
    }
  }

  public Event insertEvent(Event event) throws JsonProcessingException {
    log.info("Insert event:{}", event);
    String eventJson = objectMapper.writeValueAsString(event);

    IndexRequest indexRequest = new IndexRequest(INDEX);
    indexRequest.id(event.getId());
    indexRequest.source(eventJson, XContentType.JSON);

    try {
      IndexResponse response = restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
      log.info("Index {}", response.getResult());
      return getEventById(event.getId());
    } catch (ElasticsearchException | IOException e) {
      log.info("Index {}", e.getMessage());
    }
    return event;
  }

  public List<Event> insertEvents(List<Event> events) throws IOException {
    log.info("Insert events:{}", events);

    BulkRequest bulkRequest = new BulkRequest();

    events.forEach(
        event -> {
          try {
            bulkRequest.add(
                new IndexRequest(INDEX)
                    .id(event.getId())
                    .source(objectMapper.writeValueAsString(event), XContentType.JSON));
          } catch (JsonProcessingException e) {
            e.printStackTrace();
          }
        });

    BulkResponse bulkResponse = restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);

    return processMultipleBulkQuery(bulkResponse);
  }

  private List<Event> processMultipleBulkQuery(BulkResponse bulkResponse) {
    List<Event> events = new ArrayList<>();
    for (BulkItemResponse bulkItemResponse : bulkResponse) {
      DocWriteResponse itemResponse = bulkItemResponse.getResponse();
      log.info("Event:{}", itemResponse.getResult());
      switch (bulkItemResponse.getOpType()) {
        case INDEX:
        case CREATE:
          IndexResponse indexResponse = (IndexResponse) itemResponse;
          events.add(getEventById(indexResponse.getId()));
          break;
        case UPDATE:
          UpdateResponse updateResponse = (UpdateResponse) itemResponse;
          events.add(getEventById(updateResponse.getId()));
          break;
      }
    }
    return events;
  }

  public void deleteEvent(String id) {
    log.info("Delete event with id:{}", id);
    DeleteRequest deleteRequest = new DeleteRequest("events", id);
    try {
      DeleteResponse deleteResponse =
          restHighLevelClient.delete(deleteRequest, RequestOptions.DEFAULT);
      log.info("Delete event with id:{} : {}", id, deleteResponse.getResult());
    } catch (IOException e) {
      log.info("Error: {}", e.getMessage());
    }
  }

  public void deleteEventByTerm(String field, String value) {
    log.info("Delete event with by term:{},{}", field, value);
    DeleteByQueryRequest deleteRequest = new DeleteByQueryRequest("events");
    deleteRequest.setQuery(QueryBuilders.matchQuery(field, value));
    try {
      BulkByScrollResponse bulkResponse =
          restHighLevelClient.deleteByQuery(deleteRequest, RequestOptions.DEFAULT);
      log.info("Delete event by term: {}", bulkResponse.getStatus());
    } catch (IOException e) {
      log.info("Error: {}", e.getMessage());
    }
  }

  public Event updateEvent(String id, Event event) throws JsonProcessingException {
    log.info("Update event with id: {}", id);
    UpdateRequest updateRequest = new UpdateRequest(INDEX, id);
    String eventJson = objectMapper.writeValueAsString(event);
    updateRequest.doc(eventJson, XContentType.JSON);

    try {
      UpdateResponse response = restHighLevelClient.update(updateRequest, RequestOptions.DEFAULT);
      log.info("Index {}", response.getResult());
      return getEventById(event.getId());
    } catch (IOException | ElasticsearchException e) {
      log.info("Error: {}", e.getMessage());
    }
    return getEventById(id);
  }

  public List<Event> getEventByTerm(String field, String value) throws IOException {
    log.info("Get events by term:{}, {}", field, value);
    SearchRequest searchRequest = new SearchRequest(INDEX);
    SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
    sourceBuilder.query(QueryBuilders.matchQuery(field, value));
    searchRequest.source(sourceBuilder);

    try {
      SearchResponse searchResponse =
          restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
      return mapSearchResponseToObjects(searchResponse);
    } catch (IOException | ElasticsearchException e) {
      log.info("Error: {}", e.getMessage());
      return Collections.emptyList();
    }
  }

  public long countEventsByTerm(String field, String value) {
    log.info("Count events by term {}, {}", field, value);
    CountRequest countRequest = new CountRequest();
    countRequest.query(QueryBuilders.matchQuery(field, value));
    try {
      CountResponse countResponse = restHighLevelClient.count(countRequest, RequestOptions.DEFAULT);
      long count = countResponse.getCount();
      log.info("Counting is {}, amount: {}", countResponse.status(), count);
      return count;
    } catch (IOException e) {
      log.error(e.getMessage());
      return 0;
    }
  }

  private List<Event> mapSearchResponseToObjects(SearchResponse searchResponse)
      throws JsonProcessingException {
    List<Event> events = new ArrayList<>();
    for (SearchHit hit : searchResponse.getHits().getHits()) {
      Event event = objectMapper.readValue(hit.getSourceAsString(), Event.class);
      events.add(event);
    }

    return events;
  }
}
