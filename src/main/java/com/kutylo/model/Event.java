package com.kutylo.model;

import java.util.List;

public class Event {
  private String id;
  private String title;
  private String place;
  private EventType eventType;
  private String description;
  private List<String> subTopics;

  public Event() {}

  public String getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public String getPlace() {
    return place;
  }

  public EventType getEventType() {
    return eventType;
  }

  public String getDescription() {
    return description;
  }

  public List<String> getSubTopics() {
    return subTopics;
  }

  @Override
  public String toString() {
    return "Event{"
        + "id='"
        + id
        + '\''
        + ", title='"
        + title
        + '\''
        + ", place='"
        + place
        + '\''
        + ", eventType="
        + eventType
        + ", description='"
        + description
        + '\''
        + ", subTopics="
        + subTopics
        + '}';
  }

  public static Builder newBuilder() {
    return new Event().new Builder();
  }

  public class Builder {
    private Builder() {}

    public Builder setId(String id) {
      Event.this.id = id;
      return this;
    }

    public Builder setPlace(String place) {
      Event.this.place = place;
      return this;
    }

    public Builder setTitle(String title) {
      Event.this.title = title;
      return this;
    }

    public Builder setEventType(EventType eventType) {
      Event.this.eventType = eventType;
      return this;
    }

    public Builder setDescription(String description) {
      Event.this.description = description;
      return this;
    }

    public Builder setSubTopics(List<String> subTopics) {
      Event.this.subTopics = subTopics;
      return this;
    }

    public Event build() {
      return Event.this;
    }
  }
}
