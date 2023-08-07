package com.github.zly2006.mojangistooconservative;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.registry.BuiltinRegistries;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;

import java.util.*;
import java.util.stream.Stream;

public class MojangIsTooConservative implements ModInitializer {
    MinecraftServer server;
    public static MojangIsTooConservative INSTANCE;
    static Map<RegistryKey<Biome>, Identifier> biomeEnchantMap = new HashMap<>();
    @Override
    public void onInitialize() {
        INSTANCE = this;
        ServerLifecycleEvents.SERVER_STARTING.register(server -> this.server = server);
        ServerLifecycleEvents.SERVER_STARTED.register(server1 -> {
            var mendingId = Registries.ENCHANTMENT.getId(Enchantments.MENDING);
            biomeEnchantMap.put(BiomeKeys.MUSHROOM_FIELDS, mendingId);
            Stream<? extends RegistryKey<?>> biomes = BuiltinRegistries.createWrapperLookup().getWrapperOrThrow(RegistryKeys.BIOME).streamKeys();
            ArrayList<Identifier> enchantments = new ArrayList<>(Registries.ENCHANTMENT.getIds());
            enchantments.remove(mendingId);
            enchantments.remove(Registries.ENCHANTMENT.getId(Enchantments.SOUL_SPEED));
            enchantments.remove(Registries.ENCHANTMENT.getId(Enchantments.SWIFT_SNEAK));
            ArrayList<Identifier> chosingEnchantments = new ArrayList<>(enchantments);
            Random random = new Random(server1.getOverworld().getSeed());
            biomes.forEach(biomeKey -> {
                if (chosingEnchantments.isEmpty()) {
                    chosingEnchantments.addAll(enchantments);
                }
                Identifier enchantment = chosingEnchantments.get(random.nextInt(chosingEnchantments.size()));
                biomeEnchantMap.put((RegistryKey<Biome>) biomeKey, enchantment);
                chosingEnchantments.remove(enchantment);
            });
        });
    }

    public static final String MOD_ID = "mojang-is-too-conservative";

    public Enchantment getEnchantmentForBiome(RegistryKey<Biome> biome) {
        Enchantment enchantment = Registries.ENCHANTMENT.get(biomeEnchantMap.get(biome));
        if (enchantment == null) {
            return Registries.ENCHANTMENT.get(biomeEnchantMap.get(BiomeKeys.PLAINS));
        }
        return enchantment;
    }
}
