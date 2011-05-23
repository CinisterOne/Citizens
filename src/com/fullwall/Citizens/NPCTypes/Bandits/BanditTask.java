package com.fullwall.Citizens.NPCTypes.Bandits;

import java.util.Random;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.fullwall.Citizens.ActionManager;
import com.fullwall.Citizens.CachedAction;
import com.fullwall.Citizens.Citizens;
import com.fullwall.Citizens.Constants;
import com.fullwall.Citizens.NPCs.NPCManager;
import com.fullwall.Citizens.Utils.LocationUtils;
import com.fullwall.resources.redecouverte.NPClib.HumanNPC;

public class BanditTask implements Runnable {
	@SuppressWarnings("unused")
	private Citizens plugin;

	public BanditTask(Citizens plugin) {
		this.plugin = plugin;
	}

	@Override
	public void run() {
		HumanNPC npc;
		int UID;
		Player[] online = Bukkit.getServer().getOnlinePlayers();
		for (Entry<Integer, HumanNPC> entry : NPCManager.getList().entrySet()) {
			{
				npc = entry.getValue();
				npc.updateMovement();
				UID = entry.getKey();
				for (Player p : online) {
					String name = p.getName();
					if (npc.getNPCData().isLookClose()
							|| npc.getNPCData().isTalkClose()) {
						if (LocationUtils.checkLocation(npc.getLocation(),
								p.getLocation(), Constants.banditStealRadius)) {
							cacheActions(p, npc, UID, name);
						}
					} else if (ActionManager.actions.get(UID) != null
							&& ActionManager.actions.get(UID).get(name) != null) {
						resetActions(UID, name, npc);
					}
				}
			}
		}
	}

	private void resetActions(int entityID, String name, HumanNPC npc) {
		ActionManager.resetAction(entityID, name, "takenItem", npc.isBandit());
	}

	private void cacheActions(Player p, HumanNPC npc, int entityID, String name) {
		CachedAction cached = ActionManager.getAction(entityID, name);
		if (!cached.has("takenItem") && npc.isBandit()) {
			removeRandomItem(p, npc);
			cached.set("takenItem");
		}
		ActionManager.putAction(entityID, name, cached);
	}

	/**
	 * Clears a player's inventory
	 * 
	 * @param player
	 */
	private void removeRandomItem(Player player, HumanNPC npc) {
		Random random = new Random();
		int randomSlot;
		int count = 0;
		ItemStack item = null;
		if (npc.isBandit()) {
			if (!NPCManager.validateOwnership(player, npc.getUID())) {
				int limit = player.getInventory().getSize();
				while (true) {
					randomSlot = random.nextInt(limit);
					item = player.getInventory().getItem(randomSlot);
					if (item != null) {
						player.getInventory().removeItem(item);
						player.sendMessage(ChatColor.RED
								+ npc.getStrippedName()
								+ " has stolen from your inventory!");
						break;
					} else {
						if (count >= limit) {
							break;
						}
						count += 1;
					}
				}
			}
		}
	}
}