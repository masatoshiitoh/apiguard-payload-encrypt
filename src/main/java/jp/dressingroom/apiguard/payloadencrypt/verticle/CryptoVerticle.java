package jp.dressingroom.apiguard.payloadencrypt.verticle;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import jp.dressingroom.apiguard.payloadencrypt.ConfigKeyNames;
import jp.dressingroom.apiguard.payloadencrypt.entity.Base64Message;
import jp.dressingroom.apiguard.payloadencrypt.entity.Base64MessageCodec;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * handle encoded request payload.
 * crypt: AES, 256bit, CBC, preset IV, pre shared key.
 * user must define IV and pre shared key to users server and client.
 *
 */
public class CryptoVerticle extends AbstractVerticle {
  private Cipher encryptor;
  private Cipher decryptor;
  private final String cipherAlgorithm = "AES";
  private final String cipherModes = "AES/CBC/PKCS5Padding";

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    ConfigRetriever configRetriever = ConfigRetriever.create(vertx);
    configRetriever.getConfig((json -> {
      try {
        // setup eventbus.
        EventBus eventBus = vertx.eventBus();
        eventBus.consumer(ApiguardEventBusNames.ENCRYPT.value(), encryptMessageHandler());
        eventBus.consumer(ApiguardEventBusNames.DECRYPT.value(), decryptMessageHandler());
        eventBus.registerDefaultCodec(Base64Message.class, new Base64MessageCodec());

        // setup cipher parameters.
        JsonObject result = json.result();
        String base64Iv = result.getString(ConfigKeyNames.CRYPTO_IV_BASE64.value(), "MDAwMDAwMDAwMDAwMDAwMA=="); // default value is 0000000000000000
        String base64Psk = result.getString(ConfigKeyNames.CRYPTO_PSK_BASE64.value(), "MDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDA"); // default value is 00000000000000000000000000000000

        Base64.Decoder decoder = Base64.getDecoder();
        IvParameterSpec iv = new IvParameterSpec(decoder.decode(base64Iv));
        SecretKeySpec key = new SecretKeySpec(decoder.decode(base64Psk), cipherAlgorithm);

        encryptor = Cipher.getInstance(cipherModes);
        encryptor.init(Cipher.ENCRYPT_MODE, key, iv);
        decryptor = Cipher.getInstance(cipherModes);
        decryptor.init(Cipher.DECRYPT_MODE, key, iv);
        startPromise.complete();
      } catch (Exception e) {
        startPromise.fail(e);
      }
    }));
  }

  private Handler<Message<Base64Message>> decryptMessageHandler() {
    // this is decrypter
    return messageHandler -> {
      // System.out.println(messageHandler.body().getValue());
      cryptWork(decryptor, messageHandler.body(), messageHandler);
    };
  }

  private Handler<Message<Base64Message>> encryptMessageHandler() {
    // this is encrypter
    return messageHandler -> {
      // System.out.println(messageHandler.body().getValue());
      cryptWork(encryptor, messageHandler.body(), messageHandler);
    };
  }

  private void cryptWork(Cipher cipher, Base64Message base64Message, Message<Base64Message> messageHandler) {
    try {
      byte[] rb = cipher.doFinal(base64Message.decode());
      messageHandler.reply(new Base64Message(rb));
    } catch (IllegalBlockSizeException e) {
      e.printStackTrace();
      messageHandler.fail(1,"IllegalBlockSizeException");
    } catch (BadPaddingException e) {
      e.printStackTrace();
      messageHandler.fail(1,"BadPaddingException");
    }
  }
}
