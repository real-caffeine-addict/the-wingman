package com.wingman.sammy.shoppingbot;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.message.MessageKey;
import com.github.auties00.cobalt.wire.core.message.MessageKeyBuilder;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfoBuilder;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.linked.message.media.DocumentMessage;
import com.github.auties00.cobalt.wire.linked.message.media.DocumentMessageBuilder;
import com.wingman.sammy.MediaProperties;
import com.wingman.sammy.dto.StoreDtoMedia;
import com.wingman.sammy.utilities.HashHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@Import(ShoppingBotRepository.class)
public class ShoppingBotMediaIntegrationTest {

    @Autowired
    private ShoppingBotRepository repo;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @TempDir
    Path storageDirectory;

    @Test
    void savesReceivedPdfToDiskAndDatabase() throws IOException {
        byte[] content;
        try (InputStream input =
                     getClass().getResourceAsStream("/media/test_pdf.pdf")) {
            content = Objects.requireNonNull(input, "Missing test PDF")
                    .readAllBytes();
        }

        LinkedWhatsAppClient client =
                Mockito.mock(LinkedWhatsAppClient.class);

        MediaProperties properties = new MediaProperties(
                2097152,
                Set.of("application/pdf"),
                storageDirectory
        );

        ShoppingBot bot = new ShoppingBot(
                repo,
                new HashHandler(),
                properties
        );

        Jid sender = Jid.of("972509876543@s.whatsapp.net");
        String messageId = "pdf-integration-test";

        DocumentMessage document = new DocumentMessageBuilder()
                .mediaSize((long) content.length)
                .mimetype("application/pdf")
                .build();

        Mockito.when(client.downloadMedia(document))
                .thenReturn(new ByteArrayInputStream(content));

        MessageKey key = new MessageKeyBuilder()
                .id(messageId)
                .parentJid(Jid.of("120363415109588611@g.us"))
                .fromMe(false)
                .build();

        ChatMessageInfo info = new ChatMessageInfoBuilder()
                .key(key)
                .senderJid(sender)
                .message(LinkedMessageContainer.of(document))
                .build();

        LocalDateTime before = LocalDateTime.now(ZoneOffset.UTC);

        bot.onShoppingBotMessage(client, info);

        LocalDateTime after = LocalDateTime.now(ZoneOffset.UTC);

        StoreDtoMedia stored = jdbcTemplate.queryForObject(
                """
                SELECT file_name, mime_type, byte_size, received_time,
                       path, message_id, sender_id, decrypted_hash
                FROM downloaded_media
                WHERE message_id = ?
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
                messageId
        );

        assertNotNull(stored);
        assertEquals("application/pdf", stored.mimeType());
        assertEquals(content.length, stored.byteSize());
        assertEquals(messageId, stored.messageId());
        assertEquals(sender.toString(), stored.senderJid());
        assertEquals(
                "081c6388d48ca355dc8865af325b35888cedc48c79281375da0ac776490bcf58",
                stored.hash()
        );

        // Small margin for TIMESTAMP precision
        assertFalse(stored.received().isBefore(before.minusSeconds(1)));
        assertFalse(stored.received().isAfter(after.plusSeconds(1)));

        Path expectedDirectory = storageDirectory.resolve(sender.user());
        assertEquals(expectedDirectory, Path.of(stored.path()));

        String fileName = stored.fileName();
        assertTrue(fileName.endsWith(".pdf"));

        String uuidPart = fileName.substring(0, fileName.length() - 4);
        UUID uuid = UUID.fromString(uuidPart);
        assertEquals(uuid.toString(), uuidPart);

        Path savedFile = expectedDirectory.resolve(fileName);
        assertTrue(Files.isRegularFile(savedFile));
        assertArrayEquals(content, Files.readAllBytes(savedFile));

        try (Stream<Path> paths = Files.walk(storageDirectory)) {
            assertEquals(
                    1L,
                    paths.filter(Files::isRegularFile).count()
            );
        }

        Mockito.verify(client).downloadMedia(document);
    }
}