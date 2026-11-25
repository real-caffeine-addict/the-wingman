package com.wingman.sammy.shoppingbot;

import com.wingman.sammy.dto.SavedMedia;
import com.wingman.sammy.dto.StoreDtoMedia;
import com.wingman.sammy.shoppingbot.domain.ItemStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
        jdbcTemplate.update("INSERT INTO shopping_items (description, status) values ('חלב','COLLECTED')");

        repo.addItems(Set.of("חלב"));
        List<String> result = repo.readShoppingList();
        List<String> dbResult = jdbcTemplate.queryForList(
                "SELECT status FROM shopping_items WHERE description = 'חלב'", String.class);

        assertEquals(List.of("חלב"), result);
        assertEquals(2, dbResult.size());
        assertTrue(dbResult.contains("PENDING") && dbResult.contains("COLLECTED"));
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
        jdbcTemplate.update("INSERT INTO shopping_items (description, status) values ('milk','COLLECTED')");

        repo.attachMessageId("test-id", "milk");
        List<String> milkId = jdbcTemplate.queryForList(
                "SELECT message_id FROM shopping_items WHERE description = 'milk'",
                String.class
        );
        String milkPurchasedId = jdbcTemplate.queryForObject(
                "SELECT message_id FROM shopping_items WHERE description = 'milk' AND status = 'COLLECTED'",
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
                "SELECT COUNT(*) FROM shopping_items WHERE status = 'COLLECTED'",
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

        repo.setStatus(ItemStatus.COLLECTED, "beef-id");
        String beefStatus = jdbcTemplate.queryForObject("SELECT status FROM shopping_items WHERE message_id = 'beef-id'", String.class);
        String riceStatus = jdbcTemplate.queryForObject("SELECT status FROM shopping_items WHERE message_id = 'rice-id'", String.class);
        assertEquals("COLLECTED", beefStatus);
        assertEquals("IN_PROGRESS", riceStatus);

        repo.setStatus(ItemStatus.IN_PROGRESS, "beef-id");
        beefStatus = jdbcTemplate.queryForObject("SELECT status FROM shopping_items WHERE message_id = 'beef-id'", String.class);
        assertEquals("IN_PROGRESS", beefStatus);
    }

    @Test
    void insertsMediaDataAndReturnsGeneratedId() {
        LocalDateTime received = LocalDateTime.of(2026, 11, 25, 10, 30);

        StoreDtoMedia media = new StoreDtoMedia(
                "test-recording.ogg",
                "application/ogg",
                5970,
                received,
                "data/media/972509876543",
                "media-message-test",
                "972509876543@s.whatsapp.net",
                "58dd2968769cd2dd42a2ad04fd33ae31fcdb2aa8c7d7570d40c7f588617b7933"
        );

        SavedMedia saved = repo.insertMediaData(media);

        assertNotNull(saved);
        assertNotNull(saved.id());
        assertTrue(saved.id() > 0);
        assertEquals(
                new SavedMedia(saved.id(), media.path(), media.fileName()),
                saved
        );

        StoreDtoMedia stored = jdbcTemplate.queryForObject(
                """
                SELECT file_name, mime_type, byte_size, received_time,
                       path, message_id, sender_id, decrypted_hash
                FROM downloaded_media
                WHERE id = ?
                """,
                (rs, rowNum) -> new StoreDtoMedia(
                        rs.getString("file_name"),
                        rs.getString("mime_type"),
                        rs.getInt("byte_size"),
                        rs.getObject("received_time", LocalDateTime.class),
                        rs.getString("path"),
                        rs.getString("message_id"),
                        rs.getString("sender_id"),
                        rs.getString("decrypted_hash")
                ),
                saved.id()
        );

        assertEquals(media, stored);
    }

    @Test
    void rejectsDuplicateMediaMessageId() {
        LocalDateTime received = LocalDateTime.of(2026, 11, 25, 10, 30);
        String messageId = "duplicate-media-message-test";
        String hash =
                "58dd2968769cd2dd42a2ad04fd33ae31fcdb2aa8c7d7570d40c7f588617b7933";

        StoreDtoMedia first = new StoreDtoMedia(
                "first-recording.ogg",
                "application/ogg",
                5970,
                received,
                "data/media/972509876543",
                messageId,
                "972509876543@s.whatsapp.net",
                hash
        );

        StoreDtoMedia second = new StoreDtoMedia(
                "second-recording.ogg",
                "application/ogg",
                5970,
                received,
                "data/media/972509876543",
                messageId,
                "972509876543@s.whatsapp.net",
                hash
        );

        SavedMedia saved = repo.insertMediaData(first);

        assertThrows(
                DuplicateKeyException.class,
                () -> repo.insertMediaData(second)
        );

        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM downloaded_media WHERE message_id = ?",
                Long.class,
                messageId
        );
        assertEquals(1L, count);

        String storedFileName = jdbcTemplate.queryForObject(
                "SELECT file_name FROM downloaded_media WHERE id = ?",
                String.class,
                saved.id()
        );
        assertEquals(first.fileName(), storedFileName);
    }
}
