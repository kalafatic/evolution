const { chromium } = require('playwright');
const path = require('path');

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage();

  // Load the chat.html file
  const filePath = 'file://' + path.resolve('eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/orchestration/chat.html');
  await page.goto(filePath);

  // 1. Mock some messages and state to verify UI rendering
  await page.evaluate(() => {
    // Mock system state showing MEDIATED mode
    const mockState = {
      name: "Evo Engine",
      session: {
        status: "ACTIVE",
        cognitiveState: {
          intent: "EVOLVING",
          capability: "MEDIATED",
          direction: "STABLE",
          confidence: 0.95,
          trajectory: ["INIT", "ANALYZE", "MEDIATE"]
        }
      }
    };
    renderSystemState(mockState);

    // Mock evolution progress message
    const mockProgress = {
      sequenceNumber: 1,
      sender: "System",
      timestamp: "12:00",
      agentType: "evolution-progress",
      text: JSON.stringify({
        iterationCount: 2,
        minIterations: 3,
        maxIterations: 10,
        branchingLimit: 5,
        stage: "GENERATE_BRANCH",
        parentId: "ROOT",
        branches: [
          { id: "branch-1", strategy: "Test Strategy", status: "active", score: 0.85 }
        ]
      })
    };
    window.messages = [mockProgress];
    updateTreePanel(window.messages);
  });

  // Take a screenshot of the initial state with stats
  await page.screenshot({ path: 'mediated_ui_stats.png' });
  console.log('Saved mediated_ui_stats.png');

  // 2. Verify double-click interactivity
  // Use a small delay to ensure tree nodes are rendered
  await page.waitForSelector('.tree-node.branch');
  await page.dblclick('.tree-node.branch');

  // Verify popup content
  const popupVisible = await page.isVisible('#popup-panel');
  console.log('Popup visible after dblclick:', popupVisible);

  await page.screenshot({ path: 'mediated_ui_popup.png' });
  console.log('Saved mediated_ui_popup.png');

  await browser.close();
})();
