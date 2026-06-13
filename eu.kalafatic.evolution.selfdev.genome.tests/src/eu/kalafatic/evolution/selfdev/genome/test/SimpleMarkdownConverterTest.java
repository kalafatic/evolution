package eu.kalafatic.evolution.selfdev.genome.test;

import static org.junit.Assert.assertTrue;
import org.junit.Test;
import eu.kalafatic.evolution.selfdev.genome.util.SimpleMarkdownConverter;

public class SimpleMarkdownConverterTest {

    @Test
    public void testHeadings() {
        String md = "# Header 1\n## Header 2\n### Header 3";
        String html = SimpleMarkdownConverter.toHtml(md);
        assertTrue(html.contains("<h1>Header 1</h1>"));
        assertTrue(html.contains("<h2>Header 2</h2>"));
        assertTrue(html.contains("<h3>Header 3</h3>"));
    }

    @Test
    public void testBoldItalic() {
        String md = "**Bold** and *Italic*";
        String html = SimpleMarkdownConverter.toHtml(md);
        assertTrue(html.contains("<strong>Bold</strong>"));
        assertTrue(html.contains("<em>Italic</em>"));
    }

    @Test
    public void testCodeBlock() {
        String md = "```java\npublic class Test {}\n```";
        String html = SimpleMarkdownConverter.toHtml(md);
        assertTrue(html.contains("<pre><code>"));
        assertTrue(html.contains("public class Test {}"));
        assertTrue(html.contains("</code></pre>"));
    }

    @Test
    public void testEscaping() {
        String md = "Check this <tag>";
        String html = SimpleMarkdownConverter.toHtml(md);
        assertTrue(html.contains("Check this &lt;tag&gt;"));
    }

    @Test
    public void testNoDoubleEscapingOfGeneratedTags() {
        String md = "# Header";
        String html = SimpleMarkdownConverter.toHtml(md);
        // Correct: <h1>Header</h1>
        // Incorrect: &lt;h1&gt;Header&lt;/h1&gt;
        assertTrue(html.contains("<h1>Header</h1>"));
        assertTrue(!html.contains("&lt;h1&gt;"));
    }
}
