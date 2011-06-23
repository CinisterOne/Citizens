package com.fullwall.Citizens.Commands.Commands;

import java.util.ArrayList;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.fullwall.Citizens.Economy.EconomyHandler;
import com.fullwall.Citizens.Economy.Payment;
import com.fullwall.Citizens.Economy.ServerEconomyInterface;
import com.fullwall.Citizens.NPCTypes.Traders.ItemPrice;
import com.fullwall.Citizens.NPCTypes.Traders.Stockable;
import com.fullwall.Citizens.Utils.HelpUtils;
import com.fullwall.Citizens.Utils.MessageUtils;
import com.fullwall.Citizens.Utils.PageUtils;
import com.fullwall.Citizens.Utils.PageUtils.PageInstance;
import com.fullwall.Citizens.Utils.StringUtils;
import com.fullwall.resources.redecouverte.NPClib.HumanNPC;
import com.fullwall.resources.sk89q.commands.Command;
import com.fullwall.resources.sk89q.commands.CommandContext;
import com.fullwall.resources.sk89q.commands.CommandPermissions;
import com.fullwall.resources.sk89q.commands.CommandRequirements;

@CommandRequirements(
		requireSelected = true,
		requireOwnership = true,
		requiredType = "trader")
public class TraderCommands {

	@CommandRequirements(requiredType = "trader")
	@Command(
			aliases = "trader",
			usage = "help",
			desc = "view the trader help page",
			modifiers = "help",
			min = 1,
			max = 1)
	@CommandPermissions("use.trader")
	public static void sendTraderHelp(CommandContext args, Player player,
			HumanNPC npc) {
		HelpUtils.sendTraderHelp(player);
	}

	/**
	 * Display a trader's balance
	 * 
	 * @param player
	 * @param npc
	 */
	@CommandRequirements(requiredType = "trader", requireSelected = true)
	@Command(
			aliases = "trader",
			usage = "money",
			desc = "view a trader's balance",
			modifiers = { "money", "mon" },
			min = 1,
			max = 1)
	@CommandPermissions("use.trader")
	public static void displayMoney(CommandContext args, Player player,
			HumanNPC npc) {
		if (!EconomyHandler.useEcoPlugin())
			player.sendMessage(MessageUtils.noEconomyMessage);
		else
			player.sendMessage(StringUtils.wrap(npc.getName())
					+ " has "
					+ StringUtils.wrap(ServerEconomyInterface.format(npc
							.getBalance())) + ".");
	}

	/**
	 * Displays a list of buying/selling items for the selected trader npc.
	 * 
	 * @param player
	 * @param npc
	 * @param args
	 * @param selling
	 */
	@CommandRequirements(requiredType = "trader", requireSelected = true)
	@Command(
			aliases = "trader",
			usage = "list [buy/sell]",
			desc = "view a trader's buying/selling list",
			modifiers = "list",
			min = 2,
			max = 2)
	@CommandPermissions("use.trader")
	public static void displayList(CommandContext args, Player player,
			HumanNPC npc) {
		if (!args.getString(1).contains("s")
				&& !args.getString(1).contains("b")) {
			player.sendMessage(ChatColor.RED + "Not a valid list type.");
			return;
		}
		boolean selling = args.getString(1).contains("s");
		ArrayList<Stockable> stock = npc.getTrader().getStockables(!selling);
		int page = 1;
		String keyword = "Buying ";
		if (selling)
			keyword = "Selling ";
		if (stock.size() == 0) {
			player.sendMessage(ChatColor.GRAY + "This trader isn't "
					+ keyword.toLowerCase() + "any items.");
			return;
		}
		PageInstance instance = PageUtils.newInstance(player);
		for (Stockable stockable : stock) {
			if (stockable == null)
				continue;
			instance.push(ChatColor.GREEN
					+ keyword
					+ ": "
					+ MessageUtils.getStockableMessage(stockable,
							ChatColor.GREEN) + ".");
		}
		if (page <= instance.maxPages()) {
			instance.header(ChatColor.GOLD + "========== Trader " + keyword
					+ "List (Page %x/%y) ==========");
			instance.process(page);
		} else {
			player.sendMessage(MessageUtils.getMaxPagesMessage(page,
					instance.maxPages()));
		}
	}

	/**
	 * Sets whether the selected trader will have unlimited stock or not.
	 * 
	 * @param npc
	 * @param sender
	 * @param unlimited
	 */
	@Command(
			aliases = "trader",
			usage = "unl [true/false]",
			desc = "change the unlimited status of a trader",
			modifiers = { "unlimited", "unlim", "unl" },
			min = 2,
			max = 2)
	@CommandPermissions("admin")
	public static void changeUnlimited(CommandContext args, Player player,
			HumanNPC npc) {
		String unlimited = args.getString(1);
		if (unlimited.equalsIgnoreCase("true")
				|| unlimited.equalsIgnoreCase("on")) {
			npc.getTrader().setUnlimited(true);
			player.sendMessage(ChatColor.GREEN
					+ "The trader will now have unlimited stock!");
		} else if (unlimited.equalsIgnoreCase("false")
				|| unlimited.equalsIgnoreCase("off")) {
			npc.getTrader().setUnlimited(false);
			player.sendMessage(ChatColor.GREEN
					+ "The trader has stopped having unlimited stock.");
		} else {
			player.sendMessage(ChatColor.GREEN
					+ "Incorrect unlimited type entered. Valid values are true, on, false, off.");
		}
	}

	/**
	 * Alters the selected trader's balance depending on the arguments given.
	 * Balance is used for iConomy.
	 * 
	 * @param player
	 * @param npc
	 * @param args
	 */
	@Command(
			aliases = "trader",
			usage = "money [give/take] [amount]",
			desc = "change the balance of a trader",
			modifiers = "money",
			min = 3,
			max = 3)
	@CommandPermissions("modify.trader")
	public static void changeBalance(CommandContext args, Player player,
			HumanNPC npc) {
		if (!EconomyHandler.useEcoPlugin()) {
			player.sendMessage(MessageUtils.noEconomyMessage);
			return;
		}

		double amount;
		try {
			amount = Double.parseDouble(args.getString(2));
		} catch (NumberFormatException e) {
			player.sendMessage(ChatColor.RED
					+ "Invalid balance change amount entered.");
			return;
		}
		if (args.getString(1).contains("g")) {
			if (EconomyHandler.canBuy(new Payment(amount, true), player)) {
				EconomyHandler.pay(new Payment(-amount, true), npc, -1);
				EconomyHandler.pay(new Payment(amount, true), player, -1);
				player.sendMessage(ChatColor.GREEN
						+ "Gave "
						+ StringUtils.wrap(ServerEconomyInterface
								.format(amount))
						+ " to "
						+ StringUtils.wrap(npc.getStrippedName())
						+ ". Your balance is now "
						+ StringUtils.wrap(ServerEconomyInterface
								.getFormattedBalance(player.getName()),
								ChatColor.GREEN) + ".");
			} else {
				player.sendMessage(ChatColor.RED
						+ "You don't have enough money for that! Need "
						+ StringUtils.wrap(
								ServerEconomyInterface.format(amount
										- ServerEconomyInterface
												.getBalance(player.getName())),
								ChatColor.RED) + " more.");
			}
		} else if (args.getString(1).contains("t")) {
			if (EconomyHandler.canBuy(new Payment(amount, true), npc)) {
				EconomyHandler.pay(new Payment(amount, true), npc, -1);
				EconomyHandler.pay(new Payment(-amount, true), player, -1);
				player.sendMessage(ChatColor.GREEN
						+ "Took "
						+ StringUtils.wrap(ServerEconomyInterface
								.format(amount))
						+ " from "
						+ StringUtils.wrap(npc.getStrippedName())
						+ ". Your balance is now "
						+ StringUtils.wrap(ServerEconomyInterface
								.getFormattedBalance(player.getName())) + ".");
			} else {
				player.sendMessage(ChatColor.RED
						+ "The trader doesn't have enough money for that! It needs "
						+ StringUtils.wrap(
								ServerEconomyInterface.format(amount
										- npc.getBalance()), ChatColor.RED)
						+ " more in its balance.");
			}
		} else {
			player.sendMessage(ChatColor.RED + "Invalid argument type "
					+ StringUtils.wrap(args.getString(1), ChatColor.RED) + ".");
		}
	}

	/**
	 * Adds an item to be stocked by the selected trader.
	 * 
	 * @param player
	 * @param npc
	 * @param item
	 * @param price
	 * @param selling
	 */
	@Command(
			aliases = "trader",
			usage = "buy/sell [item] [price]",
			desc = "change the stock of a trader",
			modifiers = { "buy", "sell" },
			min = 3,
			max = 3)
	@CommandPermissions("modify.trader")
	public static void changeTraderStock(CommandContext args, Player player,
			HumanNPC npc) {
		String item = args.getString(1);
		String price = args.getString(2);
		boolean selling = args.getString(0).contains("bu");
		if (item.contains("rem")) {
			ItemStack stack = parseItemStack(price.split(":"));
			if (stack == null) {
				player.sendMessage(ChatColor.RED
						+ "Invalid item ID or name specified.");
				return;
			}
			String keyword = "buying";
			if (!selling) {
				keyword = "selling";
			}
			if (npc.getTrader().getStockable(stack.getTypeId(),
					stack.getDurability(), selling) == null) {
				player.sendMessage(ChatColor.RED
						+ "The trader is not currently " + keyword
						+ " that item.");
				return;
			} else {
				npc.getTrader().removeStockable(stack.getTypeId(),
						stack.getDurability(), selling);
				player.sendMessage(ChatColor.GREEN + "Removed "
						+ StringUtils.wrap(stack.getType().name())
						+ " from the trader's " + keyword + " list.");
			}
			return;
		}
		selling = !selling;
		String[] split = item.split(":");
		ItemStack stack = parseItemStack(split);
		if (stack == null) {
			player.sendMessage(ChatColor.RED
					+ "Invalid item ID or name specified.");
			return;
		}
		split = price.split(":");
		ItemStack cost = null;
		if (split.length != 1) {
			cost = parseItemStack(split);
			if (cost == null) {
				player.sendMessage(ChatColor.RED
						+ "Invalid item ID or name specified.");
				return;
			}
		}
		if (cost == null && !EconomyHandler.useEcoPlugin()) {
			player.sendMessage(ChatColor.GRAY
					+ "This server is not using an economy plugin, so the price cannot be "
					+ "that kind of value. If you meant to use an item as currency, "
					+ "please format it in this format: item ID:amount(:data).");
			return;
		}
		boolean iConomy = false;
		if (cost == null) {
			iConomy = true;
		}
		ItemPrice itemPrice;
		if (!iConomy) {
			itemPrice = new ItemPrice(cost);
		} else {
			itemPrice = new ItemPrice(Double.parseDouble(split[0]));
		}
		itemPrice.setiConomy(iConomy);
		Stockable s = new Stockable(stack, itemPrice, true);
		String keyword = "buying";
		if (selling) {
			keyword = "selling";
			s.setSelling(false);
		}
		if (npc.getTrader().isStocked(s)) {
			player.sendMessage(ChatColor.RED
					+ "Already "
					+ keyword
					+ " that at "
					+ MessageUtils.getStockableMessage(npc.getTrader()
							.getStockable(s), ChatColor.RED) + ".");
			return;
		}
		npc.getTrader().addStockable(s);
		player.sendMessage(ChatColor.GREEN + "The trader is now " + keyword
				+ " " + MessageUtils.getStockableMessage(s, ChatColor.GREEN)
				+ ".");
	}

	/**
	 * Creates an ItemStack from the given string ItemStack format.
	 * 
	 * @param split
	 * @return
	 */
	private static ItemStack parseItemStack(String[] split) {
		try {
			int amount = 1;
			short data = 0;
			Material mat = StringUtils.parseMaterial(split[0]);
			if (mat == null) {
				return null;
			}
			switch (split.length) {
			case 3:
				data = Short.parseShort(split[2]);
			case 2:
				amount = Integer.parseInt(split[1]);
			default:
				break;
			}
			ItemStack stack = new ItemStack(mat, amount);
			stack.setDurability(data);
			return stack;
		} catch (NumberFormatException ex) {
			return null;
		}
	}
}