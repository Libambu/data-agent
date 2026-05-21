-- 在 data_agent 库首次初始化时启用 pgvector 扩展。
-- Spring AI 启动时也会建一次（IF NOT EXISTS），这里提前建是为了
-- 让 database.sql 里 vector(2048) 类型的字段可以被识别。
CREATE EXTENSION IF NOT EXISTS vector;
