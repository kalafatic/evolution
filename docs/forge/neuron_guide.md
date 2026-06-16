# Step-by-Step Guide: Creating a Single Neuron Model

This guide explains how to create a basic Single Neuron model in the Forge UI.

### Step 1: Create a New Session
1. Click the **NEW** button in the SESSIONS sidebar.
2. Enter a name like "My First Neuron".
3. The session will appear in the list. Click it to select.

### Step 2: Configure Model Type
1. In the **Configuration** panel on the right, ensure **Model Type** is set to **Single Neuron**.

### Step 3: Build the Architecture
1. In the **DRAG NODES** palette, locate the **Neuron** button.
2. Drag one **Neuron** onto the canvas to represent the input.
3. Drag another **Neuron** onto the canvas to represent the bias (optional but recommended).
4. Drag a third **Neuron** to represent the output.
5. Currently, the UI automatically handles basic connections in some modes, but you can manually verify them in the **JSON Editor** if needed.

### Step 4: Configure Training Parameters
1. Set the **Learning Rate** to `0.01`.
2. Set the **Batch Size** to `1` (since it's a simple neuron).
3. Select **SGD** or **Adam** as the Optimizer.

### Step 5: Select Dataset
1. In the **Dataset** panel, select **Synthetic Generator**.
2. This will provide simple linear data for the neuron to learn.

### Step 6: Start Training
1. Click the **START** button in the Training & Execution panel.
2. Observe the **Loss** decreasing in the Observability panel.
3. Switch the Visualization to **Training Monitor** to see real-time performance.

### Step 7: Save Your Work
1. Click **Save Architecture** in the top toolbar to persist your model structure.
