package com.kutylo.model;

public enum EventType {
  WORKSHOP("workshop"),
  TECH_TALK("tech-talk");

  private String type;

  EventType(String type) {
    this.type = type;
  }
}
