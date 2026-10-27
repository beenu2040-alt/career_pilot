package com.careerpilot.service;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Normalizes skill names and resolves common industry aliases deterministically.
 */
@Component
public class SkillNormalizer {

    private static final Map<String, String> ALIAS_MAP;

    static {
        Map<String, String> map = new HashMap<>();

        // JavaScript
        map.put("js", "javascript");
        map.put("javascript", "javascript");
        map.put("ecmascript", "javascript");

        // TypeScript
        map.put("ts", "typescript");
        map.put("typescript", "typescript");

        // Python
        map.put("py", "python");
        map.put("python", "python");
        map.put("python3", "python");

        // Go / Golang
        map.put("go", "go");
        map.put("golang", "go");

        // PostgreSQL
        map.put("postgres", "postgresql");
        map.put("postgresql", "postgresql");
        map.put("pgsql", "postgresql");

        // Kubernetes
        map.put("k8s", "kubernetes");
        map.put("kubernetes", "kubernetes");

        // React
        map.put("react", "react");
        map.put("reactjs", "react");
        map.put("react.js", "react");
        map.put("react-js", "react");

        // Node.js
        map.put("node", "node.js");
        map.put("nodejs", "node.js");
        map.put("node.js", "node.js");
        map.put("node-js", "node.js");

        // Vue
        map.put("vue", "vue");
        map.put("vuejs", "vue");
        map.put("vue.js", "vue");

        // Next.js
        map.put("next", "next.js");
        map.put("nextjs", "next.js");
        map.put("next.js", "next.js");

        // Angular
        map.put("angular", "angular");
        map.put("angularjs", "angular");
        map.put("angular.js", "angular");

        // Cloud Platforms
        map.put("aws", "aws");
        map.put("amazon web services", "aws");
        map.put("gcp", "gcp");
        map.put("google cloud", "gcp");
        map.put("google cloud platform", "gcp");
        map.put("azure", "azure");
        map.put("microsoft azure", "azure");

        // DevOps & Architecture
        map.put("docker", "docker");
        map.put("ci/cd", "ci/cd");
        map.put("cicd", "ci/cd");
        map.put("ci-cd", "ci/cd");
        map.put("rest", "rest api");
        map.put("rest api", "rest api");
        map.put("restful", "rest api");
        map.put("restful api", "rest api");
        map.put("spring", "spring boot");
        map.put("spring boot", "spring boot");
        map.put("springboot", "spring boot");
        map.put("microservices", "microservices");
        map.put("microservice", "microservices");
        map.put("micro-services", "microservices");
        map.put("sql", "sql");
        map.put("mongodb", "mongodb");
        map.put("mongo", "mongodb");

        ALIAS_MAP = Collections.unmodifiableMap(map);
    }

    /**
     * Strips leading/trailing whitespace, converts to lowercase, and replaces consecutive spaces with a single space.
     */
    public String normalize(String skillName) {
        if (skillName == null) {
            return "";
        }
        return skillName.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    /**
     * Resolves a skill string to its canonical representation via explicit alias matching.
     */
    public String canonicalize(String skillName) {
        String normalized = normalize(skillName);
        if (normalized.isEmpty()) {
            return "";
        }
        return ALIAS_MAP.getOrDefault(normalized, normalized);
    }

    /**
     * Compares two skill names for equivalence after normalization and alias resolution.
     */
    public boolean matches(String skillA, String skillB) {
        String canonicalA = canonicalize(skillA);
        String canonicalB = canonicalize(skillB);
        return !canonicalA.isEmpty() && canonicalA.equals(canonicalB);
    }
}
