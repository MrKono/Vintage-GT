package gtclassic.recipe;

import java.util.Iterator;
import java.util.Map;

import gtclassic.material.GTMaterial;
import gtclassic.material.GTMaterialGen;
import gtclassic.util.recipe.GTMultiInputRecipeList;
import ic2.api.classic.recipe.ClassicRecipes;
import ic2.api.classic.recipe.machine.IMachineRecipeList;
import ic2.api.recipe.IRecipeInput;
import ic2.core.block.machine.low.TileEntityExtractor;
import ic2.core.block.machine.low.TileEntityMacerator;
import ic2.core.item.recipe.entry.RecipeInputItemStack;
import ic2.core.item.recipe.entry.RecipeInputOreDict;
import ic2.core.platform.registry.Ic2Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraftforge.fluids.FluidRegistry;

public class GTRecipeProcessing {

	static IMachineRecipeList smelting = ClassicRecipes.furnace;
	public static final GTMultiInputRecipeList BYPRODUCT_LIST = new GTMultiInputRecipeList("gt.byproducts");
	public static final GTMultiInputRecipeList INTERACTION_LIST = new GTMultiInputRecipeList("gt.interaction");

	public static void recipesProcessing() {
		/*
		 * Recipes specific to GT Classic blocks
		 */
		maceratorUtil("oreBauxite", 1, GTMaterialGen.getDust(GTMaterial.Bauxite, 4));
		maceratorUtil("oreIridium", 1, GTMaterialGen.getIc2(Ic2Items.iridiumOre, 2));

		
		TileEntityExtractor.addRecipe("oreRuby", 1, GTMaterialGen.getGem(GTMaterial.Ruby, 3), 0.1F);
		TileEntityExtractor.addRecipe("oreSapphire", 1, GTMaterialGen.getGem(GTMaterial.Sapphire, 3), 0.1F);

		ClassicRecipes.fluidGenerator.addEntry(FluidRegistry.getFluid("sodium"), 3800, 8);
		ClassicRecipes.fluidGenerator.addEntry(FluidRegistry.getFluid("lithium"), 3800, 8);
		ClassicRecipes.fluidGenerator.addEntry(FluidRegistry.getFluid("hydrogen"), 950, 16);
		ClassicRecipes.fluidGenerator.addEntry(FluidRegistry.getFluid("methane"), 3000, 16);
	}

	/*
	 * Adds a macerator recipe while removing duplicates generated by ic2c
	 */
	public static void maceratorUtil(String input, int amount, ItemStack output) {
		TileEntityMacerator.oreBlacklist.add(input);
		TileEntityMacerator.addRecipe(input, amount, output);
	}

	/*
	 * removing smelting recipes code by Muramasa
	 */
	public static void removeSmelting(ItemStack resultStack) {
		ItemStack recipeResult;
		Map<ItemStack, ItemStack> recipes = FurnaceRecipes.instance().getSmeltingList();
		Iterator<ItemStack> iterator = recipes.keySet().iterator();
		while (iterator.hasNext()) {
			ItemStack tmpRecipe = iterator.next();
			recipeResult = recipes.get(tmpRecipe);
			if (ItemStack.areItemStacksEqual(resultStack, recipeResult)) {
				iterator.remove();
			}
		}
	}

	/*
	 * the 2 methods below are utilities for making recipes in all tiles extended
	 * off this class
	 */
	public static IRecipeInput input(ItemStack stack) {
		return new RecipeInputItemStack(stack);
	}

	public static IRecipeInput input(String name, int amount) {
		return new RecipeInputOreDict(name, amount);
	}
}