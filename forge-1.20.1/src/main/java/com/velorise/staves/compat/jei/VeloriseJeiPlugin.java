package com.velorise.staves.compat.jei;

import com.velorise.staves.VeloriseStavesMod;
import com.velorise.staves.client.ClientAuraRenderer;
import com.velorise.staves.element.Element;
import com.velorise.staves.item.SwordImbueHelper;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
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
    private static final ResourceLocation UID = new ResourceLocation(VeloriseStavesMod.MODID, "jei_plugin");

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
        for (Item item : net.minecraftforge.registries.ForgeRegistries.ITEMS) {
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
            anvilRecipes.add(factory.createAnvilRecipe(left, allElements, outputs));
        }

        // 2. Anvil Element Repair examples
        anvilRecipes.add(createRepairExample(factory, VeloriseStavesMod.INFERNO_ROD.get(), VeloriseStavesMod.PYRO_ELEMENT.get()));
        anvilRecipes.add(createRepairExample(factory, VeloriseStavesMod.ARC_CASTER.get(), VeloriseStavesMod.OMNI_ELEMENT.get()));
        registration.addRecipes(RecipeTypes.ANVIL, anvilRecipes);

        // 3. Omni Staff Smithing upgrades are native minecraft:smithing_transform recipes in datapack JSON.

        // 4. Elemental Essence Info tabs in JEI
        for (Element e : Element.values()) {
            registration.addIngredientInfo(
                    new ItemStack(VeloriseStavesMod.getElementItem(e)),
                    VanillaTypes.ITEM_STACK,
                    Component.translatable("jei.velorise_staves.element_info.base")
            );
        }
        registration.addIngredientInfo(
                new ItemStack(VeloriseStavesMod.OMNI_ELEMENT.get()),
                VanillaTypes.ITEM_STACK,
                Component.translatable("jei.velorise_staves.element_info.omni")
        );
    }

    private static IJeiAnvilRecipe createRepairExample(IVanillaRecipeFactory factory, Item staff, Item repairItem) {
        ItemStack damaged = new ItemStack(staff);
        damaged.setDamageValue(Math.max(1, damaged.getMaxDamage() / 4));
        return factory.createAnvilRecipe(
                damaged,
                List.of(new ItemStack(repairItem)),
                List.of(new ItemStack(staff))
        );
    }
}
