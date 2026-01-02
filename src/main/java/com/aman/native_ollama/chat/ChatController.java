package com.aman.native_ollama.chat;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/chat")
public class ChatController {
    private final ChatModel chatModel;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    public ChatController(ChatModel chatModel
            , EmbeddingModel embeddingModel
            , EmbeddingStore<TextSegment> embeddingStore) {
        this.chatModel = chatModel;
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
    }

    @PostMapping
    public ResponseEntity<String> getResponse(@RequestBody String ask) {
        return ResponseEntity.ok(chatModel.chat(ask));
    }

    @PostMapping("/embed")
    public ResponseEntity<String> getEmbeddingResponse(@RequestBody String ask) {

        // 1. Embed the user question
        Embedding queryEmbedding = embeddingModel.embed(ask).content();

        // 2. Search vector store (retrieve MORE than one chunk)
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(3)
                .build();

        EmbeddingSearchResult<TextSegment> result =
                embeddingStore.search(request);

        List<EmbeddingMatch<TextSegment>> matches = result.matches();

        if (matches.isEmpty()) {
            return ResponseEntity.ok("I could not find relevant information in the CV.");
        }

        // 3. Build context from retrieved chunks
        String context = matches.stream()
                .map(m -> m.embedded().text())
                .reduce("", (a, b) -> a + "\n\n" + b);

        // 4. Ask the chat model to answer USING the context
        String prompt = """
                You are answering questions strictly based on Amanuel Temesgen's CV.
                
                Use ONLY the information provided below.
                If the answer is not present, say "Not mentioned in the CV".
                
                Context:
                %s
                
                Question:
                %s
                
                Answer concisely.
                """.formatted(context, ask);

        String answer = chatModel.chat(prompt);

        return ResponseEntity.ok(answer);
    }


    @PostMapping("/rag-chat")
    public ResponseEntity<String> getGeneralRagResponse(@RequestBody String ask) {
        Embedding queryEmbedding = embeddingModel.embed(ask).content();
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(4)
                .build();

        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);
        List<EmbeddingMatch<TextSegment>> matches = result.matches();
        if (matches.isEmpty()) {
            return ResponseEntity.ok("I don't have any documents in my knowledge base to answer that yet.");
        }
        String context = matches.stream()
                .map(m -> m.embedded().text())
                .collect(Collectors.joining("\n\n"));
        String prompt = """
        You are a helpful assistant. Use the provided context to answer the question.
        
        Guidelines:
        - If the context doesn't contain the answer, say you don't know based on the documents.
        - Be professional and concise.
        
        Context:
        %s
        
        Question:
        %s
        """.formatted(context, ask);
        return ResponseEntity.ok(chatModel.chat(prompt));
    }


}
