package com.velorise.staves.compat.jei;

import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.client.ClientAuraRenderer;
import com.velorise.staves.element.Element;
import com.velorise.staves.item.SwordImbueHelper;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.recipe.vanilla.IJeiAnvilRecipe;
import mezz.jei.api.recipe.vanilla.IVanillaRecipeFactory;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public final class VeloriseJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGenericGuiContainerHandler(EffectRenderingInventoryScreen.class, new IGuiContainerHandler<EffectRenderingInventoryScreen<?>>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(EffectRenderingInventoryScreen<?> containerScreen) {
                return ClientAuraRenderer.getInventoryExtraAreas(containerScreen);
            }
        });
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        IVanillaRecipeFactory factory = registration.getVanillaRecipeFactory();
        List<ItemStack> allElements = List.of(
                new ItemStack(VeloriseStavesMod.PYRO_ELEMENT.get()),
                new ItemStack(VeloriseStavesMod.HYDRO_ELEMENT.get()),
                new ItemStack(VeloriseStavesMod.ANEMO_ELEMENT.get()),
                new ItemStack(VeloriseStavesMod.ELECTRO_ELEMENT.get()),
                new ItemStack(VeloriseStavesMod.DENDRO_ELEMENT.get()),
                new ItemStack(VeloriseStavesMod.CRYO_ELEMENT.get()),
                new ItemStack(VeloriseStavesMod.GEO_ELEMENT.get()),
                new ItemStack(VeloriseStavesMod.OMNI_ELEMENT.get())
        );
        List<String> keys = List.of("PYRO", "HYDRO", "ANEMO", "ELECTRO", "DENDRO", "CRYO", "GEO", SwordImbueHelper.OMNI_KEY);

        // 1. Anvil Sword Imbue previews (all Vanilla & Modded SwordItems)
        List<IJeiAnvilRecipe> anvilRecipes = new ArrayList<>();
        List<Item> allSwords = new ArrayList<>(List.of(
                Items.NETHERITE_SWORD, Items.DIAMOND_SWORD, Items.IRON_SWORD,
                Items.GOLDEN_SWORD, Items.STONE_SWORD, Items.WOODEN_SWORD
        ));
        for (Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            if (item instanceof net.minecraft.world.item.SwordItem && !allSwords.contains(item)) {
                allSwords.add(item);
            }
        }
        for (Item sword : allSwords) {
            ItemStack left = new ItemStack(sword);
            List<ItemStack> outputs = new ArrayList<>(keys.size());
            for (String key : keys) {
                outputs.add(SwordImbueHelper.createImbuedCopy(left, key));
            }
            ResourceLocation itemKey = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(sword);
            String safeId = itemKey.getNamespace() + "_" + itemKey.getPath();
            anvilRecipes.add(factory.createAnvilRecipe(
                    left, allElements, outputs,
                    ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, "jei_imbue_" + safeId)
            ));
        }

        // 2. Anvil Element & Netherite Repair for all 15 Basic + 15 Netherite Staves
        Item[][] staffRepairPairs = new Item[][]{
                {VeloriseStavesMod.INFERNO_ROD.get(), VeloriseStavesMod.NETHERITE_INFERNO_ROD.get(), VeloriseStavesMod.PYRO_ELEMENT.get()},
                {VeloriseStavesMod.GHASTFIRE_STAFF.get(), VeloriseStavesMod.NETHERITE_GHASTFIRE_STAFF.get(), VeloriseStavesMod.PYRO_ELEMENT.get()},
                {VeloriseStavesMod.GUARDIAN_LENS.get(), VeloriseStavesMod.NETHERITE_GUARDIAN_LENS.get(), VeloriseStavesMod.HYDRO_ELEMENT.get()},
                {VeloriseStavesMod.SPLENDOR_OF_TRANQUIL_WATERS.get(), VeloriseStavesMod.NETHERITE_SPLENDOR_OF_TRANQUIL_WATERS.get(), VeloriseStavesMod.HYDRO_ELEMENT.get()},
                {VeloriseStavesMod.FROSTBOLT_WAND.get(), VeloriseStavesMod.NETHERITE_FROSTBOLT_WAND.get(), VeloriseStavesMod.CRYO_ELEMENT.get()},
                {VeloriseStavesMod.NIGHTS_FOCUS_STAFF.get(), VeloriseStavesMod.NETHERITE_NIGHTS_FOCUS_STAFF.get(), VeloriseStavesMod.CRYO_ELEMENT.get()},
                {VeloriseStavesMod.ARC_CASTER.get(), VeloriseStavesMod.NETHERITE_ARC_CASTER.get(), VeloriseStavesMod.ELECTRO_ELEMENT.get()},
                {VeloriseStavesMod.DRAGONFIRE_SCEPTER.get(), VeloriseStavesMod.NETHERITE_DRAGONFIRE_SCEPTER.get(), VeloriseStavesMod.ELECTRO_ELEMENT.get()},
                {VeloriseStavesMod.SHULKER_SEEKER.get(), VeloriseStavesMod.NETHERITE_SHULKER_SEEKER.get(), VeloriseStavesMod.ELECTRO_ELEMENT.get()},
                {VeloriseStavesMod.WITHER_SCEPTER.get(), VeloriseStavesMod.NETHERITE_WITHER_SCEPTER.get(), VeloriseStavesMod.DENDRO_ELEMENT.get()},
                {VeloriseStavesMod.WEBCASTER.get(), VeloriseStavesMod.NETHERITE_WEBCASTER.get(), VeloriseStavesMod.DENDRO_ELEMENT.get()},
                {VeloriseStavesMod.WIND_STAFF.get(), VeloriseStavesMod.NETHERITE_WIND_STAFF.get(), VeloriseStavesMod.ANEMO_ELEMENT.get()},
                {VeloriseStavesMod.RESONANT_ECHO.get(), VeloriseStavesMod.NETHERITE_RESONANT_ECHO.get(), VeloriseStavesMod.ANEMO_ELEMENT.get()},
                {VeloriseStavesMod.EVOKER_FANG_STAFF.get(), VeloriseStavesMod.NETHERITE_EVOKER_FANG_STAFF.get(), VeloriseStavesMod.GEO_ELEMENT.get()},
                {VeloriseStavesMod.VORTEX_VANQUISHER.get(), VeloriseStavesMod.NETHERITE_VORTEX_VANQUISHER.get(), VeloriseStavesMod.GEO_ELEMENT.get()},
                {VeloriseStavesMod.OMNI_STAFF.get(), VeloriseStavesMod.NETHERITE_OMNI_STAFF.get(), VeloriseStavesMod.OMNI_ELEMENT.get()}
        };
        for (Item[] pair : staffRepairPairs) {
            Item basicStaff = pair[0];
            Item netheriteStaff = pair[1];
            Item elemEssence = pair[2];
            String basePath = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(basicStaff).getPath();
            String nethPath = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(netheriteStaff).getPath();

            anvilRecipes.add(createRepairExample(factory, basicStaff, elemEssence, "jei_repair_elem_" + basePath));
            anvilRecipes.add(createRepairExample(factory, netheriteStaff, elemEssence, "jei_repair_elem_" + nethPath));
            if (elemEssence != VeloriseStavesMod.OMNI_ELEMENT.get()) {
                anvilRecipes.add(createRepairExample(factory, basicStaff, VeloriseStavesMod.OMNI_ELEMENT.get(), "jei_repair_omni_" + basePath));
                anvilRecipes.add(createRepairExample(factory, netheriteStaff, VeloriseStavesMod.OMNI_ELEMENT.get(), "jei_repair_omni_" + nethPath));
            }
            anvilRecipes.add(createRepairExample(factory, netheriteStaff, Items.NETHERITE_INGOT, "jei_repair_netherite_" + nethPath));
        }
        registration.addRecipes(RecipeTypes.ANVIL, anvilRecipes);

        // 3. Omni Staff Smithing upgrades are native minecraft:smithing_transform recipes in datapack JSON.

        // 4. Elemental Essence Info tabs in JEI
        for (Element e : Element.values()) {
            registration.addIngredientInfo(
                    VeloriseStavesMod.getElementItem(e),
                    Component.translatable("jei.velorise_staves.element_info.base")
            );
        }
        registration.addIngredientInfo(
                VeloriseStavesMod.OMNI_ELEMENT.get(),
                Component.translatable("jei.velorise_staves.element_info.omni")
        );
    }

    private static IJeiAnvilRecipe createRepairExample(IVanillaRecipeFactory factory, Item staff, Item repairItem, String id) {
        ItemStack damaged = new ItemStack(staff);
        damaged.setDamageValue(Math.max(1, damaged.getMaxDamage() / 4));
        return factory.createAnvilRecipe(
                damaged,
                List.of(new ItemStack(repairItem)),
                List.of(new ItemStack(staff)),
                ResourceLocation.fromNamespaceAndPath(VeloriseStavesMod.MODID, id)
        );
    }
}
