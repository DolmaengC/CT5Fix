from transformers import T5ForConditionalGeneration, AutoTokenizer

checkpoint = "JiyangZhang/CoditT5"

tokenizer = AutoTokenizer.from_pretrained(checkpoint)
model = T5ForConditionalGeneration.from_pretrained(checkpoint)

code_input = """cfa.createEdge(fromNode, Branch.UNCOND, finallyNode);"""
print(code_input)

input_ids = tokenizer(code_input, return_tensors="pt").input_ids
generated_ids = model.generate(input_ids, max_length=200)
print(tokenizer.decode(generated_ids[0], skip_special_tokens=True))
# output: <INSERT>; } } ;<INSERT_END> class HelloWorld { public static void main(String[] args) { System.out.println("Hello, World!") ; } } ;
