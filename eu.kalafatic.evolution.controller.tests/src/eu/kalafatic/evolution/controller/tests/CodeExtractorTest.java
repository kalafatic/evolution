package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;
import org.junit.Test;
import eu.kalafatic.evolution.controller.orchestration.util.CodeExtractor;

public class CodeExtractorTest {

    @Test
    public void testRawJavaCode() {
        String code = "public class SimplePrinter {\n" +
                "    public static void main(String[] args) {\n" +
                "        System.out.println(\"Hello\");\n" +
                "    }\n" +
                "}";
        String extracted = CodeExtractor.extractCode(code);
        assertEquals(code, extracted);
    }

    @Test
    public void testMarkdownBlock() {
        String input = "Here is the code you requested:\n" +
                "```java\n" +
                "public class MyClass {}\n" +
                "```\n" +
                "Hope this helps!";
        String extracted = CodeExtractor.extractCode(input);
        assertEquals("public class MyClass {}", extracted);
    }

    @Test
    public void testStripThinkBlocks() {
        String input = "<think>We need a simple Java printer class here</think>\n" +
                "```java\n" +
                "public class ThinkClass {}\n" +
                "```";
        String extracted = CodeExtractor.extractCode(input);
        assertEquals("public class ThinkClass {}", extracted);
    }

    @Test
    public void testCaseInsensitiveJsonCode() {
        // Uppercase CODE property inside JSON
        String jsonText = "{\n" +
                "  \"CLASS_NAME\": \"PrintText\",\n" +
                "  \"METHOD\": \"prints text to console\",\n" +
                "  \"CODE\": \"import java.util.Scanner;\\nclass PrintText {}\"\n" +
                "}";
        String extracted = CodeExtractor.extractCode(jsonText);
        assertEquals("import java.util.Scanner;\nclass PrintText {}", extracted);
    }

    @Test
    public void testLowercaseJsonCode() {
        // Lowercase code property inside JSON
        String jsonText = "{\n" +
                "  \"code\": \"public class App {}\"\n" +
                "}";
        String extracted = CodeExtractor.extractCode(jsonText);
        assertEquals("public class App {}", extracted);
    }

    @Test
    public void testImplementationJsonCode() {
        // Lowercase implementation property
        String jsonText = "{\n" +
                "  \"implementation\": \"public class Main {}\"\n" +
                "}";
        String extracted = CodeExtractor.extractCode(jsonText);
        assertEquals("public class Main {}", extracted);
    }

    @Test
    public void testRecursiveExtractionInMarkdownBlock() {
        // A markdown block that contains JSON inside
        String input = "```json\n" +
                "{\n" +
                "  \"CODE\": \"public class NestedClass {}\"\n" +
                "}\n" +
                "```";
        String extracted = CodeExtractor.extractCode(input);
        assertEquals("public class NestedClass {}", extracted);
    }

    @Test
    public void testFuzzyFallbackJsonValues() {
        // If no preferred keys are present, scan all values for Java signatures
        String jsonText = "{\n" +
                "  \"someRandomKey\": \"public class FuzzyClass {}\"\n" +
                "}";
        String extracted = CodeExtractor.extractCode(jsonText);
        assertEquals("public class FuzzyClass {}", extracted);
    }

    @Test
    public void testLabelPrefix() {
        String input = "CODE:\n" +
                "```java\n" +
                "public class LabelClass {}\n" +
                "```";
        String extracted = CodeExtractor.extractCode(input);
        assertEquals("public class LabelClass {}", extracted);
    }

    @Test
    public void testSignatureFallback() {
        // When there is random leading text but class signature is clearly visible
        String input = "Random reasoning prefix here...\n" +
                "import java.io.*;\n" +
                "public class SignatureClass {\n" +
                "}";
        String extracted = CodeExtractor.extractCode(input);
        assertEquals("import java.io.*;\n" +
                "public class SignatureClass {\n" +
                "}", extracted);
    }
}
