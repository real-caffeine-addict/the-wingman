package com.wingman.sammy.bots;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.message.MessageInfo;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.linked.message.text.ExtendedTextMessage;
import com.wingman.sammy.utilities.Persistence;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ShoppingBot {
    private final Jid GROUP = Jid.of("120363415109588611@g.us");
    private final Persistence persistence;

    public ShoppingBot (Persistence persistence) { this.persistence = persistence; }
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
            client.sendMessage(chat, LinkedMessageContainer.of(persistence.readShoppingList()));
        } else {
            text.text().ifPresent(list -> persistence.writeShoppingList(breakList(list)));
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


}
