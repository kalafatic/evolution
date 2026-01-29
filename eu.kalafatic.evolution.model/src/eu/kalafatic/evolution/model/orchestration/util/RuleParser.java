package eu.kalafatic.evolution.model.orchestration.util;

import eu.kalafatic.evolution.model.orchestration.*;

/**
 * @generated NOT
 */
public class RuleParser {
    public static void parseAndAddRules(Agent agent, String rulesData) {
        if (rulesData == null || rulesData.isEmpty()) return;

        String[] ruleSpecs = rulesData.split(";");
        OrchestrationFactory factory = OrchestrationFactory.eINSTANCE;

        for (String spec : ruleSpecs) {
            String[] parts = spec.split("=", 2);
            if (parts.length < 2) continue;

            String type = parts[0].trim();
            String config = parts[1].trim();

            Rule rule = null;
            if ("access".equalsIgnoreCase(type)) {
                AccessRule ar = factory.createAccessRule();
                parseKeyValuePairs(ar, config);
                rule = ar;
            } else if ("network".equalsIgnoreCase(type)) {
                NetworkRule nr = factory.createNetworkRule();
                parseKeyValuePairs(nr, config);
                rule = nr;
            } else if ("memory".equalsIgnoreCase(type)) {
                MemoryRule mr = factory.createMemoryRule();
                parseKeyValuePairs(mr, config);
                rule = mr;
            } else if ("secret".equalsIgnoreCase(type)) {
                SecretRule sr = factory.createSecretRule();
                parseKeyValuePairs(sr, config);
                rule = sr;
            }

            if (rule != null) {
                rule.setName(type + " Rule");
                agent.getRules().add(rule);
            }
        }
    }

    private static void parseKeyValuePairs(Rule rule, String config) {
        String[] pairs = config.split(",");
        for (String pair : pairs) {
            String[] kv = pair.split("=");
            if (kv.length < 2) continue;
            String key = kv[0].trim();
            String value = kv[1].trim();

            try {
                if (rule instanceof AccessRule) {
                    AccessRule ar = (AccessRule) rule;
                    if ("allowedPaths".equalsIgnoreCase(key)) {
                        ar.getAllowedPaths().add(value);
                    } else if ("deniedPaths".equalsIgnoreCase(key)) {
                        ar.getDeniedPaths().add(value);
                    }
                } else if (rule instanceof NetworkRule) {
                    NetworkRule nr = (NetworkRule) rule;
                    if ("allowedDomains".equalsIgnoreCase(key)) {
                        nr.getAllowedDomains().add(value);
                    } else if ("allowAll".equalsIgnoreCase(key)) {
                        nr.setAllowAll(Boolean.parseBoolean(value));
                    }
                } else if (rule instanceof MemoryRule) {
                    MemoryRule mr = (MemoryRule) rule;
                    if ("storageLimit".equalsIgnoreCase(key)) {
                        mr.setStorageLimit(Integer.parseInt(value));
                    } else if ("retentionPeriod".equalsIgnoreCase(key)) {
                        mr.setRetentionPeriod(Integer.parseInt(value));
                    }
                } else if (rule instanceof SecretRule) {
                    SecretRule sr = (SecretRule) rule;
                    if ("allowedSecrets".equalsIgnoreCase(key)) {
                        sr.getAllowedSecrets().add(value);
                    }
                }
            } catch (Exception e) {
                // Ignore parsing errors for individual fields
            }
        }
    }
}
