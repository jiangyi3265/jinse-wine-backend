package com.ruoyi.web.controller.business;

import static org.junit.Assert.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.time.Instant;
import java.util.Base64;
import org.junit.Test;

public class WechatPaySignatureTest
{
    @Test
    public void verifiesExactResponseBodyAndCertificateSerial() throws Exception
    {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keys = generator.generateKeyPair();
        String timestamp = "1791248400";
        String nonce = "response-nonce";
        String body = "{\"trade_state\":\"SUCCESS\",\"description\":\"测试订单\"}";
        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(keys.getPrivate());
        signer.update((timestamp + "\n" + nonce + "\n" + body + "\n").getBytes(StandardCharsets.UTF_8));
        String signature = Base64.getEncoder().encodeToString(signer.sign());

        WechatPayService.verifyWechatSignature(signature, timestamp, nonce, "AB12", body, keys.getPublic(), "ab12");
        assertThrows(IOException.class, () -> WechatPayService.verifyWechatSignature(
                signature, timestamp, nonce, "AB12", body.replace("SUCCESS", "NOTPAY"), keys.getPublic(), "ab12"));
        assertThrows(IOException.class, () -> WechatPayService.verifyWechatSignature(
                signature, timestamp, nonce, "other", body, keys.getPublic(), "ab12"));
        assertThrows(IOException.class, () -> WechatPayService.verifyWechatSignature(
                null, timestamp, nonce, "AB12", body, keys.getPublic(), "ab12"));
    }

    @Test
    public void rejectsExpiredOrMalformedNotificationTimestamp() throws Exception
    {
        long now = Instant.now().getEpochSecond();
        WechatPayService.verifyNotifyTimestamp(String.valueOf(now - 300), now);
        WechatPayService.verifyNotifyTimestamp(String.valueOf(now + 300), now);
        assertThrows(IOException.class, () -> WechatPayService.verifyNotifyTimestamp(String.valueOf(now - 301), now));
        assertThrows(IOException.class, () -> WechatPayService.verifyNotifyTimestamp(String.valueOf(now + 301), now));
        assertThrows(IOException.class, () -> WechatPayService.verifyNotifyTimestamp("invalid", now));
        assertThrows(IOException.class, () -> WechatPayService.verifyNotifyTimestamp(null, now));
    }
}
