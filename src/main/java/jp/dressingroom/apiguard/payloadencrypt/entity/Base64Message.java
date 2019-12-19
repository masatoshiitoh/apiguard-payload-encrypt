package jp.dressingroom.apiguard.payloadencrypt.entity;

import java.util.Base64;

public class Base64Message {
  String b64str;

  public Base64Message(String value) {
    this.b64str = value;
  }

  public Base64Message(byte[] byteValue) {
    this.b64str = Base64.getEncoder().encodeToString(byteValue);
  }

  public String getValue() {
    return this.b64str;
  }

  public byte[] decode() {
    return Base64.getDecoder().decode(this.b64str);
  }
}
