package io.roach.data.jpa.chat;

import java.util.List;
import java.util.UUID;

import javax.persistence.*;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import io.roach.data.jpa.support.AbstractJsonDataType;

@Entity
@Table(name = "chat_history")
@TypeDef(name = "jsonb-message", typeClass = ChatHistory.StringCollectionJsonType.class)
public class ChatHistory {
    public static class StringCollectionJsonType extends AbstractJsonDataType<String> {
        @Override
        public Class<String> returnedClass() {
            return String.class;
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
    private List<String> messages;

    public UUID getId() {
        return id;
    }

    public ChatHistory getParent() {
        return parent;
    }

    public void setParent(ChatHistory parent) {
        this.parent = parent;
    }

    public List<String> getMessages() {
        return messages;
    }

    public void setMessages(List<String> messages) {
        this.messages = messages;
    }
}
