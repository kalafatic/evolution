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

    @Test
    public void testTrailingTruncationOnJavaStarter() {
        String input = "public class TextPrinter {\n" +
                "    public void print(String text) {\n" +
                "        System.out.println(text);\n" +
                "    }\n" +
                "}\n" +
                "This class prints text to the console, satisfying the user's requirements.";
        String extracted = CodeExtractor.extractCode(input);
        assertEquals("public class TextPrinter {\n" +
                "    public void print(String text) {\n" +
                "        System.out.println(text);\n" +
                "    }\n" +
                "}", extracted);
    }

    @Test
    public void testMarkdownExtractionTakesPrecedence() {
        // Starts with Java starter (class), but also has markdown backticks block inside
        String input = "class TextPrinter {\n" +
                "```java\n" +
                "class ActualClass {}\n" +
                "```\n" +
                "}";
        String extracted = CodeExtractor.extractCode(input);
        assertEquals("class ActualClass {}", extracted);
    }

    @Test
    public void testBraceCountingWithCommentsAndStrings() {
        String input = "package com.example;\n" +
                "public class RobustPrinter {\n" +
                "    // A comment with } closed brace\n" +
                "    /* A multi-line \n" +
                "       comment with } closed brace */\n" +
                "    public String text = \"this is { nested brace } string\";\n" +
                "} \n" +
                "Trailing conversational nonsense here.";
        String extracted = CodeExtractor.extractCode(input);
        assertEquals("package com.example;\n" +
                "public class RobustPrinter {\n" +
                "    // A comment with } closed brace\n" +
                "    /* A multi-line \n" +
                "       comment with } closed brace */\n" +
                "    public String text = \"this is { nested brace } string\";\n" +
                "}", extracted);
    }

    @Test
    public void testExplanationAndMarkdownListPrefix() {
        String input = "**Explanation:**\n" +
                "\n" +
                "*   **CLASS_NAME:** `PrintText`\n" +
                "*   **METHOD:** `printText(String text)`\n" +
                "    *   This method takes a `String` argument named `text` and prints it to the console using `System.out.println()`.\n" +
                "*   **Code:**\n" +
                "\n" +
                "```java\n" +
                "package com.example;\n" +
                "\n" +
                "public class PrintText {\n" +
                "    public static void printText(String text) {\n" +
                "        System.out.println(text);\n" +
                "    }\n" +
                "}\n" +
                "```";
        String extracted = CodeExtractor.extractCode(input);
        String expected = "package com.example;\n" +
                "\n" +
                "public class PrintText {\n" +
                "    public static void printText(String text) {\n" +
                "        System.out.println(text);\n" +
                "    }\n" +
                "}";
        assertEquals(expected, extracted);
    }

    @Test
    public void testExplanationWithoutMarkdownBackticks() {
        String input = "**Explanation:**\n" +
                "* **CLASS_NAME:** PrintText\n" +
                "* **Code:**\n" +
                "package com.example;\n" +
                "public class PrintText {\n" +
                "    public void printText(String text) {\n" +
                "        System.out.println(text);\n" +
                "    }\n" +
                "}";
        String extracted = CodeExtractor.extractCode(input);
        String expected = "package com.example;\n" +
                "public class PrintText {\n" +
                "    public void printText(String text) {\n" +
                "        System.out.println(text);\n" +
                "    }\n" +
                "}";
        assertEquals(expected, extracted);
    }
}
