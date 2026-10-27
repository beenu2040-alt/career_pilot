package com.careerpilot.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SkillNormalizerTest {

    private SkillNormalizer normalizer;

    @BeforeEach
    void setUp() {
        normalizer = new SkillNormalizer();
    }

    @Test
    @DisplayName("Should normalize whitespace and case consistently")
    void testNormalizeWhitespaceAndCase() {
        assertEquals("java", normalizer.normalize("  Java  "));
        assertEquals("spring boot", normalizer.normalize("  Spring    Boot  "));
        assertEquals("c++", normalizer.normalize("C++"));
        assertEquals("", normalizer.normalize(null));
        assertEquals("", normalizer.normalize("   "));
    }

    @Test
    @DisplayName("Should resolve known developer aliases to canonical skill names")
    void testCanonicalAliases() {
        assertEquals("javascript", normalizer.canonicalize("JS"));
        assertEquals("javascript", normalizer.canonicalize("JavaScript"));
        assertEquals("javascript", normalizer.canonicalize("ecmascript"));

        assertEquals("typescript", normalizer.canonicalize("TS"));
        assertEquals("typescript", normalizer.canonicalize("TypeScript"));

        assertEquals("python", normalizer.canonicalize("py"));
        assertEquals("python", normalizer.canonicalize("python3"));

        assertEquals("kubernetes", normalizer.canonicalize("k8s"));
        assertEquals("kubernetes", normalizer.canonicalize("Kubernetes"));

        assertEquals("postgresql", normalizer.canonicalize("postgres"));
        assertEquals("postgresql", normalizer.canonicalize("pgsql"));

        assertEquals("react", normalizer.canonicalize("reactjs"));
        assertEquals("react", normalizer.canonicalize("React.js"));

        assertEquals("node.js", normalizer.canonicalize("node"));
        assertEquals("node.js", normalizer.canonicalize("NodeJS"));

        assertEquals("aws", normalizer.canonicalize("amazon web services"));
        assertEquals("aws", normalizer.canonicalize("AWS"));

        assertEquals("docker", normalizer.canonicalize("Docker"));
    }

    @Test
    @DisplayName("Should correctly evaluate skill equivalence through matches()")
    void testSkillEquivalenceMatches() {
        assertTrue(normalizer.matches("JS", "JavaScript"));
        assertTrue(normalizer.matches("k8s", "Kubernetes"));
        assertTrue(normalizer.matches("PostgreSQL", "postgres"));
        assertTrue(normalizer.matches("React", "react.js"));
        assertTrue(normalizer.matches("NodeJS", "node.js"));
        assertTrue(normalizer.matches("AWS", "amazon web services"));

        assertFalse(normalizer.matches("Java", "JavaScript"));
        assertFalse(normalizer.matches("TypeScript", "Python"));
        assertFalse(normalizer.matches("", "Java"));
        assertFalse(normalizer.matches(null, "Java"));
    }

    @Test
    @DisplayName("Should preserve unrecognized or custom skill names cleanly")
    void testPreserveCustomSkills() {
        assertEquals("solidity", normalizer.canonicalize("Solidity"));
        assertEquals("terraform", normalizer.canonicalize("  Terraform  "));
        assertTrue(normalizer.matches("Terraform", "terraform"));
    }
}
