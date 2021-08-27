package com.palmergames.bukkit.towny.object.statusscreens;

import net.kyori.adventure.text.TextComponent;

public class StatusComponent {

	public StatusComponent() {}
	
	String name;
	TextComponent component;
	
	public StatusComponent(String name, TextComponent component) {
		this.name = name;
		this.component = component;
	}
	
	public static StatusComponent of(String name,TextComponent component) {
		return new StatusComponent(name, component);
	}
	public TextComponent getComponent() {
		return component;
	}
}
