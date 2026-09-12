package org.springblade.modules.smartreminder.push;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Direct Apple APNs HTTP/2 + ES256 (.p8); no aggregator or client-side private key. */
@Component
public class ApnsClient {
    public record Result(int status, String reason) {
        public boolean accepted() { return status == 200; }
        public boolean invalidDevice() { return status == 410 || (status == 400 && Set.of("BadDeviceToken", "DeviceTokenNotForTopic").contains(reason)); }
        public boolean retryable() { return status == 0 || status == 429 || status >= 500 || "ExpiredProviderToken".equals(reason); }
    }
    private record CachedJwt(String value, Instant createdAt) {}
    private final PushProperties properties;
    private final ObjectMapper json;
    private final HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).connectTimeout(Duration.ofSeconds(5)).build();
    private final Map<String, CachedJwt> jwts = new ConcurrentHashMap<>();

    public ApnsClient(PushProperties properties, ObjectMapper json) { this.properties=properties; this.json=json; }
    public boolean ready(String environment) {
        if (!properties.isEnabled() || !Set.of("production","sandbox").contains(environment)) return false;
        String id=keyId(environment), path=keyPath(environment);
        try { return properties.getTeamId().matches("[A-Z0-9]{10}") && id.matches("[A-Z0-9]{10}") && !path.isBlank() && Path.of(path).isAbsolute() && Files.isReadable(Path.of(path)); }
        catch (RuntimeException e) { return false; }
    }
    private String keyId(String environment) { return "production".equals(environment)?properties.getProductionKeyId():properties.getSandboxKeyId(); }
    private String keyPath(String environment) { return "production".equals(environment)?properties.getProductionKeyPath():properties.getSandboxKeyPath(); }
    private static String base64(byte[] value) { return Base64.getUrlEncoder().withoutPadding().encodeToString(value); }
    static String signJwt(String teamId, String keyId, PrivateKey key, Instant now) throws Exception {
        String header=base64(("{\"alg\":\"ES256\",\"kid\":\""+keyId+"\"}").getBytes(StandardCharsets.UTF_8));
        String body=base64(("{\"iss\":\""+teamId+"\",\"iat\":"+now.getEpochSecond()+"}").getBytes(StandardCharsets.UTF_8));
        String unsigned=header+"."+body;
        Signature signer=Signature.getInstance("SHA256withECDSAinP1363Format");
        signer.initSign(key); signer.update(unsigned.getBytes(StandardCharsets.US_ASCII));
        return unsigned+"."+base64(signer.sign());
    }
    private synchronized String jwt(String environment) throws Exception {
        Instant now=Instant.now(); CachedJwt cached=jwts.get(environment);
        if(cached!=null && now.isBefore(cached.createdAt.plusSeconds(2400)) && !now.isBefore(cached.createdAt)) return cached.value;
        String pem=Files.readString(Path.of(keyPath(environment))).replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "").replaceAll("\\s", "");
        PrivateKey key=KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(pem)));
        String token=signJwt(properties.getTeamId(),keyId(environment),key,now);
        jwts.put(environment,new CachedJwt(token,now)); return token;
    }
    public Result send(String environment, String deviceToken, String apnsId, String messageId, Instant expiresAt, Map<String,Object> payload) {
        if(!ready(environment)) return new Result(0,"NotConfigured");
        try {
            byte[] body=json.writeValueAsBytes(payload);
            if(body.length>4096) return new Result(413,"PayloadTooLarge");
            if(!deviceToken.matches("[a-fA-F0-9]{32,512}")) return new Result(400,"BadDeviceToken");
            String host="production".equals(environment)?"api.push.apple.com":"api.sandbox.push.apple.com";
            HttpRequest request=HttpRequest.newBuilder(URI.create("https://"+host+"/3/device/"+deviceToken))
                .timeout(Duration.ofSeconds(15)).header("authorization","bearer "+jwt(environment))
                .header("apns-topic",properties.getBundleId()).header("apns-push-type","alert")
                .header("apns-priority","10").header("apns-id",apnsId).header("apns-collapse-id","message-"+messageId)
                .header("apns-expiration",Long.toString(expiresAt.getEpochSecond()))
                .header("content-type","application/json").POST(HttpRequest.BodyPublishers.ofByteArray(body)).build();
            HttpResponse<String> response=http.send(request,HttpResponse.BodyHandlers.ofString());
            String reason=response.statusCode()==200?"Accepted":json.readTree(response.body()).path("reason").asText("Rejected");
            if("ExpiredProviderToken".equals(reason)) jwts.remove(environment);
            // Never return/log the provider response body, JWT, device token or private key.
            return new Result(response.statusCode(),reason.matches("[A-Za-z0-9_]{1,80}")?reason:"Rejected");
        } catch(InterruptedException e) { Thread.currentThread().interrupt(); return new Result(0,"Interrupted"); }
        catch(Exception e) { return new Result(0,"ProviderConnectionFailed"); }
    }
}
