package com.velorise.staves.inventory;

import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.item.ArcaneStaffItem;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ShulkerBoxBlock;

/**
 * Portable 3-row (Basic, 27 slots) / 6-row Double-Chest (Netherite, 54 slots) Shulker Seeker storage menu
 * rendered in a fixed 3x9 viewport with a vertical scrollbar when upgraded to Netherite.
 */
public class ShulkerStaffMenu extends AbstractContainerMenu {
    public static final int VISIBLE_ROWS = 3;
    public static final int COLS = 9;
    public static final int VISIBLE_SLOTS = VISIBLE_ROWS * COLS; // 27
    public static final int MAX_CAPACITY = 54; // 6 rows
    private static final String TAG_STORAGE = "ShulkerStorage";

    private final SimpleContainer storage;
    private final ContainerData data; // [0] = isNetherite (0/1), [1] = lockedHotbarSlot (-1..8), [2] = scrollRowOffset (0..3)
    private final Player player;
    private final InteractionHand hand;
    private ItemStack backingStack;
    private boolean saving = false;

    public ShulkerStaffMenu(int containerId, Inventory playerInv) {
        this(containerId, playerInv, ItemStack.EMPTY, null, -1, false);
    }

    public ShulkerStaffMenu(
            int containerId,
            Inventory playerInv,
            ItemStack staffStack,
            InteractionHand hand,
            int lockedHotbarSlot,
            boolean netherite
    ) {
        super(VeloriseStavesMod.getShulkerStaffMenuType(), containerId);
        this.player = playerInv.player;
        this.hand = hand;
        this.backingStack = staffStack;
        this.storage = new SimpleContainer(MAX_CAPACITY);
        this.data = new SimpleContainerData(3);

        if (this.player != null && this.player.level().isClientSide()) {
            ItemStack main = this.player.getMainHandItem();
            ItemStack off = this.player.getOffhandItem();
            if (ArcaneStaffItem.getEffectiveKind(main) == ArcaneStaffItem.Kind.SHULKER) {
                this.data.set(0, ArcaneStaffItem.getEffectiveNetherite(main) ? 1 : 0);
                this.data.set(1, playerInv.selected);
            } else if (ArcaneStaffItem.getEffectiveKind(off) == ArcaneStaffItem.Kind.SHULKER) {
                this.data.set(0, ArcaneStaffItem.getEffectiveNetherite(off) ? 1 : 0);
                this.data.set(1, -1);
            } else {
                this.data.set(0, netherite ? 1 : 0);
                this.data.set(1, lockedHotbarSlot);
            }
        } else {
            this.data.set(0, netherite ? 1 : 0);
            this.data.set(1, lockedHotbarSlot);
        }
        this.data.set(2, 0);

        if (!staffStack.isEmpty() && this.player != null && !this.player.level().isClientSide()) {
            loadFromStack(staffStack, this.storage);
        }

        this.storage.addListener(c -> {
            if (!saving && this.player != null && !this.player.level().isClientSide()) {
                saveToHeldStack();
            }
        });

        this.addDataSlots(this.data);

        // 27 visible Shulker storage slots (rows 0..2 of the current scroll viewport)
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int visibleIdx = row * COLS + col;
                this.addSlot(new ScrollableStorageSlot(this.storage, visibleIdx, 8 + col * 18, 18 + row * 18));
            }
        }

        // Player main inventory (3 rows x 9 cols)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Player hotbar (9 cols)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new LockedHotbarSlot(playerInv, col, 8 + col * 18, 142));
        }
    }

    public static void open(ServerPlayer player, ItemStack stack, InteractionHand hand, boolean isNetherite) {
        if (player == null || stack.isEmpty()) return;
        int lockedSlot = (hand == InteractionHand.MAIN_HAND) ? player.getInventory().selected : -1;
        Component title = stack.getHoverName();
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.SHULKER_OPEN, SoundSource.PLAYERS, 0.75F, 1.05F);
        player.openMenu(new SimpleMenuProvider(
                (containerId, inv, p) -> new ShulkerStaffMenu(containerId, inv, stack, hand, lockedSlot, isNetherite),
                title
        ));
    }

    public static boolean canStoreItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (ArcaneStaffItem.getEffectiveKind(stack) == ArcaneStaffItem.Kind.SHULKER) return false;
        if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock) {
            return false;
        }
        return true;
    }

    public boolean isNetherite() {
        return this.data.get(0) != 0;
    }

    public int getCapacity() {
        return isNetherite() ? MAX_CAPACITY : VISIBLE_SLOTS;
    }

    public int getMaxRowOffset() {
        return isNetherite() ? 3 : 0;
    }

    public int getScrollRowOffset() {
        return Mth.clamp(this.data.get(2), 0, getMaxRowOffset());
    }

    public void setClientScrollOffset(int offset) {
        this.data.set(2, Mth.clamp(offset, 0, getMaxRowOffset()));
    }

    public int getLockedHotbarSlot() {
        return this.data.get(1);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        int next = Mth.clamp(id, 0, getMaxRowOffset());
        if (next != getScrollRowOffset()) {
            this.data.set(2, next);
            this.broadcastFullState();
        }
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        if (player == null || !player.isAlive()) return false;
        ItemStack current = resolveCurrentHeldStack(player);
        return !current.isEmpty() && ArcaneStaffItem.getEffectiveKind(current) == ArcaneStaffItem.Kind.SHULKER;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player != null && !player.level().isClientSide()) {
            saveToHeldStack();
            player.level().playSound(null, player.blockPosition(), SoundEvents.SHULKER_CLOSE, SoundSource.PLAYERS, 0.70F, 1.0F);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < VISIBLE_SLOTS) {
                if (!this.moveItemStackTo(stack, VISIBLE_SLOTS, VISIBLE_SLOTS + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!canStoreItem(stack)) {
                    return ItemStack.EMPTY;
                }
                if (!insertIntoBackingStorage(stack)) {
                    return ItemStack.EMPTY;
                }
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (stack.getCount() == result.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
        }
        return result;
    }

    private boolean insertIntoBackingStorage(ItemStack stack) {
        boolean moved = false;
        int cap = getCapacity();
        if (stack.isStackable()) {
            for (int i = 0; i < cap && !stack.isEmpty(); i++) {
                ItemStack existing = this.storage.getItem(i);
                if (!existing.isEmpty() && ItemStack.isSameItemSameTags(stack, existing)) {
                    int max = Math.min(stack.getMaxStackSize(), this.storage.getMaxStackSize());
                    int space = max - existing.getCount();
                    if (space > 0) {
                        int moveAmt = Math.min(stack.getCount(), space);
                        existing.grow(moveAmt);
                        stack.shrink(moveAmt);
                        this.storage.setItem(i, existing);
                        moved = true;
                    }
                }
            }
        }
        for (int i = 0; i < cap && !stack.isEmpty(); i++) {
            ItemStack existing = this.storage.getItem(i);
            if (existing.isEmpty()) {
                int moveAmt = Math.min(stack.getCount(), this.storage.getMaxStackSize());
                ItemStack placed = stack.split(moveAmt);
                this.storage.setItem(i, placed);
                moved = true;
            }
        }
        if (moved) {
            this.storage.setChanged();
            this.broadcastChanges();
        }
        return moved;
    }

    private ItemStack resolveCurrentHeldStack(Player player) {
        if (this.hand == InteractionHand.OFF_HAND) {
            return player.getOffhandItem();
        }
        int locked = getLockedHotbarSlot();
        if (locked >= 0 && locked < 9) {
            return player.getInventory().getItem(locked);
        }
        return player.getMainHandItem();
    }

    private void saveToHeldStack() {
        if (this.player == null || this.player.level().isClientSide()) return;
        ItemStack target = resolveCurrentHeldStack(this.player);
        if (target.isEmpty() || ArcaneStaffItem.getEffectiveKind(target) != ArcaneStaffItem.Kind.SHULKER) {
            target = this.backingStack;
        }
        if (target == null || target.isEmpty()) return;
        this.backingStack = target;
        this.saving = true;
        try {
            saveToStack(target, this.storage);
        } finally {
            this.saving = false;
        }
    }

    private static void loadFromStack(ItemStack stack, SimpleContainer target) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_STORAGE)) return;
        NonNullList<ItemStack> list = NonNullList.withSize(MAX_CAPACITY, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag.getCompound(TAG_STORAGE), list);
        for (int i = 0; i < MAX_CAPACITY; i++) {
            target.setItem(i, list.get(i));
        }
    }

    private static void saveToStack(ItemStack stack, SimpleContainer source) {
        NonNullList<ItemStack> list = NonNullList.withSize(MAX_CAPACITY, ItemStack.EMPTY);
        for (int i = 0; i < MAX_CAPACITY; i++) {
            list.set(i, source.getItem(i).copy());
        }
        CompoundTag boxTag = new CompoundTag();
        ContainerHelper.saveAllItems(boxTag, list);
        stack.getOrCreateTag().put(TAG_STORAGE, boxTag);
    }

    private final class ScrollableStorageSlot extends Slot {
        private final int visibleIndex;

        ScrollableStorageSlot(Container container, int visibleIndex, int x, int y) {
            super(container, visibleIndex, x, y);
            this.visibleIndex = visibleIndex;
        }

        private int backingIndex() {
            return Mth.clamp(this.visibleIndex + getScrollRowOffset() * COLS, 0, MAX_CAPACITY - 1);
        }

        @Override
        public ItemStack getItem() {
            return this.container.getItem(backingIndex());
        }

        @Override
        public void set(ItemStack stack) {
            this.container.setItem(backingIndex(), stack);
            this.setChanged();
        }

        @Override
        public void setByPlayer(ItemStack newStack) {
            this.container.setItem(backingIndex(), newStack);
            this.setChanged();
        }

        @Override
        public ItemStack remove(int amount) {
            return this.container.removeItem(backingIndex(), amount);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return canStoreItem(stack) && backingIndex() < getCapacity();
        }
    }

    private final class LockedHotbarSlot extends Slot {
        private final int hotbarIndex;

        LockedHotbarSlot(Container container, int hotbarIndex, int x, int y) {
            super(container, hotbarIndex, x, y);
            this.hotbarIndex = hotbarIndex;
        }

        @Override
        public boolean mayPickup(Player player) {
            return this.hotbarIndex != getLockedHotbarSlot();
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return this.hotbarIndex != getLockedHotbarSlot();
        }
    }
}
