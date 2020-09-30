package io.roach.data.jpa.department;

import java.util.List;
import java.util.UUID;

import javax.persistence.*;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import io.roach.data.jpa.support.AbstractJsonDataType;

@Entity
@Table(name = "department")
@TypeDef(name = "jsonb-users", typeClass = Department.UserCollectionJsonType.class, defaultForType = User.class)
public class Department {
    public static class UserCollectionJsonType extends AbstractJsonDataType<User> {
        @Override
        public Class<User> returnedClass() {
            return User.class;
        }

        @Override
        public boolean isCollectionType() {
            return true;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Type(type = "jsonb-users")
    @Column(name = "users")
    @Basic(fetch = FetchType.LAZY)
    private List<User> users;

    public UUID getId() {
        return id;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }
}
