package jp.dressingroom.apiguard.payloadencrypt.entity;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;

public class Base64MessageCodec implements MessageCodec<Base64Message, Base64Message> {
  @Override
  public void encodeToWire(Buffer buffer, Base64Message base64Message) {
    buffer.appendInt(base64Message.getValue().getBytes().length);
    buffer.appendBytes(base64Message.getValue().getBytes());
  }

  @Override
  public Base64Message decodeFromWire(int pos, Buffer buffer) {
    int length = buffer.getInt(pos);
    pos += 4;
    return new Base64Message(new String(buffer.getBytes(pos, pos+length)));
  }

  @Override
  public Base64Message transform(Base64Message base64Message) {
    byte[] copied = new byte[base64Message.getValue().getBytes().length];
    System.arraycopy(base64Message.getValue().getBytes(), 0, copied, 0, base64Message.getValue().getBytes().length);
    return new Base64Message(new String(copied));
  }

  @Override
  public String name() {
    return "Base64Message";
  }

  @Override
  public byte systemCodecID() {
    return -1;
  }
}
