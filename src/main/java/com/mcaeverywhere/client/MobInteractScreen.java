package com.mcaeverywhere.client;

import com.mcaeverywhere.Actions;
import com.mcaeverywhere.FriendData;
import com.mcaeverywhere.net.ActionPacket;
import com.mcaeverywhere.net.Net;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Menu de interação (equivalente ao do MCA) para qualquer mob. */
@OnlyIn(Dist.CLIENT)
public class MobInteractScreen extends Screen {
    private static final ResourceLocation ICONS = new ResourceLocation("textures/gui/icons.png");
    private static final int W = 300;
    private static final int H = 195;

    private final int entityId;
    private int friendship;
    private int mode;
    @Nullable
    private Component line;

    private Button giftButton;
    private Button followButton;
    private Button stayButton;
    private Button freeButton;

    public MobInteractScreen(int entityId, int friendship, int mode) {
        super(Component.translatable("gui.mca_everywhere.title"));
        this.entityId = entityId;
        this.friendship = friendship;
        this.mode = mode;
    }

    public int entityId() {
        return entityId;
    }

    public void update(int friendship, int mode, @Nullable Component line) {
        this.friendship = friendship;
        this.mode = mode;
        if (line != null) {
            this.line = line;
        }
        refreshButtons();
    }

    @Override
    protected void init() {
        int left = (width - W) / 2;
        int top = (height - H) / 2;
        int x = left + 170;
        int y = top + 28;
        int w = 120;
        int step = 22;

        addRenderableWidget(button("talk", x, y, w, Actions.TALK));
        y += step;
        addRenderableWidget(button("joke", x, y, w, Actions.JOKE));
        y += step;
        giftButton = addRenderableWidget(button("gift", x, y, w, Actions.GIFT));
        y += step;
        addRenderableWidget(button("hug", x, y, w, Actions.HUG));
        y += step;
        followButton = addRenderableWidget(button("follow", x, y, w, Actions.FOLLOW));
        y += step;
        stayButton = addRenderableWidget(button("stay", x, y, w, Actions.STAY));
        y += step;
        freeButton = addRenderableWidget(button("free", x, y, w, Actions.FREE));

        refreshButtons();
    }

    private Button button(String name, int x, int y, int w, int action) {
        return Button.builder(Component.translatable("gui.mca_everywhere." + name),
                b -> Net.CHANNEL.sendToServer(new ActionPacket(entityId, action)))
            .bounds(x, y, w, 20)
            .build();
    }

    private void refreshButtons() {
        if (giftButton == null || minecraft == null || minecraft.player == null) return;
        giftButton.active = !minecraft.player.getMainHandItem().isEmpty();
        followButton.active = mode != FriendData.FOLLOW;
        stayButton.active = mode != FriendData.STAY;
        freeButton.active = mode != FriendData.FREE;
    }

    @Override
    public void tick() {
        if (minecraft == null || minecraft.level == null || minecraft.player == null) return;
        Entity entity = minecraft.level.getEntity(entityId);
        if (entity == null || !entity.isAlive() || entity.distanceToSqr(minecraft.player) > 144.0D) {
            onClose();
            return;
        }
        refreshButtons();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);

        int left = (width - W) / 2;
        int top = (height - H) / 2;

        // painel + borda
        g.fill(left, top, left + W, top + H, 0xDD101018);
        g.fill(left, top, left + W, top + 1, 0xFFAAAAAA);
        g.fill(left, top + H - 1, left + W, top + H, 0xFFAAAAAA);
        g.fill(left, top, left + 1, top + H, 0xFFAAAAAA);
        g.fill(left + W - 1, top, left + W, top + H, 0xFFAAAAAA);

        Entity entity = minecraft != null && minecraft.level != null ? minecraft.level.getEntity(entityId) : null;
        int centerX = left + 85;

        if (entity != null) {
            g.drawCenteredString(font, entity.getDisplayName(), centerX, top + 10, 0xFFFFFF);
        }
        if (entity instanceof LivingEntity living) {
            float size = Math.max(Math.max(living.getBbHeight(), living.getBbWidth() * 0.9F), 0.3F);
            int scale = (int) Mth.clamp(85.0F / size, 4.0F, 60.0F);
            int ey = top + 118;
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, centerX, ey, scale,
                (float) (centerX - mouseX), (float) (ey - 50 - mouseY), living);
        }

        drawHearts(g, centerX - 45, top + 128);
        g.drawCenteredString(font, Component.translatable("gui.mca_everywhere.mode." + mode), centerX, top + 142, 0xAAAAAA);

        // fala do mob
        Component text = line != null ? line : Component.translatable("gui.mca_everywhere.hint");
        List<FormattedCharSequence> lines = font.split(text, W - 24);
        int ty = top + 158;
        for (int i = 0; i < Math.min(3, lines.size()); i++) {
            g.drawString(font, lines.get(i), left + 12, ty + i * 10, line != null ? 0xFFFFAA : 0x888888, false);
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawHearts(GuiGraphics g, int x, int y) {
        for (int i = 0; i < 10; i++) {
            int hx = x + i * 9;
            g.blit(ICONS, hx, y, 16, 0, 9, 9);          // moldura
            if (friendship >= (i + 1) * 10) {
                g.blit(ICONS, hx, y, 52, 0, 9, 9);      // cheio
            } else if (friendship >= i * 10 + 5) {
                g.blit(ICONS, hx, y, 61, 0, 9, 9);      // meio
            }
        }
    }
}
