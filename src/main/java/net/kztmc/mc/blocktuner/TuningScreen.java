package net.kztmc.mc.blocktuner;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import java.util.List;

@Environment(EnvType.CLIENT)
public class TuningScreen extends Screen {

    private final BlockPos pos;
    private PianoKeyWidget pressedKey = null;
    private final PianoKeyWidget[] pianoKeys = new PianoKeyWidget[25];
    private final MidiManager midiManager;
    private MidiDevice currentDevice;
    private final MidiReceiver receiver;
    private boolean configChanged = false;
    private Component deviceName;
    private boolean deviceAvailable = true;
    protected int backgroundWidth = 256;
    protected int backgroundHeight = 112;
    protected int x;
    protected int y;
    protected static final Component PLAY_MODE_TOGGLE_TOOLTIP = Component.translatable("settings.blocktuner.play_mode");
    protected static final Component KEY_TO_PIANO_TOGGLE_TOOLTIP = Component.translatable("settings.blocktuner.key_to_piano");
    protected static final Component EMPTY_MIDI_DEVICE = Component.translatable("midi_device.empty");
    protected static final Component MIDI_DEVICE_REFRESH_TOOLTIP = Component.translatable("settings.blocktuner.refresh");

    static final int TEXTURE_SIZE = 256;
    static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("blocktuner", "textures/gui/container/tune.png");

    public TuningScreen(Component title, BlockPos pos) {
        super(title);
        this.pos = pos;

        midiManager = MidiManager.getMidiManager();
        currentDevice = midiManager.getCurrentDevice();
        receiver = new MidiReceiver();
    }

    @Override
    protected void init() {
        super.init();

        this.x = (this.width - this.backgroundWidth) / 2;
        this.y = (this.height - this.backgroundHeight) / 2;

        // Fancy(?) keyboard

        this.addRenderableWidget(new WhiteKeyWidget(this.x + 16, this.y + 65, 1, 1));
        this.addRenderableWidget(new WhiteKeyWidget(this.x + 32, this.y + 65, 3, 1));
        this.addRenderableWidget(new WhiteKeyWidget(this.x + 48, this.y + 65, 5, 2));
        this.addRenderableWidget(new WhiteKeyWidget(this.x + 64, this.y + 65, 6, 0));
        this.addRenderableWidget(new WhiteKeyWidget(this.x + 80, this.y + 65, 8, 1));
        this.addRenderableWidget(new WhiteKeyWidget(this.x + 96, this.y + 65, 10, 2));
        this.addRenderableWidget(new WhiteKeyWidget(this.x + 112, this.y + 65, 11, 0));
        this.addRenderableWidget(new WhiteKeyWidget(this.x + 128, this.y + 65, 13, 1));
        this.addRenderableWidget(new WhiteKeyWidget(this.x + 144, this.y + 65, 15, 1));
        this.addRenderableWidget(new WhiteKeyWidget(this.x + 160, this.y + 65, 17, 2));
        this.addRenderableWidget(new WhiteKeyWidget(this.x + 176, this.y + 65, 18, 0));
        this.addRenderableWidget(new WhiteKeyWidget(this.x + 192, this.y + 65, 20, 1));
        this.addRenderableWidget(new WhiteKeyWidget(this.x + 208, this.y + 65, 22, 2));
        this.addRenderableWidget(new WhiteKeyWidget(this.x + 224, this.y + 65, 23, 0));
        this.addRenderableWidget(new BlackKeyWidget(this.x + 8, this.y + 40, 0));
        this.addRenderableWidget(new BlackKeyWidget(this.x + 24, this.y + 40, 2));
        this.addRenderableWidget(new BlackKeyWidget(this.x + 40, this.y + 40, 4));
        this.addRenderableWidget(new BlackKeyWidget(this.x + 72, this.y + 40, 7));
        this.addRenderableWidget(new BlackKeyWidget(this.x + 88, this.y + 40, 9));
        this.addRenderableWidget(new BlackKeyWidget(this.x + 120, this.y + 40, 12));
        this.addRenderableWidget(new BlackKeyWidget(this.x + 136, this.y + 40, 14));
        this.addRenderableWidget(new BlackKeyWidget(this.x + 152, this.y + 40, 16));
        this.addRenderableWidget(new BlackKeyWidget(this.x + 184, this.y + 40, 19));
        this.addRenderableWidget(new BlackKeyWidget(this.x + 200, this.y + 40, 21));
        this.addRenderableWidget(new BlackKeyWidget(this.x + 232, this.y + 40, 24));

        this.addRenderableWidget(new PlayModeToggle(this.x + 184, this.y + 8));
        this.addRenderableWidget(new KeyToPianoToggle(this.x + 200, this.y + 8));
        this.addRenderableWidget(new MidiSwitch(this.x + 216, this.y + 8));
        this.addRenderableWidget(new MidiDeviceRefreshButton(this.x + 232, this.y + 8));

        this.addRenderableOnly(new KeySignature(this.x + 112, this.y + 8));
        this.addRenderableWidget(new KeyAddSharpButton(this.x + 144, this.y + 8));
        this.addRenderableWidget(new KeyAddFlatButton(this.x + 144, this.y + 16));

        if (currentDevice != null && !currentDevice.isOpen()) {
            openCurrentDevice();
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    @Override
    public void tick() {
        if (minecraft == null || minecraft.level == null || !minecraft.level.getBlockState(pos).is(Blocks.NOTE_BLOCK)) {
            this.onClose();
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractBackground(context, mouseX, mouseY, delta);
        this.drawBackground(context, delta, mouseX, mouseY);
    }

    protected void drawBackground(GuiGraphicsExtractor context, float delta, int mouseX, int mouseY) {
        assert this.minecraft != null;
        int i = (this.width - this.backgroundWidth) / 2;
        int j = (this.height - this.backgroundHeight) / 2;
        context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, i, j, 0.0F, 0.0F, this.backgroundWidth, this.backgroundHeight, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    abstract class PianoKeyWidget extends AbstractWidget {
        private final int note;
        protected boolean played;

        protected PianoKeyWidget(int x, int y, int width, int height, int note) {
            super(x, y, width, height, Component.empty());
            this.note = note;
            pianoKeys[note] = this;
        }

        protected void drawKeyTooltip(GuiGraphicsExtractor context) {
            if (this.visible && this.isHovered()) {
                context.setComponentTooltipForNextFrame(TuningScreen.this.font, List.of(Component.literal(NoteNames.get(note))), TuningScreen.this.x - 8, TuningScreen.this.y - 2);
            }
        }

        public void playKey() {
            pressedKey = this;
            played = true;

            if (minecraft != null && minecraft.player != null && minecraft.getConnection() != null) {
                sendTuningPacket(pos, note);
                minecraft.player.swing(InteractionHand.MAIN_HAND);
            }

            if (!BlockTunerConfig.isPlayMode()){
                onClose();
            }
        }

        public void releaseKey() {
            played = false;
            pressedKey = null;
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean doubleClick) {
            playKey();
        }

        @Override
        public void onRelease(MouseButtonEvent event) {
            releaseKey();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput builder) {
        }
    }

    class BlackKeyWidget extends PianoKeyWidget {
        public BlackKeyWidget(int x, int y, int note) {
            super(x, y, 16, 38, note);
        }

        @Override
        public void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
            this.isHovered = mouseX >= this.getX() && mouseY >= this.getY() && mouseX < this.getX() + this.width && mouseY < this.getY() + this.height;

            int status = 0;
            if (played) {
                status = 1;
            } else if (this.isHovered()) {
                status = 2;
            }
            context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.getX(), this.getY(), (float)(16 * status), 112.0F, 16, 38, TEXTURE_SIZE, TEXTURE_SIZE);

            drawKeyTooltip(context);
        }
    }

    class WhiteKeyWidget extends PianoKeyWidget {
        private final int keyShape;

        public WhiteKeyWidget(int x, int y, int note, int keyShape) {
            super(x, y, 16, 38, note);
            this.keyShape = keyShape;
        }

        @Override
        public void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
            boolean mask = mouseX >= this.getX() + 8 - 8 * keyShape && mouseY >= this.getY() && mouseX < this.getX() + 24 - 8 * keyShape && mouseY < this.getY() + 13;
            boolean inBounds = mouseX >= this.getX() && mouseY >= this.getY() && mouseX < this.getX() + this.width && mouseY < this.getY() + this.height;
            this.isHovered = inBounds && !mask;

            int status = 0;
            if (played) {
                status = 1;
            } else if (this.isHovered()) {
                status = 2;
            }

            context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.getX(), this.getY(), (float)(16 * status + 48 * keyShape + 48), 112.0F, 16, 38, TEXTURE_SIZE, TEXTURE_SIZE);

            drawKeyTooltip(context);
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return this.active && this.visible && this.isHovered;
        }
    }

    class PlayModeToggle extends AbstractWidget {
        public PlayModeToggle(int x, int y) {
            super(x, y, 16, 16, Component.empty());
        }

        @Override
        public void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
            int status = 0;
            if (BlockTunerConfig.isPlayMode()) {
                status = 2;
            }
            if (this.isHovered()) {
                status += 1;
            }
            context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.getX(), this.getY(), (float)(192 + 16 * status), 112.0F, 16, 16, TEXTURE_SIZE, TEXTURE_SIZE);

            if (this.isHovered()) {
                context.setComponentTooltipForNextFrame(TuningScreen.this.font, List.of(PLAY_MODE_TOGGLE_TOOLTIP), TuningScreen.this.x - 8, TuningScreen.this.y - 2);
            }
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean doubleClick) {
            BlockTunerConfig.togglePlayMode();
            configChanged = true;
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput builder) {}
    }

    class KeyToPianoToggle extends AbstractWidget {
        public KeyToPianoToggle(int x, int y) {
            super(x, y, 16, 16, Component.empty());
        }

        @Override
        public void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
            int status = 0;
            if (BlockTunerConfig.isKeyToPiano()) {
                status = 2;
            }
            if (this.isHovered()) {
                status += 1;
            }
            context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.getX(), this.getY(), (float)(192 + 16 * status), 128.0F, 16, 16, TEXTURE_SIZE, TEXTURE_SIZE);

            if (this.isHovered()) {
                context.setComponentTooltipForNextFrame(TuningScreen.this.font, List.of(KEY_TO_PIANO_TOGGLE_TOOLTIP), TuningScreen.this.x - 8, TuningScreen.this.y - 2);
            }
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean doubleClick) {
            BlockTunerConfig.toggleKeyToPiano();
            configChanged = true;
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput builder) {}
    }

    class MidiSwitch extends AbstractWidget {

        public MidiSwitch(int x, int y) {
            super(x, y, 16, 16, Component.empty());
            if (currentDevice == null) {
                deviceName = EMPTY_MIDI_DEVICE;
            } else {
                deviceName = Component.literal(currentDevice.getDeviceInfo().getName());
            }
        }

        @Override
        public void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
            int status = 0;
            if (midiManager.getDeviceIndex() > 0) {
                status = 2;
            }
            if (this.isHovered()) {
                status += 1;
            }
            if (!deviceAvailable) {
                status += 4;
            }
            context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.getX(), this.getY(), (float)(192 + 16 * (status % 4)), (float)(144 + 16 * (status / 4)), 16, 16, TEXTURE_SIZE, TEXTURE_SIZE);

            if (this.isHovered()) {
                context.setComponentTooltipForNextFrame(TuningScreen.this.font, List.of(Component.translatable("settings.blocktuner.midi_device", deviceName)), TuningScreen.this.x - 8, TuningScreen.this.y - 2);
            }
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean doubleClick) {
            if (currentDevice != null && currentDevice.isOpen()) {
                currentDevice.close();
            }

            midiManager.loopDeviceIndex();
            currentDevice = midiManager.getCurrentDevice();

            if (currentDevice != null) {
                BlockTunerConfig.setMidiDeviceName(currentDevice.getDeviceInfo().getName());
                deviceName = Component.literal(BlockTunerConfig.getMidiDeviceName());
                openCurrentDevice();
            } else {
                BlockTunerConfig.setMidiDeviceName("");
                deviceName = EMPTY_MIDI_DEVICE;
            }
            configChanged = true;
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput builder) {}
    }

    class MidiDeviceRefreshButton extends AbstractWidget {
        public MidiDeviceRefreshButton(int x, int y) {
            super(x, y, 16, 16, Component.empty());
        }

        @Override
        public void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
            int status = 0;
            if (this.isHovered()) {
                status += 1;
            }
            context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.getX(), this.getY(), (float)(192 + 16 * status), 176.0F, 16, 16, TEXTURE_SIZE, TEXTURE_SIZE);

            if (this.isHovered()) {
                context.setComponentTooltipForNextFrame(TuningScreen.this.font, List.of(MIDI_DEVICE_REFRESH_TOOLTIP), TuningScreen.this.x - 8, TuningScreen.this.y - 2);
            }
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean doubleClick) {
            midiManager.refreshMidiDevice();
            if (currentDevice != null) {
                openCurrentDevice();
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput builder) {
        }
    }

    static class KeySignature implements Renderable {

        public int x;
        public int y;

        public KeySignature(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
            int keySignature = BlockTunerConfig.getKeySignature();
            context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.x, this.y, (float)((keySignature + 8) % 8 * 32), (float)((keySignature + 8) / 8 * 16 + 224), 32, 16, TEXTURE_SIZE, TEXTURE_SIZE);
        }
    }

    class KeyAddSharpButton extends AbstractWidget {
        public KeyAddSharpButton(int x, int y) {
            super(x, y, 8, 8, Component.empty());
        }

        @Override
        public void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
            int status = 0;
            if (this.isHovered()) {
                status += 1;
            }
            context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.getX(), this.getY(), (float)(8 * status), 152.0F, 8, 8, TEXTURE_SIZE, TEXTURE_SIZE);
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean doubleClick) {
            BlockTunerConfig.keyAddSharp();
            configChanged = true;
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput builder) {}
    }

    class KeyAddFlatButton extends AbstractWidget {
        public KeyAddFlatButton(int x, int y) {
            super(x, y, 8, 8, Component.empty());
        }

        @Override
        public void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
            int status = 0;
            if (this.isHovered()) {
                status += 1;
            }
            context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.getX(), this.getY(), (float)(8 * status + 16), 152.0F, 8, 8, TEXTURE_SIZE, TEXTURE_SIZE);
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean doubleClick) {
            BlockTunerConfig.keyAddFlat();
            configChanged = true;
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput builder) {}
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent mouseButtonEvent) {
        this.setDragging(false);
        if (pressedKey != null) {
            return pressedKey.mouseReleased(mouseButtonEvent);
        } else {
            return super.mouseReleased(mouseButtonEvent);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (BlockTunerConfig.isKeyToPiano() && keyEvent.key() != 256) {
            int note = keyToNote(keyEvent.scancode());
            if (note >= 0 && note <= 24 && !pianoKeys[note].played) {
                pianoKeys[note].playKey();
            }
            return true;
        } else {
            if (keyEvent.key() == 69) { // 'E' key
                this.onClose();
                return true;
            }
            return super.keyPressed(keyEvent);
        }
    }

    @Override
    public boolean keyReleased(KeyEvent keyEvent) {
        int note = keyToNote(keyEvent.scancode());
        if (note >= 0 && note <= 24) {
            pianoKeys[note].releaseKey();
        }
        return super.keyReleased(keyEvent);
    }

    @Override
    public void onClose() {
        if (currentDevice != null && currentDevice.isOpen()) {
            currentDevice.close();
        }
        receiver.close();
        if (configChanged) {
            BlockTunerConfig.save();
        }
        super.onClose();
    }

    class MidiReceiver implements Receiver {
        public MidiReceiver() {}
        public void send(MidiMessage msg, long timeStamp) {
            byte[] message = msg.getMessage();
            if (message.length == 3 && message[0] <= -97 && message[1] >= 54 && message[1] <= 78) {
                assert minecraft != null;
                if (message[0] >= -112 && message[2] != 0) {
                    // MIDI note on
                    minecraft.execute(() -> pianoKeys[message[1] - 54].playKey());
                } else {
                    // MIDI note off
                    minecraft.execute(() -> pianoKeys[message[1] - 54].releaseKey());
                }
            }
        }
        public void close() {}
    }

    protected void openCurrentDevice(){
        try {
            currentDevice.open();
            deviceAvailable = true;
            currentDevice.getTransmitter().setReceiver(receiver);
        } catch (MidiUnavailableException e) {
            deviceAvailable = false;
            BlockTuner.LOGGER.info("[BlockTuner] MIDI device \"" + currentDevice.getDeviceInfo().getName() + "\" is currently unavailable. Is it busy or unplugged?");
        }
    }

    public static void sendTuningPacket(BlockPos pos, int note) {
        note = Mth.clamp(note, 0, 24);
        ClientPlayNetworking.send(new TuningC2SPacket(pos, note));
    }

    protected static int keyToNote(int scanCode) {
        return switch (scanCode) {
            case 3, 38 -> 7;
            case 4, 39 -> 9;
            case 6 -> 12;
            case 7 -> 14;
            case 8 -> 16;
            case 10 -> 19;
            case 11 -> 21;
            case 13 -> 24;
            case 16, 51 -> 6;
            case 17, 52 -> 8;
            case 18, 53 -> 10;
            case 19 -> 11;
            case 20 -> 13;
            case 21 -> 15;
            case 22 -> 17;
            case 23 -> 18;
            case 24 -> 20;
            case 25 -> 22;
            case 26 -> 23;
            case 34 -> 0;
            case 35 -> 2;
            case 36 -> 4;
            case 48 -> 1;
            case 49 -> 3;
            case 50 -> 5;
            default -> -1;
        };
    }
}