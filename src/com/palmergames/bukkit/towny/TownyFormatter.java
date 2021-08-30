package com.palmergames.bukkit.towny;

import com.palmergames.bukkit.towny.event.statusscreen.NationStatusScreenEvent;
import com.palmergames.bukkit.towny.event.statusscreen.ResidentStatusScreenEvent;
import com.palmergames.bukkit.towny.event.statusscreen.TownStatusScreenEvent;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Government;
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
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class TownyFormatter {
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
	public static final String listPrefixFormat = "%3$s%1$s %4$s[%2$d]%3$s:%5$s ";
	public static final String keyValueFormat = "%s%s %s%s";
	public static final String keyFormat = "%s%s";
	public static final String hoverFormat = "%s[%s%s%s]";
	
	public static void initialize() {}

	/*
	 * TownyObject StatusScreen makers.
	 */
	
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
			screen.addComponentOf("townblock_plotType", colourKeyValue(translator.of("status_plot_type"), townBlock.getType().toString()));
		screen.addComponentOf("perm", colourKey(translator.of("status_perm")) + ((owner instanceof Resident) ? townBlock.getPermissions().getColourString().replace("n", "t") : townBlock.getPermissions().getColourString().replace("f", "r")));
		screen.addComponentOf("pvp", colourKeyValue(translator.of("status_pvp"), ((!preventPVP) ? translator.of("status_on"): translator.of("status_off")))); 
		screen.addComponentOf("explosion", colourKeyValue(translator.of("explosions"), ((world.isForceExpl() || townBlock.getPermissions().explosion) ? translator.of("status_on"): translator.of("status_off")))); 
		screen.addComponentOf("firespread", colourKeyValue(translator.of("firespread"), ((town.isFire() || world.isForceFire() || townBlock.getPermissions().fire) ? translator.of("status_on"):translator.of("status_off")))); 
		screen.addComponentOf("mobspawns", colourKeyValue(translator.of("mobspawns"), ((world.isForceTownMobs() || townBlock.getPermissions().mobs) ?  translator.of("status_on"): translator.of("status_off"))));

		if (townBlock.hasPlotObjectGroup())
			screen.addComponentOf("plotgroup", colourKey(translator.of("status_plot_group_name_and_size", townBlock.getPlotObjectGroup().getName(), townBlock.getPlotObjectGroup().getTownBlocks().size())));
		if (townBlock.getClaimedAt() > 0)
			screen.addComponentOf("claimedat", colourKeyValue(translator.of("msg_plot_perm_claimed_at"), registeredFormat.format(townBlock.getClaimedAt())));
		
		if (townBlock.getTrustedResidents().size() > 0)
			screen.addComponentOf("trusted", getFormattedTownyObjects(translator.of("status_trustedlist"), new ArrayList<>(townBlock.getTrustedResidents())));
		
		// Add any metadata which opt to be visible.
		List<String> fields = getExtraFields(townBlock);
		if (!fields.isEmpty())
			for (int i = 0; i < fields.size(); i++) 
				screen.addComponentOf("extraField" + i, colourKey(fields.get(i)));
		
		return screen;
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
		List<String> registeredLine = getResidentRegisteredLine(resident, translator);
		for (int i = 0; i < registeredLine.size(); i++)
			screen.addComponentOf("registered"+i, registeredLine.get(i));
		
		// Owner of: 4 plots
		// Perm: Build = f-- Destroy = fa- Switch = fao Item = ---
		screen.addComponentOf("ownsXPlots", colourKey(translator.of("owner_of_x_plots", resident.getTownBlocks().size())));
		screen.addComponentOf("perm1", colourKey(translator.of("status_perm")) + resident.getPermissions().getColourString().replace("n", "t"));
		screen.addComponentOf("pvp", colourKeyValue(translator.of("status_pvp"), (resident.getPermissions().pvp) ? translator.of("status_on"): translator.of("status_off")));
		screen.addComponentOf("explosions", colourKeyValue(translator.of("explosions"), (resident.getPermissions().explosion) ? translator.of("status_on"): translator.of("status_off"))); 
		screen.addComponentOf("firespread", colourKeyValue(translator.of("firespread"), (resident.getPermissions().fire) ? translator.of("status_on"): translator.of("status_off"))); 
		screen.addComponentOf("mobsspawns", colourKeyValue(translator.of("mobspawns"), (resident.getPermissions().mobs) ? translator.of("status_on"): translator.of("status_off")));

		// Bank: 534 coins
		if (TownyEconomyHandler.isActive())
			screen.addComponentOf("bank", colourKeyValue(translator.of("status_bank"), resident.getAccount().getHoldingFormattedBalance()));

		// Town: Camelot
		String townLine = colourKeyValue(translator.of("status_town"), (!resident.hasTown() ? translator.of("status_no_town") : resident.getTownOrNull().getFormattedName()));
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
					.append(Component.text(colourKeyValue(translator.of("rank_list_mayor"), town.getMayor().getFormattedName())))
					.append(Component.newline())
					.append(Component.text(getFormattedTownyObjects(translator.of("res_list"), new ArrayList<>(town.getNumResidents()))))),
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
			screen.addComponentOf("embassiesInTowns", getFormattedTownyObjects(translator.of("status_embassy_town"), new ArrayList<>(townEmbassies)));
			
		// Town ranks
		if (resident.hasTown() && !resident.getTownRanks().isEmpty())
			screen.addComponentOf("townRanks", colourKeyValue(translator.of("status_town_ranks"), StringMgmt.capitalize(StringMgmt.join(resident.getTownRanks(), ", "))));
		
		//Nation ranks
		if (resident.hasNation() && !resident.getNationRanks().isEmpty())
			screen.addComponentOf("nationRanks", colourKeyValue(translator.of("status_nation_ranks"),StringMgmt.capitalize(StringMgmt.join(resident.getNationRanks(), ", "))));
		
		// Jailed: yes if they are jailed.
		if (resident.isJailed())
			screen.addComponentOf("jailLine", translator.of("jailed_in_town", resident.getJailTown().getName()) + (resident.hasJailTime() ? translator.of("msg_jailed_for_x_hours", resident.getJailHours()) :  ""));
		
		// Friends [12]: James, Carry, Mason
		if (resident.getFriends() != null && !resident.getFriends().isEmpty())
			screen.addComponentOf("friendsLine", getFormattedTownyObjects(translator.of("status_friends"), new ArrayList<>(resident.getFriends())));
		
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
			screen.addComponentOf("board", colourKeyValue(translator.of("status_town_board"), town.getBoard()));

		// Created Date
		if (town.getRegistered() != 0) 
			screen.addComponentOf("registered", colourKeyValue(translator.of("status_founded"), registeredFormat.format(town.getRegistered())));

		// Town Size: 0 / 16 [Bought: 0/48] [Bonus: 0] [Home: 33,44]
		screen.addComponentOf("townblocks", colourKeyValue(translator.of("status_town_size_part_1"), translator.of("status_fractions", town.getTownBlocks().size(), TownySettings.getMaxTownBlocks(town))) +
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
				outpostLine = colourKeyValue(translator.of("status_town_outposts"), translator.of("status_fractions", town.getMaxOutpostSpawn(), town.getOutpostLimit()));
				int nationBonus = (Integer) TownySettings.getNationLevel(town.getNationOrNull()).get(TownySettings.NationLevel.NATION_BONUS_OUTPOST_LIMIT);
				if (town.hasNation() && nationBonus > 0)					
					outpostLine += translator.of("status_town_outposts2", nationBonus);

			} else if (town.hasOutpostSpawn()) {
				outpostLine = colourKeyValue(translator.of("status_town_outposts"), String.valueOf(town.getMaxOutpostSpawn()));
			}
			screen.addComponentOf("outposts", outpostLine, ClickEvent.runCommand("/towny:town outpost list"));
		}

		// Permissions: B=rnao D=---- S=rna- I=rnao
		screen.addComponentOf("perm1", colourKey(translator.of("status_perm")) + town.getPermissions().getColourString().replace("f", "r"));
		screen.addComponentOf("explosion", colourKeyValue(translator.of("explosions"), (town.isBANG() || world.isForceExpl()) ? translator.of("status_on"): translator.of("status_off")));
		screen.addComponentOf("firespread", colourKeyValue(translator.of("firespread"), (town.isFire() || world.isForceFire()) ? translator.of("status_on"): translator.of("status_off"))); 
		screen.addComponentOf("mobsspawns", colourKeyValue(translator.of("mobspawns"), (town.hasMobs() || world.isForceTownMobs()) ? translator.of("status_on"): translator.of("status_off")));

		if (town.isRuined()) {
			screen.addComponentOf("ruinedTime", colourKey(translator.of("msg_time_remaining_before_full_removal", TownRuinSettings.getTownRuinsMaxDurationHours() - TownRuinUtil.getTimeSinceRuining(town))));
			if (TownRuinSettings.getTownRuinsReclaimEnabled()) {
				if (TownRuinUtil.getTimeSinceRuining(town) < TownRuinSettings.getTownRuinsMinDurationHours())
					screen.addComponentOf("reclaim", colourKey(translator.of("msg_time_until_reclaim_available", TownRuinSettings.getTownRuinsMinDurationHours() - TownRuinUtil.getTimeSinceRuining(town))));
				else 
					screen.addComponentOf("reclaim", colourKey(translator.of("msg_reclaim_available")));
			}
			// Only display the remaining fields if town is not ruined
		} else {
			// | Bank: 534 coins
			if (TownyEconomyHandler.isActive())
				screen.addComponentOf("bankstring", getTownBankString(town, translator));
			// Mayor: MrSand
			screen.addComponentOf("mayor", colourKeyValue(translator.of("rank_list_mayor"), town.getMayor().getFormattedName()),
					HoverEvent.showText(Component.text(translator.of("registered_last_online", registeredFormat.format(town.getMayor().getRegistered()), lastOnlineFormatIncludeYear.format(town.getMayor().getLastOnline())))),
					ClickEvent.runCommand("/towny:resident " + town.getMayor().getName())
					);

			// Nation: Azur Empire
			if (town.hasNation()) {
				// Shown in Hover Text: Towns [44]: James City, Carry Grove, Mason Town
				String[] towns = getFormattedNames(town.getNationOrNull().getTowns().toArray(new Town[0]));
				if (towns.length > 10)
					towns = shortenOverlengthArray(towns, 11, translator);

				screen.addComponentOf("nation", colourKeyValue(translator.of("status_town_nation"), town.getNationOrNull().getName()), 
						HoverEvent.showText(Component.text(Colors.translateColorCodes(String.format(TownySettings.getPAPIFormattingNation(), town.getNationOrNull().getFormattedName())))
								.append(Component.newline())
								.append(Component.text(colourKeyValue(translator.of("status_nation_king"), town.getNationOrNull().getCapital().getMayor().getFormattedName())))
								.append(Component.newline())
								.append(Component.text(colourKeyValue(translator.of("town_plu"), StringMgmt.join(towns, ", "))))),
						ClickEvent.runCommand("/towny:nation " + town.getNationOrNull().getName())
						);
			}
			
			screen.addComponentOf("newline1", Component.newline());
			// Assistants [2]: Sammy, Ginger
			List<String> ranklist = getRanks(town, locale);
			screen.addComponentOf("townranks", colourHoverKey(translator.of("status_rank_list")),
					HoverEvent.showText(Component.text(String.join("\n", ranklist))),
					ClickEvent.runCommand("/towny:town ranklist " + town.getName()));

			// Residents [12]: James, Carry, Mason
			String[] residents = getFormattedNames(town.getResidents().toArray(new Resident[0]));
			if (residents.length > 34)
				residents = shortenOverlengthArray(residents, 35, translator);
			screen.addComponentOf("residents", colourHoverKey(translator.of("res_list")),
				HoverEvent.showText(Component.text(getFormattedStrings(translator.of("res_list"), Arrays.stream(residents).collect(Collectors.toList())))),
				ClickEvent.runCommand("/towny:town reslist "+ town.getName()));

		}
		
		screen.addComponentOf("newline2", Component.newline());
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
			screen.addComponentOf("board", colourKeyValue(translator.of("status_town_board"), nation.getBoard()));
		
		// Created Date
		long registered = nation.getRegistered();
		if (registered != 0)
			screen.addComponentOf("registered", colourKeyValue(translator.of("status_founded"), registeredFormat.format(nation.getRegistered())));

		// Bank: 534 coins
		if (TownyEconomyHandler.isActive()) {
			String bankline = colourKeyValue(translator.of("status_bank"), nation.getAccount().getHoldingFormattedBalance());

			if (TownySettings.getNationUpkeepCost(nation) > 0)
				bankline += translator.of("status_bank_town2", TownySettings.getNationUpkeepCost(nation));
			
			bankline += translator.of("status_nation_tax", nation.getTaxes());
			screen.addComponentOf("bankLine", bankline);
		}

		if (nation.isPublic())
			screen.addComponentOf("home", translator.of("status_town_size_part_5") + (nation.hasSpawn() ? Coord.parseCoord(nation.getSpawnOrNull()).toString() : translator.of("status_no_town")) + "]");

		// King: King Harlus
		if (nation.getNumTowns() > 0 && nation.hasCapital() && nation.getCapital().hasMayor())
			screen.addComponentOf("king", colourKeyValue(translator.of("status_nation_king"), nation.getCapital().getMayor().getFormattedName()));
		
		screen.addComponentOf("newline1", Component.newline());
		
		// Assistants [2]: Sammy, Ginger
		List<String> ranklist = getRanks(nation, locale);
		screen.addComponentOf("nationranks", colourHoverKey(translator.of("status_rank_list")),
			HoverEvent.showText(Component.text(String.join("\n", ranklist))),
			ClickEvent.runCommand("/towny:nation ranklist " + nation.getName()));
		
		// Towns [44]: James City, Carry Grove, Mason Town
		String[] towns = getFormattedNames(nation.getTowns().toArray(new Town[0]));
		if (towns.length > 10)
			towns = shortenOverlengthArray(towns, 11, translator);
		screen.addComponentOf("towns", colourHoverKey(translator.of("status_nation_towns")),
			HoverEvent.showText(Component.text(getFormattedStrings(translator.of("status_nation_towns"), Arrays.stream(towns).collect(Collectors.toList())))),
			ClickEvent.runCommand("/towny:nation townlist " + nation.getName()));
		
		// Allies [4]: James Nation, Carry Territory, Mason Country
		String[] allies = getFormattedNames(nation.getAllies().toArray(new Nation[0]));
		if (allies.length > 10)
			allies = shortenOverlengthArray(allies, 11, translator);
		if (allies.length > 0)
			screen.addComponentOf("allies", colourHoverKey(translator.of("status_nation_allies")),
				HoverEvent.showText(Component.text(getFormattedStrings(translator.of("status_nation_allies"), Arrays.stream(allies).collect(Collectors.toList())))),
				ClickEvent.runCommand("/towny:nation allylist " + nation.getName()));

		// Enemies [4]: James Nation, Carry Territory, Mason Country
		String[] enemies = getFormattedNames(nation.getEnemies().toArray(new Nation[0]));
		if (enemies.length > 10)
			enemies = shortenOverlengthArray(enemies, 11, translator);
		if (enemies.length > 0)
			screen.addComponentOf("enemies", colourHoverKey(translator.of("status_nation_enemies")),
				HoverEvent.showText(Component.text(getFormattedStrings(translator.of("status_nation_enemies"), Arrays.stream(enemies).collect(Collectors.toList())))),
				ClickEvent.runCommand("/towny:nation enemylist " + nation.getName()));

		screen.addComponentOf("newline2", Component.newline());
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
			screen.addComponentOf("pvp", colourKeyValue(translator.of("status_world_forcepvp"), (world.isForcePVP() ? translator.of("status_on") : translator.of("status_off"))) + Colors.Gray + " | " + 
					colourKeyValue(translator.of("status_world_friendlyfire"), (world.isFriendlyFireEnabled() ? translator.of("status_on") : translator.of("status_off"))));
			// Fire: ON | ForceFire: ON
			screen.addComponentOf("fire", colourKeyValue(translator.of("status_world_fire"), (world.isFire() ? translator.of("status_on") : translator.of("status_off"))) + Colors.Gray + " | " + 
					colourKeyValue(translator.of("status_world_forcefire"), (world.isForceFire() ? translator.of("status_forced") : translator.of("status_adjustable"))));
			// Explosion: ON | ForceExplosion: ON
			screen.addComponentOf("explosions", colourKeyValue(translator.of("explosions"), (world.isExpl() ? translator.of("status_on") : translator.of("status_off"))) + Colors.Gray + " | " + 
				    colourKeyValue(translator.of("status_world_forceexplosion"), (world.isForceExpl() ? translator.of("status_forced") : translator.of("status_adjustable"))));
			// WorldMobs: ON | Wilderness Mobs: ON
			screen.addComponentOf("mobs", colourKeyValue(translator.of("status_world_worldmobs"), (world.hasWorldMobs() ? translator.of("status_on") : translator.of("status_off"))) + Colors.Gray + " | " + 
				    colourKeyValue(translator.of("status_world_wildernessmobs"), (world.hasWildernessMobs() ? translator.of("status_on") : translator.of("status_off"))));
			// ForceTownMobs: ON
			screen.addComponentOf("townmobs", colourKeyValue(translator.of("status_world_forcetownmobs"), (world.isForceTownMobs() ? translator.of("status_forced") : translator.of("status_adjustable"))));
			// War will be allowed in this world.
			screen.addComponentOf("war_allowed", colourKey(world.isWarAllowed() ? translator.of("msg_set_war_allowed_on") : translator.of("msg_set_war_allowed_off")));
			// Unclaim Revert: ON
			screen.addComponentOf("unclaim_revert", colourKeyValue(translator.of("status_world_unclaimrevert"), (world.isUsingPlotManagementRevert() ? translator.of("status_on_good") : translator.of("status_off_bad")))); 
			// Entity Explosion Revert: ON | Block Explosion Revert: ON
			screen.addComponentOf("explosion_reverts", colourKeyValue(translator.of("status_world_explrevert_entity"), (world.isUsingPlotManagementWildEntityRevert() ? translator.of("status_on_good") : translator.of("status_off_bad"))) + Colors.Gray + " | " +
					colourKeyValue(translator.of("status_world_explrevert_block"), (world.isUsingPlotManagementWildBlockRevert() ? translator.of("status_on_good") : translator.of("status_off_bad"))));
			// Plot Clear Block Delete: ON (see /towny plotclearblocks) | OFF
			screen.addComponentOf("plot_clear", colourKeyValue(translator.of("status_plot_clear_deletion"), (world.isUsingPlotManagementMayorDelete() ? translator.of("status_on") + Colors.LightGreen +" (see /towny plotclearblocks)" : translator.of("status_off")))); 
			// Wilderness:
			//     Build, Destroy, Switch, ItemUse
			//     Ignored Blocks: see /towny wildsblocks
			screen.addComponentOf("wilderness", colourKey(world.getUnclaimedZoneName() + ":"));
			screen.addComponentOf("perms1", "    " + (world.getUnclaimedZoneBuild() ? Colors.LightGreen : Colors.Rose) + "Build" + Colors.Gray + ", " + (world.getUnclaimedZoneDestroy() ? Colors.LightGreen : Colors.Rose) + "Destroy" + Colors.Gray + ", " + (world.getUnclaimedZoneSwitch() ? Colors.LightGreen : Colors.Rose) + "Switch" + Colors.Gray + ", " + (world.getUnclaimedZoneItemUse() ? Colors.LightGreen : Colors.Rose) + "ItemUse");
			screen.addComponentOf("perms2", "    " + colourKey(translator.of("status_world_ignoredblocks") + Colors.LightGreen + " see /towny wildsblocks"));

			// Add any metadata which opt to be visible.
			List<String> fields = getExtraFields(world);
			if (!fields.isEmpty())
				for (int i = 0; i < fields.size() -1; i++) 
					screen.addComponentOf("extraField" + i, fields.get(i));
		}
		
		return screen;
	}
	
	/*
	 * Utility methods used in the Status Screens.
	 */
	
	private static String colourKeyValue(String key, String value) {
		return String.format(keyValueFormat, Translation.of("status_format_key_value_key"), key, Translation.of("status_format_key_value_value"), value); 
	}
	
	private static String colourKey(String key) {
		return String.format(keyFormat, Translation.of("status_format_key_value_key"), key); 
	}
	
	private static String colourHoverKey(String key) {
		return String.format(hoverFormat, Translation.of("status_format_hover_bracket_colour"), Translation.of("status_format_hover_key"), key, Translation.of("status_format_hover_bracket_colour"));
	}
	

	/**
	 * Gets the registered/last online line for the Resident StatusScreen.
	 * @param resident Resident who's status we are getting.
	 * @param translator Translator used for lang choice.
	 * @return String with registered date and last online times formatted for use in the StatusScreen. 
	 */
	private static List<String> getResidentRegisteredLine(Resident resident, Translator translator) {
		List<String> out = new ArrayList<>();
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(resident.getLastOnline());
		int currentYear = cal.get(Calendar.YEAR);
		cal.setTimeInMillis(System.currentTimeMillis());
		int lastOnlineYear = cal.get(Calendar.YEAR);
		if (!resident.isNPC()) // Not an NPC: show more detailed info.
			if (currentYear == lastOnlineYear) { 
				out.add(colourKeyValue(translator.of("status_registered"), registeredFormat.format(resident.getRegistered())));
				out.add(colourKeyValue(translator.of("status_lastonline"), lastOnlineFormat.format(resident.getLastOnline())));
			} else {
				out.add(colourKeyValue(translator.of("status_registered"), registeredFormat.format(resident.getRegistered())));
				out.add(colourKeyValue(translator.of("status_lastonline"), lastOnlineFormatIncludeYear.format(resident.getLastOnline())));
			}
		else // An NPC: show their created date.
			out.add(colourKeyValue(translator.of("npc_created"), registeredFormat.format(resident.getRegistered())));
		
		return out;
	}


	/**
	 * Gets a list of Towns which the given resident owns embassy plots in.
	 * @param resident the given Resident
	 * @return List of Towns in which the resident owns embassies.
	 */
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
	

	/**
	 * Returns a List of Strings, in which each string is formatted: RankName [#]: names, of, people, with, the, rank.
	 * @param gov Government (Town or Nation) for which to gather ranks of.
	 * @param locale Locale used in lang selection.
	 * @return List of Strings describe above.
	 */
	private static List<String> getRanks(Government gov, Locale locale) {
		List<String> ranklist = new ArrayList<>();
		List<Resident> residents = new ArrayList<>(gov.getResidents());
		List<String> ranks;
		if (gov instanceof Nation)
			ranks = TownyPerms.getNationRanks();
		else 
			ranks = TownyPerms.getTownRanks();
		List<Resident> residentWithRank = new ArrayList<>();

		for (String rank : ranks) {
			for (Resident r : residents) {
				if (gov instanceof Nation)
					if ((r.getNationRanks() != null) && (r.getNationRanks().contains(rank)))
						residentWithRank.add(r);
				if (gov instanceof Town)
					if ((r.getTownRanks() != null) && (r.getTownRanks().contains(rank)))
						residentWithRank.add(r);
			}
			if (!residentWithRank.isEmpty())
				ranklist.add(getFormattedTownyObjects(StringMgmt.capitalize(rank), new ArrayList<>(residentWithRank)));
			residentWithRank.clear();
		}
		if (gov instanceof Town && ((Town) gov).getTrustedResidents().size() > 0)
			ranklist.add(getFormattedTownyObjects(Translation.of("status_trustedlist", locale), new ArrayList<>(((Town) gov).getTrustedResidents())));
		
		return ranklist;
	}
	

	/**
	 * Shortens and array if longer than i, postfixing with "and more..."
	 * @param array List of Strings, usually names of TownyObjects.
	 * @param i int representing which index of the array will be the cutoff and changed to "and more..."
	 * @param translator Translator used to select language for message.
	 * @return Shortened Array of Strings.
	 */
	private static String[] shortenOverlengthArray(String[] array, int i, Translator translator) {
		String[] entire = array;
		array = new String[i + 1];
		System.arraycopy(entire, 0, array, 0, i);
		array[i] = translator.of("status_town_reslist_overlength");
		return array;
	}


	/**
	 * Returns the formatted bank line for the Town StatusScreen.
	 * @param town Town of which to generate a bankstring.
	 * @param translator Translator used in choosing language.
	 * @return bankString used in the Town StatusScreen.
	 */
	private static String getTownBankString(Town town, Translator translator) {
		String bankString = colourKeyValue(translator.of("status_bank"), town.getAccount().getHoldingFormattedBalance());
		if (town.isBankrupt()) {
			bankString += translator.of("status_bank_bankrupt");
			if (town.getAccount().getDebtCap() == 0)
				town.getAccount().setDebtCap(MoneyUtil.getEstimatedValueOfTown(town));
			bankString += " " + colourKeyValue(translator.of("status_debtcap"), "-" + TownyEconomyHandler.getFormattedBalance(town.getAccount().getDebtCap()));
		}
		if (town.hasUpkeep())
			bankString += translator.of("status_bank_town2", BigDecimal.valueOf(TownySettings.getTownUpkeepCost(town)).setScale(2, RoundingMode.HALF_UP).doubleValue());
		if (TownySettings.getUpkeepPenalty() > 0 && town.isOverClaimed())
			bankString += translator.of("status_bank_town_penalty_upkeep", TownySettings.getTownPenaltyUpkeepCost(town));
		bankString += translator.of("status_bank_town3", town.getTaxes()) + (town.isTaxPercentage() ? "%" : "");
		return bankString;
	}


	/**
	 * Returns the 2nd line of the Town StatusScreen.
	 * @param town Town for which to get the StatusScreen.
	 * @param world TownyWorld in which the town considers home. 
	 * @param translator Translator used in language selection.
	 * @return Formatted 2nd line of the Town StatusScreen.
	 */
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


	/**
	 * Returns the 2nd line of the Nation StatusScreen.
	 * @param nation Nation for which to get the StatusScreen.
	 * @param translator Translator used in language selection.
	 * @return Formatted 2nd line of the Nation StatusScreen.
	 */
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
	 * Returns a list of MetaData used in the StatusScreens.
	 * @param to TownyObject for which to gather the metadata of.
	 * @return List of visible metadata.
	 */
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


	/*
	 * Methods used throughout Towny. 
	 */
	

	/**
	 * Used in /n online and /t online.
	 * @param prefix String prefix to use.
	 * @param residentList ResidentList representing the town or nation.
	 * @param player Player which is doing the looking, used for Vanishing.
	 * @return String of formatted residents listed and prefixed.
	 */
	public static String getFormattedOnlineResidents(String prefix, ResidentList residentList, Player player) {
		return getFormattedTownyObjects(prefix, new ArrayList<>(ResidentUtil.getOnlineResidentsViewable(player, residentList)));
	}

	/**
	 * Used to prefix, count and list the given object.
	 * @param prefix String applied to beginning of the list.
	 * @param objectlist List of TownyObjects to list.
	 * @return Formatted, prefixed list of TownyObjects.
	 */
	public static String getFormattedTownyObjects(String prefix, List<TownyObject> objectlist) {
		return String.format(listPrefixFormat, prefix, objectlist.size(), Translation.of("status_format_list_1"), Translation.of("status_format_list_2"), Translation.of("status_format_list_3")) + StringMgmt.join(getFormattedTownyNames(objectlist), ", "); 
	}
	
	/**
	 * Used to prefix, count and list the given strings.
	 * @param prefix String applied to beginning of the list.
	 * @param objectlist List of Strings to list.
	 * @return Formatted, prefixed list of Strings.
	 */
	public static String getFormattedStrings(String prefix, List<String> list) {
		return String.format(listPrefixFormat, prefix, list.size(), Translation.of("status_format_list_1"), Translation.of("status_format_list_2"), Translation.of("status_format_list_3")) + StringMgmt.join(list, ", "); 
	}

	/**
	 * Returns a list of names, using their Formatted (long) names, with ColourCodes translated.
	 * @param objs List of TownyObjects of which to make a list of names.
	 * @return List of Names, formatted and coloured.
	 */
	public static List<String> getFormattedTownyNames(List<TownyObject> objs) {
		List<String> names = new ArrayList<>();
		for (TownyObject obj : objs) {
			names.add(Colors.translateColorCodes(obj.getFormattedName()) + Colors.White);
		}
		
		return names;
	}


	/**
	 * Returns an Array of names, using their Formatted (long) names, with ColourCodes translated.
	 * @param objs Array of TownyObjects of which to make a list of names.
	 * @return Array of Names, formatted and coloured.
	 */
	public static String[] getFormattedNames(TownyObject[] objs) {
		List<String> names = new ArrayList<>();
		for (TownyObject obj : objs) {
			names.add(Colors.translateColorCodes(obj.getFormattedName()) + Colors.White);
		}
		
		return names.toArray(new String[0]);
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
	 * Returns a Chat Formatted List of all town residents who hold a rank.
	 * 
	 * @param town the town for which to check against.
	 * @return a list containing formatted rank data.
	 */
	public static List<String> getRanksForTown(Town town, Locale locale) {
		final Translator translator = Translator.locale(locale);
		List<String> ranklist = new ArrayList<>();
		ranklist.add(ChatTools.formatTitle(translator.of("rank_list_title", town.getFormattedName())));
		ranklist.add(colourKeyValue(translator.of("rank_list_mayor"), town.getMayor().getFormattedName()));

		ranklist.addAll(getRanks(town, locale));
		return ranklist;
	}

	/**
	 * Returns a Chat Formatted List of all nation residents who hold a rank.
	 * 
	 * @param nation the nation for which to check against.
	 * @return a list containing formatted rank data.
	 */
	public static List<String> getRanksForNation(Nation nation, Locale locale) {
		final Translator translator = Translator.locale(locale);
		List<String> ranklist = new ArrayList<>();
		ranklist.add(ChatTools.formatTitle(translator.of("rank_list_title", nation.getFormattedName())));
		ranklist.add(colourKeyValue(translator.of("status_nation_king"), nation.getKing().getFormattedName()));

		ranklist.addAll(getRanks(nation, locale));
		return ranklist;
	}
	

	/**
	 * @return the Time.
	 */
	public static String getTime() {
		SimpleDateFormat sdf = new SimpleDateFormat("hh:mm aa");
		return sdf.format(System.currentTimeMillis());
	}
	

	/*
	 * Deprecated methods.
	 */
	

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
