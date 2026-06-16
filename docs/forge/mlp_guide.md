# Step-by-Step Guide: Creating a Multi-Layer Perceptron (MLP)

MLPs are versatile models consisting of several fully connected layers.

### Step 1: Initialize Session
1. Create a new session named "MLP Experiment".
2. Select **Multi-Layer Perceptron** as the Model Type.

### Step 2: Design the Layers
1. Drag **Layer** nodes from the palette.
2. Typical MLP structure:
   - **Input Layer**: 1 node
   - **Hidden Layer 1**: 1 node (e.g., 64 units)
   - **Hidden Layer 2**: 1 node (e.g., 32 units)
   - **Output Layer**: 1 node (e.g., 10 units for classification)

### Step 3: Connect and Configure
1. Ensure the flow goes from Input -> Hidden -> Output.
2. In the configuration, set **Learning Rate** to `0.001`.

### Step 4: Data Preparation
1. Select the **MNIST (Digits)** dataset for a classic classification task.

### Step 5: Execute Training
1. Click **START**.
2. Monitor the **Accuracy** metric.
3. Use the **Dataset Explorer** to see the samples being processed.

### Step 6: Snapshotting
1. Once satisfied with the results, click **SNAPSHOT** in the sidebar to save a versioned milestone.
