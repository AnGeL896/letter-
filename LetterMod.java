package com.surprise.letter;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(LetterMod.MODID)
public class LetterMod {
    public static final String MODID = "letter";

    private static final String TRIGGER = "письмо";
    private static final String TITLE = "Самой лучшей девушке";
    private static final String AUTHOR = "Твой любимый";

    private static final String[] PAGES = {
        "Котик, привет!\n\nЭто письмо только для тебя. Устройся поудобнее: я приготовил его с любовью.",
        "17 февраля 2022 года я встретил человека, который изменил мою жизнь. С того дня ты рядом, и каждый день с тобой для меня особенный.",
        "Спасибо, что всегда помогала мне во всём. Спасибо, что не сдалась и не ушла, когда было по-настоящему трудно. Я этого никогда не забуду.",
        "Были и тяжёлые моменты, но мы прошли через них вместе и стали только сильнее. Я знаю, как мне повезло с тобой.",
        "Ты мой комочек счастья. Я не хочу даже думать о ком-то другом: лучше тебя для меня нет и не будет.",
        "Скоро мы съедемся, и начнётся наша новая глава. А пока давай заведём в нашем домике целую стаю котиков и посадим рядом сакуру.",
        "Желаю тебе море счастья, тёплых дней и исполнения всех желаний.\n\nЛюблю тебя, моё солнышко. ♥"
    };

    public LetterMod() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onChat(ServerChatEvent event) {
        if (!event.getRawText().trim().equalsIgnoreCase(TRIGGER)) {
            return;
        }

        event.setCanceled(true);

        ServerPlayer player = event.getPlayer();
        ItemStack book = createBook();

        if (!player.getInventory().add(book)) {
            player.drop(book, false);
        }

        player.level().playSound(null, player.blockPosition(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private ItemStack createBook() {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag tag = book.getOrCreateTag();
        tag.putString("title", TITLE);
        tag.putString("author", AUTHOR);

        ListTag pages = new ListTag();
        for (String page : PAGES) {
            pages.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(page))));
        }
        tag.put("pages", pages);
        tag.putBoolean("resolved", true);
        return book;
    }
}
