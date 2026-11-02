package com.wingman.sammy.shoppingbot;

import com.wingman.sammy.shoppingbot.domain.ItemStatus;
import org.junit.jupiter.api.Test;
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

import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    public void attachMessageId () {
        repo.addItems(Set.of("eggs", "milk"));
        jdbcTemplate.update("INSERT INTO shopping_items (description, status) values ('milk','PURCHASED')");

        repo.attachMessageId("test-id", "milk");
        List<String> milkId = jdbcTemplate.queryForList(
                "SELECT message_id FROM shopping_items WHERE description = 'milk'",
                String.class
        );
        String milkPurchasedId = jdbcTemplate.queryForObject(
                "SELECT message_id FROM shopping_items WHERE description = 'milk' AND status = 'PURCHASED'",
                String.class
        );
        String milkInProgressId = jdbcTemplate.queryForObject(
                "SELECT message_id FROM shopping_items WHERE description = 'milk' AND status = 'IN_PROGRESS'",
                String.class
        );
        Integer inProgressCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM shopping_items WHERE status = 'IN_PROGRESS'"
                ,int.class
        );
        Integer purchasedCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM shopping_items WHERE status = 'PURCHASED'",
                int.class
        );
        Integer hasIdCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM shopping_items WHERE message_id <> ''",
                int.class
        );
        assertEquals(2, milkId.size());
        assertTrue(milkId.contains(null) && milkId.contains("test-id"));
        assertEquals("test-id", milkInProgressId);
        assertNull(milkPurchasedId);
        assertEquals(1, inProgressCount);
        assertEquals(1, hasIdCount);
        assertEquals(1, purchasedCount);
    }

    @Test
    public void  dBVerification () {
        repo.addItems(Set.of("beef", "rice"));
        repo.attachMessageId("beef-id", "beef");
        repo.attachMessageId("rice-id", "rice");

        repo.setStatus(ItemStatus.PURCHASED, "beef-id");
        String beefStatus = jdbcTemplate.queryForObject("SELECT status FROM shopping_items WHERE message_id = 'beef-id'", String.class);
        String riceStatus = jdbcTemplate.queryForObject("SELECT status FROM shopping_items WHERE message_id = 'rice-id'", String.class);
        assertEquals("PURCHASED", beefStatus);
        assertEquals("IN_PROGRESS", riceStatus);

        repo.setStatus(ItemStatus.IN_PROGRESS, "beef-id");
        beefStatus = jdbcTemplate.queryForObject("SELECT status FROM shopping_items WHERE message_id = 'beef-id'", String.class);
        assertEquals("IN_PROGRESS", beefStatus);
    }
}
