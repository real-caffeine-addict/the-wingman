package com.wingman.sammy.bots;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppAccountStore;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.message.MessageKey;
import com.github.auties00.cobalt.wire.core.message.MessageKeyBuilder;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfoBuilder;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.linked.message.context.ContextInfoBuilder;
import com.github.auties00.cobalt.wire.linked.message.text.ExtendedTextMessage;
import com.github.auties00.cobalt.wire.linked.message.text.ExtendedTextMessageBuilder;
import com.wingman.sammy.utilities.Persistence;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class ShoppingBotTest {
    @Test
    public void writeShoppingListHappyPath () {
        String msg = "Lettuce\nעגבניה \n $Gד \n   Lettuce";
        Persistence repo = Mockito.mock(Persistence.class);
        LinkedWhatsAppClient client = Mockito.mock(LinkedWhatsAppClient.class);
        ShoppingBot bot = new ShoppingBot(repo);
        var key = new MessageKeyBuilder()
                .parentJid(Jid.of("120363415109588611@g.us"))
                .fromMe(false)
                .build();
        ChatMessageInfo info = new ChatMessageInfoBuilder()
                .key(key)
                .message(LinkedMessageContainer.of(msg))
                .build();
        bot.onAddItem(client, info);
        Mockito.verify(repo).writeShoppingList(
                Set.of("Lettuce", "עגבניה", "$Gד")
        );
    }

    @Test
    public void readShoppingListHappyPath () {
        String msg = ("@סמי");
        var sammyJid = Jid.of("972501234567@s.whatsapp.net");
        var context = new ContextInfoBuilder()
                .mentionedJids(List.of(sammyJid))
                .build();
        var text = new ExtendedTextMessageBuilder()
                .text(msg)
                .contextInfo(context)
                .build();

        LinkedWhatsAppClient client = Mockito.mock(LinkedWhatsAppClient.class);
        Persistence repo = Mockito.mock(Persistence.class);
        var store = Mockito.mock(LinkedWhatsAppStore.class);
        var account = Mockito.mock(LinkedWhatsAppAccountStore.class);
        Mockito.when(repo.readShoppingList())
                .thenReturn("test-list");
        Mockito.when(client.store()).thenReturn(store);
        Mockito.when(store.accountStore()).thenReturn(account);
        Mockito.when(account.jid()).thenReturn(Optional.of(sammyJid));
        Mockito.when(account.lid()).thenReturn(Optional.empty());

        ShoppingBot bot = new ShoppingBot(repo);

        var key = new MessageKeyBuilder()
                .parentJid(Jid.of("120363415109588611@g.us"))
                .fromMe(false)
                .build();
        ChatMessageInfo info = new ChatMessageInfoBuilder()
                .key(key)
                .message(LinkedMessageContainer.of(text))
                .build();
        bot.onAddItem(client, info);

        Mockito.verify(client).sendMessage(
                Mockito.eq(Jid.of("120363415109588611@g.us")),
                Mockito.argThat(container ->
                        container.content() instanceof ExtendedTextMessage sentText
                                && sentText.text().orElse("").equals("test-list")
                )
        );
        Mockito.verify(repo).readShoppingList();
        Mockito.verify(repo, Mockito.never())
                .writeShoppingList(Mockito.anySet());
    }

    @Test
    void ignoresOwnMessages() {
        var repo = Mockito.mock(Persistence.class);
        var client = Mockito.mock(LinkedWhatsAppClient.class);
        var bot = new ShoppingBot(repo);

        var key = new MessageKeyBuilder()
                .parentJid(Jid.of("120363415109588611@g.us"))
                .fromMe(true)
                .build();

        var info = new ChatMessageInfoBuilder()
                .key(key)
                .message(LinkedMessageContainer.of("חלב\nביצים"))
                .build();

        bot.onAddItem(client, info);

        Mockito.verifyNoInteractions(repo, client);
    }

    @Test
    void ignoresMessagesFromOtherChats() {
        var repo = Mockito.mock(Persistence.class);
        var client = Mockito.mock(LinkedWhatsAppClient.class);
        var bot = new ShoppingBot(repo);

        var key = new MessageKeyBuilder()
                .parentJid(Jid.of("120363000000000000@g.us"))
                .fromMe(false)
                .build();

        var info = new ChatMessageInfoBuilder()
                .key(key)
                .message(LinkedMessageContainer.of("חלב\nביצים"))
                .build();

        bot.onAddItem(client, info);

        Mockito.verifyNoInteractions(repo, client);
    }
    
}
