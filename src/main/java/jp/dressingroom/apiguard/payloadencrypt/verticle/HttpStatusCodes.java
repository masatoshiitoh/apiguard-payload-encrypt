package jp.dressingroom.apiguard.payloadencrypt.verticle;

public enum HttpStatusCodes {
  OK(200),
  NOT_FOUND(404),
  INTERNAL_SERVER_ERROR(500),
  ;

  private final Integer status;

  HttpStatusCodes(final Integer status) {
    this.status = status;
  }

  public Integer value() {
    return this.status;
  }
}
