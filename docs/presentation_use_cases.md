# Evo Presentation Use Cases

This document outlines the step-by-step use cases for the Evo presentation, including subtitles and voice-over scripts.

---

## 1. Create New Project
**Subtitle:** Bootstrapping an evolutionary workspace.
**Voice Reading:**
"We start by initializing our evolutionary environment. Using the 'New Evo Project' wizard, we define our project identity and target location. Evo automatically applies the 'Evolution Project Nature' and registers the underlying Git repository with the Eclipse EGit view. Notice that by default, the project is configured in Local Mode—prioritizing data sovereignty by keeping your code and AI interactions within your infrastructure. As the wizard completes, the Evo Navigator populates with the project's structural DNA, ready for orchestration."

### Steps:
1. Navigate to **File > New > New Evo Project**.
2. Enter **Project Name** (e.g., 'EvolutionCore') and choose a location.
3. Observe the **Orchestration General** page settings (ID, Name).
4. Click **Finish**.
5. Locate the project in the **Evo Navigator** and notice the specialized **Evolution Nature** icon.
6. Verify the repository is visible in the **Git Repositories** view.

---

## 2. Local Coding Assist Task
**Subtitle:** Cognitive task execution with local models.
**Voice Reading:**
"With our project active, let's execute a development task. First, we'll verify our 'Ollama' configuration in the Properties tab, ensuring our local llama3 model is reachable. Switching to the AiChat tab, we provide a high-level intent: 'Create a StringUtils class with basic formatting methods'. The Intent Expansion Engine instantly deconstructs this into a multi-step implementation plan. Evo then iterates through the generation and verification phases, using the project's compiler to ensure syntactical correctness. Once finished, we provide a 5-star rating, which Neuron immediately uses to train its local memory for future tasks."

### Steps:
1. Open the **Properties** tab and expand **Ollama Settings**. Ensure the status is **Online**.
2. Switch to the **AiChat** tab.
3. Type: `Create a StringUtils class with basic formatting methods`.
4. Press **Send**. Observe the **Intent Expansion** log showing the deconstructed sub-tasks.
5. Watch the **Thinking** phase as code is generated and validated.
6. Click the generated file link to view the code in the **Compare** view.
7. Expand the **Feedback** section at the bottom, select **5 stars**, and enter 'Perfect implementation'.
8. Click **Submit Feedback** and notice the **Neuron** training notification.

---

## 3. Mediated Coding Assist Task
**Subtitle:** Collaborative evolution for high-risk architectural changes.
**Voice Reading:**
"For significant architectural shifts, we employ Mediated Mode. We'll ask Evo to refactor our core EventBus into an asynchronous model. Because of the high EPS score—or evolutionary pressure—the Darwin Engine engages, spawning divergent implementation variants. In the Evolution Tree panel, we can see the lineage of these proposals. We'll double-click a branch to inspect its specific strategy and predicted score. This human-in-the-loop approach ensures that while the AI explores the solution space, the human supervisor maintains ultimate authority over the final architecture."

### Steps:
1. In the **Properties** tab, change the **AI Mode** to **MEDIATED** in the **Orchestrator** section.
2. In **AiChat**, enter: `Refactor the EventBus to support asynchronous execution using a lock-free queue`.
3. Monitor the **Evolution Tree** panel as multiple branch nodes appear.
4. Double-click a branch node (e.g., 'v1.1') to open the **Branch Details** popup.
5. In the chat, review the proposal summary for the selected variant.
6. Click **Approve Variant** on the preferred proposal.
7. Observe the kernel merging the winner and performing a final **Maven Build** verification.

---

## 4. Architecture Visualization
**Subtitle:** Real-time structural awareness.
**Voice Reading:**
"Maintaining a clear mental model of a complex system is vital. Evo's Architecture tab provides a live, interactive map of your project's components. These visual nodes represent the 'hotspots' and artifacts identified by our Reality Discovery Agent. As we evolve the code, a simple refresh keeps our visualization synchronized with the ground truth of the implementation, ensuring architectural integrity is preserved throughout the development lifecycle."

### Steps:
1. Switch to the **Architecture** tab.
2. Interact with the graph: zoom, pan, and hover over nodes to see artifact details.
3. Modify a class in the project (e.g., add a method).
4. Return to the **Architecture** tab and click the **Refresh** button.
5. Observe the visual update reflecting the new structural state.

---

## 5. Forge Models & Neuron
**Subtitle:** Monitoring the cognitive engine and semantic memory.
**Voice Reading:**
"The Forge Models tab is our window into the AI's internal state, showing active sessions and the status of our generative models. This is where we monitor the engine's heartbeat. Simultaneously, Neuron—our semantic memory—is working in the background. In the Context tab, we can see Neuron's learning statistics. As it absorbs our project's patterns and feedback, it begins providing specialized autocomplete suggestions in the chat, effectively evolving into a dedicated domain expert for our codebase."

### Steps:
1. Open the **Forge Models** tab to see the list of active AI sessions.
2. Switch to the **Context** tab and find the **Neuron Context Assist** section.
3. Observe the **Global Stats** and **Local Stats** showing the number of learned behaviors.
4. Go back to **AiChat** and type `/` or a partial keyword to trigger the **Neuron Proposals** in the autocomplete list.

---

## 6. Development & Self-Development
**Subtitle:** Autonomous optimization and pre-flight checks.
**Voice Reading:**
"The Development tab serves as our mission control for autonomy. Here, the Self-Development table allows us to run 'Pre-flight' checks—verifying our Git, Maven, and LLM environments. Notice the color-coding: green indicates success, while orange shows we are ready to proceed. Most impressively, we can trigger the 'Self-Dev Loop'. In this mode, Evo analyzes its own internal kernel, identifies architectural debt or bottlenecks, and proposes optimizations to its own source code, demonstrating the platform's capacity for recursive self-improvement."

### Steps:
1. Open the **Development** tab.
2. Observe the **Self-Development** table with its indexed rows (1-10).
3. Click the **Play** (▶) icon on the **Git Check** row and watch it turn **Green**.
4. Select the **Self-Dev Loop** row.
5. Click **▶ Run Selected** to start the autonomous cycle.
6. Monitor the **Session Status** and **Progress** labels as Evo analyzes its own architecture.

---

## 7. Task Stack
**Subtitle:** Inspecting the hierarchical execution trace.
**Voice Reading:**
"Transparency is a core principle of Evo. The Task Stack tab provides a deep, hierarchical view of the kernel's execution history. We can drill down from high-level orchestration goals into specific agent strategies and individual tool calls. This view is invaluable for understanding the 'how' and 'why' behind every AI action, providing the detailed provenance required for enterprise-grade AI development."

### Steps:
1. Open the **Task Stack** tab.
2. Expand a top-level task node to reveal its constituent sub-tasks.
3. Observe the status icons (Checkmark for success, Spinner for active) and execution timestamps.

---

## 8. Orchestration Graph
**Subtitle:** Navigating the interconnected cognitive network.
**Voice Reading:**
"To visualize the complexity of our AI's reasoning, we use the Graph tab. This Zest-powered visualization maps the relationships between agents, tasks, and system entities like Git repositories and LLM providers. By hovering over connections, we can see the data flow between components, giving us a unique perspective on the interconnected cognitive network that drives our evolutionary process."

### Steps:
1. Open the **Graph** tab.
2. Use the interactive layout to reposition nodes and explore the network.
3. Hover over a connection between an **Agent** and a **Task** to see relationship metadata.
4. Notice how the graph updates dynamically as new tasks are scheduled.

---

## 9. Tools
**Subtitle:** Managing the platform's extensible capability registry.
**Voice Reading:**
"Evo's versatility stems from its extensible tool architecture. In the Tools tab, we manage the 'capabilities' available to our agents. From core utilities like Shell and Git tools to specialized validators for Maven and unit tests, this registry defines what the AI can do. This modular design allows us to easily plug in new capabilities, ensuring the platform can adapt to any technology stack or organizational requirement."

### Steps:
1. Open the **Tools** tab.
2. Review the list of tools, noting their **ID** and **Type**.
3. Verify that core tools like **ShellTool** and **GitTool** are registered and 'Ready'.

---

## 10. Properties
**Subtitle:** Centralized command and control.
**Voice Reading:**
"Finally, the Properties tab is our central command and control center. Here, we manage AI providers, fine-tune model parameters, and monitor system-wide progress. The prominent Mode Indicator at the top provides an at-a-glance status of our active AI mode. Whether we are adjusting Darwinian branching limits or clearing Neuron's memory, this page gives us the granular control needed to steer the platform's evolutionary trajectory."

### Steps:
1. Open the **Properties** tab.
2. Observe the **AI MODE INDICATOR** banner (e.g., 'LOCAL MODE ACTIVE' in Dark Green).
3. Expand **Context Assist** to see real-time Neuron memory statistics.
4. Expand **Orchestrator** to modify the project name or ID.
5. Notice the **Status Canvas** showing the progress of active orchestrations.
