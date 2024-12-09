package dev.shadowsoffire.apotheosis.potion;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dev.shadowsoffire.placebo.tabs.ITabFiller;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;

public class PotionCharmItem extends Item implements ITabFiller {

    public static final Set<ResourceLocation> EXTENDED_POTIONS = new HashSet<>();
    public static final Set<ResourceLocation> BLACKLIST = new HashSet<>();

    public PotionCharmItem() {
        super(new Item.Properties().stacksTo(1).durability(192).setNoRepair());
    }

    @Override
    public ItemStack getDefaultInstance() {
        return PotionUtils.setPotion(super.getDefaultInstance(), Potions.LONG_INVISIBILITY);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean isSelected) {
        if (!hasEffect(stack)) return;
        if (PotionModule.charmsInCuriosOnly && slot != -1) return;
        if (stack.getOrCreateTag().getBoolean("charm_enabled") && entity instanceof ServerPlayer) {
            MobEffectInstance contained = getEffect(stack);
            MobEffectInstance active = ((ServerPlayer) entity).getEffect(contained.getEffect());
            if (active == null || active.getDuration() < getCriticalDuration(active.getEffect())) {
                int durationOffset = getCriticalDuration(contained.getEffect());
                if (contained.getEffect() == MobEffects.REGENERATION) durationOffset += 50 >> contained.getAmplifier();
                MobEffectInstance newEffect = new MobEffectInstance(contained.getEffect(), (int) Math.ceil(contained.getDuration() / 24D) + durationOffset, contained.getAmplifier(), false, false);
                ((ServerPlayer) entity).addEffect(newEffect);
                if (stack.hurt(contained.getEffect() == MobEffects.REGENERATION ? 2 : 1, world.random, (ServerPlayer) entity)) stack.shrink(1);
            }
        }
    }

    private static int getCriticalDuration(MobEffect effect) {
        return EXTENDED_POTIONS.contains(ForgeRegistries.MOB_EFFECTS.getKey(effect)) ? 210 : 5;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.getOrCreateTag().getBoolean("charm_enabled");
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!world.isClientSide) {
            stack.getOrCreateTag().putBoolean("charm_enabled", !stack.getTag().getBoolean("charm_enabled"));
        }
        else if (!stack.getTag().getBoolean("charm_enabled")) world.playSound(player, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1, 0.3F);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return false;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, Level world, List<Component> tooltip, TooltipFlag flag) {
        if (PotionModule.charmsInCuriosOnly) {
            tooltip.add(Component.translatable(this.getDescriptionId() + ".curios_only").withStyle(ChatFormatting.RED));
        }
        if (hasEffect(stack)) {
            MobEffectInstance effect = getEffect(stack);
            MutableComponent potionCmp = Component.translatable(effect.getDescriptionId());
            if (effect.getAmplifier() > 0) {
                potionCmp = Component.translatable("potion.withAmplifier", potionCmp, Component.translatable("potion.potency." + effect.getAmplifier()));
            }
            potionCmp.withStyle(effect.getEffect().getCategory().getTooltipFormatting());
            tooltip.add(Component.translatable(this.getDescriptionId() + ".desc", potionCmp).withStyle(ChatFormatting.GRAY));
            boolean enabled = stack.getOrCreateTag().getBoolean("charm_enabled");
            MutableComponent enabledCmp = Component.translatable(this.getDescriptionId() + (enabled ? ".enabled" : ".disabled"));
            enabledCmp.withStyle(enabled ? ChatFormatting.BLUE : ChatFormatting.RED);
            if (effect.getDuration() > 20) {
                potionCmp = Component.translatable("potion.withDuration", potionCmp, MobEffectUtil.formatDuration(effect, 1));
            }
            potionCmp.withStyle(effect.getEffect().getCategory().getTooltipFormatting());
            tooltip.add(Component.translatable(this.getDescriptionId() + ".desc3", potionCmp).withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        if (!hasEffect(stack)) return 1;
        return 192;
    }

    @Override
    public Component getName(ItemStack stack) {
        if (!hasEffect(stack)) return Component.translatable("item.apotheosis.potion_charm_broke");
        MobEffectInstance effect = getEffect(stack);
        MutableComponent potionCmp = Component.translatable(effect.getDescriptionId());
        if (effect.getAmplifier() > 0) {
            potionCmp = Component.translatable("potion.withAmplifier", potionCmp, Component.translatable("potion.potency." + effect.getAmplifier()));
        }
        return Component.translatable("item.apotheosis.potion_charm", potionCmp);
    }

    /**
     * Returns true if the charm's NBT data contains a valid mob effect instance.
     * <p>
     * This will check the encoded potion type and then fall back to encoded custom NBT effects.
     */
    public static boolean hasEffect(ItemStack stack) {
        List<MobEffectInstance> effects = PotionUtils.getMobEffects(stack);
        return !effects.isEmpty();
    }

    /**
     * Returns the mob effect instance stored in the charm's NBT data.
     * <p>
     * This will check the encoded potion type and then fall back to encoded custom NBT effects.
     * <p>
     * Only a single effect is permitted, even when using custom NBT.
     */
    public static MobEffectInstance getEffect(ItemStack stack) {
        return PotionUtils.getMobEffects(stack).get(0);
    }

    @Override
    public void fillItemCategory(CreativeModeTab group, CreativeModeTab.Output out) {
        for (Potion potion : ForgeRegistries.POTIONS) {
            if (isValidPotion(potion)) {
                out.accept(PotionUtils.setPotion(new ItemStack(this), potion));
            }
        }
    }

    @Override
    public String getCreatorModId(ItemStack itemStack) {
        Potion potionType = PotionUtils.getPotion(itemStack);
        ResourceLocation resourceLocation = ForgeRegistries.POTIONS.getKey(potionType);
        if (resourceLocation != null) {
            return resourceLocation.getNamespace();
        }
        return ForgeRegistries.ITEMS.getKey(this).getNamespace();
    }

    @Override
    public int getEnchantmentValue() {
        return 0;
    }

    /**
     * Checks if a potion may be converted into a potion charm.
     * <p>
     * By default, only single-effect potions that are not instantaneous are allowed.
     * Additional potions may be blacklisted via config file.
     * 
     * @return True if the potion may be converted into a potion charm.
     */
    @SuppressWarnings("deprecation")
    public static boolean isValidPotion(Potion potion) {
        if (potion.getEffects().size() != 1) {
            return false;
        }

        MobEffect effect = potion.getEffects().get(0).getEffect();
        if (effect.isInstantenous()) {
            return false;
        }

        return !BLACKLIST.contains(BuiltInRegistries.MOB_EFFECT.getKey(effect));
    }

}