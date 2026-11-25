package com.wingman.sammy.shoppingbot;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.message.MessageInfo;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.linked.message.media.MediaMessage;
import com.github.auties00.cobalt.wire.linked.message.text.ExtendedTextMessage;
import com.github.auties00.cobalt.wire.linked.message.text.ReactionMessage;
import com.wingman.sammy.MediaProperties;
import com.wingman.sammy.dto.SavedMedia;
import com.wingman.sammy.dto.StoreDtoMedia;
import com.wingman.sammy.exceptions.InvalidMediaException;
import com.wingman.sammy.shoppingbot.domain.ItemStatus;
import com.wingman.sammy.utilities.HashHandler;
import org.apache.tika.Tika;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ShoppingBot {
    private final Jid GROUP = Jid.of("120363415109588611@g.us");
    private final ShoppingBotRepository persistence;
    private final MediaProperties mediaProperties;
    private final Tika tika = new Tika();
    private final HashHandler encoder;

    public ShoppingBot (ShoppingBotRepository persistence,
                        HashHandler encoder,
                        MediaProperties mediaProperties) {
        this.persistence = persistence;
        this.encoder = encoder;
        this.mediaProperties = mediaProperties;
    }

    public void onShoppingBotMessage(LinkedWhatsAppClient client, MessageInfo info){
        // Global validations
        if (info.key().fromMe()) { return; }
        if (!(info instanceof ChatMessageInfo message)) { return; }
        var chat = message.key().parentJid().orElse(null);
        if (!GROUP.equals(chat)){
            if (chat == null) {
                System.out.println("Missing parentJid. Ignoring message " + message.message().content().toString());
            }
            return;
        }
        if (message.message().content() instanceof MediaMessage media) {
            var key = message.key().id().orElse(null);
            var sender = message.senderJid().orElse(null);
            if (key != null && sender != null)
                saveMedia(client, media, key, sender);
            return;
        }

        if (info.message().content() instanceof ReactionMessage reaction) { handleReaction(reaction); }
        if (!(info.message().content() instanceof ExtendedTextMessage text)) { return; }
        if (mentionsSammy(client, text)){
            sendPending(client, chat);
        } else {
            text.text().ifPresent(list -> persistence.addItems(breakList(list)));
        }

    }

    private void handleReaction(ReactionMessage reaction) {
        var target = reaction.key().orElse(null);
        var emoji = reaction.text().orElse(null);
        if (target == null || emoji == null) {
            return;
        }
        var messageId = target.id().orElse(null);
        if (messageId == null) {
            return;
        }

        if (emoji.startsWith("\uD83D\uDC4D")) {
            persistence.setStatus(ItemStatus.COLLECTED, messageId);
        } else {
            persistence.setStatus(ItemStatus.IN_PROGRESS, messageId);
        }
    }

    private boolean mentionsSammy(
            LinkedWhatsAppClient client,
            ExtendedTextMessage text
    ) {
        return text.contextInfo()
                .map(context -> context.mentionedJids().stream()
                        .anyMatch(jid -> isSammy(client, jid)))
                .orElse(false);
    }

    private boolean isSammy(LinkedWhatsAppClient client, Jid jid) {
        var account = client.store().accountStore();

        return account.jid().filter(jid::isSameAccount).isPresent()
                || account.lid().filter(jid::isSameAccount).isPresent();
    }

    private Set<String> breakList (String list){
        return list.lines()
                .map(String::strip)
                .filter(line -> !line.isEmpty())
                .collect(Collectors.toSet());
    }

    private void sendPending (LinkedWhatsAppClient client, Jid chat){
        List<String> rawList = persistence.readShoppingList();
        if (rawList.isEmpty()){
            client.sendMessage(chat, LinkedMessageContainer.of("הרשימה ריקה"));
            return;
        }
        for (String item : rawList) {
            var id = client.sendMessage(chat, LinkedMessageContainer.of(item)).id().orElse(null);
            if (id == null)
                throw new RuntimeException("Sent message with no id " + item);
            persistence.attachMessageId(id, item);
        }
    }

    private SavedMedia saveMedia (LinkedWhatsAppClient client, MediaMessage media, String key, Jid sender) {
        var declaredSize = media.fileLength()
                .orElseThrow(() -> new InvalidMediaException("File size missing"));

        if (declaredSize > mediaProperties.maxSizeBytes() || declaredSize < 0) {
            throw new InvalidMediaException("File size outside allowed range");
        }

        try (InputStream input = client.downloadMedia(media)) {
            byte[] bytes = input.readNBytes(mediaProperties.maxSizeBytes() + 1);
            if (bytes.length > mediaProperties.maxSizeBytes()) {
                throw new InvalidMediaException("File exceeds maximum size");
            }

            MimeType detectedMedia = MimeTypes.getDefaultMimeTypes()
                    .forName(tika.detect(bytes));

            if (!mediaProperties.allowedMimeTypes().contains(detectedMedia.getName())) {
                throw new InvalidMediaException(
                        "Unsupported media type: " + detectedMedia.getName()
                );
            }

            String extension = detectedMedia.getExtension();

            Path path = mediaProperties.storageDirectory().resolve(sender.user());
            Files.createDirectories(path);
            String fileName = UUID.randomUUID() + extension;
            Path target = path.resolve(fileName);
            Files.write(target, bytes, StandardOpenOption.CREATE_NEW);
            StoreDtoMedia storeDtoMedia = new StoreDtoMedia(
                    fileName,
                    detectedMedia.getName(),
                    bytes.length,
                    LocalDateTime.now(ZoneOffset.UTC),
                    path.toString(),
                    key,
                    sender.toString(),
                    encoder.encode(bytes)
            );
            return persistence.insertMediaData(storeDtoMedia);
        } catch (DataAccessException e){
            throw new RuntimeException("Temporary");
            // TODO: handle recovery if applicable and file deletion if not
        } catch (MimeTypeException e){
            throw new IllegalStateException("Tika returned an invalid MIME type", e);
        } catch (IOException e){
            throw new RuntimeException(e); //TODO: Classify recoverable/non-recoverable and replace temporary RuntimeException
        }
    }

}
