package io.roach.data.json.chat;

import java.util.List;
import java.util.UUID;

import javax.persistence.*;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import com.fasterxml.jackson.databind.JsonNode;

import io.roach.data.json.support.AbstractJsonDataType;

@Entity
@Table(name = "chat_history")
@TypeDef(name = "jsonb-message", typeClass = ChatHistory.StringCollectionJsonType.class)
public class ChatHistory {
    public static class StringCollectionJsonType extends AbstractJsonDataType<JsonNode> {
        @Override
        public Class<JsonNode> returnedClass() {
            return JsonNode.class;
        }

        @Override
        public boolean isCollectionType() {
            return true;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "parent_id")
    private ChatHistory parent;

    @Type(type = "jsonb-message")
    @Column(name = "messages")
    @Basic(fetch = FetchType.LAZY)
    private List<JsonNode> messages;

    public UUID getId() {
        return id;
    }

    public ChatHistory getParent() {
        return parent;
    }

    public void setParent(ChatHistory parent) {
        this.parent = parent;
    }

    public List<JsonNode> getMessages() {
        return messages;
    }

    public void setMessages(List<JsonNode> messages) {
        this.messages = messages;
    }
}
