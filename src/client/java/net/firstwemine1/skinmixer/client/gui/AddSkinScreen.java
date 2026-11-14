package net.firstwemine1.skinmixer.client.gui;

import net.firstwemine1.skinmixer.client.RemoteSkins;
import net.firstwemine1.skinmixer.client.SkinLibrary;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Adds a skin to the mixer by Minecraft username. The skin is fetched from Mojang, as vanilla does
 * for player heads, and appears at the end of the mixer's grid.
 */
public final class AddSkinScreen extends Screen {
	private static final int WHITE = 0xFFFFFFFF;
	private static final int GREY = 0xFFA0A0A0;
	private static final int RED = 0xFFFF6060;

	private final SkinMixerScreen parent;
	private EditBox name;
	private Button add;
	private Component status = Component.empty();
	private int statusColour = GREY;
	private boolean looking;

	public AddSkinScreen(SkinMixerScreen parent) {
		super(Component.translatable("skinmixer.add.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int x = width / 2 - 100;
		int y = height / 2 - 30;
		name = addRenderableWidget(new EditBox(font, x, y, 200, 20, Component.translatable("skinmixer.add.name")));
		name.setMaxLength(16);
		name.setHint(Component.translatable("skinmixer.add.name"));
		name.setResponder(text -> updateButtons());
		add = addRenderableWidget(Button.builder(Component.translatable("skinmixer.add.button"), b -> lookUp())
				.bounds(x, y + 44, 98, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
				.bounds(x + 102, y + 44, 98, 20).build());
		setInitialFocus(name);
		updateButtons();
	}

	private void updateButtons() {
		add.active = !looking && isValidName(name.getValue().trim()) && !name.getValue().isBlank();
	}

	private void lookUp() {
		String wanted = name.getValue().trim();
		if (looking || !isValidName(wanted) || wanted.isEmpty()) {
			return;
		}
		looking = true;
		status = Component.translatable("skinmixer.add.looking", wanted);
		statusColour = GREY;
		updateButtons();
		RemoteSkins.lookUp(wanted).thenAccept(found -> {
			looking = false;
			if (found.isPresent()) {
				SkinLibrary.add(found.get().id(), found.get().name());
				parent.skinAdded();
				minecraft.setScreen(parent);
			} else {
				status = Component.translatable("skinmixer.add.not_found", wanted);
				statusColour = RED;
				updateButtons();
			}
		});
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) && add.active) {
			lookUp();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		renderBackground(graphics);
		super.render(graphics, mouseX, mouseY, partialTick);
		graphics.drawCenteredString(font, title, width / 2, height / 2 - 70, WHITE);
		graphics.drawCenteredString(font, Component.translatable("skinmixer.add.help"), width / 2, height / 2 - 52, GREY);
		graphics.drawCenteredString(font, status, width / 2, height / 2 - 2, statusColour);
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
	}

	/** A possible Minecraft username: 1 to 16 letters, digits and underscores. */
	private static boolean isValidName(String name) {
		return name.matches("[A-Za-z0-9_]{1,16}");
	}
}
