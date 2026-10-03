package cofh.core.gametest;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import static cofh.lib.util.constants.ModIds.ID_COFH_CORE;

@EventBusSubscriber (modid = ID_COFH_CORE)
public class CoreGameTests {

    private static final Map<String, Consumer<GameTestHelper>> TESTS = new LinkedHashMap<>();

    static {
        TESTS.put("energy_storage_transactions", StorageTests::energyStorageTransactions);
        TESTS.put("item_handler_transactions", StorageTests::itemHandlerTransactions);
        TESTS.put("managed_item_handler_rules", StorageTests::managedItemHandlerRules);
        TESTS.put("shared_slot_journal", StorageTests::sharedSlotJournal);
        TESTS.put("fluid_handler_transactions", StorageTests::fluidHandlerTransactions);
        TESTS.put("recipes_sync", RecipeTests::recipesSync);
    }

    @SubscribeEvent
    public static void registerFunctions(RegisterEvent event) {

        event.register(Registries.TEST_FUNCTION, helper -> TESTS.forEach((name, test) -> helper.register(id(name), test)));
    }

    @SubscribeEvent
    public static void registerTests(RegisterGameTestsEvent event) {

        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id("default"));
        TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(environment, id("empty"), 20, 0, true);
        TESTS.keySet().forEach(name -> event.registerTest(id(name), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id(name)), data)));
    }

    private static Identifier id(String path) {

        return Identifier.fromNamespaceAndPath(ID_COFH_CORE, path);
    }

}
