package com.github.zly2006.mojangistooconservative.mixin;

import com.github.zly2006.mojangistooconservative.MojangIsTooConservative;
import com.google.common.collect.ImmutableMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.village.*;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(VillagerEntity.class)
public abstract class MixinVillager extends MerchantEntity {
    @Shadow public abstract VillagerData getVillagerData();
    @Unique
    private static final int[] experience = new int[]{0, 1, 5, 10, 15, 30};
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Unique
    private static final Int2ObjectMap<TradeOffers.Factory[]> librarianMap = new Int2ObjectOpenHashMap(ImmutableMap.builder()
            .put(1, new TradeOffers.Factory[]{
                    new TradeOffers.BuyForOneEmeraldFactory(Items.PAPER, 24, 16, 2),
                    new TradeOffers.SellItemFactory(Blocks.BOOKSHELF, 9, 1, 12, 1)
            })
            .put(2, new TradeOffers.Factory[]{
                    new TradeOffers.BuyForOneEmeraldFactory(Items.BOOK, 4, 12, 10),
                    new TradeOffers.SellItemFactory(Items.LANTERN, 1, 1, 5)
            })
            .put(3, new TradeOffers.Factory[]{
                    new TradeOffers.BuyForOneEmeraldFactory(Items.INK_SAC, 5, 12, 20),
                    new TradeOffers.SellItemFactory(Items.GLASS, 1, 4, 10)
            })
            .put(4, new TradeOffers.Factory[]{
                    new TradeOffers.BuyForOneEmeraldFactory(Items.WRITABLE_BOOK, 2, 12, 30),
                    new TradeOffers.SellItemFactory(Items.CLOCK, 5, 1, 15),
                    new TradeOffers.SellItemFactory(Items.COMPASS, 4, 1, 15)
            })
            .put(5, new TradeOffers.Factory[]{
                    new TradeOffers.SellItemFactory(Items.NAME_TAG, 20, 1, 30)
            }).build());

    @Unique
    RegistryKey<Biome> spawnBiome;

    @Inject(
            method = "fillRecipes",
            at = @At("HEAD"),
            cancellable = true
    )
    private void fillRecipes(CallbackInfo ci) {
        if (getVillagerData().getProfession() == VillagerProfession.LIBRARIAN) {
            int villagerLevel = getVillagerData().getLevel();
            Enchantment enchantment = MojangIsTooConservative.INSTANCE.getEnchantmentForBiome(spawnBiome);
            int remainingLevel = enchantment.getMaxLevel() - (5 - villagerLevel)
                    - (enchantment.getMinLevel() == enchantment.getMaxLevel() ? 0 : 1);
            List<TradeOffers.Factory> factories = new ArrayList<>(List.of(librarianMap.get(villagerLevel)));
            if (remainingLevel >= enchantment.getMinLevel()) {
                factories.add((entity, random) -> {
                    ItemStack itemStack = EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(enchantment, remainingLevel));
                    int j = 2 + random.nextInt(5 + remainingLevel * 10) + 3 * remainingLevel;
                    if (enchantment.isTreasure()) {
                        j *= 2;
                    }
                    if (j > 64) {
                        j = 64;
                    }
                    return new TradeOffer(
                            new ItemStack(Items.EMERALD, j),
                            new ItemStack(Items.BOOK),
                            itemStack,
                            12,
                            experience[remainingLevel],
                            0.2F
                    );
                });
            }
            this.fillRecipesFromPool(getOffers(), factories.toArray(factories.toArray(new TradeOffers.Factory[0])), 2);
            ci.cancel();
        }
    }

    public MixinVillager(EntityType<? extends MerchantEntity> entityType, World world) {
        super(entityType, world);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Inject(
            method = "initialize",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/passive/VillagerEntity;setVillagerData(Lnet/minecraft/village/VillagerData;)V",
                    ordinal = 1
            )
    )
    private void initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, EntityData entityData, NbtCompound entityNbt, CallbackInfoReturnable<EntityData> cir) {
        spawnBiome = world.getBiome(this.getBlockPos()).getKey().get();
    }

    @SuppressWarnings({"rawtypes", "OptionalGetWithoutIsPresent"})
    @Inject(
            method = "<init>(Lnet/minecraft/entity/EntityType;Lnet/minecraft/world/World;Lnet/minecraft/village/VillagerType;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/passive/VillagerEntity;setVillagerData(Lnet/minecraft/village/VillagerData;)V",
                    ordinal = 0
            )
    )
    private void init(EntityType entityType, World world, VillagerType type, CallbackInfo ci) {
        spawnBiome = world.getBiome(this.getBlockPos()).getKey().get();
    }

    @Inject(
            method = "readCustomDataFromNbt",
            at = @At("HEAD")
    )
    private void readCustomDataFromNbt(NbtCompound nbt, CallbackInfo ci) {
        String id = nbt.getString("FuckMojang:SpawnBiome");
        spawnBiome = RegistryKey.of(RegistryKeys.BIOME, new Identifier(id));
    }

    @Inject(
            method = "writeCustomDataToNbt",
            at = @At("HEAD")
    )
    private void writeCustomDataToNbt(NbtCompound nbt, CallbackInfo ci) {
        nbt.putString("FuckMojang:SpawnBiome", spawnBiome.getValue().toString());
    }
}
