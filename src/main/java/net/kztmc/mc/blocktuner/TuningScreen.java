package net.kztmc.mc.blocktuner;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.block.Blocks;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.MouseInput;
// NOTE: keyPressed/keyReleasedの引数型・Click/MouseInputのコンストラクタは未検証(推測)です。
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;

@Environment(EnvType.CLIENT)
public class TuningScreen extends Screen {

    private final BlockPos pos;
    private PianoKeyWidget pressedKey = null;
    private final PianoKeyWidget[] pianoKeys = new PianoKeyWidget[25];
    private final MidiManager midiManager;
    private MidiDevice currentDevice;
    private final MidiReceiver receiver;
    private boolean configChanged = false;
    private Text deviceName;
    private boolean deviceAvailable = true;
    protected int backgroundWidth = 256;
    protected int backgroundHeight = 112;
    protected int x;
    protected int y;
    protected static final Text PLAY_MODE_TOGGLE_TOOLTIP = Text.translatable("settings.blocktuner.play_mode");
    protected static final Text KEY_TO_PIANO_TOGGLE_TOOLTIP =Text.translatable("settings.blocktuner.key_to_piano");
    protected static final Text EMPTY_MIDI_DEVICE = Text.translatable("midi_device.empty");
    protected static final Text MIDI_DEVICE_REFRESH_TOOLTIP = Text.translatable("settings.blocktuner.refresh");

    static final int TEXTURE_SIZE = 256;
    static final Identifier TEXTURE = Identifier.of("blocktuner", "textures/gui/container/tune.png");

    public TuningScreen(Text title, BlockPos pos) {
        super(title);
        this.pos = pos;

        midiManager = MidiManager.getMidiManager();
        currentDevice = midiManager.getCurrentDevice();
        receiver = new MidiReceiver();
    }

    private static Click syntheticClick() {
        return new Click(0, 0, new MouseInput(0, 0));
    }

    @Override
    protected void init() {
        super.init();

        this.x = (this.width - this.backgroundWidth) / 2;
        this.y = (this.height - this.backgroundHeight) / 2;

        // Fancy(?) keyboard

        this.addDrawableChild(new WhiteKeyWidget(this.x + 16, this.y + 65, 1, 1));
        this.addDrawableChild(new WhiteKeyWidget(this.x + 32, this.y + 65, 3, 1));
        this.addDrawableChild(new WhiteKeyWidget(this.x + 48, this.y + 65, 5, 2));
        this.addDrawableChild(new WhiteKeyWidget(this.x + 64, this.y + 65, 6, 0));
        this.addDrawableChild(new WhiteKeyWidget(this.x + 80, this.y + 65, 8, 1));
        this.addDrawableChild(new WhiteKeyWidget(this.x + 96, this.y + 65, 10, 2));
        this.addDrawableChild(new WhiteKeyWidget(this.x + 112, this.y + 65, 11, 0));
        this.addDrawableChild(new WhiteKeyWidget(this.x + 128, this.y + 65, 13, 1));
        this.addDrawableChild(new WhiteKeyWidget(this.x + 144, this.y + 65, 15, 1));
        this.addDrawableChild(new WhiteKeyWidget(this.x + 160, this.y + 65, 17, 2));
        this.addDrawableChild(new WhiteKeyWidget(this.x + 176, this.y + 65, 18, 0));
        this.addDrawableChild(new WhiteKeyWidget(this.x + 192, this.y + 65, 20, 1));
        this.addDrawableChild(new WhiteKeyWidget(this.x + 208, this.y + 65, 22, 2));
        this.addDrawableChild(new WhiteKeyWidget(this.x + 224, this.y + 65, 23, 0));
        this.addDrawableChild(new BlackKeyWidget(this.x + 8, this.y + 40, 0));
        this.addDrawableChild(new BlackKeyWidget(this.x + 24, this.y + 40, 2));
        this.addDrawableChild(new BlackKeyWidget(this.x + 40, this.y + 40, 4));
        this.addDrawableChild(new BlackKeyWidget(this.x + 72, this.y + 40, 7));
        this.addDrawableChild(new BlackKeyWidget(this.x + 88, this.y + 40, 9));
        this.addDrawableChild(new BlackKeyWidget(this.x + 120, this.y + 40, 12));
        this.addDrawableChild(new BlackKeyWidget(this.x + 136, this.y + 40, 14));
        this.addDrawableChild(new BlackKeyWidget(this.x + 152, this.y + 40, 16));
        this.addDrawableChild(new BlackKeyWidget(this.x + 184, this.y + 40, 19));
        this.addDrawableChild(new BlackKeyWidget(this.x + 200, this.y + 40, 21));
        this.addDrawableChild(new BlackKeyWidget(this.x + 232, this.y + 40, 24));

        this.addDrawableChild(new PlayModeToggle(this.x + 184, this.y + 8));
        this.addDrawableChild(new KeyToPianoToggle(this.x + 200, this.y + 8));
        this.addDrawableChild(new MidiSwitch(this.x + 216, this.y + 8));
        this.addDrawableChild(new MidiDeviceRefreshButton(this.x + 232, this.y + 8));

        this.addDrawable(new KeySignature(this.x + 112, this.y + 8));
        this.addDrawableChild(new KeyAddSharpButton(this.x + 144, this.y + 8));
        this.addDrawableChild(new KeyAddFlatButton(this.x + 144, this.y + 16));

        if (currentDevice != null && !currentDevice.isOpen()) {
            openCurrentDevice();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void tick() {
        if (client == null || client.world == null || client.world.getBlockState(pos).getBlock() != Blocks.NOTE_BLOCK) {
            this.close();
        }
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
        this.drawBackground(context, delta, mouseX, mouseY);
    }

    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        assert this.client != null;
        int i = (this.width - this.backgroundWidth) / 2;
        int j = (this.height - this.backgroundHeight) / 2;
        context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, i, j, 0, 0, this.backgroundWidth, this.backgroundHeight, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    abstract class PianoKeyWidget extends ClickableWidget {
        private final int note;
        protected boolean played;

        protected PianoKeyWidget(int x, int y, int width, int height, int note) {
            super(x, y, width, height, Text.empty());
            this.note = note;
            pianoKeys[note] = this;
        }

        protected void drawKeyTooltip(DrawContext context) {
            if (this.visible && this.hovered) {
                context.drawTooltip(TuningScreen.this.textRenderer, Text.literal(NoteNames.get(note)), TuningScreen.this.x - 8, TuningScreen.this.y - 2);
            }
        }

        @Override
        public void onClick(Click mouseButtonEvent, boolean doubleClick){

            pressedKey = this;
            played = true;

            if (client != null && client.player != null && client.getNetworkHandler() != null) {
                sendTuningPacket(pos, note);
                client.player.swingHand(Hand.MAIN_HAND);
            }

            if (!BlockTunerConfig.isPlayMode()){
                close();
            }
        }

        @Override
        public void onRelease(Click mouseButtonEvent) {
            played = false;
            pressedKey = null;
        }

        @Override
        public boolean mouseClicked(Click mouseButtonEvent, boolean doubleClick) {
            if (this.active && this.visible) {
                if (this.isValidClickButton(mouseButtonEvent.buttonInfo())) {
                    boolean bl = this.isMouseOver(mouseButtonEvent.x(), mouseButtonEvent.y());
                    if (bl) {
                        this.onClick(mouseButtonEvent, doubleClick);
                        return true;
                    }
                }
            }
            return false;
        }
        @Override
        public void appendClickableNarrations(NarrationMessageBuilder builder) {
        }
    }

    class BlackKeyWidget extends PianoKeyWidget{
        public BlackKeyWidget(int x, int y, int note) {
            super(x, y, 16, 38, note);
        }

        @Override
        public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            this.hovered = mouseX >= this.getX() && mouseY >= this.getY() && mouseX < this.getX() + this.width && mouseY < this.getY() + this.height;

            int status = 0;
            if (played) {
                status = 1;
            } else if (this.isHovered()) {
                status = 2;
            }
            context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, this.getX(), this.getY(), 16 * status, 112, 16, 38, TEXTURE_SIZE, TEXTURE_SIZE);

            drawKeyTooltip(context);
        }
    }

    class WhiteKeyWidget extends PianoKeyWidget{
        private final int keyShape;

        public WhiteKeyWidget(int x, int y, int note, int keyShape) {
            super(x, y, 16, 38, note);
            this.keyShape = keyShape;
        }

        @Override
        public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            boolean mask = mouseX >= this.getX() + 8 - 8 * keyShape && mouseY >= this.getY() && mouseX < this.getX() + 24 - 8 * keyShape && mouseY < this.getY() + 13;
            boolean inBounds = mouseX >= this.getX() && mouseY >= this.getY() && mouseX < this.getX() + this.width && mouseY < this.getY() + this.height;
            this.hovered = inBounds && !mask;

            int status = 0;
            if (played) {
                status = 1;
            } else if (this.isHovered()) {
                status = 2;
            }

            context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, this.getX(), this.getY(), 16 * status + 48 * keyShape + 48, 112, 16, 38, TEXTURE_SIZE, TEXTURE_SIZE);

            drawKeyTooltip(context);
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return this.active && this.visible && this.hovered;
        }

    }

    class PlayModeToggle extends ClickableWidget{
        public PlayModeToggle(int x, int y) {
            super(x, y, 16, 16, Text.empty());
        }

        @Override
        public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            int status = 0;
            if (BlockTunerConfig.isPlayMode()) {
                status = 2;
            }
            if (this.isHovered()) {
                status += 1;
            }
            context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, this.getX(), this.getY(), 192 + 16 * status, 112, 16, 16, TEXTURE_SIZE, TEXTURE_SIZE);

            if (this.isHovered()) {
                context.drawTooltip(TuningScreen.this.textRenderer, PLAY_MODE_TOGGLE_TOOLTIP, TuningScreen.this.x - 8 , TuningScreen.this.y - 2);
            }
        }

        @Override
        public void onClick(Click mouseButtonEvent, boolean doubleClick){
            BlockTunerConfig.togglePlayMode();
            configChanged = true;
        }

        @Override
        public void appendClickableNarrations(NarrationMessageBuilder builder) {}
    }

    class KeyToPianoToggle extends ClickableWidget{
        public KeyToPianoToggle(int x, int y) {
            super(x, y, 16, 16, Text.empty());
        }

        @Override
        public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            int status = 0;
            if (BlockTunerConfig.isKeyToPiano()) {
                status = 2;
            }
            if (this.isHovered()) {
                status += 1;
            }
            context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, this.getX(), this.getY(), 192 + 16 * status, 128, 16, 16, TEXTURE_SIZE, TEXTURE_SIZE);

            if (this.isHovered()) {
                context.drawTooltip(TuningScreen.this.textRenderer, KEY_TO_PIANO_TOGGLE_TOOLTIP, TuningScreen.this.x - 8 , TuningScreen.this.y - 2);
            }
        }

        @Override
        public void onClick(Click mouseButtonEvent, boolean doubleClick){
            BlockTunerConfig.toggleKeyToPiano();
            configChanged = true;
        }

        @Override
        public void appendClickableNarrations(NarrationMessageBuilder builder) {}
    }

    class MidiSwitch extends ClickableWidget{

        public MidiSwitch(int x, int y) {
            super(x, y, 16, 16, Text.empty());
            if (currentDevice == null) {
                deviceName = EMPTY_MIDI_DEVICE;
            } else {
                deviceName = Text.literal(currentDevice.getDeviceInfo().getName());
            }
        }

        @Override
        public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
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
            context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, this.getX(), this.getY(), 192 + 16 * (status % 4), 144 + 16 * (status / 4), 16, 16, TEXTURE_SIZE, TEXTURE_SIZE);

            if (this.isHovered()) {
                context.drawTooltip(TuningScreen.this.textRenderer, Text.translatable("settings.blocktuner.midi_device", deviceName), TuningScreen.this.x - 8 , TuningScreen.this.y - 2);
            }
        }

        @Override
        public void onClick(Click mouseButtonEvent, boolean doubleClick){

            if (currentDevice != null && currentDevice.isOpen()) {
                currentDevice.close();
            }

            midiManager.loopDeviceIndex();
            currentDevice = midiManager.getCurrentDevice();

            if (currentDevice != null) {
                BlockTunerConfig.setMidiDeviceName(currentDevice.getDeviceInfo().getName());
                deviceName = Text.literal(BlockTunerConfig.getMidiDeviceName());
                openCurrentDevice();
            } else {
                BlockTunerConfig.setMidiDeviceName("");
                deviceName = EMPTY_MIDI_DEVICE;
            }
            configChanged = true;
        }

        @Override
        public void appendClickableNarrations(NarrationMessageBuilder builder) {}
    }

    class MidiDeviceRefreshButton extends ClickableWidget{
        public MidiDeviceRefreshButton(int x, int y) {
            super(x, y, 16, 16, Text.empty());
        }

        @Override
        public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            int status = 0;
            if (this.isHovered()) {
                status += 1;
            }
            context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, this.getX(), this.getY(), 192 + 16 * status, 176, 16, 16, TEXTURE_SIZE, TEXTURE_SIZE);

            if (this.isHovered()) {
                context.drawTooltip(TuningScreen.this.textRenderer, MIDI_DEVICE_REFRESH_TOOLTIP, TuningScreen.this.x - 8 , TuningScreen.this.y - 2);
            }
        }

        @Override
        public void onClick(Click mouseButtonEvent, boolean doubleClick){
            midiManager.refreshMidiDevice();
            if (currentDevice != null) {
                openCurrentDevice();
            }
        }

        @Override
        public void appendClickableNarrations(NarrationMessageBuilder builder) {
        }
    }

    static class KeySignature implements Drawable {

        public int x;
        public int y;

        public KeySignature(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            int keySignature = BlockTunerConfig.getKeySignature();
            context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, this.x, this.y, (keySignature + 8) % 8 * 32, (keySignature + 8) / 8 * 16 + 224, 32, 16, TEXTURE_SIZE, TEXTURE_SIZE);
        }
    }

    class KeyAddSharpButton extends ClickableWidget {
        public KeyAddSharpButton(int x, int y) {
            super(x, y, 8, 8, Text.empty());
        }

        @Override
        public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            int status = 0;
            if (this.isHovered()) {
                status += 1;
            }
            context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, this.getX(), this.getY(), 8 * status, 152, 8, 8, TEXTURE_SIZE, TEXTURE_SIZE);
        }

        @Override
        public void onClick(Click mouseButtonEvent, boolean doubleClick) {
            BlockTunerConfig.keyAddSharp();
            configChanged = true;
        }

        @Override
        public void appendClickableNarrations(NarrationMessageBuilder builder) {}
    }

    class KeyAddFlatButton extends ClickableWidget{
        public KeyAddFlatButton(int x, int y) {
            super(x, y, 8, 8, Text.empty());
        }

        @Override
        public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            int status = 0;
            if (this.isHovered()) {
                status += 1;
            }
            context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, this.getX(), this.getY(), 8 * status + 16, 152, 8, 8, TEXTURE_SIZE, TEXTURE_SIZE);
        }

        @Override
        public void onClick(Click mouseButtonEvent, boolean doubleClick) {
            BlockTunerConfig.keyAddFlat();
            configChanged = true;
        }

        @Override
        public void appendClickableNarrations(NarrationMessageBuilder builder) {}
    }

    @Override
    public boolean mouseReleased(Click mouseButtonEvent) {

        this.setDragging(false);
        if (pressedKey != null) {
            return pressedKey.mouseReleased(mouseButtonEvent);
        } else {
            return super.mouseReleased(mouseButtonEvent);
        }
    }

    @Override
    public boolean keyPressed(KeyInput keyEvent) {

        if (BlockTunerConfig.isKeyToPiano() && keyEvent.key() != 256) {
            int note = keyToNote(keyEvent.scancode());
            if (note >= 0 && note <= 24 && !pianoKeys[note].played) {
                pianoKeys[note].onClick(syntheticClick(), false);
            }
            return true;
        } else {
            if (keyEvent.key() == 69) {
                this.close();
                return true;
            }
            return super.keyPressed(keyEvent);
        }
    }

    @Override
    public boolean keyReleased(KeyInput keyEvent) {
        int note = keyToNote(keyEvent.scancode());
        if (note >= 0 && note <= 24) {
            pianoKeys[note].onRelease(syntheticClick());
        }
        return super.keyReleased(keyEvent);
    }

    public void close() {
        if (currentDevice != null && currentDevice.isOpen()) {
            currentDevice.close();
        }
        receiver.close();
        if (configChanged) {
            BlockTunerConfig.save();
        }
        super.close();
    }

    class MidiReceiver implements Receiver {
        public MidiReceiver() {}
        public void send(MidiMessage msg, long timeStamp) {
            byte[] message = msg.getMessage();
            if (message.length == 3 && message[0] <= -97 && message[1] >= 54 && message[1] <= 78) {
                assert client != null;
                if (message[0] >= -112 && message[2] != 0) {

                    // MIDI note on
                    client.execute(()-> pianoKeys[message[1] - 54].onClick(syntheticClick(), false));

                } else {

                    // MIDI note off
                    client.execute(()-> pianoKeys[message[1] - 54].onRelease(syntheticClick()));

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
        note = MathHelper.clamp(note, 0, 24);
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