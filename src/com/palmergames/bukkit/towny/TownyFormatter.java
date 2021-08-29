package com.palmergames.bukkit.towny;

import com.palmergames.bukkit.towny.event.statusscreen.NationStatusScreenEvent;
import com.palmergames.bukkit.towny.event.statusscreen.ResidentStatusScreenEvent;
import com.palmergames.bukkit.towny.event.statusscreen.TownStatusScreenEvent;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.ResidentList;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownyObject;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.Translator;
import com.palmergames.bukkit.towny.object.metadata.CustomDataField;
import com.palmergames.bukkit.towny.object.statusscreens.StatusScreen;
import com.palmergames.bukkit.towny.object.statusscreens.StatusScreenType;
import com.palmergames.bukkit.towny.permissions.TownyPerms;
import com.palmergames.bukkit.towny.utils.CombatUtil;
import com.palmergames.bukkit.towny.utils.MoneyUtil;
import com.palmergames.bukkit.towny.utils.ResidentUtil;
import com.palmergames.bukkit.towny.war.common.townruin.TownRuinSettings;
import com.palmergames.bukkit.towny.war.common.townruin.TownRuinUtil;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.util.StringMgmt;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.util.ChatPaginator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

public class TownyFormatter {

	// private static Towny plugin = null;

	public static final SimpleDateFormat lastOnlineFormat = new SimpleDateFormat("MMMMM dd '@' HH:mm");
	public static final SimpleDateFormat lastOnlineFormatIncludeYear = new SimpleDateFormat("MMMMM dd yyyy");
	public static final SimpleDateFormat registeredFormat = new SimpleDateFormat("MMM d yyyy");
	public static final SimpleDateFormat fullDateFormat = new SimpleDateFormat("MMMMM dd yyyy '@' HH:mm");

	/**
	 * 1 = Description 2 = Count
	 * 
	 * Colours: 3 = Description and : 4 = Count 5 = Colour for the start of the
	 * list
	 */
	public static final String residentListPrefixFormat = "%3$s%1$s %4$s[%2$d]%3$s:%5$s ";
    public static final String embassyTownListPrefixFormat = "%3$s%1$s:%5$s ";

	public static void initialize() {}

	public static List<String> getFormattedOnlineResidents(String prefix, ResidentList residentList, Player player) {
		List<Resident> onlineResidents = ResidentUtil.getOnlineResidentsViewable(player, residentList);
		return getFormattedResidents(prefix, onlineResidents);
	}

	public static List<String> getFormattedResidents(Town town) {
		String[] residents = getFormattedNames(town.getResidents().toArray(new Resident[0]));

		return new ArrayList<>(ChatTools.listArr(residents, Colors.Green + Translation.of("res_list") + " " + Colors.LightGreen + "[" + town.getNumResidents() + "]" + Colors.Green + ":" + Colors.White + " "));

	}

	public static List<String> getFormattedOutlaws(Town town) {

		String[] residents = getFormattedNames(town.getOutlaws().toArray(new Resident[0]));

		return new ArrayList<>(ChatTools.listArr(residents, Translation.of("outlaws") + " "));

	}
	
	public static List<String> getFormattedResidents(String prefix, List<Resident> residentList) {

		return ChatTools.listArr(getFormattedNames(residentList), String.format(residentListPrefixFormat, prefix, residentList.size(), Translation.of("res_format_list_1"), Translation.of("res_format_list_2"), Translation.of("res_format_list_3")));
	}
	
	public static List<String> getFormattedTowns(String prefix, List<Town> townList) {
		
		Town[] arrayTowns = townList.toArray(new Town[0]);

		return ChatTools.listArr(getFormattedNames(arrayTowns), String.format(embassyTownListPrefixFormat, prefix, townList.size(), Translation.of("res_format_list_1"), Translation.of("res_format_list_2"), Translation.of("res_format_list_3")));
	}

	public static String[] getFormattedNames(List<Resident> residentList) {

		return getFormattedNames(residentList.toArray(new Resident[0]));
	}

	public static String getTime() {

		SimpleDateFormat sdf = new SimpleDateFormat("hh:mm aa");
		return sdf.format(System.currentTimeMillis());
	}

	/**
	 * Gets the status screen of a TownBlock
	 * 
	 * @param townBlock the TownBlock to check
	 * @param locale Locale to use while translating   
	 * @return StatusScreen containing the results.
	 */
	public static StatusScreen getStatus(TownBlock townBlock, Locale locale) {

		StatusScreen screen = new StatusScreen(StatusScreenType.TOWNBLOCK_STATUS, new LinkedHashMap<>());
		final Translator translator = Translator.locale(locale);
		
		TownyObject owner;
		Town town = townBlock.getTownOrNull();
		TownyWorld world = townBlock.getWorld();
		boolean preventPVP = CombatUtil.preventPvP(world, townBlock);

		if (townBlock.hasResident())
			owner = townBlock.getResidentOrNull();
		else
			owner = town;

		screen.addComponentOf("townblock_title", ChatTools.formatTitle("(" + townBlock.getCoord().toString() + ") " + owner.getFormattedName() + ((BukkitTools.isOnline(owner.getName())) ? translator.of("online") : "")));
		if (!townBlock.getType().equals(TownBlockType.RESIDENTIAL))
			screen.addComponentOf("townblock_plotType", translator.of("status_plot_type") + townBlock.getType().toString());
		screen.addComponentOf("perm1", translator.of("status_perm") + ((owner instanceof Resident) ? townBlock.getPermissions().getColourString().replace("n", "t") : townBlock.getPermissions().getColourString().replace("f", "r")));
		screen.addComponentOf("perm2", translator.of("status_perm") + ((owner instanceof Resident) ? townBlock.getPermissions().getColourString2().replace("n", "t") : townBlock.getPermissions().getColourString2().replace("f", "r")));
		screen.addComponentOf("perm3", translator.of("status_pvp") + ((!preventPVP) ? translator.of("status_on"): translator.of("status_off")) + 
				translator.of("explosions") + ((world.isForceExpl() || townBlock.getPermissions().explosion) ? translator.of("status_on"): translator.of("status_off")) + 
				translator.of("firespread") + ((town.isFire() || world.isForceFire() || townBlock.getPermissions().fire) ? translator.of("status_on"):translator.of("status_off")) + 
				translator.of("mobspawns") + ((world.isForceTownMobs() || townBlock.getPermissions().mobs) ?  translator.of("status_on"): translator.of("status_off")));

		if (townBlock.hasPlotObjectGroup())
			screen.addComponentOf("plotgroup", translator.of("status_plot_group_name_and_size", townBlock.getPlotObjectGroup().getName(), townBlock.getPlotObjectGroup().getTownBlocks().size()));
		if (townBlock.getClaimedAt() > 0)
			screen.addComponentOf("claimedat", translator.of("msg_plot_perm_claimed_at", registeredFormat.format(townBlock.getClaimedAt())));
		
		if (townBlock.getTrustedResidents().size() > 0)
			screen.addComponentOf("trusted", translator.of("status_trustedlist") + StringMgmt.join(new ArrayList<>(townBlock.getTrustedResidents()), ", "));
		
		// Add any metadata which opt to be visible.
		List<String> fields = getExtraFields(townBlock);
		if (!fields.isEmpty())
			for (int i = 0; i < fields.size(); i++) 
				screen.addComponentOf("extraField" + i, fields.get(i));
		
		return screen;
	}

	/**
	 *  Gets the status screen of a Resident, using the default locale
	 *
	 * @param resident the resident to check the status of
	 * @param player make sure the resident is an online player
	 * @return StatusScreen containing the results.
	 */
	public static StatusScreen getStatus(Resident resident, Player player) {
		return getStatus(resident, player, Translation.getDefaultLocale());
	}
	
	/**
	 *  Gets the status screen of a Resident
	 *  
	 * @param resident the resident to check the status of
	 * @param player make sure the resident is an online player
	 * @param locale Locale to use while translating   
	 * @return StatusScreen containing the results.
	 */
	public static StatusScreen getStatus(Resident resident, Player player, Locale locale) {

		StatusScreen screen = new StatusScreen(StatusScreenType.TOWNBLOCK_STATUS, new LinkedHashMap<>());
		final Translator translator = Translator.locale(locale);

		// ___[ King Harlus ]___
		screen.addComponentOf("title", ChatTools.formatTitle(resident.getFormattedName() + ((BukkitTools.isOnline(resident.getName()) && (player != null) && (player.canSee(BukkitTools.getPlayer(resident.getName())))) ? translator.of("online2") : "")));

		// First used if last online is this year, 2nd used if last online is early than this year.
		// Registered: Sept 3 2009 | Last Online: March 7 @ 14:30
		// Registered: Sept 3 2009 | Last Online: March 7 2009
		screen.addComponentOf("registered", getResidentRegisteredLine(resident, translator));
		
		// Owner of: 4 plots
		// Perm: Build = f-- Destroy = fa- Switch = fao Item = ---
		screen.addComponentOf("ownsXPlots", translator.of("owner_of_x_plots", resident.getTownBlocks().size()));
		screen.addComponentOf("perm1", translator.of("status_perm") + resident.getPermissions().getColourString().replace("n", "t"));
		screen.addComponentOf("perm2", translator.of("status_perm") + resident.getPermissions().getColourString2().replace("n", "t"));
		screen.addComponentOf("perm3", translator.of("status_pvp") + ((resident.getPermissions().pvp) ? translator.of("status_on"): translator.of("status_off")) + 
				translator.of("explosions") + ((resident.getPermissions().explosion) ? translator.of("status_on"): translator.of("status_off")) + 
				translator.of("firespread") + ((resident.getPermissions().fire) ? translator.of("status_on"): translator.of("status_off")) + 
				translator.of("mobspawns") + ((resident.getPermissions().mobs) ? translator.of("status_on"): translator.of("status_off")));

		// Bank: 534 coins
		if (TownyEconomyHandler.isActive())
			screen.addComponentOf("bank", translator.of("status_bank", resident.getAccount().getHoldingFormattedBalance()));

		// Town: Camelot
		String townLine = translator.of("status_town") + (!resident.hasTown() ? translator.of("status_no_town") : resident.getTownOrNull().getFormattedName());
		if (!resident.hasTown())
			screen.addComponentOf("town", townLine);
		else {
			Town town = resident.getTownOrNull();
			String[] residents = getFormattedNames(town.getResidents().toArray(new Resident[0]));
			if (residents.length > 34)
				residents = shortenOverlengthArray(residents, 35, translator);
			screen.addComponentOf("town", townLine,
				HoverEvent.showText(Component.text( Colors.translateColorCodes(String.format(TownySettings.getPAPIFormattingTown(), town.getFormattedName())))
					.append(Component.newline())
					.append(Component.text(translator.of("rank_list_mayor", town.getMayor().getFormattedName())))
					.append(Component.newline())
					.append(Component.text(translator.of("status_town_reslist", town.getNumResidents()) + StringMgmt.join(residents, ", ")))),
				ClickEvent.runCommand("/towny:town " + town.getName())
			);
		}
		if (resident.isNPC()) {
			screen.addComponentOf("npcstatus", translator.of("msg_status_npc", resident.getName()));
			// Add any metadata which opt to be visible.
			List<String> fields = getExtraFields(resident);
			if (!fields.isEmpty())
				for (int i = 0; i < fields.size(); i++) 
					screen.addComponentOf("extraField" + i, fields.get(i));
			return screen;
		}
		
		// Embassies in: Camelot, London, Tokyo.
		List<Town> townEmbassies = getResidentsEmbassyTowns(resident);
		if (townEmbassies.size() > 0)
			screen.addComponentOf("embassiesInTowns", translator.of("status_embassy_town") + StringMgmt.join(townEmbassies, ", "));
			
		// Town ranks
		if (resident.hasTown() && !resident.getTownRanks().isEmpty())
			screen.addComponentOf("townRanks", translator.of("status_town_ranks") + StringMgmt.capitalize(StringMgmt.join(resident.getTownRanks(), ", ")));
		
		//Nation ranks
		if (resident.hasNation() && !resident.getNationRanks().isEmpty())
			screen.addComponentOf("nationRanks", translator.of("status_nation_ranks") + StringMgmt.capitalize(StringMgmt.join(resident.getNationRanks(), ", ")));
		
		// Jailed: yes if they are jailed.
		if (resident.isJailed())
			screen.addComponentOf("jailLine", translator.of("jailed_in_town", resident.getJailTown().getName()) + ( resident.hasJailTime() ? translator.of("msg_jailed_for_x_hours", resident.getJailHours()) :  ""));
		
		// Friends [12]: James, Carry, Mason
		if (resident.getFriends() != null && !resident.getFriends().isEmpty())
			screen.addComponentOf("friendsLine", translator.of("status_friends", resident.getFriends().size()) + StringMgmt.join(resident.getFriends(), ", "));
		
		// Add any metadata which opt to be visible.
		List<String> fields = getExtraFields(resident);
		if (!fields.isEmpty())
			for (int i = 0; i < fields.size(); i++) 
				screen.addComponentOf("extraField" + i, fields.get(i));
		
		ResidentStatusScreenEvent event = new ResidentStatusScreenEvent(screen, resident);
		Bukkit.getPluginManager().callEvent(event);
		if (event.hasAdditionalLines())
			for (int i = 0; i < event.getAdditionalLines().size(); i++)
				screen.addComponentOf("eventAddedLines"+i, event.getAdditionalLines().get(i));

		return screen;
	}
	
	private static String getResidentRegisteredLine(Resident resident, Translator translator) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(resident.getLastOnline());
		int currentYear = cal.get(Calendar.YEAR);
		cal.setTimeInMillis(System.currentTimeMillis());
		int lastOnlineYear = cal.get(Calendar.YEAR);
		if (!resident.isNPC()) // Not an NPC: show more detailed info.
			if (currentYear == lastOnlineYear) 
				return translator.of("registered_last_online", registeredFormat.format(resident.getRegistered()), lastOnlineFormat.format(resident.getLastOnline()));
			else 
				return translator.of("registered_last_online", registeredFormat.format(resident.getRegistered()), lastOnlineFormatIncludeYear.format(resident.getLastOnline()));
		else // An NPC: show their created date.
			return translator.of("npc_created", registeredFormat.format(resident.getRegistered()));
	}

	private static List<Town> getResidentsEmbassyTowns(Resident resident) {
		List<Town> townEmbassies = new ArrayList<>();
		String actualTown = resident.hasTown() ? TownyAPI.getInstance().getResidentTownOrNull(resident).getName() : "";
		
		for(TownBlock tB : resident.getTownBlocks()) {
			Town town = tB.getTownOrNull();
			if (town == null) continue;
			if (!actualTown.equals(town.getName()) && !townEmbassies.contains(town))
				townEmbassies.add(town);
		}
		return townEmbassies;
	}

	public static List<String> getRanks(Town town) {
		return getRanks(town, Translation.getDefaultLocale());
	}

	/**
	 * Returns a Chat Formatted List of all town residents who hold a rank.
	 * 
	 * @param town the town for which to check against.
	 * @return a list containing formatted rank data.
	 */
	public static List<String> getRanks(Town town, Locale locale) {

		List<String> ranklist = new ArrayList<>();

		String towntitle = town.getFormattedName();
		towntitle += Translation.of("rank_list_title", locale);
		ranklist.add(ChatTools.formatTitle(towntitle));
		ranklist.add(Translation.of("rank_list_mayor", locale, town.getMayor().getFormattedName()));

		getRanks(town, ranklist, locale);
		return ranklist;
	}

	private static void getRanks(Town town, List<String> ranklist, Locale locale) {
		List<Resident> residents = town.getResidents();
		List<String> townRanks = TownyPerms.getTownRanks();
		List<Resident> residentWithRank = new ArrayList<>();

		for (String rank : townRanks) {
			for (Resident r : residents) {

				if ((r.getTownRanks() != null) && (r.getTownRanks().contains(rank))) {
					residentWithRank.add(r);
				}
			}
			ranklist.addAll(getFormattedResidents(StringMgmt.capitalize(rank), residentWithRank));
			residentWithRank.clear();
		}
		
		if (town.getTrustedResidents().size() > 0)
			ranklist.addAll(getFormattedResidents(Translation.of("status_trustedlist", locale), new ArrayList<>(town.getTrustedResidents())));
	}

	/**
	 * 
	 * @param out - List&lt;String&gt;
	 * @return a string list with all lines formatted to fit within the MC chat box. 
	 */
	public static List<String> formatStatusScreens(List<String> out) {

		List<String> formattedOut = new ArrayList<>();
		for (String line: out) {
			for (String string : ChatPaginator.wordWrap(line, ChatPaginator.UNBOUNDED_PAGE_WIDTH))
				formattedOut.add(string);
		}
		return formattedOut;
	}

	/**
	 * Gets the status screen of a Town
	 * 
	 * @param town the town in which to check
	 * @param locale Locale to use while translating   
	 * @return StatusScreen containing the results.
	 */
	public static StatusScreen getStatus(Town town, Locale locale) {

		final Translator translator = Translator.locale(locale);
		StatusScreen screen = new StatusScreen(StatusScreenType.TOWN_STATUS, new LinkedHashMap<>());
		TownyWorld world = town.getHomeblockWorld();

		// ___[ Raccoon City ]___
		screen.addComponentOf("title", ChatTools.formatTitle(town));
		
		// (PvP) (Open) (Peaceful)
		List<String> sub = getTownSubtitle(town, world, translator);
		if (!sub.isEmpty())
			screen.addComponentOf("subtitle", ChatTools.formatSubTitle(StringMgmt.join(sub, " ")));
		
		// Board: Get your fried chicken
		if (town.getBoard() != null && !town.getBoard().isEmpty())
			screen.addComponentOf("board", translator.of("status_town_board", town.getBoard()));

		// Created Date
		if (town.getRegistered() != 0) 
			screen.addComponentOf("registered", translator.of("status_founded", registeredFormat.format(town.getRegistered())));

		// Town Size: 0 / 16 [Bought: 0/48] [Bonus: 0] [Home: 33,44]
		screen.addComponentOf("townblocks", translator.of("status_town_size_part_1", town.getTownBlocks().size(), TownySettings.getMaxTownBlocks(town)) +
	            (TownySettings.isSellingBonusBlocks(town) ? translator.of("status_town_size_part_2", town.getPurchasedBlocks(), TownySettings.getMaxPurchasedBlocks(town)) : "") + 
	            (town.getBonusBlocks() > 0 ? translator.of("status_town_size_part_3", town.getBonusBlocks()) : "") + 
	            (TownySettings.getNationBonusBlocks(town) > 0 ? translator.of("status_town_size_part_4", TownySettings.getNationBonusBlocks(town)) : "") + 
	            (town.isPublic() ? translator.of("status_town_size_part_5") + 
	            		(TownySettings.getTownDisplaysXYZ() ? (town.hasSpawn() ? BukkitTools.convertCoordtoXYZ(town.getSpawnOrNull()) : translator.of("status_no_town"))  + "]" 
	            				: (town.hasHomeBlock() ? town.getHomeBlockOrNull().getCoord().toString() : translator.of("status_no_town")) + "]") : "")
	           );

		// Outposts: 3
		if (TownySettings.isAllowingOutposts()) {
			String outpostLine = "";
			if (TownySettings.isOutpostsLimitedByLevels()) {
				if (town.hasOutpostSpawn())
					if (!town.hasNation())
						outpostLine = translator.of("status_town_outposts", town.getMaxOutpostSpawn(), town.getOutpostLimit());
					else {
						int nationBonus = (Integer) TownySettings.getNationLevel(town.getNationOrNull()).get(TownySettings.NationLevel.NATION_BONUS_OUTPOST_LIMIT);
						outpostLine = translator.of("status_town_outposts", town.getMaxOutpostSpawn(), town.getOutpostLimit()) +
								(nationBonus > 0 ? translator.of("status_town_outposts2", nationBonus) : "");
						}
				else 
					outpostLine = translator.of("status_town_outposts3", town.getOutpostLimit());
			} else if (town.hasOutpostSpawn()) {
				outpostLine = translator.of("status_town_outposts4", town.getMaxOutpostSpawn());
			}
			screen.addComponentOf("outposts", outpostLine, ClickEvent.runCommand("/towny:town outpost list"));
		}

		// Permissions: B=rao D=--- S=ra-
		screen.addComponentOf("perm1", translator.of("status_perm") + town.getPermissions().getColourString().replace("f", "r"));
		screen.addComponentOf("perm2", translator.of("status_perm") + town.getPermissions().getColourString2().replace("f", "r"));
		screen.addComponentOf("perm3", translator.of("explosions2") + ((town.isBANG() || world.isForceExpl()) ? translator.of("status_on"): translator.of("status_off")) + 
				translator.of("firespread") + ((town.isFire() || world.isForceFire()) ? translator.of("status_on"): translator.of("status_off")) + 
				translator.of("mobspawns") + ((town.hasMobs() || world.isForceTownMobs()) ? translator.of("status_on"): translator.of("status_off")));

		if (town.isRuined()) {
			screen.addComponentOf("ruinedTime", translator.of("msg_time_remaining_before_full_removal", TownRuinSettings.getTownRuinsMaxDurationHours() - TownRuinUtil.getTimeSinceRuining(town)));
			if (TownRuinSettings.getTownRuinsReclaimEnabled()) {
				if (TownRuinUtil.getTimeSinceRuining(town) < TownRuinSettings.getTownRuinsMinDurationHours())
					screen.addComponentOf("reclaim", translator.of("msg_time_until_reclaim_available", TownRuinSettings.getTownRuinsMinDurationHours() - TownRuinUtil.getTimeSinceRuining(town)));
				else 
					screen.addComponentOf("reclaim", translator.of("msg_reclaim_available"));
			}
			// Only display the remaining fields if town is not ruined
		} else {
			// | Bank: 534 coins
			if (TownyEconomyHandler.isActive())
				screen.addComponentOf("bankstring", getTownBankString(town, translator));

			// Nation: Azur Empire
			if (town.hasNation()) {
				// Shown in Hover Text: Towns [44]: James City, Carry Grove, Mason Town
				String[] towns2 = getFormattedNames(town.getNationOrNull().getTowns().toArray(new Town[0]));
				if (towns2.length > 10)
					towns2 = shortenOverlengthArray(towns2, 11, translator);

				screen.addComponentOf("nation", translator.of("status_town_nation", town.getNationOrNull().getName()), 
						HoverEvent.showText(Component.text(Colors.translateColorCodes(String.format(TownySettings.getPAPIFormattingNation(), town.getNationOrNull().getFormattedName())))
								.append(Component.newline())
								.append(Component.text(translator.of("status_nation_king", town.getNationOrNull().getCapital().getMayor().getFormattedName())))
								.append(Component.newline())
								.append(Component.text(translator.of("status_nation_towns", town.getNationOrNull().getNumTowns()) + StringMgmt.join(towns2, ", ")))),
						ClickEvent.runCommand("/towny:nation " + town.getNationOrNull().getName())
						);
			}
			// Mayor: MrSand
			screen.addComponentOf("mayor", translator.of("rank_list_mayor", town.getMayor().getFormattedName()),
					HoverEvent.showText(Component.text(translator.of("registered_last_online", registeredFormat.format(town.getMayor().getRegistered()), lastOnlineFormatIncludeYear.format(town.getMayor().getLastOnline())))),
					ClickEvent.runCommand("/towny:resident " + town.getMayor().getName())
					);

			// Assistants [2]: Sammy, Ginger
			List<String> ranklist = new ArrayList<>();
			getRanks(town, ranklist, locale);
			for (String string : ranklist) 
			for (int i = 0; i < ranklist.size(); i++) 
				screen.addComponentOf("ranks"+i, string);

			// Residents [12]: James, Carry, Mason
			String[] residents = getFormattedNames(town.getResidents().toArray(new Resident[0]));
			if (residents.length > 34)
				residents = shortenOverlengthArray(residents, 35, translator);
			screen.addComponentOf("residents", translator.of("status_town_reslist", town.getNumResidents()) + StringMgmt.join(residents, ", "),
				ClickEvent.runCommand("/towny:town reslist "+ town.getName()));

		}
		// Add any metadata which opt to be visible.
		List<String> fields = getExtraFields(town);
		if (!fields.isEmpty())
			for (int i = 0; i < fields.size(); i++) 
				screen.addComponentOf("extraField" + i, fields.get(i));
		
		TownStatusScreenEvent event = new TownStatusScreenEvent(screen, town);
		Bukkit.getPluginManager().callEvent(event);
		if (event.hasAdditionalLines())
			for (int i = 0; i < event.getAdditionalLines().size(); i++)
				screen.addComponentOf("eventAddedLines"+i, event.getAdditionalLines().get(i));

		return screen;

	}

	private static String[] shortenOverlengthArray(String[] array, int i, Translator translator) {
		String[] entire = array;
		array = new String[i + 1];
		System.arraycopy(entire, 0, array, 0, i);
		array[i] = translator.of("status_town_reslist_overlength");
		return array;
	}

	private static String getTownBankString(Town town, Translator translator) {
		String bankString = translator.of(town.isBankrupt() ? "status_bank_bankrupt" : "status_bank", town.getAccount().getHoldingFormattedBalance());
		if (town.isBankrupt()) {
			if (town.getAccount().getDebtCap() == 0)
				town.getAccount().setDebtCap(MoneyUtil.getEstimatedValueOfTown(town));
			bankString += " " + translator.of("status_debtcap", "-" + TownyEconomyHandler.getFormattedBalance(town.getAccount().getDebtCap()));
		}
		if (town.hasUpkeep())
			bankString += translator.of("status_bank_town2", BigDecimal.valueOf(TownySettings.getTownUpkeepCost(town)).setScale(2, RoundingMode.HALF_UP).doubleValue());
		if (TownySettings.getUpkeepPenalty() > 0 && town.isOverClaimed())
			bankString += translator.of("status_bank_town_penalty_upkeep", TownySettings.getTownPenaltyUpkeepCost(town));
		bankString += translator.of("status_bank_town3", town.getTaxes()) + (town.isTaxPercentage() ? "%" : "");
		return bankString;
	}

	private static List<String> getTownSubtitle(Town town, TownyWorld world, Translator translator) {
		List<String> sub = new ArrayList<>();
		if (!town.isAdminDisabledPVP() && (town.isPVP() || world.isForcePVP()))
			sub.add(translator.of("status_title_pvp"));
		if (town.isOpen())
			sub.add(translator.of("status_title_open"));
		if (town.isPublic())
			sub.add(translator.of("status_public"));
		if (town.isNeutral())
			sub.add(translator.of("status_town_title_peaceful"));
		if (town.isConquered())
			sub.add(translator.of("msg_conquered"));
		return sub;
	}

	private static List<String> getNationSubtitle(Nation nation, Translator translator) {
		List<String> sub = new ArrayList<>();
		if (nation.isOpen())
			sub.add(translator.of("status_title_open"));
		if (nation.isPublic())
			sub.add(translator.of("status_public"));
		if (nation.isNeutral())
			sub.add(translator.of("status_town_title_peaceful"));
		return sub;
	}
	
	/**
	 * Gets the status screen of a Nation, using the default locale.
	 *
	 * @param nation the nation to check against
	 * @return StatusScreen containing the results.
	 */
	public static StatusScreen getStatus(Nation nation) {
		return getStatus(nation, Translation.getDefaultLocale());
	}

	/**
	 * Gets the status screen of a Nation
	 * 
	 * @param nation the nation to check against
	 * @param locale The locale to use while translating   
	 * @return StatusScreen containing the results.
	 */
	public static StatusScreen getStatus(Nation nation, Locale locale) {

		StatusScreen screen = new StatusScreen(StatusScreenType.NATION_STATUS, new LinkedHashMap<>());
		final Translator translator = Translator.locale(locale);

		// ___[ Azur Empire (Open)]___
		screen.addComponentOf("nation_title", ChatTools.formatTitle(nation));
		List<String> sub = getNationSubtitle(nation, translator);
		if (!sub.isEmpty())
			screen.addComponentOf("subtitle", ChatTools.formatSubTitle(StringMgmt.join(sub, " ")));

		// Board: Get your fried chicken
		if (nation.getBoard() != null &&!nation.getBoard().isEmpty())
			screen.addComponentOf("board", translator.of("status_town_board", nation.getBoard()));
		
		// Created Date
		long registered = nation.getRegistered();
		if (registered != 0)
			screen.addComponentOf("registered", translator.of("status_founded", registeredFormat.format(nation.getRegistered())));

		// Bank: 534 coins
		if (TownyEconomyHandler.isActive()) {
			String bankline = translator.of("status_bank", nation.getAccount().getHoldingFormattedBalance());

			if (TownySettings.getNationUpkeepCost(nation) > 0)
				bankline += translator.of("status_bank_town2", TownySettings.getNationUpkeepCost(nation));
			
			bankline += translator.of("status_nation_tax", nation.getTaxes());
			screen.addComponentOf("bankLine", bankline);
		}

		if (nation.isPublic())
			screen.addComponentOf("home", translator.of("status_town_size_part_5") + (nation.hasSpawn() ? Coord.parseCoord(nation.getSpawnOrNull()).toString() : translator.of("status_no_town")) + "]");

		// King: King Harlus
		if (nation.getNumTowns() > 0 && nation.hasCapital() && nation.getCapital().hasMayor())
			screen.addComponentOf("king", translator.of("status_nation_king", nation.getCapital().getMayor().getFormattedName()));
		
		// Assistants [2]: Sammy, Ginger
		List<String> ranklist = new ArrayList<>();
		List<Resident> residents = nation.getResidents();
		
		List<String> nationranks = TownyPerms.getNationRanks();
		List<Resident> residentwithrank = new ArrayList<>();

		for (String rank : nationranks) {
			for (Resident r : residents) {
				if ((r.getNationRanks() != null) && (r.getNationRanks().contains(rank))) {
					residentwithrank.add(r);
				}
			}
			ranklist.addAll(getFormattedResidents(StringMgmt.capitalize(rank), residentwithrank));
			residentwithrank.clear();
		}
		for (int i = 0; i < nationranks.size(); i++)
			screen.addComponentOf("rank" + 1, nationranks.get(i));
		
		// Towns [44]: James City, Carry Grove, Mason Town
		String[] towns = getFormattedNames(nation.getTowns().toArray(new Town[0]));
		if (towns.length > 10)
			towns = shortenOverlengthArray(towns, 11, translator);

		screen.addComponentOf("towns", translator.of("status_nation_towns", nation.getNumTowns()) + StringMgmt.join(towns, ", "));
		
		// Allies [4]: James Nation, Carry Territory, Mason Country
		String[] allies = getFormattedNames(nation.getAllies().toArray(new Nation[0]));
		if (allies.length > 10)
			allies = shortenOverlengthArray(allies, 11, translator);
		screen.addComponentOf("allies", translator.of("status_nation_allies", nation.getAllies().size()) + StringMgmt.join(allies, ", "));

		// Enemies [4]: James Nation, Carry Territory, Mason Country
		String[] enemies = getFormattedNames(nation.getEnemies().toArray(new Nation[0]));
		if (enemies.length > 10)
			enemies = shortenOverlengthArray(enemies, 11, translator);
		screen.addComponentOf("allies", translator.of("status_nation_enemies", nation.getEnemies().size()) + StringMgmt.join(enemies, ", "));

		// Add any metadata which opt to be visible.
		List<String> fields = getExtraFields(nation);
		if (!fields.isEmpty())
			for (int i = 0; i < fields.size(); i++) 
				screen.addComponentOf("extraField" + i, fields.get(i));
		
		NationStatusScreenEvent event = new NationStatusScreenEvent(screen, nation);
		Bukkit.getPluginManager().callEvent(event);
		if (event.hasAdditionalLines())
			for (int i = 0; i < event.getAdditionalLines().size(); i++)
				screen.addComponentOf("eventAddedLines"+i, event.getAdditionalLines().get(i));

		return screen;
	}

	/**
	 * Gets the status screen for a World, using the default locale
	 *
	 * @param world the world to check
	 * @return StatusScreen containing the results.
	 */
	public static StatusScreen getStatus(TownyWorld world) {
		return getStatus(world, Translation.getDefaultLocale());
	}

	/**
	 * Gets the status screen for a World
	 * 
	 * @param world the world to check
	 * @param locale Locale to use while translating   
	 * @return StatusScreen containing the results.
	 */
	public static StatusScreen getStatus(TownyWorld world, Locale locale) {

		StatusScreen screen = new StatusScreen(StatusScreenType.TOWNYWORLD_STATUS, new LinkedHashMap<>());
		final Translator translator = Translator.locale(locale);

		// ___[ World (PvP) ]___
		String title = world.getFormattedName();
		title += ((world.isPVP() || world.isForcePVP()) ? translator.of("status_title_pvp") : "");
		title += (world.isClaimable() ? translator.of("status_world_claimable") : translator.of("status_world_noclaims"));
		screen.addComponentOf("townyworld_title", ChatTools.formatTitle(title));

		if (!world.isUsingTowny()) {
			screen.addComponentOf("not_using_towny", translator.of("msg_set_use_towny_off"));
		} else {
			// ForcePvP: ON | FriendlyFire: ON 
			// ForcePvP: ON | FriendlyFire: ON 
			screen.addComponentOf("pvp", translator.of("status_world_forcepvp") + (world.isForcePVP() ? translator.of("status_on") : translator.of("status_off")) + Colors.Gray + " | " + 
					translator.of("status_world_friendlyfire") + (world.isFriendlyFireEnabled() ? translator.of("status_on") : translator.of("status_off")));
			// Fire: ON | ForceFire: ON
			screen.addComponentOf("fire", translator.of("status_world_fire") + (world.isFire() ? translator.of("status_on") : translator.of("status_off")) + Colors.Gray + " | " + 
					translator.of("status_world_forcefire") + (world.isForceFire() ? translator.of("status_forced") : translator.of("status_adjustable")));
			// Explosion: ON | ForceExplosion: ON
			screen.addComponentOf("explosions", translator.of("explosions2") + ": " + (world.isExpl() ? translator.of("status_on") : translator.of("status_off")) + Colors.Gray + " | " + 
				    translator.of("status_world_forceexplosion") + (world.isForceExpl() ? translator.of("status_forced") : translator.of("status_adjustable")));
			// WorldMobs: ON | Wilderness Mobs: ON
			screen.addComponentOf("mobs", translator.of("status_world_worldmobs") + (world.hasWorldMobs() ? translator.of("status_on") : translator.of("status_off")) + Colors.Gray + " | " + 
				    translator.of("status_world_wildernessmobs") + (world.hasWildernessMobs() ? translator.of("status_on") : translator.of("status_off")));
			// ForceTownMobs: ON
			screen.addComponentOf("townmobs", translator.of("status_world_forcetownmobs") + (world.isForceTownMobs() ? translator.of("status_forced") : translator.of("status_adjustable")));
			// War will be allowed in this world.
			screen.addComponentOf("war_allowed", Colors.Green + (world.isWarAllowed() ? translator.of("msg_set_war_allowed_on") : translator.of("msg_set_war_allowed_off")));
			// Unclaim Revert: ON
			screen.addComponentOf("unclaim_revert", translator.of("status_world_unclaimrevert") + (world.isUsingPlotManagementRevert() ? translator.of("status_on_good") : translator.of("status_off_bad"))); 
			// Entity Explosion Revert: ON | Block Explosion Revert: ON
			screen.addComponentOf("explosion_reverts", translator.of("status_world_explrevert_entity") + (world.isUsingPlotManagementWildEntityRevert() ? translator.of("status_on_good") : translator.of("status_off_bad")) + Colors.Gray + " | " +
			        translator.of("status_world_explrevert_block") + (world.isUsingPlotManagementWildBlockRevert() ? translator.of("status_on_good") : translator.of("status_off_bad")));
			// Plot Clear Block Delete: ON (see /towny plotclearblocks) | OFF
			screen.addComponentOf("plot_clear", translator.of("status_plot_clear_deletion") + (world.isUsingPlotManagementMayorDelete() ? translator.of("status_on") + Colors.LightGreen +" (see /towny plotclearblocks)" : translator.of("status_off"))); 
			// Wilderness:
			//     Build, Destroy, Switch, ItemUse
			//     Ignored Blocks: see /towny wildsblocks
			screen.addComponentOf("wilderness", Colors.Green + world.getUnclaimedZoneName() + ":");
			screen.addComponentOf("perms1", "    " + (world.getUnclaimedZoneBuild() ? Colors.LightGreen : Colors.Rose) + "Build" + Colors.Gray + ", " + (world.getUnclaimedZoneDestroy() ? Colors.LightGreen : Colors.Rose) + "Destroy" + Colors.Gray + ", " + (world.getUnclaimedZoneSwitch() ? Colors.LightGreen : Colors.Rose) + "Switch" + Colors.Gray + ", " + (world.getUnclaimedZoneItemUse() ? Colors.LightGreen : Colors.Rose) + "ItemUse");
			screen.addComponentOf("perms2", "    " + translator.of("status_world_ignoredblocks") + Colors.LightGreen + " see /towny wildsblocks");

			// Add any metadata which opt to be visible.
			List<String> fields = getExtraFields(world);
			if (!fields.isEmpty())
				for (int i = 0; i < fields.size() -1; i++) 
					screen.addComponentOf("extraField" + i, fields.get(i));
		}
		
		return screen;
	}

	public static List<String> getTaxStatus(Resident resident) {
		return getTaxStatus(resident, Translation.getDefaultLocale());
	}
	
	/**
	 * Returns the tax info this resident will have to pay at the next new day.
	 * 
	 * @param resident the resident to check
	 * @param locale Locale to use while translating   
	 * @return tax status message
	 */
	public static List<String> getTaxStatus(Resident resident, Locale locale) {

		List<String> out = new ArrayList<>();
		final Translator translator = Translator.locale(locale);
		
		Town town;
		boolean taxExempt = TownyPerms.getResidentPerms(resident).containsKey("towny.tax_exempt");
		double plotTax = 0.0;
		double townTax = 0.0;

		out.add(ChatTools.formatTitle(resident.getFormattedName() + ((BukkitTools.isOnline(resident.getName())) ? Colors.LightGreen + " (Online)" : "")));

		out.add(Translatable.of("owner_of_x_plots", resident.getTownBlocks().size()).forLocale(resident));

		/*
		 * Calculate what the player will be paying their town for tax.
		 */
		if (resident.hasTown()) {
			town = TownyAPI.getInstance().getResidentTownOrNull(resident);

			if (taxExempt) {
				out.add(translator.of("status_res_taxexempt"));
			} else {
				if (town.isTaxPercentage())
					townTax = Math.min(resident.getAccount().getHoldingBalance() * town.getTaxes() / 100, town.getMaxPercentTaxAmount());
				else
					townTax = town.getTaxes();
				out.add(translator.of("status_res_tax", TownyEconomyHandler.getFormattedBalance(townTax)));
			}
		}

		/*
		 * Calculate what the player will be paying for their plots' tax.
		 */
		if (resident.getTownBlocks().size() > 0) {

			for (TownBlock townBlock : new ArrayList<>(resident.getTownBlocks())) {
				town = townBlock.getTownOrNull();
				if (town != null) {
					if (taxExempt && town.hasResident(resident)) // Resident will not pay any tax for plots owned by their towns.
						continue;
					plotTax += townBlock.getType().getTax(town);
				}
			}

			out.add(translator.of("status_res_plottax") + TownyEconomyHandler.getFormattedBalance(plotTax));
		}
		out.add(translator.of("status_res_totaltax") + TownyEconomyHandler.getFormattedBalance(townTax + plotTax));

		return out;
	}

	/**
	 * @param obj The {@link TownyObject} to get the formatted name from.
	 * 
	 * @return The formatted name of the object.
	 * 
	 * @deprecated Since 0.96.0.0 use {@link TownyObject#getFormattedName()} instead.
	 */
	@Deprecated
	public static String getFormattedName(TownyObject obj) {
		return obj.getFormattedName();
	}

	/**
	 * @param resident The {@link Resident} to get the formatted name from.
	 *                    
	 * @return The formatted name of the object.
	 * 
	 * @deprecated Since 0.96.0.0 use {@link Resident#getFormattedName()} instead.
	 */
	@Deprecated
	public static String getFormattedResidentName(Resident resident) {
		return resident.getFormattedName();
	}

	/**
	 * @param town The {@link Town} to get the formatted name from.
	 *                
	 * @return The formatted name of the object.
	 * 
	 * @deprecated Since 0.96.0.0 use {@link Town#getFormattedName()} instead.
	 */
	@Deprecated
	public static String getFormattedTownName(Town town) {
		return town.getFormattedName();
	}

	/**
	 * @param nation The {@link Nation} to get the formatted name from.
	 * 
	 * @return The formatted name of the object.
	 *
	 * @deprecated Since 0.96.0.0 use {@link Nation#getFormattedName()} instead.
	 */
	@Deprecated
	public static String getFormattedNationName(Nation nation) {
		return nation.getFormattedName();
	}

	/**
	 * @param resident The {@link Resident} to get the formatted title name from.
	 *                    
	 * @return The formatted title name of the resident.
	 * 
	 * @deprecated Since 0.96.0.0 use {@link Resident#getFormattedTitleName()} instead.
	 */
	@Deprecated
	public static String getFormattedResidentTitleName(Resident resident) {
		return resident.getFormattedTitleName();
	}
	
	public static String[] getFormattedNames(TownyObject[] objs) {
		List<String> names = new ArrayList<>();
		for (TownyObject obj : objs) {
			names.add(Colors.translateColorCodes(obj.getFormattedName()) + Colors.White);
		}
		
		return names.toArray(new String[0]);
	}
	
	public static List<String> getExtraFields(TownyObject to) {
		if (!to.hasMeta())
			return new ArrayList<>();
		
		String field = "";
		List<String> extraFields = new ArrayList<>();
		for (CustomDataField<?> cdf : to.getMetadata()) {
			String newAdd = "";
			if (!cdf.shouldDisplayInStatus())
				continue;
			
			newAdd = Colors.Green + cdf.getLabel() + ": ";
			newAdd += cdf.displayFormattedValue();
			newAdd += "  ";
			if ((field + newAdd).length() > ChatPaginator.AVERAGE_CHAT_PAGE_WIDTH) {
				extraFields.add(field);
				field = newAdd;
			} else {
				field += newAdd;
			}
		}
		if (!field.isEmpty())
			extraFields.add(field);
		
		return extraFields;
	}
	
	/**
	 * @param resident The {@link Resident} to get the king or mayor prefix from.
	 *                    
	 * @return The king or mayor prefix of the resident.
	 *
	 * @deprecated Since 0.96.0.0 use {@link Resident#getNamePrefix()} instead.
	 */
	@Deprecated	
	public static String getNamePrefix(Resident resident) {
		return resident.getNamePrefix();	
	}	

	/**
	 * @param resident The {@link Resident} to get the king or mayor postfix from.
	 *                    
	 * @return The king or mayor postfix of the resident.
	 *
	 * @deprecated Since 0.96.0.0 use {@link Resident#getNamePostfix()} instead.
	 */
	@Deprecated	
	public static String getNamePostfix(Resident resident) {
		return resident.getNamePostfix();
	}	

}
