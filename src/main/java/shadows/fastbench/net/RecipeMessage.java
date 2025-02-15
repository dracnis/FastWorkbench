package shadows.fastbench.net;

import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.inventory.InventoryScreen;
import net.minecraft.inventory.CraftResultInventory;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkEvent.Context;
import shadows.fastbench.api.ICraftingContainer;
import shadows.fastbench.api.ICraftingScreen;
import shadows.placebo.util.NetworkUtils;
import shadows.placebo.util.NetworkUtils.MessageProvider;

public class RecipeMessage extends MessageProvider<RecipeMessage> {

	public static final ResourceLocation NULL = new ResourceLocation("null", "null");

	ResourceLocation recipeId;
	ItemStack output;

	public RecipeMessage() {
	}

	public RecipeMessage(IRecipe<CraftingInventory> recipeId, ItemStack output) {
		this.recipeId = recipeId == null ? NULL : recipeId.getId();
		this.output = output;
	}

	public RecipeMessage(ResourceLocation recipeId, ItemStack output) {
		this.recipeId = recipeId;
		this.output = output;
	}

	@Override
	public Class<RecipeMessage> getMsgClass() {
		return RecipeMessage.class;
	}

	@Override
	public RecipeMessage read(PacketBuffer buf) {
		ResourceLocation rec = new ResourceLocation(buf.readUtf());
		return new RecipeMessage(rec, rec.equals(NULL) ? ItemStack.EMPTY : buf.readItem());
	}

	@Override
	public void write(RecipeMessage msg, PacketBuffer buf) {
		buf.writeUtf(msg.recipeId.toString());
		if (!msg.recipeId.equals(NULL)) buf.writeItem(msg.output);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void handle(RecipeMessage msg, Supplier<Context> ctx) {
		NetworkUtils.handlePacket(() -> () -> {
			IRecipe<CraftingInventory> recipe = (IRecipe<CraftingInventory>) Minecraft.getInstance().level.getRecipeManager().byKey(msg.recipeId).orElse(null);
			if (Minecraft.getInstance().screen instanceof ICraftingScreen) {
				ICraftingContainer c = ((ICraftingScreen) Minecraft.getInstance().screen).getContainer();
				updateLastRecipe(c.getResult(), recipe, msg.output);
			} else if (Minecraft.getInstance().screen instanceof InventoryScreen) {
				PlayerContainer c = ((InventoryScreen) Minecraft.getInstance().screen).getMenu();
				updateLastRecipe(c.resultSlots, recipe, msg.output);
			}
		}, ctx.get());
	}

	public static void updateLastRecipe(CraftResultInventory craftResult, IRecipe<CraftingInventory> rec, ItemStack output) {
		craftResult.setRecipeUsed(rec);
		craftResult.setItem(0, output);
	}

}
