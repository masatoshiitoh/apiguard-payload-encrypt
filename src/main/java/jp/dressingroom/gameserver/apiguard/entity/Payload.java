package jp.dressingroom.gameserver.apiguard.entity;

public class Payload {
  String value;

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public Payload(String value) {
    this.value = value;
  }
}