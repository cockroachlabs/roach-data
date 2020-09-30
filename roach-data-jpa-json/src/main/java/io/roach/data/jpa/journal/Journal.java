package io.roach.data.jpa.journal;

import java.time.LocalDateTime;

import javax.persistence.*;

import org.hibernate.annotations.Type;

@Entity
@Table(name = "journal")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(
        name = "event_type",
        discriminatorType = DiscriminatorType.STRING,
        length = 15
)
public abstract class Journal<T> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Computed column
    private String id;

    @Column(name = "tag", updatable = false)
    private String tag;

    @Basic
    @Column(name = "updated", updatable = false)
    private LocalDateTime updated;

    @Type(type = "jsonb")
    @Column(name = "payload", updatable = false)
    @Basic(fetch = FetchType.LAZY)
    private T event;

    @PrePersist
    protected void onCreate() {
        if (updated == null) {
            updated = LocalDateTime.now();
        }
    }

    public String getId() {
        return id;
    }

    public T getEvent() {
        return event;
    }

    public void setEvent(T payload) {
        this.event = payload;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String origin) {
        this.tag = origin;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }
}
