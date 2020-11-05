package io.roach.data.json.department;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import io.roach.data.json.AbstractIntegrationTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DepartmentRepositoryTest extends AbstractIntegrationTest {
    @Autowired
    private DepartmentRepository departmentRepository;

    private UUID id;

    public static byte[] hashSecret(String password) {
        try {
            byte[] salt = new byte[16];
            SecureRandom random = new SecureRandom();
            random.nextBytes(salt);
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            md.update(salt);
            return md.digest(password.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Commit
    @Order(1)
    public void whenCreatingDepartmentUser_thenSuccess() {
        User customer = new User();
        customer.setUserName("alice");
        customer.setPassword(hashSecret("password"));
        customer.setFirstName("Alice");
        customer.setLastName("Alison");
        customer.setTelephone("555-5555");
        customer.setEmail("alice@roaches.io");

        Address address1 = new Address.Builder()
                .setAddress1("street1")
                .setAddress2("street2")
                .setCity("street2")
                .setCountry(Locale.US.getCountry())
                .setPostcode("street2").build();
        customer.setAddress(address1);

        Department department = new Department();
        department.setUsers(Collections.singletonList(customer));
        department = departmentRepository.save(department);

        assertNotNull(department);
        assertNotNull(department.getId());

        id = department.getId();
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Commit
    @Order(2)
    public void whenRetrievingById_thenReturnAlice() {
        Department department = departmentRepository.getOne(id);
        List<User> result = department.getUsers();
        assertTrue(result.size() > 0);
        assertTrue(result.stream()
                .map(User::getUserName)
                .anyMatch(name -> Objects.equals("alice", name)));
    }
}
