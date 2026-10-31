package com.wingman.sammy.shoppingbot;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@JdbcTest
@Import(ShoppingBotRepository.class)
public class ShoppingBotRepositoryTest {
    @Autowired
    private ShoppingBotRepository repo;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void listWithDuplicate () {
        Set<String> firstMessage = Set.of("חלב", "ביצים");
        Set<String> secondMessage = Set.of("בננות", "חלב", "יוגורט");

        repo.addItems(firstMessage);
        repo.addItems(secondMessage);
        List<String> result = repo.readShoppingList();

        assertEquals(4, result.size());
        assertEquals(Set.of("חלב", "ביצים", "בננות", "יוגורט"), Set.copyOf(result));
    }

    @Test
    public void ignorePurchasedWhileInserting () {
        jdbcTemplate.update("INSERT INTO shopping_items (description, status) values ('חלב','PURCHASED')");

        repo.addItems(Set.of("חלב"));
        List<String> result = repo.readShoppingList();
        List<String> dbResult = jdbcTemplate.queryForList(
                "SELECT status FROM shopping_items WHERE description = 'חלב'", String.class);

        assertEquals(List.of("חלב"), result);
        assertEquals(2, dbResult.size());
        assertTrue(dbResult.contains("PENDING") && dbResult.contains("PURCHASED"));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void rollbackOnDbException () {
        String overflow = "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz";
        LinkedHashSet<String> msg = new LinkedHashSet<>();
        msg.addFirst("ביצים");
        msg.addLast(overflow);

        assertThrows(DataIntegrityViolationException.class, () -> repo.addItems(msg));

        List<String> reading = repo.readShoppingList();
        assertTrue(reading.isEmpty());
    }
}
