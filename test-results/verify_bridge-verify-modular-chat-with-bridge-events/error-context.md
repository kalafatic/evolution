# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: verify_bridge.spec.js >> verify modular chat with bridge events
- Location: verify_bridge.spec.js:6:5

# Error details

```
Error: expect(locator).toBeVisible() failed

Locator: locator('.branch-column').first().locator('.branch-btn.approve')
Expected: visible
Timeout: 5000ms
Error: element(s) not found

Call log:
  - Expect "toBeVisible" with timeout 5000ms
  - waiting for locator('.branch-column').first().locator('.branch-btn.approve')

```

# Page snapshot

```yaml
- generic [ref=e2]:
  - generic [ref=e3]:
    - generic [ref=e5]:
      - generic [ref=e6]:
        - generic [ref=e7]:
          - generic [ref=e8]: 👤
          - generic [ref=e9]: You
          - generic [ref=e10]: 12:00
        - generic [ref=e11]:
          - generic [ref=e13] [cursor=pointer]: Hello
          - generic [ref=e14]:
            - button "📋" [ref=e15] [cursor=pointer]
            - button "💬" [ref=e16] [cursor=pointer]
      - generic [ref=e17]:
        - generic [ref=e18]:
          - generic [ref=e19]: 🤖
          - generic [ref=e20]: Darwin
          - generic [ref=e21]: 12:02
        - generic [ref=e23]:
          - generic [ref=e24]:
            - generic [ref=e25]: PROPOSAL 1
            - generic [ref=e26]: A
            - generic [ref=e27]:
              - generic [ref=e28]: "actions:"
              - generic [ref=e30]: • write A
            - strong [ref=e32]: "content:"
            - button "Copy" [ref=e34] [cursor=pointer]
          - generic [ref=e35]:
            - generic [ref=e36]: PROPOSAL 2
            - generic [ref=e37]: B
            - generic [ref=e38]:
              - generic [ref=e39]: "actions:"
              - generic [ref=e41]: • write B
            - strong [ref=e43]: "content:"
            - button "Copy" [ref=e45] [cursor=pointer]
      - generic [ref=e46]:
        - generic [ref=e47]:
          - generic [ref=e48]: 📋
          - generic [ref=e49]: Evo
          - generic [ref=e50]: 12:01
        - generic [ref=e51]:
          - generic [ref=e53] [cursor=pointer]: Waiting for you...
          - generic [ref=e54]:
            - button "📋" [ref=e55] [cursor=pointer]
            - button "💬" [ref=e56] [cursor=pointer]
            - button "✅" [active] [ref=e57] [cursor=pointer]
    - generic [ref=e58]:
      - button "↑" [ref=e59] [cursor=pointer]
      - button "↓" [ref=e60] [cursor=pointer]
  - generic [ref=e61]:
    - generic [ref=e63]:
      - generic [ref=e64]:
        - generic [ref=e65]: CHANGES
        - generic [ref=e66]:
          - button "↕️" [ref=e67] [cursor=pointer]
          - button "🔄" [ref=e68] [cursor=pointer]
          - button "📦" [ref=e69] [cursor=pointer]
          - button "⚖️" [ref=e70] [cursor=pointer]
          - button "✕" [ref=e71] [cursor=pointer]
      - generic [ref=e72]:
        - generic [ref=e73]: 🔍
        - textbox "Search files..." [ref=e74]
    - button "Commit Changes" [ref=e78] [cursor=pointer]
```

# Test source

```ts
  1  |
  2  | import { test, expect } from '@playwright/test';
  3  | import * as fs from 'fs';
  4  | import * as path from 'path';
  5  |
  6  | test('verify modular chat with bridge events', async ({ page }) => {
  7  |     const chatHtmlPath = 'file://' + path.resolve('eu.kalafatic.evolution.view/chat.html');
  8  |
  9  |     // Mock JavaHandler
  10 |     let lastAction = null;
  11 |     let lastIndex = null;
  12 |     let lastPayload = null;
  13 |
  14 |     await page.exposeFunction('JavaHandler', (action, index, payload) => {
  15 |         lastAction = action;
  16 |         lastIndex = index;
  17 |         lastPayload = payload;
  18 |         console.log(`JavaHandler called: ${action}, ${index}, ${payload}`);
  19 |         return null;
  20 |     });
  21 |
  22 |     await page.goto(chatHtmlPath);
  23 |
  24 |     // Initial state with a waiting message and a darwin message
  25 |     const initialState = [
  26 |         { index: 0, sender: 'You', text: 'Hello', agentType: 'user', timestamp: '12:00' },
  27 |         { index: 1, sender: 'Evo', text: 'Waiting for you...', agentType: 'planner waiting', timestamp: '12:01' },
  28 |         { index: 2, sender: 'Darwin', text: '[DARWIN_BRANCHES][{"strategy":"A","actions":["write A"]},{"strategy":"B","actions":["write B"]}]', agentType: 'darwin-branches', timestamp: '12:02' }
  29 |     ];
  30 |
  31 |     await page.evaluate((state) => {
  32 |         window.updateMessages(state);
  33 |     }, initialState);
  34 |
  35 |     // 1. Verify standard approve button
  36 |     const approveBtn = page.locator('.message.planner.waiting .action-btn.approve');
  37 |     await expect(approveBtn).toBeVisible();
  38 |     await approveBtn.click();
  39 |
  40 |     expect(lastAction).toBe('approve');
  41 |     expect(lastIndex).toBe('1');
  42 |
  43 |     // 2. Verify Darwin approve button (Proposal 1)
  44 |     const darwinApproveBtn = page.locator('.branch-column').first().locator('.branch-btn.approve');
> 45 |     await expect(darwinApproveBtn).toBeVisible();
     |                                    ^ Error: expect(locator).toBeVisible() failed
  46 |     await darwinApproveBtn.click();
  47 |
  48 |     expect(lastAction).toBe('approveDarwinVariant');
  49 |     expect(lastIndex).toBe('2');
  50 |     expect(lastPayload).toBe('0'); // First variant
  51 |
  52 |     // 3. Verify Darwin approve button (Proposal 2)
  53 |     const darwinApproveBtn2 = page.locator('.branch-column').nth(1).locator('.branch-btn.approve');
  54 |     await darwinApproveBtn2.click();
  55 |
  56 |     expect(lastAction).toBe('approveDarwinVariant');
  57 |     expect(lastIndex).toBe('2');
  58 |     expect(lastPayload).toBe('1'); // Second variant
  59 |
  60 |     await page.screenshot({ path: '/home/jules/verification/screenshots/bridge_verification.png', fullPage: true });
  61 | });
  62 |
```