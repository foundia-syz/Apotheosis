package shadows.ench.altar;

import java.util.List;

import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraftforge.items.ItemStackHandler;
import shadows.Apotheosis;
import shadows.ApotheosisObjects;
import shadows.placebo.util.VanillaPacketDispatcher;
import shadows.util.EnchantmentUtils;
import shadows.util.ParticleMessage;

public class TilePrismaticAltar extends TileEntity implements ITickable {

	protected ItemStackHandler inv = new ItemStackHandler(5);
	protected float xpDrained = 0;
	protected ItemStack target = ItemStack.EMPTY;
	protected float targetXP = 0;
	int unusableValue = Integer.MAX_VALUE;
	int soundTick = 0;

	@Override
	public void update() {
		if (world.isRemote) return;
		if (!inv.getStackInSlot(4).isEmpty()) return;
		for (int i = 0; i < 4; i++) {
			if (inv.getStackInSlot(i).isEmpty()) {
				target = ItemStack.EMPTY;
				targetXP = 0;
				return;
			}
		}
		if (!target.isEmpty()) {
			drainXP();
			if (xpDrained >= targetXP) {
				inv.setStackInSlot(4, target);
				target = ItemStack.EMPTY;
				xpDrained = targetXP = 0;
				for (int i = 0; i < 4; i++)
					inv.setStackInSlot(i, ItemStack.EMPTY);
				markAndNotify();
				world.playSound(null, pos, SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.BLOCKS, 1, 1);
			}
		} else {
			findTarget(calcProvidedEnchValue());
		}
	}

	int[] rarityVals = { 1, 2, 4, 8 };

	public int calcProvidedEnchValue() {
		int value = 0;
		for (int i = 0; i < 4; i++) {
			value += EnchantmentHelper.getEnchantments(inv.getStackInSlot(i)).entrySet().stream().map(e -> rarityVals[e.getKey().getRarity().ordinal()] * e.getValue()).collect(IntCollector.INSTANCE);
		}
		return value;
	}

	public void drainXP() {
		List<EntityPlayer> nearby = world.getEntities(EntityPlayer.class, p -> p.getDistanceSq(pos) <= 25D);
		for (EntityPlayer p : nearby) {
			int removable = Math.min(3, p.experienceTotal);
			EnchantmentUtils.addPlayerXP(p, -removable);
			xpDrained += removable;
			if (removable > 0) trySpawnParticles(p, removable);
		}
		if (soundTick++ % 50 == 0) world.playSound(null, pos, ApotheosisObjects.ALTAR_SOUND, SoundCategory.BLOCKS, 0.5F, 0.9F);
	}

	public void findTarget(int value) {
		if (value >= unusableValue) return;
		ItemStack book = new ItemStack(Items.BOOK);
		target = new ItemStack(Items.ENCHANTED_BOOK);
		targetXP = value * 4;
		List<EnchantmentData> datas = EnchantmentHelper.buildEnchantmentList(world.rand, book, value, true);
		if (!datas.isEmpty()) for (EnchantmentData d : datas)
			ItemEnchantedBook.addEnchantment(target, d);
		else {
			target = ItemStack.EMPTY;
			targetXP = 0;
			unusableValue = value;
		}
	}

	public void trySpawnParticles(EntityPlayer player, int xpDrain) {
		TargetPoint point = new TargetPoint(world.provider.getDimension(), pos.getX(), pos.getY(), pos.getZ(), 0);
		Vec3d to = new Vec3d(player.posX - (pos.getX() + 0.5), player.posY - pos.getY(), player.posZ - (pos.getZ() + 0.5));
		ParticleMessage msg = new ParticleMessage(EnumParticleTypes.ENCHANTMENT_TABLE, pos.getX() + world.rand.nextDouble(), pos.getY() + 1 + world.rand.nextDouble(), pos.getZ() + world.rand.nextDouble(), to.x, to.y, to.z, xpDrain);
		Apotheosis.NETWORK.sendToAllTracking(msg, point);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tag) {
		tag.setTag("inv", inv.serializeNBT());
		tag.setFloat("xp", xpDrained);
		return super.writeToNBT(tag);
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		inv.deserializeNBT(tag.getCompoundTag("inv"));
		xpDrained = tag.getFloat("xp");
	}

	@Override
	public NBTTagCompound getUpdateTag() {
		NBTTagCompound tag = super.getUpdateTag();
		tag.setTag("inv", inv.serializeNBT());
		return tag;
	}

	@Override
	public void handleUpdateTag(NBTTagCompound tag) {
		super.handleUpdateTag(tag);
		inv.deserializeNBT(tag.getCompoundTag("inv"));
	}

	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		return new SPacketUpdateTileEntity(pos, -1, getUpdateTag());
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
		handleUpdateTag(pkt.getNbtCompound());
	}

	public ItemStackHandler getInv() {
		return inv;
	}

	public void markAndNotify() {
		markDirty();
		VanillaPacketDispatcher.dispatchTEToNearbyPlayers(this);
	}

}