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

@RestController
@RequestMapping("/chat")
public class ChatController {
private final ChatModel chatModel;
private final EmbeddingModel embeddingModel;
private final EmbeddingStore<TextSegment> embeddingStore;
public ChatController(ChatModel chatModel
        , EmbeddingModel embeddingModel
        , EmbeddingStore<TextSegment> embeddingStore){
    this.chatModel=chatModel;
    this.embeddingModel=embeddingModel;
    this.embeddingStore=embeddingStore;
}
@PostMapping
public ResponseEntity<String> getResponse(@RequestBody String ask){
    return ResponseEntity.ok(chatModel.chat(ask));
}
    @PostMapping("/embed")
    public ResponseEntity<String> getEmbeddingResponse(@RequestBody String ask) {

        // Knowledge base (demo only — load once in real apps)
        TextSegment info1 = TextSegment.from(
                "The password for the secret lab is: JAVA_ROCKS_2025"
        );
        TextSegment info2 = TextSegment.from(
                "The Satellite L855 laptop was released around 2012."
        );

        embeddingStore.add(embeddingModel.embed(info1).content(), info1);
        embeddingStore.add(embeddingModel.embed(info2).content(), info2);

        // Create query embedding
        Embedding queryEmbedding = embeddingModel.embed(ask).content();

        // Build search request (NEW API)
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(1)
                .build();

        // Perform search
        EmbeddingSearchResult<TextSegment> result =
                embeddingStore.search(request);

        List<EmbeddingMatch<TextSegment>> matches = result.matches();

        if (matches.isEmpty()) {
            return ResponseEntity.ok("No relevant information found.");
        }

        EmbeddingMatch<TextSegment> bestMatch = matches.get(0);

        return ResponseEntity.ok(
                "Answer: " + bestMatch.embedded().text() +
                        "\nSimilarity Score: " + bestMatch.score()
        );
    }

}
