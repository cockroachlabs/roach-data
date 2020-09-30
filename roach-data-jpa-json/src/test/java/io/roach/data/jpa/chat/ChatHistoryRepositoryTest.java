package io.roach.data.jpa.chat;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import io.roach.data.jpa.AbstractIntegrationTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ChatHistoryRepositoryTest extends AbstractIntegrationTest {
    @Autowired
    private ChatHistoryRepository chatHistoryRepository;

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Commit
    @Order(1)
    public void whenCreatingChatHistory_thenSuccess() {
        ChatHistory l1 = new ChatHistory();
        l1.setMessages(Arrays.asList("hello", "world"));

        ChatHistory l2a = new ChatHistory();
        l2a.setParent(l1);
        l2a.setMessages(Arrays.asList("hello from", "alice"));

        ChatHistory l2b = new ChatHistory();
        l2b.setParent(l1);
        l2b.setMessages(Arrays.asList("hello from", "bob"));

        ChatHistory l3 = new ChatHistory();
        l3.setParent(l2a);
        l3.setMessages(Arrays.asList("hello from", "bobby tables"));

        chatHistoryRepository.save(l1);
        chatHistoryRepository.save(l2a);
        chatHistoryRepository.save(l2b);
        chatHistoryRepository.save(l3);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Commit
    @Order(2)
    public void whenRetrievingById_thenReturnStuff() {
        List<ChatHistory> chatHistory = chatHistoryRepository.findAll();
        chatHistory.forEach(h -> {
            List<String> result = h.getMessages();
            assertEquals(2, result.size());
        });
    }
}
