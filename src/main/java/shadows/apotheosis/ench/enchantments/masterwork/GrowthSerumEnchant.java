package shadows.apotheosis.ench.enchantments.masterwork;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import shadows.apotheosis.ench.EnchModule;

public class GrowthSerumEnchant extends Enchantment {

	public GrowthSerumEnchant() {
		super(Rarity.VERY_RARE, EnchModule.SHEARS, new EquipmentSlot[] { EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND });
	}

	public int getMinCost(int pLevel) {
		return 60;
	}

	@Override
	public Component getFullname(int level) {
		return ((MutableComponent) super.getFullname(level)).withStyle(ChatFormatting.DARK_GREEN);
	}

	public void unshear(Sheep sheep, ItemStack shears) {
		if (EnchantmentHelper.getItemEnchantmentLevel(this, shears) > 0 && sheep.random.nextBoolean()) sheep.setSheared(false);
	}

}