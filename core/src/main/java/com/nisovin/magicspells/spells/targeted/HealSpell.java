package com.nisovin.magicspells.spells.targeted;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.events.MagicSpellsEntityRegainHealthEvent;

public class HealSpell extends TargetedSpell implements TargetedEntitySpell {
	
	private double healAmount;

	private boolean checkPlugins;
	private boolean cancelIfFull;

	private String strMaxHealth;

	private ValidTargetChecker checker;

	public HealSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		healAmount = getConfigFloat("heal-amount", 10);

		checkPlugins = getConfigBoolean("check-plugins", true);
		cancelIfFull = getConfigBoolean("cancel-if-full", true);

		strMaxHealth = getConfigString("str-max-health", "%t is already at max health.");

		checker = (LivingEntity entity) -> entity.getHealth() < Util.getMaxHealth(entity);
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> targetInfo = getTargetedEntity(player, power, checker);
			if (targetInfo == null) return noTarget(player);
			LivingEntity target = targetInfo.getTarget();
			power = targetInfo.getPower();
			if (cancelIfFull && target.getHealth() == Util.getMaxHealth(target)) return noTarget(player, formatMessage(strMaxHealth, "%t", getTargetName(target)));
			boolean healed = heal(player, target, power);
			if (!healed) return noTarget(player);
			sendMessages(player, target);
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		if (validTargetList.canTarget(caster, target) && target.getHealth() < Util.getMaxHealth(target)) return heal(caster, target, power);
		return false;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		if (validTargetList.canTarget(target) && target.getHealth() < Util.getMaxHealth(target)) return heal(null, target, power);
		return false;
	}

	@Override
	public ValidTargetChecker getValidTargetChecker() {
		return checker;
	}
	
	private boolean heal(Player player, LivingEntity target, float power) {
		double health = target.getHealth();
		double amt = healAmount * power;

		if (checkPlugins) {
			MagicSpellsEntityRegainHealthEvent evt = new MagicSpellsEntityRegainHealthEvent(target, amt, RegainReason.CUSTOM);
			EventUtil.call(evt);
			if (evt.isCancelled()) return false;
			amt = evt.getAmount();
		}

		health += amt;
		if (health > Util.getMaxHealth(target)) health = Util.getMaxHealth(target);
		target.setHealth(health);

		if (player == null) playSpellEffects(EffectPosition.TARGET, target);
		else playSpellEffects(player, target);
		return true;
	}

}
