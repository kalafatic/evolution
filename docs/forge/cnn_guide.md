# Step-by-Step Guide: Convolutional Neural Networks (CNN)

CNNs are optimized for spatial data like images.

### Step 1: Session Setup
1. Create a session named "Image Classifier".
2. Set Model Type to **CNN**.

### Step 2: Build Conv-Pool Blocks
1. Drag **Layer** nodes and rename them to represent `Conv2D` and `MaxPooling`.
2. Architecture example:
   - Input (Data)
   - Conv2D (32 filters, 3x3)
   - MaxPool (2x2)
   - Conv2D (64 filters, 3x3)
   - Flatten
   - Dense (128)
   - Output (Softmax)

### Step 3: Dataset Selection
1. Select **CIFAR-10** for color image classification.

### Step 4: Training Monitor
1. High-capacity models like CNNs benefit from **Training Monitor** visualization.
2. Observe the Loss curve for convergence.

### Step 5: Exporting
1. Use the **Evolution Timeline** to select the best performing snapshot.
2. Click **EXPORT FOR OLLAMA** to generate a GGUF/Modelfile compatible version.
