package net.firstwemine1.skinmixer.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import com.mojang.blaze3d.platform.InputConstants;

import net.firstwemine1.skinmixer.client.ClientOutfits;
import net.firstwemine1.skinmixer.client.RemoteSkins;
import net.firstwemine1.skinmixer.client.SkinLibrary;
import net.firstwemine1.skinmixer.client.SkinBaker;
import net.firstwemine1.skinmixer.client.gui.Category.Side;
import net.firstwemine1.skinmixer.outfit.Outfit;
import net.firstwemine1.skinmixer.skin.Part;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

/**
 * The mixer, laid out like a character creator:
 * <ul>
 * <li>left, a flat <b>body map</b> of the outfit: click a part to edit it;</li>
 * <li>middle, the <b>3D preview</b>, which zooms to the part being edited (drag to turn it);</li>
 * <li>right, picture <b>tabs</b> for the outfit, head, body, arms and legs, and a <b>grid</b> showing that
 * part of every skin. Hovering a choice tries it on; clicking keeps it.</li>
 * </ul>
 * Nothing changes for anyone until Apply.
 */
public final class SkinMixerScreen extends Screen {
	private static final int TOP = 24;
	private static final int GAP = 6;
	private static final int PREVIEW_WIDTH = 110;
	private static final int TAB_WIDTH = 28;
	private static final int TAB_HEIGHT = 22;
	private static final int CARD_WIDTH = 36;
	private static final int CARD_HEIGHT = 44;
	private static final int CARD_GAP = 2;
	private static final int COLUMNS = 4;
	private static final int SIDE_COLUMN_MIN = 64;

	private static final int WHITE = 0xFFFFFFFF;
	private static final int GREY = 0xFFA0A0A0;
	private static final int YELLOW = 0xFFFFD84A;
	private static final int PANEL = 0x66000000;
	private static final int CARD = 0xFF2A2A2E;
	private static final int CARD_HOVER = 0xFF3D3D44;
	private static final int TAB_SELECTED = 0xFF55555E;

	private final Screen parent;
	private final Random random = new Random();
	private PlayerSkin selfSkin;
	private boolean selfSlim;
	private Outfit outfit;
	private Category category = Category.OUTFIT;
	private Side side = Side.BOTH;

	/** What the preview shows: the outfit, or the outfit with the hovered choice tried on. */
	private PlayerSkin previewSkin;
	private Outfit previewed;
	private String hovered;

	private OutfitPreview preview;
	private final List<AbstractButton> cards = new ArrayList<>();
	/** First row of cards shown; the grid scrolls when there are more skins than fit. */
	private int scrollRow;
	private int visibleRows;
	private Button armsButton;
	private Button sideButton;
	private int gridX;
	private int gridY;
	private int sideColumnX;
	private int sideColumnWidth;
	private int dollPanelHeight;

	public SkinMixerScreen(Screen parent) {
		super(Component.translatable("skinmixer.screen.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		if (minecraft.player == null) {
			onClose();
			return;
		}
		// Kept across resizes: init runs again then, and the half-made outfit must survive.
		if (outfit == null) {
			selfSkin = ClientOutfits.originalSkin(minecraft.player.getUUID());
			selfSlim = selfSkin.model() == PlayerModelType.SLIM;
			Outfit current = ClientOutfits.get(minecraft.player.getUUID());
			outfit = current != null ? current : Outfit.ofSelf(selfSlim);
		}
		cards.clear();
		previewed = null;

		int dollScale = Mth.clamp((height - TOP - 110) / PartArt.DOLL_HEIGHT, 2, 5);
		sideColumnWidth = Math.max(SIDE_COLUMN_MIN, PartArt.DOLL_WIDTH * dollScale + 12);
		int gridWidth = COLUMNS * (CARD_WIDTH + CARD_GAP) - CARD_GAP;
		int total = sideColumnWidth + GAP + PREVIEW_WIDTH + GAP + gridWidth;
		sideColumnX = (width - total) / 2;
		int previewX = sideColumnX + sideColumnWidth + GAP;
		gridX = previewX + PREVIEW_WIDTH + GAP;
		gridY = TOP + TAB_HEIGHT + 4;
		int previewHeight = Math.max(100, height - TOP - 46);

		// Left: body map, arm model, which side.
		dollPanelHeight = PartArt.DOLL_HEIGHT * dollScale + 10;
		addRenderableWidget(new BodyMap(sideColumnX, TOP, sideColumnWidth, dollPanelHeight, dollScale));
		int y = TOP + dollPanelHeight + 4;
		armsButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
			outfit = outfit.withSlim(!outfit.slim());
			refresh();
		}).bounds(sideColumnX, y, sideColumnWidth, 20).build());
		armsButton.setTooltip(Tooltip.create(Component.translatable("skinmixer.screen.arms.tooltip")));
		sideButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
			side = side.next();
			preview.frame(category, side, false);
			refresh();
		}).bounds(sideColumnX, y + 24, sideColumnWidth, 20).build());
		sideButton.setTooltip(Tooltip.create(Component.translatable("skinmixer.screen.side.tooltip")));

		// Middle: the zooming 3D preview.
		preview = addRenderableWidget(new OutfitPreview(PREVIEW_WIDTH, previewHeight, minecraft.getEntityModels(), () -> previewSkin));
		preview.setPosition(previewX, TOP);
		preview.frame(category, side, true);

		// Right: category tabs and the choices.
		for (Category tab : Category.values()) {
			addRenderableWidget(new CategoryTab(gridX + tab.ordinal() * (TAB_WIDTH + 2), TOP, tab));
		}
		for (String source : sources()) {
			cards.add(addRenderableWidget(new ChoiceCard(source)));
		}
		cards.add(addRenderableWidget(new AddCard()));
		visibleRows = Math.max(1, (height - 42 - gridY) / (CARD_HEIGHT + CARD_GAP));
		layoutCards();

		int buttonWidth = 90;
		int buttonsX = width / 2 - (4 * buttonWidth + 3 * 4) / 2;
		int buttonsY = height - 26;
		addRenderableWidget(Button.builder(Component.translatable("skinmixer.screen.randomize"), b -> randomize())
				.bounds(buttonsX, buttonsY, buttonWidth, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("skinmixer.screen.reset"), b -> {
			outfit = Outfit.ofSelf(selfSlim);
			refresh();
		}).bounds(buttonsX + (buttonWidth + 4), buttonsY, buttonWidth, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
				.bounds(buttonsX + 2 * (buttonWidth + 4), buttonsY, buttonWidth, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("skinmixer.screen.apply"), b -> apply())
				.bounds(buttonsX + 3 * (buttonWidth + 4), buttonsY, buttonWidth, 20).build());

		refresh();
	}

	// ---- the grid ----

	/** Every skin to choose from: your own, the defaults, then the ones added by username. */
	private static List<String> sources() {
		List<String> sources = new ArrayList<>(Outfit.SOURCES);
		for (SkinLibrary.Entry entry : SkinLibrary.entries()) {
			sources.add(Outfit.mojangSource(entry.id()));
		}
		return sources;
	}

	private int rowCount() {
		return (cards.size() + COLUMNS - 1) / COLUMNS;
	}

	private void layoutCards() {
		scrollRow = Mth.clamp(scrollRow, 0, Math.max(0, rowCount() - visibleRows));
		for (int i = 0; i < cards.size(); i++) {
			AbstractButton card = cards.get(i);
			int row = i / COLUMNS - scrollRow;
			card.visible = row >= 0 && row < visibleRows;
			card.setPosition(gridX + (i % COLUMNS) * (CARD_WIDTH + CARD_GAP), gridY + row * (CARD_HEIGHT + CARD_GAP));
		}
	}

	private boolean overGrid(double mouseX, double mouseY) {
		int gridWidth = COLUMNS * (CARD_WIDTH + CARD_GAP);
		return mouseX >= gridX && mouseX < gridX + gridWidth + 6 && mouseY >= gridY
				&& mouseY < gridY + visibleRows * (CARD_HEIGHT + CARD_GAP);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (overGrid(mouseX, mouseY) && scrollY != 0) {
			scrollRow -= (int) Math.signum(scrollY);
			layoutCards();
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	/** Back from the add-skin screen with a new skin: show the end of the grid, where it went. */
	void skinAdded() {
		scrollRow = Integer.MAX_VALUE;
	}

	// ---- state changes ----

	private void selectCategory(Category next) {
		category = next;
		preview.frame(category, side, false);
		refresh();
	}

	/** A click on the body map: edit that part's category, and that side if one side is being edited. */
	private void selectPart(Part part) {
		Category next = Category.of(part);
		if (next.isPaired() && side != Side.BOTH) {
			side = part.isRight() ? Side.RIGHT : Side.LEFT;
		}
		selectCategory(next);
	}

	private List<Part> targets() {
		return category.parts(side);
	}

	private Outfit wearing(Outfit base, String source) {
		Outfit result = base;
		for (Part part : targets()) {
			result = result.with(part, source);
		}
		return result;
	}

	/** Whether every part being edited already comes from {@code source}. */
	private boolean isWorn(String source) {
		return targets().stream().allMatch(part -> outfit.source(part).equals(source));
	}

	private void choose(String source) {
		outfit = wearing(outfit, source);
		refresh();
	}

	private void randomize() {
		List<String> skins = new ArrayList<>(Outfit.DEFAULT_SKINS);
		for (SkinLibrary.Entry entry : SkinLibrary.entries()) {
			if (RemoteSkins.get(entry.id()) != null) {
				skins.add(Outfit.mojangSource(entry.id()));
			}
		}
		for (Category region : List.of(Category.HEAD, Category.BODY, Category.ARMS, Category.LEGS)) {
			String source = skins.get(random.nextInt(skins.size()));
			for (Part part : region.parts(Side.BOTH)) {
				outfit = outfit.with(part, source);
			}
		}
		refresh();
	}

	private void apply() {
		boolean unchanged = outfit.allSelf() && outfit.slim() == selfSlim;
		ClientOutfits.choose(unchanged ? null : outfit);
		onClose();
	}

	private void refresh() {
		armsButton.setMessage(Component.translatable(outfit.slim() ? "skinmixer.screen.arms.slim" : "skinmixer.screen.arms.wide"));
		sideButton.setMessage(side.title());
		sideButton.active = category.isPaired();
		updatePreview();
	}

	/** Re-bakes the preview when what it should show changed. */
	private void updatePreview() {
		Outfit shown = hovered == null ? outfit : wearing(outfit, hovered);
		if (shown.equals(previewed)) {
			return;
		}
		Identifier texture = SkinBaker.bakePreview(shown, selfSkin);
		if (texture == null) {
			// A skin is still being fetched: keep showing what was there, and try again next frame.
			if (previewSkin == null) {
				previewSkin = selfSkin;
			}
			return;
		}
		previewed = shown;
		previewSkin = SkinBaker.skinOf(texture, shown.slim(), selfSkin);
	}

	// ---- what the widgets draw ----

	private Identifier shownTexture() {
		return previewSkin.body().texturePath();
	}

	private boolean shownSlim() {
		return previewSkin.model() == PlayerModelType.SLIM;
	}

	/** The texture a choice is drawn from, or null while a fetched skin is still loading. */
	private Identifier sourceTexture(String source) {
		if (Outfit.SELF.equals(source)) {
			return selfSkin.body().texturePath();
		}
		UUID remote = Outfit.mojangPlayer(source);
		if (remote != null) {
			PlayerSkin skin = RemoteSkins.get(remote);
			return skin == null ? null : skin.body().texturePath();
		}
		return SkinBaker.defaultTexture(source, outfit.slim());
	}

	/** Whether a choice's arms are slim. */
	private boolean sourceSlim(String source) {
		if (Outfit.SELF.equals(source)) {
			return selfSlim;
		}
		UUID remote = Outfit.mojangPlayer(source);
		if (remote != null) {
			PlayerSkin skin = RemoteSkins.get(remote);
			return skin != null && skin.model() == PlayerModelType.SLIM;
		}
		return outfit.slim();
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		// Try on whatever choice is under the cursor.
		String under = null;
		for (AbstractButton card : cards) {
			if (card instanceof ChoiceCard choice && choice.visible && choice.isMouseOver(mouseX, mouseY)) {
				under = choice.source;
			}
		}
		hovered = under;
		updatePreview();

		super.render(graphics, mouseX, mouseY, partialTick);
		graphics.drawCenteredString(font, Component.translatable("skinmixer.screen.title_with", title, category.title()), width / 2, 8, WHITE);
		Component status = Component.translatable(ClientOutfits.serverHasMod()
				? "skinmixer.screen.status.synced" : "skinmixer.screen.status.local");
		graphics.drawCenteredString(font, status, width / 2, height - 38, GREY);
		if (rowCount() > visibleRows) {
			// Scrollbar beside the grid.
			int barX = gridX + COLUMNS * (CARD_WIDTH + CARD_GAP) + 1;
			int barHeight = visibleRows * (CARD_HEIGHT + CARD_GAP) - CARD_GAP;
			int thumbHeight = Math.max(8, barHeight * visibleRows / rowCount());
			int thumbY = gridY + (barHeight - thumbHeight) * scrollRow / (rowCount() - visibleRows);
			graphics.fill(barX, gridY, barX + 3, gridY + barHeight, CARD);
			graphics.fill(barX, thumbY, barX + 3, thumbY + thumbHeight, GREY);
		}
		graphics.drawCenteredString(font, Component.translatable("skinmixer.screen.preview.hint"),
				preview.getX() + preview.getWidth() / 2, preview.getBottom() - 11, GREY);
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.renderBackground(graphics, mouseX, mouseY, partialTick);
		graphics.fill(preview.getX(), preview.getY(), preview.getRight(), preview.getBottom(), PANEL);
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
	}

	// ---- widgets ----

	/** The outfit laid flat. Hover a part to highlight it, click it to edit it. */
	private final class BodyMap extends AbstractWidget {
		private final int scale;

		BodyMap(int x, int y, int width, int height, int scale) {
			super(x, y, width, height, Component.translatable("skinmixer.screen.body_map"));
			this.scale = scale;
		}

		private float dollX() {
			return getX() + (getWidth() - PartArt.DOLL_WIDTH * scale) / 2f;
		}

		private float dollY() {
			return getY() + 5;
		}

		private Part partAt(double mouseX, double mouseY) {
			for (Part part : Part.values()) {
				int[] rect = PartArt.dollRect(part, shownSlim());
				float x = dollX() + rect[0] * scale;
				float y = dollY() + rect[1] * scale;
				if (mouseX >= x && mouseX < x + rect[2] * scale && mouseY >= y && mouseY < y + rect[3] * scale) {
					return part;
				}
			}
			return null;
		}

		@Override
		protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
			graphics.fill(getX(), getY(), getRight(), getBottom(), PANEL);
			PartArt.drawDoll(graphics, shownTexture(), shownSlim(), dollX(), dollY(), scale);
			Part under = isHovered() ? partAt(mouseX, mouseY) : null;
			for (Part part : Part.values()) {
				boolean editing = category != Category.OUTFIT && targets().contains(part);
				if (editing || part == under) {
					int[] rect = PartArt.dollRect(part, shownSlim());
					int x = Math.round(dollX() + rect[0] * scale);
					int y = Math.round(dollY() + rect[1] * scale);
					graphics.renderOutline(x - 1, y - 1, rect[2] * scale + 2, rect[3] * scale + 2, editing ? YELLOW : WHITE);
				}
			}
		}

		@Override
		public void onClick(MouseButtonEvent event, boolean doubleClick) {
			Part part = partAt(event.x(), event.y());
			if (part != null) {
				selectPart(part);
			}
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
			defaultButtonNarrationText(output);
		}
	}

	/** A tab showing its region of the current outfit. */
	private final class CategoryTab extends AbstractButton {
		private final Category tab;

		CategoryTab(int x, int y, Category tab) {
			super(x, y, TAB_WIDTH, TAB_HEIGHT, tab.title());
			this.tab = tab;
			setTooltip(Tooltip.create(tab.title()));
		}

		@Override
		public void onPress(InputWithModifiers input) {
			selectCategory(tab);
		}

		@Override
		protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
			boolean selected = tab == category;
			graphics.fill(getX(), getY(), getRight(), getBottom(), selected ? TAB_SELECTED : isHoveredOrFocused() ? CARD_HOVER : CARD);
			if (selected) {
				graphics.fill(getX(), getBottom() - 2, getRight(), getBottom(), YELLOW);
			}
			PartArt.drawItem(graphics, shownTexture(), shownSlim(), tab.parts(Side.BOTH), getX() + 3, getY() + 2, getWidth() - 6, getHeight() - 6);
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
			defaultButtonNarrationText(output);
		}
	}

	/** One choice: the part being edited, as it looks on one skin. */
	private final class ChoiceCard extends AbstractButton {
		private final String source;

		/** For a skin added by username: whose it is. Null for the built-in skins. */
		private final UUID remote;

		ChoiceCard(String source) {
			super(0, 0, CARD_WIDTH, CARD_HEIGHT, cardName(source));
			this.source = source;
			this.remote = Outfit.mojangPlayer(source);
			if (Outfit.SELF.equals(source)) {
				setTooltip(Tooltip.create(Component.translatable("skinmixer.screen.own_skin")));
			} else if (remote != null) {
				setTooltip(Tooltip.create(Component.translatable("skinmixer.screen.added_skin", cardName(source))));
			}
		}

		@Override
		protected boolean isValidClickButton(MouseButtonInfo info) {
			return info.button() == InputConstants.MOUSE_BUTTON_LEFT
					|| (remote != null && info.button() == InputConstants.MOUSE_BUTTON_RIGHT);
		}

		@Override
		public void onClick(MouseButtonEvent event, boolean doubleClick) {
			if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT && remote != null) {
				SkinLibrary.remove(remote);
				rebuildWidgets();
			} else {
				super.onClick(event, doubleClick);
			}
		}

		@Override
		public void onPress(InputWithModifiers input) {
			if (sourceTexture(source) != null) {
				choose(source);
			}
		}

		@Override
		protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
			int x = getX();
			int y = getY();
			graphics.fill(x, y, getRight(), getBottom(), isHoveredOrFocused() ? CARD_HOVER : CARD);
			Identifier texture = sourceTexture(source);
			if (texture != null) {
				PartArt.drawItem(graphics, texture, sourceSlim(source), targets(), x + 3, y + 3, getWidth() - 6, getHeight() - 6);
			} else {
				// Still being fetched from Mojang, or couldn't be.
				String mark = RemoteSkins.isLoading(remote) ? "..." : "?";
				graphics.drawCenteredString(font, mark, x + getWidth() / 2, y + getHeight() / 2 - 4, GREY);
			}
			if (isWorn(source)) {
				graphics.renderOutline(x, y, getWidth(), getHeight(), YELLOW);
			} else if (isHoveredOrFocused()) {
				graphics.renderOutline(x, y, getWidth(), getHeight(), WHITE);
			}
			if (Outfit.SELF.equals(source)) {
				// A gold corner marks the player's own skin.
				for (int i = 0; i < 5; i++) {
					graphics.fill(x + 1, y + 1 + i, x + 6 - i, y + 2 + i, YELLOW);
				}
			}
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
			defaultButtonNarrationText(output);
		}
	}

	/** The last card: add any Minecraft player's skin by username. */
	private final class AddCard extends AbstractButton {
		AddCard() {
			super(0, 0, CARD_WIDTH, CARD_HEIGHT, Component.translatable("skinmixer.screen.add"));
			setTooltip(Tooltip.create(Component.translatable("skinmixer.screen.add.tooltip")));
		}

		@Override
		public void onPress(InputWithModifiers input) {
			minecraft.setScreen(new AddSkinScreen(SkinMixerScreen.this));
		}

		@Override
		protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
			int x = getX();
			int y = getY();
			int colour = isHoveredOrFocused() ? WHITE : GREY;
			graphics.fill(x, y, getRight(), getBottom(), isHoveredOrFocused() ? CARD_HOVER : CARD);
			graphics.renderOutline(x, y, getWidth(), getHeight(), colour);
			int cx = x + getWidth() / 2;
			int cy = y + getHeight() / 2;
			graphics.fill(cx - 7, cy - 1, cx + 7, cy + 1, colour);
			graphics.fill(cx - 1, cy - 7, cx + 1, cy + 7, colour);
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
			defaultButtonNarrationText(output);
		}
	}

	/** A card's name for narration and tooltips: the skin's name, or the added player's name. */
	private static Component cardName(String source) {
		UUID remote = Outfit.mojangPlayer(source);
		if (remote != null) {
			String name = SkinLibrary.nameOf(remote);
			return Component.literal(name != null ? name : remote.toString());
		}
		return Component.translatable("skinmixer.skin." + source);
	}
}
