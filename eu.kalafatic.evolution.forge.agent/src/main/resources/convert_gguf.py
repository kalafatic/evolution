import argparse
from unsloth import FastLanguageModel

def convert_to_gguf(adapter_path, output_file):
    model, tokenizer = FastLanguageModel.from_pretrained(
        model_name = adapter_path,
        max_seq_length = 2048,
        load_in_4bit = True,
    )

    print(f"Exporting to {output_file}...")
    model.save_pretrained_gguf(output_file, tokenizer, quantization_method = "q4_k_m")

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--adapter-path", type=str, required=True)
    parser.add_argument("--output-file", type=str, required=True)
    args = parser.parse_args()
    convert_to_gguf(args.adapter_path, args.output_file)
