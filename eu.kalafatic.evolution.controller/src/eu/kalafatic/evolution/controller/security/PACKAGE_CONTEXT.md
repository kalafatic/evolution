# PACKAGE CONTEXT

## Directory: eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/security/

## Domain: general

## Components
* `TokenSecurityService.java`: package eu.kalafatic.evolution.controller.security; import java.nio.charset.StandardCharsets; import java.util.Base64; import javax.crypto.Cipher; import javax.crypto.spec.SecretKeySpec; import eu.kalafatic.evolution.model.orchestration.AIProvider; import eu.kalafatic.evolution.model.orchestration.Orchestrator; import eu.kalafatic.evolution.controller.providers.AiProviders; import eu.kalafatic.evolution.controller.providers.ProviderConfig; public class TokenSecurityService { public static class ResolvedProvider { public String name; public String url; public String token; public String format; public String model; public boolean local; public ResolvedProvider(String name, String url, String token, String format, String model, boolean local) { this.name = name; this.url = url;
