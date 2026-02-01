package eu.kalafatic.evolution.view.editors;

import java.io.File;

import com.github.tjake.jlama.model.AbstractModel;
import com.github.tjake.jlama.model.ModelSupport;
import com.github.tjake.jlama.safetensors.DType;
import com.github.tjake.jlama.safetensors.prompt.PromptContext;
import com.github.tjake.jlama.util.Downloader;

public class TestAI {

	public static void main(String[] args) throws Exception {
//      String modelName = "tjake/Llama-3.2-1B-Instruct-JQ4"; // A tiny, fast model
//      String workingDir = "./models";
//
//      // 1. Download model from Hugging Face if not present
//      File modelPath = new Downloader(workingDir, modelName).huggingFaceModel();
      
//      File modelPath = new File("c:\\Users\\petrk\\Documents\\Projects\\AI\\evo\\Qwen3-0.6B.Q2_K.gguf");
      
   // 1. Point to the DIRECTORY, not the .gguf file
      File modelDir = new File("c:\\Users\\petrk\\Documents\\Projects\\AI\\evo");

      // 2. Load the model using the directory
      // Jlama will scan this folder for .gguf or .safetensors and config.json
//      AbstractModel model = ModelSupport.loadModel(modelDir, DType.F32, DType.I8);


      // 2. Load the model into memory
      // DType.I8 uses 8-bit quantization to save RAM
//      AbstractModel model = ModelSupport.loadModel(modelPath, DType.F32, DType.I8);
      
      
		AbstractModel model =ModelSupport.loadModel(modelDir, DType.F32, DType.I8);

      // 3. Create a prompt with a System Message
      PromptContext ctx = model.promptSupport().get().builder()
              .addSystemMessage("You are a helpful assistant.")
              .addUserMessage("Explain what a Java Vector is in one sentence.")
              .build();

      // 4. Generate and print the response
      System.out.print("AI Response: ");
      model.generateBuilder()
              .promptContext(ctx)
              .ntokens(128)      // Max length of response
              .temperature(0.7f) // Creativity (0.0 is robotic, 1.0 is creative)
              // This callback prints tokens as they are generated (streaming)
              .onTokenWithTimings((token, time) -> System.out.print(token))
              .generate();
  }

}
