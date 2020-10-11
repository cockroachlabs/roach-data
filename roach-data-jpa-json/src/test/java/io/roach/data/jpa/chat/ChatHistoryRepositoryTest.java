package io.roach.data.jpa.chat;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.roach.data.jpa.AbstractIntegrationTest;

public class ChatHistoryRepositoryTest extends AbstractIntegrationTest {
    @Autowired
    private ChatHistoryRepository chatHistoryRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Commit
    @Order(1)
    public void whenCreatingChatHistory_thenSuccess() throws Exception {
        String data1 = "{\"title\": \"Sleeping Beauties\", \"genres\": [\"Fiction\", \"Thriller\", \"Horror\"], \"published\": false}";
        String data2 = "{\"title\": \"The Dictator''s Handbook\", \"genres\": [\"Law\", \"Politics\"], \"authors\": [\"Bruce Bueno de Mesquita\", \"Alastair Smith\"], \"published\": true}";
        String data3 = "{\"review\": \"A good book\", \"visitor\": \"alice\"}";
        String data4 = "{\"review\": \"A really good book\", \"visitor\": \"bob\"}";

        ChatHistory h1 = new ChatHistory();
        h1.setMessages(Arrays.asList(objectMapper.readTree(data1), objectMapper.readTree(data2)));

        ChatHistory h2 = new ChatHistory();
        h2.setParent(h1);
        h2.setMessages(Collections.singletonList(objectMapper.readTree(data3)));

        ChatHistory h3 = new ChatHistory();
        h3.setParent(h1);
        h3.setMessages(Collections.singletonList(objectMapper.readTree(data4)));

        chatHistoryRepository.saveAll(Arrays.asList(h1, h2, h3));
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Commit
    @Order(2)
    public void whenRetrievingById_thenReturnStuff() {
        List<ChatHistory> chatHistory = chatHistoryRepository.findAll();
        chatHistory.forEach(h -> {
            List<JsonNode> result = h.getMessages();
            result.forEach(jsonNode -> {
                StringWriter w = new StringWriter();
                try {
                    objectMapper.writeValue(w, jsonNode);
                    System.out.println(w);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        });
    }
}
