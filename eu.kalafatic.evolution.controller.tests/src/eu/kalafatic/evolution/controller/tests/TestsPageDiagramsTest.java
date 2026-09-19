package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;
import org.junit.Test;

import eu.kalafatic.evolution.view.editors.pages.TestsPage;

/**
 * Unit tests verifying TestsPage HTML5/CSS dynamic diagram templates and JS contracts.
 */
public class TestsPageDiagramsTest {

    @Test
    public void testIterativeHtmlTemplateStructure() {
        String html = TestsPage.getIterativeHtmlTemplate();

        assertNotNull("Iterative HTML template must not be null", html);
        assertTrue("Template must contain doctype and html tags", html.contains("<!DOCTYPE html>"));
        assertTrue("Template must include EvoStyleManager CSS tokens", html.contains("--evo-primary"));
        assertTrue("Template must include --evo-bg", html.contains("--evo-bg"));

        // SVG Canvas and nodes
        assertTrue("Template must define SVG canvas", html.contains("<svg id='lifecycle-svg'"));
        assertTrue("Template must contain Observe node", html.contains("id='n_observe'"));
        assertTrue("Template must contain Analyze node", html.contains("id='n_analyze'"));
        assertTrue("Template must contain Plan node", html.contains("id='n_plan'"));
        assertTrue("Template must contain Validate node", html.contains("id='n_validate'"));
        assertTrue("Template must contain Execute node", html.contains("id='n_execute'"));
        assertTrue("Template must contain Test node", html.contains("id='n_test'"));
        assertTrue("Template must contain Evaluate node", html.contains("id='n_evaluate'"));
        assertTrue("Template must contain Commit node", html.contains("id='n_commit'"));
        assertTrue("Template must contain PR node", html.contains("id='n_PR'"));
        assertTrue("Template must contain Feedback node", html.contains("id='n_feedback'"));
        assertTrue("Template must contain Refine node", html.contains("id='n_refine'"));
        assertTrue("Template must contain Learn node", html.contains("id='n_learn'"));

        // JavaScript API contracts
        assertTrue("Template must declare setNodeStatus JS function", html.contains("function setNodeStatus"));
        assertTrue("Template must declare setEdgeStatus JS function", html.contains("function setEdgeStatus"));
        assertTrue("Template must declare resetDiagram JS function", html.contains("function resetDiagram"));
    }

    @Test
    public void testStatusDashboardHtmlTemplateStructure() {
        String html = TestsPage.getHtmlTemplate();

        assertNotNull("Status dashboard HTML template must not be null", html);
        assertTrue("Template must contain doctype and html tags", html.contains("<!DOCTYPE html>"));
        assertTrue("Template must include EvoStyleManager CSS tokens", html.contains("--evo-primary"));

        // Metrics counter elements
        assertTrue("Template must contain m-total counter", html.contains("id='m-total'"));
        assertTrue("Template must contain m-passed counter", html.contains("id='m-passed'"));
        assertTrue("Template must contain m-failed counter", html.contains("id='m-failed'"));
        assertTrue("Template must contain m-running counter", html.contains("id='m-running'"));
        assertTrue("Template must contain m-pending counter", html.contains("id='m-pending'"));

        // Filter toolbar buttons
        assertTrue("Template must contain flt-ALL filter button", html.contains("id='flt-ALL'"));
        assertTrue("Template must contain flt-PASSED filter button", html.contains("id='flt-PASSED'"));
        assertTrue("Template must contain flt-FAILED filter button", html.contains("id='flt-FAILED'"));
        assertTrue("Template must contain flt-RUNNING filter button", html.contains("id='flt-RUNNING'"));
        assertTrue("Template must contain flt-PENDING filter button", html.contains("id='flt-PENDING'"));

        // Grid container & JS API contract
        assertTrue("Template must contain grid-container div", html.contains("id='grid-container'"));
        assertTrue("Template must declare updateDiagram JS function", html.contains("function updateDiagram"));
    }
}
