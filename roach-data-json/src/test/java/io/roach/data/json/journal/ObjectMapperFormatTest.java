package io.roach.data.json.journal;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class ObjectMapperFormatTest {
    @Test
    public void testSerialisation() {
        Transaction transaction = Transaction.builder()
                .withGeneratedId()
                .withBookingDate(LocalDate.now().minusDays(2))
                .withTransferDate(LocalDate.now())
                .andItem()
                .withAccount(Account.builder()
                        .withGeneratedId()
                        .build())
                .withAmount(BigDecimal.valueOf(-50.00))
                .withNote("debit A")
                .then()
                .andItem()
                .withAccount(Account.builder()
                        .withGeneratedId()
                        .build())
                .withAmount(BigDecimal.valueOf(50.00))
                .withNote("credit A")
                .then()
                .build();

        ObjectMapper mapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
        try {
            String s = mapper.writeValueAsString(transaction);
            System.out.println(s);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}
