package net.firstwemine1.skinmixer.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.firstwemine1.skinmixer.client.RemoteSkins;
import net.firstwemine1.skinmixer.client.SkinLibrary;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.chat.TextComponent;
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
	private Component status = TextComponent.EMPTY;
	private int statusColour = GREY;
	private boolean looking;

	public AddSkinScreen(SkinMixerScreen parent) {
		super(new TranslatableComponent("skinmixer.add.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int x = width / 2 - 100;
		int y = height / 2 - 30;
		name = addRenderableWidget(new EditBox(font, x, y, 200, 20, new TranslatableComponent("skinmixer.add.name")));
		name.setMaxLength(16);
		name.setResponder(text -> updateButtons());
		add = addRenderableWidget(button(new TranslatableComponent("skinmixer.add.button"), b -> lookUp(), x, y + 44, 98, 20));
		addRenderableWidget(button(new TranslatableComponent("gui.cancel"), b -> onClose(), x + 102, y + 44, 98, 20));
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
		status = new TranslatableComponent("skinmixer.add.looking", wanted);
		statusColour = GREY;
		updateButtons();
		RemoteSkins.lookUp(wanted).thenAccept(found -> {
			looking = false;
			if (found.isPresent()) {
				SkinLibrary.add(found.get().id(), found.get().name());
				parent.skinAdded();
				minecraft.setScreen(parent);
			} else {
				status = new TranslatableComponent("skinmixer.add.not_found", wanted);
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
	public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
		GuiGraphics graphics = new GuiGraphics(poseStack);
		renderBackground(poseStack);
		super.render(poseStack, mouseX, mouseY, partialTick);
		renderTooltips(poseStack, mouseX, mouseY);
		graphics.drawCenteredString(font, title, width / 2, height / 2 - 70, WHITE);
		graphics.drawCenteredString(font, new TranslatableComponent("skinmixer.add.help"), width / 2, height / 2 - 52, GREY);
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

	// ---- widgets on this version ----

	private final java.util.Map<net.minecraft.client.gui.components.AbstractWidget, Component> tooltips = new java.util.LinkedHashMap<>();

	private static Button button(Component message, Button.OnPress onPress, int x, int y, int width, int height) {
		return new Button(x, y, width, height, message, onPress);
	}

	private static void place(net.minecraft.client.gui.components.AbstractWidget widget, int x, int y) {
		widget.x = x;
		widget.y = y;
	}

	private void tooltip(net.minecraft.client.gui.components.AbstractWidget widget, Component text) {
		tooltips.put(widget, text);
	}

	/** Draws the tooltip of whichever widget is under the cursor; this version's widgets have none of their own. */
	private void renderTooltips(PoseStack poseStack, int mouseX, int mouseY) {
		for (java.util.Map.Entry<net.minecraft.client.gui.components.AbstractWidget, Component> entry : tooltips.entrySet()) {
			net.minecraft.client.gui.components.AbstractWidget widget = entry.getKey();
			if (widget.visible && widget.isMouseOver(mouseX, mouseY)) {
				renderTooltip(poseStack, font.split(entry.getValue(), 200), mouseX, mouseY);
				return;
			}
		}
	}
}
