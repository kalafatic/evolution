package eu.kalafatic.evolution.media.generator;

import eu.kalafatic.evolution.controller.mediation.model.Subsystem;
import eu.kalafatic.evolution.controller.mediation.model.TargetRealityModel;

public class PresentationGenerator {

    public String generate(TargetRealityModel model) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html>\n<head>\n");
        html.append("<meta charset=\"UTF-8\">\n");
        html.append("<title>EVO Presentation: ").append(model.getDomain()).append("</title>\n");
        appendStyles(html);
        html.append("</head>\n<body>\n");

        html.append("<div class=\"reveal\">\n");
        html.append("  <div class=\"slides\">\n");

        // Slide 1: Overview
        addSlide(html, "Overview", "Domain: " + model.getDomain() + "<br>Purpose: " + model.getPurpose());

        // Slide 2: System Vision
        addSlide(html, "System Vision", model.getArchitectureSummary());

        // Slide 3: Architecture
        addSlide(html, "Architecture", "Complexity: " + model.getArchitectureNodes() + " nodes, " + model.getArchitectureRelationships() + " relationships");

        // Slide 4: Core Subsystems
        StringBuilder subsystems = new StringBuilder("<ul>");
        for (Subsystem s : model.getSubsystems()) {
            subsystems.append("<li><strong>").append(s.getName()).append("</strong>: ").append(s.getPurpose()).append("</li>");
        }
        subsystems.append("</ul>");
        addSlide(html, "Core Subsystems", subsystems.toString());

        // Slide 5-10: Placeholders for other sections as required
        addSlide(html, "Cognitive Engine", "Analyzed " + model.getMetadataEntries() + " metadata entries.");
        addSlide(html, "Reality Discovery", "Completeness: " + (int)(model.getRealityCompleteness() * 100) + "%");
        addSlide(html, "Genome Extraction", "Identified " + model.getGenes().size() + " architectural genes.");
        addSlide(html, "Execution Flow Analysis", "Captured " + model.getExecutionFlows().size() + " execution flows.");
        addSlide(html, "Demonstration Examples", "Reference implementations: " + model.getReferenceImplementations().size());
        addSlide(html, "Roadmap", "Addressing " + model.getKnowledgeGaps().size() + " knowledge gaps.");

        html.append("  </div>\n");
        html.append("</div>\n");

        appendScripts(html);
        html.append("</body>\n</html>");
        return html.toString();
    }

    private void addSlide(StringBuilder html, String title, String content) {
        html.append("    <section class=\"slide\">\n");
        html.append("      <h2>").append(title).append("</h2>\n");
        html.append("      <div class=\"content\">").append(content).append("</div>\n");
        html.append("    </section>\n");
    }

    private void appendStyles(StringBuilder html) {
        html.append("<style>\n");
        html.append("  body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; background: #1a1a1a; color: #fff; overflow: hidden; }\n");
        html.append("  .slides { width: 100vw; height: 100vh; display: flex; transition: transform 0.5s ease-in-out; }\n");
        html.append("  .slide { min-width: 100vw; height: 100vh; display: flex; flex-direction: column; justify-content: center; align-items: center; padding: 50px; box-sizing: border-box; text-align: center; }\n");
        html.append("  h2 { font-size: 3em; color: #00d4ff; margin-bottom: 30px; }\n");
        html.append("  .content { font-size: 1.5em; line-height: 1.6; max-width: 800px; }\n");
        html.append("  ul { text-align: left; }\n");
        html.append("  .nav { position: fixed; bottom: 20px; right: 20px; display: flex; gap: 10px; }\n");
        html.append("  button { background: rgba(255,255,255,0.1); border: 1px solid #fff; color: #fff; padding: 10px 20px; cursor: pointer; }\n");
        html.append("  button:hover { background: rgba(255,255,255,0.2); }\n");
        html.append("</style>\n");
    }

    private void appendScripts(StringBuilder html) {
        html.append("<div class=\"nav\"><button onclick=\"prev()\">Prev</button><button onclick=\"next()\">Next</button></div>\n");
        html.append("<script>\n");
        html.append("  let currentSlide = 0;\n");
        html.append("  const slides = document.querySelector('.slides');\n");
        html.append("  const totalSlides = document.querySelectorAll('.slide').length;\n");
        html.append("  function showSlide(index) {\n");
        html.append("    currentSlide = (index + totalSlides) % totalSlides;\n");
        html.append("    slides.style.transform = `translateX(-${currentSlide * 100}vw)`;\n");
        html.append("  }\n");
        html.append("  function next() { showSlide(currentSlide + 1); }\n");
        html.append("  function prev() { showSlide(currentSlide - 1); }\n");
        html.append("  window.addEventListener('keydown', e => {\n");
        html.append("    if (e.key === 'ArrowRight') next();\n");
        html.append("    if (e.key === 'ArrowLeft') prev();\n");
        html.append("  });\n");
        html.append("</script>\n");
    }
}
