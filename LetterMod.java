package com.surprise.letter;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Mod(LetterMod.MODID)
public class LetterMod {
    public static final String MODID = "letter";

    // ====== ЗДЕСЬ МЕНЯЕШЬ ТЕКСТ ======
    private static final String TRIGGER = "письмо";
    private static final String TITLE = "Самой лучшей девушке";
    private static final String AUTHOR = "Твой любимый";

    private static final String TITLE_TEXT = "Карина ♥";
    private static final String SUBTITLE_TEXT = "Твой любимый";

    private static final String[] PAGES = {
        "Котик, привет!\n\nЭто письмо только для тебя. Устройся поудобнее: я приготовил его с любовью.",
        "17 февраля 2022 года я встретил человека, который изменил мою жизнь. С того дня ты рядом, и каждый день с тобой для меня особенный.",
        "Спасибо, что всегда помогала мне во всём. Спасибо, что не сдалась и не ушла, когда было по-настоящему трудно. Я этого никогда не забуду.",
        "Были и тяжёлые моменты, но мы прошли через них вместе и стали только сильнее. Я знаю, как мне повезло с тобой.",
        "Ты мой комочек счастья. Я не хочу даже думать о ком-то другом: лучше тебя для меня нет и не будет.",
        "Скоро мы съедемся, и начнётся наша новая глава. А пока давай заведём в нашем домике целую стаю котиков и посадим рядом сакуру.",
        "Желаю тебе море счастья, тёплых дней и исполнения всех желаний.\n\nЛюблю тебя, моё солнышко. ♥"
    };
    // =================================

    private static class Task {
        int ticks;
        final Runnable run;
        Task(int ticks, Runnable run) {
            this.ticks = ticks;
            this.run = run;
        }
    }

    private final List<Task> tasks = new ArrayList<>();

    public LetterMod() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void schedule(int ticks, Runnable run) {
        synchronized (tasks) {
            tasks.add(new Task(ticks, run));
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        List<Task> due = new ArrayList<>();
        synchronized (tasks) {
            if (tasks.isEmpty()) {
                return;
            }
            Iterator<Task> it = tasks.iterator();
            while (it.hasNext()) {
                Task t = it.next();
                t.ticks--;
                if (t.ticks <= 0) {
                    due.add(t);
                    it.remove();
                }
            }
        }
        for (Task t : due) {
            try {
                t.run.run();
            } catch (Exception ignored) {
            }
        }
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

        player.serverLevel().playSound(null, player.blockPosition(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);

        // Через 1 секунду: салют, надпись и сердечко
        schedule(20, () -> {
            if (player.isRemoved()) return;
            launchFireworks(player);
            showTitle(player);
            drawHeart(player);
        });
        schedule(35, () -> {
            if (player.isRemoved()) return;
            drawHeart(player);
        });
        schedule(50, () -> {
            if (player.isRemoved()) return;
            drawHeart(player);
        });
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

    private void showTitle(ServerPlayer player) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
        player.connection.send(new ClientboundSetSubtitleTextPacket(
                Component.literal(SUBTITLE_TEXT).withStyle(ChatFormatting.LIGHT_PURPLE)));
        player.connection.send(new ClientboundSetTitleTextPacket(
                Component.literal(TITLE_TEXT).withStyle(ChatFormatting.RED)));
    }

    private ItemStack makeRocket(int type, int[] colors, int[] fade) {
        ItemStack rocket = new ItemStack(Items.FIREWORK_ROCKET);
        CompoundTag fw = rocket.getOrCreateTagElement("Fireworks");
        fw.putByte("Flight", (byte) 1);

        CompoundTag explosion = new CompoundTag();
        explosion.putByte("Type", (byte) type);
        explosion.putIntArray("Colors", colors);
        explosion.putIntArray("FadeColors", fade);
        explosion.putBoolean("Trail", true);
        explosion.putBoolean("Flicker", true);

        ListTag list = new ListTag();
        list.add(explosion);
        fw.put("Explosions", list);
        return rocket;
    }

    private void launchFireworks(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        double x = player.getX();
        double y = player.getY() + 0.5;
        double z = player.getZ();

        int pink = 0xFF69B4;
        int red = 0xFF2040;
        int white = 0xFFFFFF;

        double[][] offsets = {{3, 0}, {-3, 0}, {0, 3}, {0, -3}};
        ItemStack[] rockets = {
            makeRocket(1, new int[]{pink, red}, new int[]{white}),
            makeRocket(2, new int[]{red, white}, new int[]{pink}),
            makeRocket(4, new int[]{pink, white}, new int[]{red}),
            makeRocket(1, new int[]{red, pink}, new int[]{white})
        };

        for (int i = 0; i < offsets.length; i++) {
            FireworkRocketEntity e = new FireworkRocketEntity(
                    level, x + offsets[i][0], y, z + offsets[i][1], rockets[i]);
            level.addFreshEntity(e);
        }
    }

    private void drawHeart(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle();
        double lx = look.x;
        double lz = look.z;
        double len = Math.sqrt(lx * lx + lz * lz);
        if (len < 0.01) {
            lx = 0;
            lz = 1;
            len = 1;
        }
        lx /= len;
        lz /= len;

        double cx = player.getX() + lx * 5;
        double cy = player.getY() + 4;
        double cz = player.getZ() + lz * 5;
        double px = -lz;
        double pz = lx;

        DustParticleOptions dust = new DustParticleOptions(new Vector3f(1.0F, 0.2F, 0.45F), 1.6F);

        int points = 120;
        double scale = 0.16;
        for (int i = 0; i < points; i++) {
            double t = i * Math.PI * 2 / points;
            double hx = 16 * Math.pow(Math.sin(t), 3);
            double hy = 13 * Math.cos(t) - 5 * Math.cos(2 * t) - 2 * Math.cos(3 * t) - Math.cos(4 * t);
            level.sendParticles(dust,
                    cx + px * hx * scale,
                    cy + hy * scale,
                    cz + pz * hx * scale,
                    1, 0, 0, 0, 0);
        }
    }
}
