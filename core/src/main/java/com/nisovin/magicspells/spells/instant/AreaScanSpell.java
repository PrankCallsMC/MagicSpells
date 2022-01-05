package com.nisovin.magicspells.spells.instant;

import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class AreaScanSpell extends InstantSpell {

	private Material material;

	private ConfigData<Integer> radius;
	private ConfigData<Integer> maxBlocks;

	private Vector offset;

	private String strNotFound;
	private String spellToCast;

	private Subspell spell;

	private boolean getDistance;
	private boolean powerAffectsRadius;

	public AreaScanSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		String blockName = getConfigString("block-type", "");

		if (!blockName.isEmpty()) material = Util.getMaterial(blockName);

		radius = getConfigDataInt("radius", 4);
		maxBlocks = getConfigDataInt("max-blocks", 0);

		offset = getConfigVector("offset", "0.5,0.5,0.5");

		strNotFound = getConfigString("str-not-found", "No blocks target found.");
		spellToCast = getConfigString("spell", "");

		getDistance = strCastSelf != null && strCastSelf.contains("%b");
		powerAffectsRadius = getConfigBoolean("power-affects-radius", true);

		if (material == null) MagicSpells.error("AreaScanSpell '" + internalName + "' has no target block defined!");
	}

	@Override
	public void initialize() {
		super.initialize();

		if (!spellToCast.isEmpty()) {
			spell = new Subspell(spellToCast);
			spellToCast = null;

			if (!spell.process()) {
				MagicSpells.error("AreaScanSpell '" + internalName + "' has an invalid spell defined!");
				spell = null;
			}
		}
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL && caster instanceof Player) {
			int distance = -1;
			if (material != null) {
				Block foundBlock = null;

				Location loc = caster.getLocation();
				World world = caster.getWorld();
				int cx = loc.getBlockX();
				int cy = loc.getBlockY();
				int cz = loc.getBlockZ();

				int maxBlocks = this.maxBlocks.get(caster, null, power, args);
				int radius = this.radius.get(caster, null, power, args);
				if (powerAffectsRadius) radius = Math.round(radius * power);

				radius = Math.min(radius, MagicSpells.getGlobalRadius());

				int count = 0;
				loop:
				for (int r = 1; r <= radius; r++) {
					for (int x = -r; x <= r; x++) {
						for (int y = -r; y <= r; y++) {
							for (int z = -r; z <= r; z++) {
								if (x == r || y == r || z == r || -x == r || -y == r || -z == r) {
									Block block = world.getBlockAt(cx + x, cy + y, cz + z);
									if (material.equals(block.getType())) {
										foundBlock = block;

										if (spell != null) {
											if (spell.isTargetedLocationSpell())
												spell.castAtLocation(caster, block.getLocation().add(offset), power);
											else spell.cast(caster, power);
										}

										playSpellEffects(EffectPosition.TARGET, block.getLocation());
										playSpellEffectsTrail(caster.getLocation(), block.getLocation());

										if (maxBlocks > 0) {
											count++;
											if (count >= maxBlocks) break loop;
										}
									}
								}
							}
						}
					}
				}

				if (foundBlock == null) {
					sendMessage(strNotFound, caster, args);
					return PostCastAction.ALREADY_HANDLED;
				}

				if (getDistance) distance = (int) Math.round(caster.getLocation().distance(foundBlock.getLocation()));
			}

			playSpellEffects(EffectPosition.CASTER, caster);
			if (getDistance) {
				sendMessage(strCastSelf, caster, args, "%d", distance + "");
				sendMessageNear(caster, strCastOthers);
				return PostCastAction.NO_MESSAGES;
			}
		}

		return PostCastAction.HANDLE_NORMALLY;
	}

	public Material getMaterial() {
		return material;
	}

	public void setMaterial(Material material) {
		this.material = material;
	}

	public String getStrNotFound() {
		return strNotFound;
	}

	public void setStrNotFound(String strNotFound) {
		this.strNotFound = strNotFound;
	}

	public boolean shouldGetDistance() {
		return getDistance;
	}

	public void setGetDistance(boolean getDistance) {
		this.getDistance = getDistance;
	}

}
