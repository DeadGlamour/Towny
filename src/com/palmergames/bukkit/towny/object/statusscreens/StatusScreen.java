package com.palmergames.bukkit.towny.object.statusscreens;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.util.ChatPaginator;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TextComponent;


public class StatusScreen {

	public StatusScreen() {}
	
	String name;
	List<StatusComponent> components = new ArrayList<>();
	
	public StatusScreen(String name, List<StatusComponent> components) {
		this.name = name;
		this.components = components;
	}
	
	public void addComponent(StatusComponent component) {
		components.add(component);
	}
	
	public void addComponentOf(String name, TextComponent component) {
		components.add(StatusComponent.of(name, component));
	}
	
	public List<String> getFormattedStatusScreen() {
		List<String> lines = new ArrayList<>();
		String line;
		for (StatusComponent comp : components) {
			line comp.getComponent()
		}
			
		
//		for (StatusComponent component : components) {
//			for (String string : ChatPaginator.wordWrap(components.stream().collect(Collectors.toList()), ChatPaginator.UNBOUNDED_PAGE_WIDTH)) {
//				lines.add(string);
//			}
//		}
		return lines;
		 
	}
}
