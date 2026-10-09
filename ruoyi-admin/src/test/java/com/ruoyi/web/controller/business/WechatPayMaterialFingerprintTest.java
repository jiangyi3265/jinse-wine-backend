package com.ruoyi.web.controller.business;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.junit.After;
import org.junit.Test;
import com.fasterxml.jackson.databind.ObjectMapper;

public class WechatPayMaterialFingerprintTest
{
    private final List<Path> files = new ArrayList<>();

    @After
    public void cleanUp() throws Exception
    {
        for (Path file : files) Files.deleteIfExists(file);
    }

    @Test
    public void fileContentsAndPublicKeyIdInvalidateVerification() throws Exception
    {
        KeyPair merchant = keyPair();
        KeyPair platform = keyPair();
        Path privateFile = pemFile("PRIVATE KEY", merchant.getPrivate().getEncoded());
        Path publicFile = pemFile("PUBLIC KEY", platform.getPublic().getEncoded());
        WechatPayService service = new WechatPayService(null, new ObjectMapper(), null);
        field(service, "privateKeyPem", privateFile.toString());
        field(service, "platformCertPem", publicFile.toString());
        field(service, "platformSerial", "PUB_KEY_ID_FIRST");

        String first = service.verificationMaterialFingerprint();
        assertNotNull(first);
        field(service, "platformSerial", "PUB_KEY_ID_SECOND");
        assertNotEquals(first, service.verificationMaterialFingerprint());

        field(service, "platformSerial", "PUB_KEY_ID_FIRST");
        Files.write(privateFile, pem("PRIVATE KEY", keyPair().getPrivate().getEncoded()).getBytes(StandardCharsets.UTF_8));
        assertNotEquals(first, service.verificationMaterialFingerprint());
        Files.delete(privateFile);
        assertNull(service.verificationMaterialFingerprint());
    }

    @Test
    public void malformedPemOrMissingPublicKeyIdIsNotReady() throws Exception
    {
        WechatPayService service = new WechatPayService(null, new ObjectMapper(), null);
        field(service, "privateKeyPem", pem("PRIVATE KEY", keyPair().getPrivate().getEncoded()));
        field(service, "platformCertPem", pem("PUBLIC KEY", keyPair().getPublic().getEncoded()));
        assertNull(service.verificationMaterialFingerprint());

        field(service, "platformSerial", "PUB_KEY_ID_FIRST");
        field(service, "platformCertPem", "not-a-pem");
        assertNull(service.verificationMaterialFingerprint());
    }

    private Path pemFile(String label, byte[] encoded) throws Exception
    {
        Path file = Files.createTempFile("wechat-pay-material-", ".pem");
        files.add(file);
        Files.write(file, pem(label, encoded).getBytes(StandardCharsets.UTF_8));
        return file;
    }

    private static String pem(String label, byte[] encoded)
    {
        return "-----BEGIN " + label + "-----\n"
                + Base64.getMimeEncoder(64, new byte[] { '\n' }).encodeToString(encoded)
                + "\n-----END " + label + "-----\n";
    }

    private static KeyPair keyPair() throws Exception
    {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private static void field(WechatPayService service, String name, Object value) throws Exception
    {
        Field field = WechatPayService.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(service, value);
    }
}
