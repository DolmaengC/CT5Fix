from transformers import T5ForConditionalGeneration, T5Tokenizer, Trainer, TrainingArguments
import torch

# 데이터 수집 및 전처리 (예제에서는 간단한 데이터 생성)
train_dataset = [
    {"input": "Translate the following English sentence to French: Hello!", "target": "Bonjour!"},
    {"input": "Translate the following English sentence to French: How are you?", "target": "Comment ça va?"}
]

# 모델 및 토크나이저 로드
model_name = "JiyangZhang/CoditT5"
model = T5ForConditionalGeneration.from_pretrained(model_name)
tokenizer = T5Tokenizer.from_pretrained(model_name)

# 모델 아키텍처 변경 및 초기화
model.resize_token_embeddings(len(tokenizer))

# Fine-tuning 수행을 위한 Trainer 및 TrainingArguments 설정
training_args = TrainingArguments(
    output_dir="./t5_finetuned",
    overwrite_output_dir=True,
    num_train_epochs=3,
    per_device_train_batch_size=2,
    save_steps=10_000,
    save_total_limit=2,
)

trainer = Trainer(
    model=model,
    args=training_args,
    train_dataset=train_dataset,
)

# Fine-tuning 수행
trainer.train()

# 모델 저장 (선택 사항)
model.save_pretrained("./model/t5_finetuned")
