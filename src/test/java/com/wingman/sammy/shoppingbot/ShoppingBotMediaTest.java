package com.wingman.sammy.shoppingbot;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.message.MessageKey;
import com.github.auties00.cobalt.wire.core.message.MessageKeyBuilder;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfoBuilder;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.linked.message.media.*;
import com.wingman.sammy.MediaProperties;
import com.wingman.sammy.dto.StoreDtoMedia;
import com.wingman.sammy.exceptions.InvalidMediaException;
import com.wingman.sammy.utilities.HashHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class ShoppingBotMediaTest {
    LinkedWhatsAppClient client = Mockito.mock(LinkedWhatsAppClient.class);
    ShoppingBotRepository repo = Mockito.mock(ShoppingBotRepository.class);

    @TempDir
    Path storageDirectory;

    private MediaProperties mediaProperties;
    private HashHandler hashHandler;
    private ShoppingBot bot;
    private final Jid sender = Jid.of("972509876543@s.whatsapp.net");

    byte[] audio;
    byte[] image;
    byte[] pdf;

    private void loadAudioHelper () {
        try (InputStream input = getClass().getResourceAsStream("/media/test_recording.ogg")) {
            audio = Objects.requireNonNull(input, "Missing test recording")
                    .readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("IOException thrown while loading resource\n" + e);
        }
    }
    private void loadImageHelper () {
        try (InputStream input = getClass().getResourceAsStream("/media/test_image.jpeg")) {
            image = Objects.requireNonNull(input, "Missing test image")
                    .readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("IOException thrown while loading resource\n" + e);
        }
    }
    private void loadDocumentHelper () {
        try (InputStream input = getClass().getResourceAsStream("/media/test_pdf.pdf")) {
            pdf = Objects.requireNonNull(input, "Missing test document")
                    .readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("IOException thrown while loading resource\n" + e);
        }
    }

    private ChatMessageInfo mediaInfo (MediaMessage media, String messageId) {
        MessageKey key = new MessageKeyBuilder()
                .id(messageId)
                .parentJid(Jid.of("120363415109588611@g.us"))
                .fromMe(false)
                .build();

        return new ChatMessageInfoBuilder()
                .key(key)
                .senderJid(sender)
                .message(LinkedMessageContainer.of(media))
                .build();
    }

    private void assertStorageIsEmpty() throws IOException {
        try (Stream<Path> files = Files.list(storageDirectory)) {
            assertEquals(0L, files.count());
        }
    }

    @BeforeEach
    void setUp() {
        mediaProperties = new MediaProperties(
                2097152,
                Set.of("image/jpeg", "application/pdf", "application/ogg"),
                storageDirectory
        );
        hashHandler = new HashHandler();
        bot = new ShoppingBot(repo, hashHandler, mediaProperties);
    }

    @Test
    public void audioDownloadHappyPath () throws IOException {
        loadAudioHelper();
        AudioMessage audioMessage = new  AudioMessageBuilder()
                .mediaSize((long) audio.length)
                .mimetype("audio/ogg; codecs=opus")
                .ptt(true)
                .build();

        Mockito.when(client.downloadMedia(audioMessage))
                .thenReturn(new ByteArrayInputStream(audio));

        ChatMessageInfo info = mediaInfo(audioMessage, "audio-message-test");

        bot.onShoppingBotMessage(client, info);

        ArgumentCaptor<StoreDtoMedia> captor =
                ArgumentCaptor.forClass(StoreDtoMedia.class);

        Mockito.verify(repo).insertMediaData(captor.capture());

        StoreDtoMedia stored = captor.getValue();

        Path savedFile = Path.of(stored.path()).resolve(stored.fileName());

        assertTrue(Files.isRegularFile(savedFile));
        assertArrayEquals(audio, Files.readAllBytes(savedFile));
        assertEquals(
                "58dd2968769cd2dd42a2ad04fd33ae31fcdb2aa8c7d7570d40c7f588617b7933",
                stored.hash()
        );
        assertEquals("application/ogg", stored.mimeType());
        assertEquals(audio.length, stored.byteSize());
        assertEquals("audio-message-test", stored.messageId());
        assertEquals(sender.toString(), stored.senderJid());
        assertEquals(
                storageDirectory.resolve(sender.user()),
                Path.of(stored.path())
        );
    }

    @Test
    public void imageDownloadHappyPath () throws IOException {
        loadImageHelper();
        ImageMessage imageMessage =  new ImageMessageBuilder()
                .mediaSize((long) image.length)
                .mimetype("image/jpeg")
                .build();
        Mockito.when(client.downloadMedia(imageMessage))
                .thenReturn(new ByteArrayInputStream(image));

        ChatMessageInfo info = mediaInfo(imageMessage, "image-message-test");

        bot.onShoppingBotMessage(client, info);

        ArgumentCaptor<StoreDtoMedia> captor =
                ArgumentCaptor.forClass(StoreDtoMedia.class);

        Mockito.verify(repo).insertMediaData(captor.capture());

        StoreDtoMedia stored = captor.getValue();

        Path savedFile = Path.of(stored.path()).resolve(stored.fileName());

        assertTrue(Files.isRegularFile(savedFile));
        assertArrayEquals(image, Files.readAllBytes(savedFile));
        assertEquals(
                "4feb63cdb29b4c5d8738fb9a408d9900c81c4c1846777a119bfc53eca4846d50",
                stored.hash()
        );
        assertEquals("image/jpeg", stored.mimeType());
        assertEquals(image.length, stored.byteSize());
        assertEquals("image-message-test", stored.messageId());
        assertEquals(sender.toString(), stored.senderJid());
        assertEquals(
                storageDirectory.resolve(sender.user()),
                Path.of(stored.path())
        );
    }

    @Test
    void documentDownloadHappyPath() throws IOException {
        loadDocumentHelper();

        DocumentMessage document = new DocumentMessageBuilder()
                .mediaSize((long) pdf.length)
                .mimetype("application/pdf")
                .build();

        Mockito.when(client.downloadMedia(document))
                .thenReturn(new ByteArrayInputStream(pdf));

        bot.onShoppingBotMessage(client, mediaInfo(document, "pdf-message-test"));

        ArgumentCaptor<StoreDtoMedia> captor =
                ArgumentCaptor.forClass(StoreDtoMedia.class);
        Mockito.verify(repo).insertMediaData(captor.capture());

        StoreDtoMedia stored = captor.getValue();
        Path expectedDirectory = storageDirectory.resolve(sender.user());
        Path savedFile = Path.of(stored.path()).resolve(stored.fileName());

        assertEquals("application/pdf", stored.mimeType());
        assertEquals(pdf.length, stored.byteSize());
        assertEquals("pdf-message-test", stored.messageId());
        assertEquals(sender.toString(), stored.senderJid());
        assertEquals(expectedDirectory, Path.of(stored.path()));
        assertTrue(stored.fileName().endsWith(".pdf"));

        assertEquals(
                "081c6388d48ca355dc8865af325b35888cedc48c79281375da0ac776490bcf58",
                stored.hash()
        );
        assertTrue(Files.isRegularFile(savedFile));
        assertArrayEquals(pdf, Files.readAllBytes(savedFile));
    }

    @Test
    void rejectsMediaWithDeclaredSizeAboveLimit() throws IOException {
        AudioMessage audioMessage = new AudioMessageBuilder()
                .mediaSize((long) mediaProperties.maxSizeBytes() + 1)
                .mimetype("audio/ogg; codecs=opus")
                .ptt(true)
                .build();

        ChatMessageInfo info = mediaInfo(audioMessage, "oversized-audio-test");

        assertThrows(
                InvalidMediaException.class,
                () -> bot.onShoppingBotMessage(client, info)
        );

        Mockito.verify(client, Mockito.never())
                .downloadMedia(Mockito.any());
        Mockito.verifyNoInteractions(repo);

        assertStorageIsEmpty();
    }

    @Test
    void rejectsMediaWithActualSizeAboveLimit() throws IOException {
        byte[] oversizedContent = new byte[mediaProperties.maxSizeBytes() + 1];

        AudioMessage audioMessage = new AudioMessageBuilder()
                .mediaSize(100L)
                .mimetype("audio/ogg; codecs=opus")
                .ptt(true)
                .build();

        Mockito.when(client.downloadMedia(audioMessage))
                .thenReturn(new ByteArrayInputStream(oversizedContent));

        ChatMessageInfo info = mediaInfo(audioMessage, "actual-oversized-audio-test");

        assertThrows(
                InvalidMediaException.class,
                () -> bot.onShoppingBotMessage(client, info)
        );

        Mockito.verify(client).downloadMedia(audioMessage);
        Mockito.verifyNoInteractions(repo);

        assertStorageIsEmpty();
    }

    @Test
    void rejectsMediaWithUnsupportedDetectedMimeType() throws IOException {
        byte[] content = "This is plain text, not a JPEG."
                .getBytes(StandardCharsets.UTF_8);

        ImageMessage imageMessage = new ImageMessageBuilder()
                .mediaSize((long) content.length)
                .mimetype("image/jpeg")
                .build();

        Mockito.when(client.downloadMedia(imageMessage))
                .thenReturn(new ByteArrayInputStream(content));

        ChatMessageInfo info = mediaInfo(imageMessage, "unsupported-media-test");

        assertThrows(
                InvalidMediaException.class,
                () -> bot.onShoppingBotMessage(client, info)
        );

        Mockito.verify(client).downloadMedia(imageMessage);
        Mockito.verifyNoInteractions(repo);

        assertStorageIsEmpty();
    }

    @Test
    void rejectsMediaWithMissingDeclaredSize() throws IOException {
        AudioMessage media = new AudioMessageBuilder()
                .mimetype("audio/ogg; codecs=opus")
                .build();

        ChatMessageInfo info = mediaInfo(media, "missing-size-test");

        assertThrows(
                InvalidMediaException.class,
                () -> bot.onShoppingBotMessage(client, info)
        );

        Mockito.verifyNoInteractions(client, repo);
        assertStorageIsEmpty();
    }

    @Test
    void rejectsMediaWithNegativeDeclaredSize() throws IOException {
        AudioMessage media = new AudioMessageBuilder()
                .mediaSize(-1L)
                .mimetype("audio/ogg; codecs=opus")
                .build();

        ChatMessageInfo info = mediaInfo(media, "negative-size-test");

        assertThrows(
                InvalidMediaException.class,
                () -> bot.onShoppingBotMessage(client, info)
        );

        Mockito.verifyNoInteractions(client, repo);
        assertStorageIsEmpty();
    }

    @Test
    void acceptsMediaExactlyAtSizeLimit() throws IOException {
        loadDocumentHelper();

        // המגבלה שווה לגודל קובץ תקין; אין צורך לייצר קובץ של 2 MiB.
        MediaProperties properties = new MediaProperties(
                pdf.length,
                Set.of("application/pdf"),
                storageDirectory
        );
        ShoppingBot boundaryBot = new ShoppingBot(repo, hashHandler, properties);

        DocumentMessage document = new DocumentMessageBuilder()
                .mediaSize((long) pdf.length)
                .mimetype("application/pdf")
                .build();

        Mockito.when(client.downloadMedia(document))
                .thenReturn(new ByteArrayInputStream(pdf));

        boundaryBot.onShoppingBotMessage(
                client,
                mediaInfo(document, "exact-size-limit-test")
        );

        ArgumentCaptor<StoreDtoMedia> captor =
                ArgumentCaptor.forClass(StoreDtoMedia.class);
        Mockito.verify(repo).insertMediaData(captor.capture());

        StoreDtoMedia stored = captor.getValue();
        Path savedFile = Path.of(stored.path()).resolve(stored.fileName());

        assertEquals(pdf.length, stored.byteSize());
        assertArrayEquals(pdf, Files.readAllBytes(savedFile));
    }

    @Test
    void downloadFailureDoesNotSaveMedia() throws IOException {
        AudioMessage media = new AudioMessageBuilder()
                .mediaSize(100L)
                .build();

        IllegalStateException failure =
                new IllegalStateException("Simulated download failure");

        Mockito.when(client.downloadMedia(media))
                .thenThrow(failure);

        ChatMessageInfo info = mediaInfo(media, "download-failure-test");

        IllegalStateException thrown = assertThrows(
                IllegalStateException.class,
                () -> bot.onShoppingBotMessage(client, info)
        );

        assertSame(failure, thrown);
        Mockito.verifyNoInteractions(repo);
        assertStorageIsEmpty();
    }

    @Test
    void streamReadFailureDoesNotSaveMedia() throws IOException {
        AudioMessage media = new AudioMessageBuilder()
                .mediaSize(100L)
                .build();

        IOException failure = new IOException("Simulated read failure");

        InputStream failingStream = new InputStream() {
            @Override
            public int read() throws IOException {
                throw failure;
            }

            @Override
            public int read(byte[] bytes, int offset, int length)
                    throws IOException {
                throw failure;
            }
        };

        Mockito.when(client.downloadMedia(media))
                .thenReturn(failingStream);

        ChatMessageInfo info = mediaInfo(media, "read-failure-test");

        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> bot.onShoppingBotMessage(client, info)
        );

        assertSame(failure, thrown.getCause());
        Mockito.verifyNoInteractions(repo);
        assertStorageIsEmpty();
    }
}
