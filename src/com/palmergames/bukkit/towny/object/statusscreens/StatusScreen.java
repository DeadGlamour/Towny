package com.palmergames.bukkit.towny.object.statusscreens;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.util.ChatPaginator;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;


public class StatusScreen {

	public StatusScreen() {}
	
	StatusScreenType type;
	Map<String, TextComponent> components = new LinkedHashMap<>();
	final static int maxWidth = ChatPaginator.AVERAGE_CHAT_PAGE_WIDTH;
	final TextComponent space = Component.text(" ").color(NamedTextColor.WHITE);
	
	public StatusScreen(StatusScreenType type, LinkedHashMap<String, TextComponent> components) {
		this.type = type;
		this.components = components;
	}

	public void addComponentOf(String name, String component) {
		components.put(name, Component.text(component));
	}
	
	public void addComponentOf(String name, TextComponent component) {
		components.put(name, component);
	}

	public void removeStatusComponent(String name) {
		components.remove(name);
	}
	
	public Collection<TextComponent> getComponents() {
		return Collections.unmodifiableCollection(components.values());
	}
	
	public boolean hasComponent(String name) {
		return components.containsKey(name);
	}
	
	public TextComponent getComponentOrNull(String name) {
		return components.get(name);
	}
	
	public List<TextComponent> getFormattedStatusScreen() {
		List<TextComponent> lines = new ArrayList<>();
		TextComponent line = Component.empty();
		List<TextComponent> components = new ArrayList<>(this.components.values());
		String string = "";
		for (int i = 0; i <= components.size() - 1; i++) {
			if (line.content().isEmpty()) {
				line = components.get(i);
				string = line.content();
				continue;
			}
			TextComponent nextComp = components.get(i);
			if ((string.length() + nextComp.content().length() + 1) > maxWidth) {
				lines.add(line);
				line = nextComp;
				string = line.content();
				continue;
			}
			line = line.append(space).append(nextComp).toBuilder().build();
			string += " " + nextComp.content();
		}
		if (!line.content().isEmpty())
			lines.add(line.toBuilder().build());

		return lines;
	}
}
