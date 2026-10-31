package com.wingman.sammy.shoppingbot;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.message.MessageInfo;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.linked.message.text.ExtendedTextMessage;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ShoppingBot {
    private final Jid GROUP = Jid.of("120363415109588611@g.us");
    private final ShoppingBotRepository persistence;

    public ShoppingBot (ShoppingBotRepository persistence) { this.persistence = persistence; }
    public void onAddItem (LinkedWhatsAppClient client, MessageInfo info){
        if (info.key().fromMe()) { return; }
        if (!(info instanceof ChatMessageInfo message)) { return; }
        if (!(info.message().content() instanceof ExtendedTextMessage text)) { return; }
        var chat = message.key().parentJid().orElse(null);
        if (!GROUP.equals(chat)){
            if (chat == null) {
                System.out.println("Missing parentJid. Ignoring message " + text);
            }
            return;
        }
        if (mentionsSammy(client, text)){
            sendPending(client, chat);
        } else {
            text.text().ifPresent(list -> persistence.addItems(breakList(list)));
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
            client.sendMessage(chat, LinkedMessageContainer.of(item));
        }
    }
}
