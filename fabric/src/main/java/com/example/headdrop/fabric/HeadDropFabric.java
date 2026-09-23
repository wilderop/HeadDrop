package com.example.headdrop.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.ResolvableProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HeadDropFabric implements ModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("HeadDrop");
    private static final Path PAPER_CONFIG = Path.of("/mnt/pool/survival/plugins/HeadDrop/config.yml");
    private static final Pattern PLAYER_CHANCE = Pattern.compile(
            "(?m)^PLAYER:\\s*$\\s*^\\s*Drop:\\s*(\\w+)\\s*$\\s*^\\s*Chance:\\s*([0-9.]+)");

    private volatile float chancePercent = 1.0f;
    private volatile boolean dropEnabled = true;

    @Override
    public void onInitialize() {
        reloadChance();
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof ServerPlayer victim)) return;
            Entity attacker = source.getEntity();
            if (!(attacker instanceof ServerPlayer killer)) return;
            if (killer.getUUID().equals(victim.getUUID())) return;
            if (!dropEnabled) return;
            if (ThreadLocalRandom.current().nextFloat() * 100.0f >= chancePercent) return;
            dropHead(victim, killer);
        });
        LOG.info("HeadDrop Fabric 1.0.0: player heads {}% (1 in {})", chancePercent, Math.round(100.0 / Math.max(0.0001, chancePercent)));
    }

    private void reloadChance() {
        try {
            if (!Files.isRegularFile(PAPER_CONFIG)) return;
            String raw = Files.readString(PAPER_CONFIG, StandardCharsets.UTF_8);
            Matcher m = PLAYER_CHANCE.matcher(raw);
            if (!m.find()) return;
            dropEnabled = Boolean.parseBoolean(m.group(1));
            chancePercent = Float.parseFloat(m.group(2));
        } catch (Exception e) {
            LOG.warn("Could not read HeadDrop PLAYER chance, using 1%: {}", e.toString());
        }
    }

    private static void dropHead(ServerPlayer victim, ServerPlayer killer) {
        ItemStack head = new ItemStack(Items.PLAYER_HEAD);
        head.set(DataComponents.PROFILE, ResolvableProfile.createResolved(victim.getGameProfile()));
        String date = LocalDate.now().toString();
        String killerName = killer.getGameProfile().name();
        head.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("Killed by " + killerName),
                Component.literal("Killed on " + date)
        )));
        ServerLevel level = victim.level();
        ItemEntity dropped = new ItemEntity(level, victim.getX(), victim.getY(), victim.getZ(), head);
        dropped.setDefaultPickUpDelay();
        level.addFreshEntity(dropped);
    }
}
