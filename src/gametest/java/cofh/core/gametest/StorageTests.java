package cofh.core.gametest;

import cofh.lib.api.IStorageCallback;
import cofh.lib.common.energy.EnergyStorageCoFH;
import cofh.lib.common.fluid.FluidStorageCoFH;
import cofh.lib.common.fluid.SimpleFluidHandler;
import cofh.lib.common.inventory.ItemStorageCoFH;
import cofh.lib.common.inventory.ManagedItemInv;
import cofh.lib.common.inventory.SimpleItemHandler;
import cofh.lib.common.inventory.SimpleItemInv;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.List;

import static cofh.lib.api.StorageGroup.*;

public class StorageTests {

    private static final ItemResource COBBLESTONE = ItemResource.of(Items.COBBLESTONE);
    private static final ItemResource STONE = ItemResource.of(Items.STONE);

    public static void energyStorageTransactions(GameTestHelper helper) {

        EnergyStorageCoFH storage = new EnergyStorageCoFH(1000, 100);

        try (Transaction tx = Transaction.openRoot()) {
            helper.assertValueEqual(storage.insert(60, tx), 60, "inserted");
        }
        helper.assertValueEqual(storage.getEnergyStored(), 0, "energy after abort");

        try (Transaction tx = Transaction.openRoot()) {
            storage.insert(60, tx);
            try (Transaction inner = Transaction.open(tx)) {
                storage.insert(30, inner);
            }
            tx.commit();
        }
        helper.assertValueEqual(storage.getEnergyStored(), 60, "energy after inner abort");

        try (Transaction tx = Transaction.openRoot()) {
            helper.assertValueEqual(storage.insert(500, tx), 100, "insert over max receive");
            helper.assertValueEqual(storage.extract(500, tx), 100, "extract over max extract");
            tx.commit();
        }
        helper.assertValueEqual(storage.getEnergyStored(), 60, "energy after commit");
        helper.succeed();
    }

    public static void itemHandlerTransactions(GameTestHelper helper) {

        int[] changes = new int[1];
        SimpleItemInv inv = new SimpleItemInv(new IStorageCallback() {
            @Override
            public void onInventoryChanged(int slot) {
                ++changes[0];
            }
        });
        inv.addSlot(new ItemStorageCoFH(64));

        try (Transaction tx = Transaction.openRoot()) {
            helper.assertValueEqual(inv.insert(0, COBBLESTONE, 10, tx), 10, "inserted");
        }
        helper.assertTrue(inv.getStackInSlot(0).isEmpty(), "Slot should be empty after abort");
        helper.assertValueEqual(changes[0], 0, "change callbacks after abort");

        try (Transaction tx = Transaction.openRoot()) {
            inv.insert(0, COBBLESTONE, 10, tx);
            tx.commit();
        }
        helper.assertValueEqual(inv.getStackInSlot(0).getCount(), 10, "count after commit");
        helper.assertValueEqual(changes[0], 1, "change callbacks after commit");

        try (Transaction tx = Transaction.openRoot()) {
            try (Transaction inner = Transaction.open(tx)) {
                helper.assertValueEqual(inv.extract(0, COBBLESTONE, 4, inner), 4, "extracted");
                inner.commit();
            }
        }
        helper.assertValueEqual(inv.getStackInSlot(0).getCount(), 10, "count after outer abort");
        helper.assertValueEqual(changes[0], 1, "change callbacks after outer abort");
        helper.succeed();
    }

    public static void managedItemHandlerRules(GameTestHelper helper) {

        ManagedItemInv inv = new ManagedItemInv(null);
        inv.addSlots(INPUT, 1);
        inv.addSlots(OUTPUT, 1);
        inv.initHandlers();
        inv.getSlot(1).setItemStack(new ItemStack(Items.STONE, 8));
        SimpleItemHandler io = inv.getHandler(INPUT_OUTPUT);
        SimpleItemHandler output = inv.getHandler(OUTPUT);

        try (Transaction tx = Transaction.openRoot()) {
            helper.assertValueEqual(io.insert(0, COBBLESTONE, 5, tx), 5, "insert into input");
            helper.assertValueEqual(io.insert(1, STONE, 5, tx), 0, "insert into output");
            helper.assertValueEqual(io.extract(0, COBBLESTONE, 5, tx), 0, "extract from restricted input");
            helper.assertValueEqual(io.extract(1, STONE, 3, tx), 3, "extract from output");
            helper.assertValueEqual(output.insert(0, STONE, 1, tx), 0, "insert through output handler");
            tx.commit();
        }
        helper.assertValueEqual(inv.getSlot(0).getCount(), 5, "input count");
        helper.assertValueEqual(inv.getSlot(1).getCount(), 5, "output count");
        helper.succeed();
    }

    public static void sharedSlotJournal(GameTestHelper helper) {

        ManagedItemInv inv = new ManagedItemInv(null);
        inv.addSlots(INPUT, 1);
        inv.initHandlers();
        inv.getSlot(0).setItemStack(new ItemStack(Items.COBBLESTONE, 10));
        SimpleItemHandler input = inv.getHandler(INPUT);
        SimpleItemHandler accessible = inv.getHandler(ACCESSIBLE);

        // Two handlers over one slot must revert to the original, whatever order they touched it in.
        try (Transaction tx = Transaction.openRoot()) {
            input.insert(0, COBBLESTONE, 5, tx);
            try (Transaction inner = Transaction.open(tx)) {
                accessible.extract(0, COBBLESTONE, 12, inner);
                inner.commit();
            }
            input.insert(0, COBBLESTONE, 2, tx);
        }
        helper.assertValueEqual(inv.getSlot(0).getCount(), 10, "count after abort");
        helper.succeed();
    }

    public static void fluidHandlerTransactions(GameTestHelper helper) {

        int[] changes = new int[1];
        FluidStorageCoFH tank = new FluidStorageCoFH(1000);
        SimpleFluidHandler handler = new SimpleFluidHandler(new IStorageCallback() {
            @Override
            public void onTankChanged(int index) {
                ++changes[0];
            }
        }, List.of(tank));
        FluidResource water = FluidResource.of(Fluids.WATER);

        try (Transaction tx = Transaction.openRoot()) {
            helper.assertValueEqual(handler.insert(0, water, 600, tx), 600, "filled");
        }
        helper.assertValueEqual(tank.getAmount(), 0, "amount after abort");
        helper.assertValueEqual(changes[0], 0, "change callbacks after abort");

        try (Transaction tx = Transaction.openRoot()) {
            handler.insert(0, water, 600, tx);
            helper.assertValueEqual(handler.insert(0, water, 600, tx), 400, "fill over capacity");
            tx.commit();
        }
        helper.assertValueEqual(tank.getAmount(), 1000, "amount after commit");
        helper.assertValueEqual(changes[0], 1, "change callbacks after commit");

        try (Transaction tx = Transaction.openRoot()) {
            helper.assertValueEqual(tank.extract(0, water, 250, tx), 250, "drained from tank");
            tx.commit();
        }
        helper.assertValueEqual(tank.getAmount(), 750, "amount after drain");
        helper.succeed();
    }

}
