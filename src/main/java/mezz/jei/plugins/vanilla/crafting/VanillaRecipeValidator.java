package mezz.jei.plugins.vanilla.crafting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipe;
import net.minecraft.item.crafting.ICraftingRecipe;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.RecipeManager;
import net.minecraft.util.ResourceLocation;

import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.util.ErrorUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class VanillaRecipeValidator {
	private static final Logger LOGGER = LogManager.getLogger();

	public static class Results {
		private final List<IRecipe> craftingRecipes = new ArrayList<>();
		private final List<FurnaceRecipe> furnaceRecipes = new ArrayList<>();

		public List<IRecipe> getCraftingRecipes() {
			return craftingRecipes;
		}

		public List<FurnaceRecipe> getFurnaceRecipes() {
			return furnaceRecipes;
		}
	}

	private VanillaRecipeValidator() {
	}

	public static Results getValidRecipes(IRecipeCategory<ICraftingRecipe> craftingCategory, IRecipeCategory<FurnaceRecipe> furnaceCategory) {
		CategoryRecipeValidator<ICraftingRecipe> craftingRecipesValidator = new CategoryRecipeValidator<>(craftingCategory, 9);
		CategoryRecipeValidator<FurnaceRecipe> furnaceRecipesValidator = new CategoryRecipeValidator<>(furnaceCategory, 1);

		Results results = new Results();
		ClientWorld world = Minecraft.getInstance().world;
		RecipeManager recipeManager = world.getRecipeManager();
		for (ICraftingRecipe recipe : getRecipes(recipeManager, IRecipeType.CRAFTING)) {
			if (craftingRecipesValidator.isRecipeValid(recipe)) {
				results.craftingRecipes.add(recipe);
			}
		}
		for (FurnaceRecipe recipe : getRecipes(recipeManager, IRecipeType.SMELTING)) {
			if (furnaceRecipesValidator.isRecipeValid(recipe)) {
				results.furnaceRecipes.add(recipe);
			}
		}
		// TODO other recipe types: BLASTING, SMOKING, CAMPFIRE_COOKING, STONECUTTING
		return results;
	}

	private static <C extends IInventory, T extends IRecipe<C>> Collection<T> getRecipes(RecipeManager recipeManager, IRecipeType<T> recipeType) {
		Map<ResourceLocation, IRecipe<C>> recipesMap = recipeManager.getRecipes(recipeType);
		//noinspection unchecked
		return (Collection<T>) recipesMap.values();
	}

	private static final class CategoryRecipeValidator<T extends IRecipe<?>> {
		private static final int INVALID_COUNT = -1;
		private final IRecipeCategory<T> recipeCategory;
		private final int maxInputs;

		public CategoryRecipeValidator(IRecipeCategory<T> recipeCategory, int maxInputs) {
			this.recipeCategory = recipeCategory;
			this.maxInputs = maxInputs;
		}

		@SuppressWarnings("ConstantConditions")
		public boolean isRecipeValid(T recipe) {
			if (recipe.isDynamic()) {
				return false;
			}
			ItemStack recipeOutput = recipe.getRecipeOutput();
			if (recipeOutput == null || recipeOutput.isEmpty()) {
				String recipeInfo = getInfo(recipe);
				LOGGER.error("Recipe has no output. {}", recipeInfo);
				return false;
			}
			List<Ingredient> ingredients = recipe.getIngredients();
			if (ingredients == null) {
				String recipeInfo = getInfo(recipe);
				LOGGER.error("Recipe has no input Ingredients. {}", recipeInfo);
				return false;
			}
			int inputCount = getInputCount(ingredients);
			if (inputCount == INVALID_COUNT) {
				return false;
			} else if (inputCount > maxInputs) {
				String recipeInfo = getInfo(recipe);
				LOGGER.error("Recipe has too many inputs. {}", recipeInfo);
				return false;
			} else if (inputCount == 0) {
				String recipeInfo = getInfo(recipe);
				LOGGER.error("Recipe has no inputs. {}", recipeInfo);
				return false;
			}
			return true;
		}

		private String getInfo(T recipe) {
			return ErrorUtil.getInfoFromRecipe(recipe, recipeCategory);
		}

		@SuppressWarnings("ConstantConditions")
		protected static int getInputCount(List<Ingredient> ingredientList) {
			int inputCount = 0;
			for (Ingredient ingredient : ingredientList) {
				ItemStack[] input = ingredient.getMatchingStacks();
				if (input == null) {
					return INVALID_COUNT;
				} else {
					inputCount++;
				}
			}
			return inputCount;
		}
	}
}
