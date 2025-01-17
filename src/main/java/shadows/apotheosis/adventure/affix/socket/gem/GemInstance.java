package shadows.apotheosis.adventure.affix.socket.gem;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.phys.HitResult;
import shadows.apotheosis.adventure.affix.AffixInstance;
import shadows.apotheosis.adventure.affix.socket.gem.bonus.GemBonus;
import shadows.apotheosis.adventure.loot.LootCategory;
import shadows.apotheosis.adventure.loot.LootRarity;

/**
 * A Gem Instance is a live copy of a Gem with all context needed to call Gem methods.<br>
 * This is the Gem counterparty of {@link AffixInstance}.
 * <p>
 * The major difference between them is that most methods do not live on {@link Gem} but rather on {@link GemBonus}.
 * @param gem The socketed Gem.
 * @param cate The LootCategory of the item the Gem is socketed into.
 * @param gemStack The itemstack form of the sockted Gem.
 * @param rarity The rarity of the Gem. Not the rarity of the item the Gem is socketed into.
 */
public record GemInstance(Gem gem, LootCategory cat, ItemStack gemStack, LootRarity rarity) {

	public GemInstance(ItemStack socketed, ItemStack gemStack) {
		this(GemItem.getGem(gemStack), LootCategory.forItem(socketed), gemStack, GemItem.getLootRarity(gemStack));
	}

	/**
	 * Creates a {@link GemInstance} with {@link LootCategory#NONE}.<br>
	 * This instance will be unable to invoke bonus methods, but may be used to easily retrieve the gem properties.
	 */
	public static GemInstance unsocketed(ItemStack gemStack) {
		return new GemInstance(GemItem.getGem(gemStack), LootCategory.NONE, gemStack, GemItem.getLootRarity(gemStack));
	}

	/**
	 * Checks if both the gem and rarity are not null.<br>
	 * This should only be used in conjunction with {@link #unsocketed(ItemStack)}.<br>
	 * Otherwise, use {@link #isValid()}.
	 */
	public boolean isValidUnsocketed() {
		return this.gem != null && this.rarity != null;
	}

	/**
	 * Checks if the gem and rarity are not null, and there is a valid bonus for the socketed category.<br>
	 * Will always return false if using {@link #unsocketed(ItemStack)}
	 */
	public boolean isValid() {
		return isValidUnsocketed() && this.gem.getBonus(cat).isPresent();
	}

	/**
	 * @see GemBonus#addModifiers(ItemStack, LootRarity, BiConsumer)
	 */
	public void addModifiers(EquipmentSlot slot, BiConsumer<Attribute, AttributeModifier> map) {
		for (EquipmentSlot itemSlot : cat.getSlots()) {
			if (itemSlot == slot) {
				ifPresent(b -> b.addModifiers(gemStack, rarity, map));
			}
		}
	}

	/**
	 * @see GemBonus#getSocketBonusTooltip(ItemStack, LootRarity)
	 */
	public Component getSocketBonusTooltip() {
		return map(b -> b.getSocketBonusTooltip(gemStack, rarity)).orElse(Component.literal("Invalid Gem Category"));
	}

	/**
	 * @see GemBonus#getDamageProtection(ItemStack, LootRarity, DamageSource)
	 */
	public int getDamageProtection(DamageSource source) {
		return map(b -> b.getDamageProtection(gemStack, rarity, source)).orElse(0);
	}

	/**
	 * @see GemBonus#getDamageBonus(ItemStack, LootRarity, MobType)
	 */
	public float getDamageBonus(MobType creatureType) {
		return map(b -> b.getDamageBonus(gemStack, rarity, creatureType)).orElse(0F);
	}

	/**
	 * @see GemBonus#doPostAttack(ItemStack, LootRarity, LivingEntity, Entity)
	 */
	public void doPostAttack(LivingEntity user, @Nullable Entity target) {
		ifPresent(b -> b.doPostAttack(gemStack, rarity, user, target));
	}

	/**
	 * @see GemBonus#doPostHurt(ItemStack, LootRarity, LivingEntity, Entity)
	 */
	public void doPostHurt(LivingEntity user, @Nullable Entity attacker) {
		ifPresent(b -> b.doPostHurt(gemStack, rarity, user, attacker));
	}

	/**
	 * @see GemBonus#onArrowFired(ItemStack, LootRarity, LivingEntity, AbstractArrow)
	 */
	public void onArrowFired(LivingEntity user, AbstractArrow arrow) {
		ifPresent(b -> b.onArrowFired(gemStack, rarity, user, arrow));
	}

	/**
	 * @see GemBonus#onItemUse(ItemStack, LootRarity, UseOnContext)
	 */
	@Nullable
	public InteractionResult onItemUse(UseOnContext ctx) {
		return map(b -> b.onItemUse(gemStack, rarity, ctx)).orElse(null);
	}

	/**
	 * @see {@link GemBonus#onArrowImpact(AbstractArrow, LootRarity, HitResult, HitResult.Type)}
	 */
	public void onArrowImpact(AbstractArrow arrow, ItemStack gem, LootRarity rarity, HitResult res, HitResult.Type type) {
		//TODO: getBonus(arrow).ifPresent(b -> b.onArrowImpact(gem, arrow, res, type));
	}

	/**
	 * @see GemBonus#onShieldBlock(ItemStack, LootRarity, LivingEntity, DamageSource, float)
	 */
	public float onShieldBlock(LivingEntity entity, DamageSource source, float amount) {
		return map(b -> b.onShieldBlock(gemStack, rarity, entity, source, amount)).orElse(amount);
	}

	/**
	 * @see GemBonus#onBlockBreak(ItemStack, LootRarity, Player, LevelAccessor, BlockPos, BlockState)
	 */
	public void onBlockBreak(Player player, LevelAccessor world, BlockPos pos, BlockState state) {
		ifPresent(b -> b.onBlockBreak(gemStack, rarity, player, world, pos, state));
	}

	/**
	 * @see GemBonus#getDurabilityBonusPercentage(ItemStack, LootRarity, ServerPlayer)
	 */
	public float getDurabilityBonusPercentage(ServerPlayer user) {
		return map(b -> b.getDurabilityBonusPercentage(gemStack, rarity, user)).orElse(0F);
	}

	/**
	 * @see GemBonus#onHurt(ItemStack, LootRarity, DamageSource, LivingEntity, float)
	 */
	public float onHurt(DamageSource src, LivingEntity ent, float amount) {
		return map(b -> b.onHurt(gemStack, rarity, src, ent, amount)).orElse(amount);
	}

	/**
	 * @see GemBonus#getEnchantmentLevels(ItemStack, LootRarity, Map)
	 */
	public void getEnchantmentLevels(Map<Enchantment, Integer> enchantments) {
		ifPresent(b -> b.getEnchantmentLevels(gemStack, rarity, enchantments));
	}

	/**
	 * @see GemBonus#modifyLoot(ItemStack, LootRarity, ObjectArrayList, LootContext)
	 */
	public void modifyLoot(ObjectArrayList<ItemStack> loot, LootContext ctx) {
		ifPresent(b -> b.modifyLoot(gemStack, rarity, loot, ctx));
	}

	private <T> Optional<T> map(Function<GemBonus, T> function) {
		return gem.getBonus(cat).map(function);
	}

	private void ifPresent(Consumer<GemBonus> function) {
		gem.getBonus(cat).ifPresent(function);
	}
}
