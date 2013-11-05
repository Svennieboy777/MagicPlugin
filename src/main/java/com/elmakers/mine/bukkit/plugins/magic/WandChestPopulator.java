package com.elmakers.mine.bukkit.plugins.magic;

import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.generator.BlockPopulator;

import com.elmakers.mine.bukkit.utilities.borrowed.ConfigurationNode;

public class WandChestPopulator extends BlockPopulator {

	private final static Logger log = Logger.getLogger("Minecraft");
	private final Random random = new Random();
	private final Spells spells; 
	private final TreeMap<Float, Integer> baseProbability = new TreeMap<Float, Integer>();
	private final TreeMap<Float, String> wandProbability = new TreeMap<Float, String>();
	
	public WandChestPopulator(Spells spells, ConfigurationNode config) {
		this.spells = spells;
		
		// Fetch base probabilities
		Float currentThreshold = 0.0f;
		ConfigurationNode base = config.getNode("base_probability");
		if (base != null) {
			List<String> keys = base.getKeys();
			for (String key : keys) {
				Integer wandCount = Integer.parseInt(key);
				Float threshold = (float)base.getDouble(key, 0);
				currentThreshold += threshold;
				baseProbability.put(currentThreshold, wandCount);
			}
		}
		
		// Fetch wand probabilities
		currentThreshold = 0.0f;
		ConfigurationNode wands = config.getNode("wand_probability");
		if (wands != null) {
			List<String> keys = wands.getKeys();
			for (String key : keys) {
				String wandName = key;
				Float threshold = (float)wands.getDouble(key, 0);
				currentThreshold += threshold;
				wandProbability.put(currentThreshold, wandName);
			}
		}
	}
	
	protected <T extends Object> T weightedRandom(TreeMap<Float, T> weightMap) {
		if (weightMap.size() == 0) return null;
		
		Float maxWeight = weightMap.lastKey();
		Float selectedWeight = random.nextFloat() * maxWeight;
		for (Entry<Float, T> entry : weightMap.entrySet()) {
			if (selectedWeight < entry.getKey()) {
				return entry.getValue();
			}
		}
		
		return weightMap.lastEntry().getValue();
	}
	
	protected String[] populateChest(Chest chest) {
		// First determine how many wands to add
		Integer wandCount = weightedRandom(baseProbability);
		String[] wandNames = new String[wandCount];
		for (int i = 0; i < wandCount; i++) {
			String wandName = weightedRandom(wandProbability);
			Wand wand = Wand.createWand(spells, wandName);
			if (wand != null) {
				chest.getInventory().addItem(wand.getItem());
			} else {
				wandName = "*" + wandName;
			}
			wandNames[i] = wandName;
		}
		
		return wandNames;
	}
	
	@Override
	public void populate(World world, Random random, Chunk source) {
		for (int x = 0; x < 15; x++) {
			for (int z = 0; z < 15; z++) {
				for (int y = 0; y < 255; y++) {
					Block block = source.getBlock(x, y, z);
					if (block.getType() == Material.CHEST) {
						Chest chest = (Chest)block.getState();
						String[] wandNames = populateChest(chest);
						if (wandNames.length > 0) {
							log.info("Added wands to chest: " + StringUtils.join(wandNames, ", ") + " at " + (x + source.getX() * 16) + "," + y + "," + (z + source.getZ() * 16));
						}
					}	
				}
			}
		}
	}

}
