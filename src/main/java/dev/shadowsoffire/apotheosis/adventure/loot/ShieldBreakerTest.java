package dev.shadowsoffire.apotheosis.adventure.loot;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Predicate;

import dev.shadowsoffire.apotheosis.Apotheosis;
import dev.shadowsoffire.apotheosis.adventure.AdventureModule;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.server.ServerLifecycleHooks;

// This is kind of horrible, but we have to try and make entities to call the method and not leak those entities
// TODO: Delete heavy weapons
@EventBusSubscriber(modid = Apotheosis.MODID, bus = Bus.FORGE)
class ShieldBreakerTest implements Predicate<ItemStack> {

    private static Map<Level, Zombies> zombieCache = new IdentityHashMap<>();

    @Override
    public boolean test(ItemStack t) {
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            Level level = server != null ? server.getLevel(Level.OVERWORLD) : Client.getLevel();
            Zombies zombies = zombieCache.computeIfAbsent(level, Zombies::new);
            return t.canDisableShield(zombies.target.getOffhandItem(), zombies.target, zombies.attacker);
        }
        catch (Exception ex) {
            AdventureModule.LOGGER.error("Failed to execute ShieldBreakerTest", ex);
            return false;
        }
    }

    @SubscribeEvent
    public static void unload(LevelEvent.Unload e) {
        zombieCache.remove(e.getLevel());
    }

    private record Zombies(Zombie attacker, Zombie target) {

        public Zombies(Level level) {
            this(new Zombie(level), new Zombie(level));
            this.target.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));
        }

    }

    private static class Client {
        static Level getLevel() {
            return Minecraft.getInstance().level;
        }
    }

}