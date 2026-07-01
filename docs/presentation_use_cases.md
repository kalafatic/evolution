# Evo Presentation Use Cases

This document outlines the step-by-step use cases for the Evo presentation, including subtitles and voice-over scripts.

---

## 1. Create New Project
**Subtitle:** Setting up your first evolutionary project.
**Voice Reading:**
"To begin, we'll create a new Evo project. Open the 'New Evo Project' wizard from the menu. Give your project a name and specify its location. By default, Evo starts in Local Mode, utilizing your on-premise models for maximum privacy and speed. Once complete, the system automatically registers the repository and initializes the evolutionary kernel, which you can see appearing in the Evo Navigator."

### Steps:
1. Navigate to **File > New > New Evo Project**.
2. Enter **Project Name** (e.g., 'EvoDemo').
3. Verify **Local Mode** is selected in the wizard.
4. Click **Finish** and observe the project appearing in the **Evo Navigator**.

---

## 2. Local Coding Assist Task
**Subtitle:** Seamless coding assistance with local AI.
**Voice Reading:**
"Now let's start a coding task. First, we'll verify our local model selection in the Properties page. Back in the AiChat page, we provide a simple intent, like 'Create a StringUtils class with basic formatting methods'. Notice how the Intent Expansion Engine analyzes the request and automatically breaks it down. Evo iterates through the plan, generates the source code, and verifies it against the project's compiler. After execution, we can provide feedback using the satisfaction stars, allowing Neuron to learn from this interaction."

### Steps:
1. Go to **Properties** and ensure the **Local Model** (e.g., 'llama3') is selected.
2. Open the **AiChat** page.
3. Type: `Create a StringUtils class with basic formatting methods`.
4. Press **Send** and watch the **Thinking** indicator and **Intent Expansion** log.
5. Review the generated code in the **Compare** or **Diff** view.
6. Rate the result using the **Feedback** stars (1-5).

---

## 3. Mediated Coding Assist Task
**Subtitle:** High-impact architectural evolution with human-in-the-loop.
**Voice Reading:**
"For complex architectural changes, we use Mediated Mode. We'll ask Evo to refactor our event bus to be fully asynchronous. Because this is a high-risk change, the Darwin Engine spawns multiple variant branches. We can inspect each proposal, compare their strategies, and select the most robust implementation. This ensures that the human supervisor remains the ultimate authority for critical technical decisions."

### Steps:
1. Go to **Properties** and switch **AI Mode** to **MEDIATED**.
2. In **AiChat**, enter: `Refactor the EventBus to support asynchronous execution`.
3. Observe the **Darwinian Branching** in the chat or tree panel.
4. Select a variant (e.g., 'Variant 1.1') and click **Approve**.
5. Wait for the final **Verification** and merge.

---

## 4. Architecture Visualization
**Subtitle:** Real-time visibility into your system's components.
**Voice Reading:**
"Understanding a complex codebase is easy with Evo's Architecture view. Here, we see a live visualization of our components and their relationships. As we modify the code, we can trigger a refresh to see the architecture update instantly. This ensures our mental model always stays in sync with the actual implementation."

### Steps:
1. Open the **Architecture** tab in the editor.
2. Explore the visual nodes representing classes and modules.
3. Click the **Refresh** icon to synchronize with the latest file changes.

---

## 5. Forge Interactive & Neuron
**Subtitle:** Monitoring the engine and leveraging learned context.
**Voice Reading:**
"The Forge page provides an interactive look at the evolutionary engine's heartbeat. You can monitor active agents and the real-time task progress. Complementing this is Neuron, our semantic memory system. Neuron learns from every task and feedback loop, offering context-aware suggestions as you type in the chat, effectively growing as a specialized expert for your specific project."

### Steps:
1. Switch to the **Forge** page to see the active task network.
2. Open the **Context** page to view the **Neuron Context (Learned Behavior)**.
3. Start typing in **AiChat** to see **Neuron Proposals** appearing in the autocomplete (Ctrl+Space).

---

## 6. Development & Self-Development
**Subtitle:** Autonomous self-improvement and project bootstrapping.
**Voice Reading:**
"The Development page is where Evo's autonomy truly shines. Here, we can run pre-flight checks like Git and Maven validations, or perform advanced sandbox actions like building the project and exporting artifacts. Most importantly, this is where we initiate the 'Self-Dev Loop', where Evo analyzes its own architecture and proposes optimizations to its internal kernel."

### Steps:
1. Open the **Development** page.
2. Review the **Self-Development** table and run the **Git Check** or **Maven Check**.
3. Observe the color-coded status (Green for success, Red for errors).
4. Click **Run Selected** on the **Self-Dev Loop** row to start an autonomous evolution cycle.

---

## 7. Task Stack
**Subtitle:** Deep inspection of the orchestration pipeline.
**Voice Reading:**
"The Task Stack view provides a granular, hierarchical look at every operation performed by the kernel. From high-level strategies down to individual tool calls, you can trace exactly how Evo is reasoning and executing. This transparency is crucial for debugging complex refactoring tasks and understanding the system's decision-making process."

### Steps:
1. Open the **Task Stack** view.
2. Expand the orchestration nodes to see the sub-tasks and their execution status.
3. Observe the real-time updates as tasks transition from 'Scheduled' to 'Completed'.

---

## 8. Orchestration Graph
**Subtitle:** Visualizing the cognitive network.
**Voice Reading:**
"The Graph view offers a Zest-powered visualization of the entire orchestration network. It shows the relationships between agents, tasks, and system components like the Git repository and LLM providers. This bird's-eye view helps supervisors understand the interconnected nature of the evolutionary process."

### Steps:
1. Open the **Orchestration Graph** view.
2. Use the interactive layout to see how agents (like 'Planner' or 'Analytic') connect to specific tasks.
3. Zoom and pan to explore the complexity of the current session's cognitive state.

---

## 9. Tools
**Subtitle:** Managing the platform's extensible capabilities.
**Voice Reading:**
"Evo's power comes from its extensive set of tools. In the Tools page, you can see all registered capabilities, from terminal execution and file management to specialized agents for testing and review. This extensible architecture allows Evo to adapt to any development environment and technology stack."

### Steps:
1. Open the **Tools** page in the editor.
2. Browse the list of available tools and their categories.
3. Verify that core tools like **ShellTool**, **GitTool**, and **MavenTool** are ready.

---

## 10. Properties
**Subtitle:** Centralized configuration and mode management.
**Voice Reading:**
"Finally, the Properties page is your command center for platform configuration. Here, you can manage AI providers, select models, and adjust Darwinian branching limits. The Mode Indicator provides instant feedback on whether you are running in Local, Hybrid, or Remote mode, ensuring you always have the right balance of performance and intelligence."

### Steps:
1. Open the **Properties** page.
2. Check the **AI Mode Indicator** at the top.
3. Expand sections like **Ollama Settings** or **Darwin Engine** to fine-tune the platform's behavior.
4. Click **Save** to persist your changes across sessions.
