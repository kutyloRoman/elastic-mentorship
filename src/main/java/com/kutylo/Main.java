package com.kutylo;

import com.kutylo.config.ConnectionConfig;
import com.kutylo.model.Event;
import com.kutylo.model.EventType;
import com.kutylo.service.EventSearchService;

import java.io.IOException;
import java.util.List;

public class Main {
  public static void main(String[] args) throws IOException {
    EventSearchService eventSearchService = new EventSearchService();
    Event event =
        Event.newBuilder()
            .setId("1")
            .setTitle("Spring tutorial")
            .setEventType(EventType.WORKSHOP)
            .setPlace("Zoom")
            .setDescription("Spring boot tutorial")
            .setSubTopics(List.of("Java", "Spring"))
            .build();
    Event event2 =
        Event.newBuilder()
            .setId("2")
            .setTitle("Ruby tutorial")
            .setEventType(EventType.TECH_TALK)
            .setPlace("Lviv")
            .setDescription("Ruby tutorial")
            .setSubTopics(List.of("Ruby", "Js"))
            .build();
    Event event3 =
        Event.newBuilder()
            .setId("3")
            .setTitle("React tutorial")
            .setEventType(EventType.WORKSHOP)
            .setPlace("Teams")
            .setDescription("React tutorial")
            .setSubTopics(List.of("React", "Web"))
            .build();
    Event event4 =
        Event.newBuilder()
            .setId("4")
            .setTitle("React Native tutorial")
            .setEventType(EventType.WORKSHOP)
            .setPlace("Teams")
            .setDescription("React tutorial")
            .setSubTopics(List.of("React", "Web"))
            .build();

    eventSearchService.createIndex("test-events");
    System.out.println(eventSearchService.checkIfIndexExist("test-events"));
    eventSearchService.insertEvents(List.of(event, event2, event3));
    eventSearchService.insertEvent(event4);
    System.out.println(eventSearchService.getAllEvents());
    System.out.println(eventSearchService.getEventById("2"));
    eventSearchService.updateEvent("2", event2);
    System.out.println(eventSearchService.getEventById("2"));
    eventSearchService.deleteEvent("1");
    eventSearchService.deleteEventByTerm("place", "Zoom");
    eventSearchService.countEventsByTerm("title", "React Native tutorial");
    ConnectionConfig.closeConnection();
  }
}
