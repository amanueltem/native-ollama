# Native Ollama (Spring Boot 4 + LangChain4j)

A **Spring Boot 4–compatible, native-friendly Retrieval-Augmented Generation (RAG) project** using **LangChain4j** and **Ollama**, without Spring AI.

This repository demonstrates how to build a **local LLM-powered CV chatbot** that:

* Runs fully **offline** (after models are pulled)
* Uses **Ollama** for chat and embeddings
* Implements **manual RAG** (document loading, chunking, embeddings, retrieval)
* Is future-ready for **GraalVM native images**

> ⚠️ This project intentionally avoids Spring AI because Spring Boot 4 currently breaks Spring AI compatibility.

---

## ✨ What is implemented so far

✅ Spring Boot 4 application (no Spring AI)

✅ Ollama integration via **LangChain4j**

✅ Local embedding model (`nomic-embed-text`)

✅ Local chat model (e.g. `phi`)

✅ Manual document loading from classpath

✅ Manual text chunking with overlap

✅ In-memory vector store

✅ Similarity search over embeddings

✅ Context-grounded prompt (strict CV-based answers)

✅ REST API for chat and RAG-based Q&A

---

## 🧠 Architecture overview

```
cv.txt
  ↓ (chunking + overlap)
TextSegment
  ↓
EmbeddingModel (Ollama)
  ↓
EmbeddingStore (InMemory)
  ↓
Similarity Search
  ↓
Context Injection
  ↓
ChatModel (Ollama)
```

This is a **true RAG pipeline**, not prompt stuffing.

---

## 📂 Project structure

```
src/main/java
├── com.aman.native_ollama
│   ├── chat
│   │   ├── ChatController.java
│   │   └── conf
│   │       └── BeansConf.java
│   └── document
│       └── DocumentLoader.java

src/main/resources
└── Documents
    └── cv.txt
```

---

## ⚙️ Requirements

* **Java 17+** (Java 21 recommended)
* **Spring Boot 4.x**
* **Ollama** installed locally
* At least **8 GB RAM** recommended for local models

---

## 🦙 Ollama setup

Install Ollama:

```
https://ollama.com
```

Pull required models:

```bash
ollama pull phi
ollama pull nomic-embed-text
```

Ensure Ollama is running:

```bash
ollama serve
```

Default endpoint used:

```
http://localhost:11434
```

---

## 🔧 Configuration

All beans are configured manually to stay Spring Boot 4–safe.

### Chat model

```java
@Bean
public ChatModel chatModel() {
    return OllamaChatModel.builder()
            .baseUrl("http://localhost:11434")
            .modelName("phi")
            .temperature(0.7)
            .timeout(Duration.ofSeconds(60))
            .build();
}
```

### Embedding model

```java
@Bean
public EmbeddingModel embeddingModel() {
    return OllamaEmbeddingModel.builder()
            .baseUrl("http://localhost:11434")
            .modelName("nomic-embed-text")
            .timeout(Duration.ofSeconds(60))
            .build();
}
```

### Embedding store

```java
@Bean
public EmbeddingStore<TextSegment> embeddingStore() {
    return new InMemoryEmbeddingStore<>();
}
```

---

## 📄 Document loading

The CV is loaded at startup from:

```
src/main/resources/Documents/cv.txt
```

Key features:

* Manual chunking
* Chunk overlap
* TextSegment-based embeddings

This ensures **high-quality semantic retrieval** for CV-style documents.

---

## 🌐 REST API

### Ask a CV-grounded question (RAG)

```
POST /chat/embed
Content-Type: text/plain
```

Example request:

```text
What programming languages does Amanuel know?
```

Example response:

```text
Java, Spring Boot, and related Java ecosystem technologies.
```

If information is missing:

```text
Not mentioned in the CV.
```

---

## 🚀 Why this project exists

* Spring AI is **not yet stable with Spring Boot 4**
* LangChain4j provides **clean, explicit control**
* Ollama enables **local, private AI**
* Manual RAG gives **full transparency and extensibility**

This repository is meant to evolve into:

* Persistent vector stores (PostgreSQL / Qdrant)
* Conversation memory
* Native-image builds (GraalVM)
* Multi-document ingestion
* UI-based chat interfaces

---

## 🛣️ Planned roadmap

* [ ] Persistent embedding store
* [ ] Re-indexing endpoint
* [ ] Conversation memory
* [ ] GraalVM native image support
* [ ] Document loaders (PDF, DOCX)
* [ ] Authentication & role-based access
* [ ] Web UI (Angular / React)

---

## 🔐 Notes on production usage

* `InMemoryEmbeddingStore` is **not production-ready**
* Load documents lazily or via admin endpoints
* Apply similarity score thresholds
* Use structured chunking for large documents

---

## 👤 Author

**Amanuel Temesgen**
Java & Spring Boot Engineer
Focused on native Java, AI integration, and enterprise systems

---

## 📜 License

This project is open for learning and experimentation.
License to be added.
