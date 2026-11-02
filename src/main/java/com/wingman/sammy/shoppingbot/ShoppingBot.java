package com.wingman.sammy.shoppingbot;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.message.MessageInfo;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.linked.message.text.ExtendedTextMessage;
import com.github.auties00.cobalt.wire.linked.message.text.ReactionMessage;
import com.wingman.sammy.shoppingbot.domain.ItemStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ShoppingBot {
    private final Jid GROUP = Jid.of("120363415109588611@g.us");
    private final ShoppingBotRepository persistence;

    public ShoppingBot (ShoppingBotRepository persistence) { this.persistence = persistence; }
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
            persistence.setStatus(ItemStatus.PURCHASED, messageId);
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

}
