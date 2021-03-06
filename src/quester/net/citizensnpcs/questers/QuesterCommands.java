package net.citizensnpcs.questers;

import java.util.concurrent.TimeUnit;

import net.citizensnpcs.api.CitizensManager;
import net.citizensnpcs.api.CommandHandler;
import net.citizensnpcs.npctypes.NPCTypeManager;
import net.citizensnpcs.questers.quests.CompletedQuest;
import net.citizensnpcs.questers.quests.ObjectiveProgress;
import net.citizensnpcs.resources.npclib.HumanNPC;
import net.citizensnpcs.resources.sk89q.Command;
import net.citizensnpcs.resources.sk89q.CommandContext;
import net.citizensnpcs.resources.sk89q.CommandPermissions;
import net.citizensnpcs.resources.sk89q.CommandRequirements;
import net.citizensnpcs.resources.sk89q.ServerCommand;
import net.citizensnpcs.utils.HelpUtils;
import net.citizensnpcs.utils.Messaging;
import net.citizensnpcs.utils.PageUtils;
import net.citizensnpcs.utils.PageUtils.PageInstance;
import net.citizensnpcs.utils.StringUtils;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandRequirements(
		requireSelected = true,
		requireOwnership = true,
		requiredType = "quester")
public class QuesterCommands extends CommandHandler {
	public static final QuesterCommands INSTANCE = new QuesterCommands();

	private QuesterCommands() {
	}

	@CommandRequirements()
	@ServerCommand()
	@Command(
			aliases = "quester",
			usage = "help",
			desc = "view the quester help page",
			modifiers = "help",
			min = 1,
			max = 1)
	@CommandPermissions("quester.use.help")
	public static void questerHelp(CommandContext args, CommandSender sender,
			HumanNPC npc) {
		NPCTypeManager.getType("quester").getCommands().sendHelpPage(sender, 1);
	}

	@Command(
			aliases = "quester",
			usage = "assign [quest]",
			desc = "assign a quest to an NPC",
			modifiers = "assign",
			min = 2,
			max = 2)
	@CommandPermissions("quester.modify.assignquest")
	public static void assign(CommandContext args, Player player, HumanNPC npc) {
		if (!QuestManager.validQuest(args.getString(1))) {
			player.sendMessage(ChatColor.GRAY
					+ "There is no quest by that name.");
			return;
		}
		Quester quester = npc.getType("quester");
		if (quester.hasQuest(args.getString(1))) {
			player.sendMessage(ChatColor.GRAY
					+ "The quester already has that quest.");
			return;
		}
		quester.addQuest(args.getString(1));
		player.sendMessage(ChatColor.GREEN + "Quest "
				+ StringUtils.wrap(args.getString(1)) + " added to "
				+ StringUtils.wrap(npc.getName()) + "'s quests. "
				+ StringUtils.wrap(npc.getName()) + " now has "
				+ StringUtils.wrap(quester.getQuests().size()) + " quests.");
	}

	@Command(
			aliases = "quester",
			usage = "remove [quest]",
			desc = "remove a quest from an NPC",
			modifiers = "remove",
			min = 2,
			max = 2)
	@CommandPermissions("quester.modify.assignquest")
	public static void remove(CommandContext args, Player player, HumanNPC npc) {
		Quester quester = npc.getType("quester");
		if (!quester.hasQuest(args.getString(1))) {
			player.sendMessage(ChatColor.GRAY
					+ "The quester doesn't have any quests by that name.");
			return;
		}
		quester.removeQuest(args.getString(1));
		player.sendMessage(ChatColor.GREEN + "Quest "
				+ StringUtils.wrap(args.getString(1)) + " removed from "
				+ StringUtils.wrap(npc.getName()) + "'s quests. "
				+ StringUtils.wrap(npc.getName()) + " now has "
				+ StringUtils.wrap(quester.getQuests().size()) + " quests.");
	}

	@Command(
			aliases = "quester",
			usage = "quests (page)",
			desc = "view the assigned quests of a quester",
			modifiers = "quests",
			min = 1,
			max = 2)
	@CommandPermissions("quester.modify.assignquest")
	public static void quests(CommandContext args, Player player, HumanNPC npc) {
		int page = args.argsLength() == 2 ? args.getInteger(2) : 1;
		if (page < 0)
			page = 1;
		PageInstance instance = PageUtils.newInstance(player);
		Quester quester = npc.getType("quester");
		instance.header(ChatColor.GREEN
				+ StringUtils.listify("Completed Quests " + ChatColor.WHITE
						+ "<%x/%y>" + ChatColor.GREEN));
		for (String quest : quester.getQuests()) {
			if (instance.maxPages() > page)
				break;
			instance.push(ChatColor.GREEN + "   - " + StringUtils.wrap(quest));
		}
		if (page > instance.maxPages()) {
			player.sendMessage(ChatColor.GRAY
					+ "Invalid page entered. There are only "
					+ instance.maxPages() + " pages.");
			return;
		}
		instance.process(page);
	}

	@CommandRequirements()
	@ServerCommand()
	@Command(
			aliases = "quest",
			usage = "help",
			desc = "view the quests help page",
			modifiers = "help",
			min = 1,
			max = 1)
	@CommandPermissions("quester.use.help")
	public static void questHelp(CommandContext args, CommandSender sender,
			HumanNPC npc) {
		sendQuestHelp(sender);
	}

	@CommandRequirements()
	@Command(
			aliases = "quest",
			usage = "abort",
			desc = "aborts current quest",
			modifiers = "abort",
			min = 1,
			max = 1)
	@CommandPermissions("quester.use.abort")
	public static void abort(CommandContext args, Player player, HumanNPC npc) {
		PlayerProfile profile = PlayerProfile.getProfile(player.getName());
		if (!profile.hasQuest()) {
			player.sendMessage(ChatColor.GRAY
					+ "You don't have a quest at the moment.");
		} else {
			profile.setProgress(null);
			player.sendMessage(ChatColor.GREEN + "Quest cleared.");
		}
	}

	@CommandRequirements()
	@Command(
			aliases = "quest",
			usage = "completed (page)",
			desc = "view completed quests",
			modifiers = "completed",
			min = 1,
			max = 2)
	@CommandPermissions("quester.use.status")
	public static void completed(CommandContext args, Player player,
			HumanNPC npc) {
		player.shootArrow();
		PlayerProfile profile = PlayerProfile.getProfile(player.getName());
		if (profile.getAllCompleted().size() == 0) {
			player.sendMessage(ChatColor.GRAY
					+ "You haven't completed any quests yet.");
			return;
		}
		int page = args.argsLength() == 2 ? args.getInteger(2) : 1;
		if (page < 0)
			page = 1;
		PageInstance instance = PageUtils.newInstance(player);
		instance.header(ChatColor.GREEN
				+ StringUtils.listify("Completed Quests " + ChatColor.WHITE
						+ "<%x/%y>" + ChatColor.GREEN));
		for (CompletedQuest quest : profile.getAllCompleted()) {
			if (instance.maxPages() > page)
				break;
			instance.push(StringUtils.wrap(quest.getName()) + " - taking "
					+ StringUtils.wrap(quest.getHours()) + " hours. Completed "
					+ StringUtils.wrap(quest.getTimesCompleted()) + " times.");
		}
		if (page > instance.maxPages()) {
			player.sendMessage(ChatColor.GRAY
					+ "Invalid page entered. There are only "
					+ instance.maxPages() + " pages.");
			return;
		}
		instance.process(page);
	}

	@CommandRequirements()
	@Command(
			aliases = "quest",
			usage = "status",
			desc = "view current quest status",
			modifiers = "status",
			min = 1,
			max = 1)
	@CommandPermissions("quester.use.status")
	public static void status(CommandContext args, Player player, HumanNPC npc) {
		PlayerProfile profile = PlayerProfile.getProfile(player.getName());
		if (!profile.hasQuest()) {
			player.sendMessage(ChatColor.GRAY
					+ "You don't have a quest at the moment.");
		} else {
			player.sendMessage(ChatColor.GREEN
					+ "Currently in the middle of "
					+ StringUtils.wrap(profile.getProgress().getQuestName())
					+ ". You have been on this quest for "
					+ TimeUnit.HOURS.convert(System.currentTimeMillis()
							- profile.getProgress().getStartTime(),
							TimeUnit.MILLISECONDS) + " hours.");
			if (profile.getProgress().fullyCompleted()) {
				player.sendMessage(ChatColor.AQUA + "Quest is completed.");
			} else {
				player.sendMessage(ChatColor.GREEN + "-" + ChatColor.AQUA
						+ " Progress report " + ChatColor.GREEN + "-");
				for (ObjectiveProgress progress : profile.getProgress()
						.getProgress()) {
					try {
						Messaging.send(player, progress.getQuestUpdater()
								.getStatus(progress));
					} catch (QuestCancelException ex) {
						player.sendMessage(ChatColor.GRAY
								+ "Cancelling quest. Reason: " + ex.getReason());
						profile.setProgress(null);
					}
				}
			}
		}
	}

	@Override
	public void addPermissions() {
		CitizensManager.addPermission("quester.use.help");
		CitizensManager.addPermission("quester.modify.assignquest");
		CitizensManager.addPermission("quester.modify.removequest");
		CitizensManager.addPermission("quester.use.status");
		CitizensManager.addPermission("quester.use.abort");
	}

	@Override
	public void sendHelpPage(CommandSender sender, int page) {
		HelpUtils.header(sender, "Quester", 1, 1);
		HelpUtils.format(sender, "quest", "help",
				"see more commands for quests");
		HelpUtils.format(sender, "quester", "assign [quest]",
				"assign a quest to an NPC");
		HelpUtils.format(sender, "quester", "remove [quest]",
				"remove a quest from an NPC");
		HelpUtils.footer(sender);
	}

	private static void sendQuestHelp(CommandSender sender) {
		HelpUtils.header(sender, "Quests", 1, 1);
		HelpUtils.format(sender, "quest", "status",
				"view your current quest status");
		HelpUtils.format(sender, "quest", "completed (page)",
				"view your completed quests");
		HelpUtils.format(sender, "quest", "abort", "abort your current quest");
		HelpUtils.footer(sender);
	}
}