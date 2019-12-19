package jp.dressingroom.apiguard.payloadencrypt;

public enum ConfigKeyNames {
  PAYLOAD_ENCRYPT_SERVER_PORT("payloadencrypt.server.port"),

  PAYLOAD_ENCRYPT_PROXY_HOSTNAME("payloadencrypt.proxy.hostname"),
  PAYLOAD_ENCRYPT_PROXY_PORT("payloadencrypt.proxy.port"),
  PAYLOAD_ENCRYPT_PROXY_USERAGENT("payloadencrypt.proxy.ua"),
  PAYLOAD_ENCRYPT_PROXY_USESSL("payload.proxy.usessl"),

  CRYPTO_IV_BASE64("payloadencrypt.iv.base64"),
  CRYPTO_PSK_BASE64("payloadencrypt.psk.base64"),
  ;

  private final String text;

  ConfigKeyNames(final String text) {
    this.text = text;
  }

  public String value() {
    return this.text;
  }
}
