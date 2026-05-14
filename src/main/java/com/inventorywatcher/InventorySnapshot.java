package com.inventorywatcher;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import java.util.ArrayList;
import java.util.List;

public class InventorySnapshot {
    private final List<SnapshotSlot> slots;

    public InventorySnapshot(List<SnapshotSlot> slots) {
        this.slots = new ArrayList<>(slots);
    }

    /**
     * Captures the current state of the player's inventory.
     * Iterates through main inventory (0-35), armor (36-39), and offhand (40).
     * Skips empty slots.
     */
    public static InventorySnapshot capture(PlayerInventory inventory) {
        List<SnapshotSlot> slots = new ArrayList<>();

        // Main inventory (0-35)
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inventory.getStack(i);
            if (!stack.isEmpty()) {
                slots.add(createSlot(i, stack));
            }
        }

        // Armor slots (36-39)
        for (int i = 0; i < 4; i++) {
            ItemStack stack = inventory.getStack(36 + i);
            if (!stack.isEmpty()) {
                slots.add(createSlot(36 + i, stack));
            }
        }

        // Offhand slot (40)
        ItemStack offhandStack = inventory.getStack(40);
        if (!offhandStack.isEmpty()) {
            slots.add(createSlot(40, offhandStack));
        }

        return new InventorySnapshot(slots);
    }

    /**
     * Helper method to create a SnapshotSlot from an ItemStack.
     */
    private static SnapshotSlot createSlot(int index, ItemStack stack) {
        String itemId = stack.getItem().getTranslationKey();
        int count = stack.getCount();
        
        // Note: NBT handling is optional for this phase
        // Can be enhanced later if needed
        NbtCompound nbt = null;
        
        return new SnapshotSlot(index, itemId, count, nbt);
    }

    /**
     * Compares this snapshot with another.
     * Returns true only if both snapshots contain exactly the same slots,
     * item IDs, counts, and NBT.
     */
    public boolean isIdenticalTo(InventorySnapshot other) {
        if (other == null) {
            return false;
        }

        if (this.slots.size() != other.slots.size()) {
            return false;
        }

        // Compare each slot
        for (int i = 0; i < this.slots.size(); i++) {
            SnapshotSlot thisSlot = this.slots.get(i);
            SnapshotSlot otherSlot = other.slots.get(i);

            if (!thisSlot.equals(otherSlot)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns a human-readable multi-line string of the inventory snapshot.
     */
    public String toLogString() {
        StringBuilder sb = new StringBuilder("Inventory Snapshot:\n");
        for (int i = 0; i <= 40; i++) {
            SnapshotSlot slot = null;
            for (SnapshotSlot candidate : slots) {
                if (candidate.slotIndex == i) {
                    slot = candidate;
                    break;
                }
            }

            if (slot == null) {
                sb.append("  Slot ").append(i).append(": (empty)").append("\n");
                continue;
            }

            sb.append("  Slot ").append(slot.slotIndex).append(": ")
              .append(slot.itemId).append(" x").append(slot.count);
            if (slot.nbt != null && !slot.nbt.isEmpty()) {
                sb.append(" [nbt: ").append(slot.nbt).append("]");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Internal record for storing a single slot's data.
     */
    public record SnapshotSlot(int slotIndex, String itemId, int count, NbtCompound nbt) {
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof SnapshotSlot other)) return false;

            if (this.slotIndex != other.slotIndex) return false;
            if (!this.itemId.equals(other.itemId)) return false;
            if (this.count != other.count) return false;

            // Compare NBT
            if (this.nbt == null && other.nbt == null) return true;
            if (this.nbt == null || other.nbt == null) return false;

            return this.nbt.equals(other.nbt);
        }

        @Override
        public int hashCode() {
            int result = slotIndex;
            result = 31 * result + itemId.hashCode();
            result = 31 * result + count;
            result = 31 * result + (nbt != null ? nbt.hashCode() : 0);
            return result;
        }
    }
}
