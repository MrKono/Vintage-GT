package gtclassic.common.tile.datanet;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import gtclassic.api.helpers.GTUtility;
import gtclassic.api.interfaces.IGTDataNetObject;
import gtclassic.api.interfaces.IGTDebuggableTile;
import gtclassic.common.GTBlocks;
import gtclassic.common.container.GTContainerComputerCube;
import gtclassic.common.util.GTIBlockFilters;
import ic2.core.RotationList;
import ic2.core.block.base.tile.TileEntityElecMachine;
import ic2.core.inventory.base.IHasGui;
import ic2.core.inventory.container.ContainerIC2;
import ic2.core.inventory.gui.GuiComponentContainer;
import ic2.core.inventory.management.InventoryHandler;
import ic2.core.util.helpers.AabbUtil;
import ic2.core.util.helpers.AabbUtil.BoundingBox;
import ic2.core.util.helpers.AabbUtil.Processor;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class GTTileComputerCube extends TileEntityElecMachine
		implements IHasGui, IGTDebuggableTile, ITickable, IGTDataNetObject {

	private boolean isOnlyComputer = true;
	private Processor task = null;
	private static final AabbUtil.IBlockFilter DATA_FILTER = new GTIBlockFilters.DataNetFilter();
	private Set<BlockPos> dataNet = new HashSet<>();
	private int nodeCount = 0;
	private int energyCost = 0;

	public GTTileComputerCube() {
		super(0, 2048);
		this.maxEnergy = 10000;
		this.addGuiFields(new String[] { "nodeCount", "energyCost" });
	}

	@Override
	public void onUnloaded() {
		if (this.isSimulating() && this.hasDataNetwork()) {
			for (BlockPos nodePos : dataNet) {
				TileEntity tile = world.getTileEntity(nodePos);
				if (tile instanceof GTTileBaseDataNode) {
					((GTTileBaseDataNode) tile).setComputer(null);
				}
			}
		}
		super.onUnloaded();
	}

	@Override
	@SideOnly(Side.CLIENT)
	public Class<? extends GuiScreen> getGuiClass(EntityPlayer player) {
		return GuiComponentContainer.class;
	}

	@Override
	public ContainerIC2 getGuiContainer(EntityPlayer player) {
		return new GTContainerComputerCube(player.inventory, this);
	}

	@Override
	protected void addSlots(InventoryHandler handler) {
		// nothing for now
	}

	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return !this.isInvalid();
	}

	@Override
	public void onGuiClosed(EntityPlayer entityPlayer) {
		// Empty because its unused but required by interface implementation
	}

	@Override
	public boolean hasGui(EntityPlayer player) {
		return true;
	}

	@Override
	public boolean canRemoveBlock(EntityPlayer player) {
		return true;
	}

	@Override
	public boolean canSetFacing(EntityPlayer player, EnumFacing facing) {
		return facing != getFacing() && facing.getAxis().isHorizontal();
	}

	@Override
	public boolean supportsNotify() {
		return true;
	}

	@Override
	public double getWrenchDropRate() {
		return 1.0D;
	}

	public void tryUseEnergy() {
		if (this.energyCost > 0) {
			this.useEnergy(this.energyCost);
		}
	}

	@Override
	public void update() {
		this.setActive(this.isOnlyComputer && this.energy > 0);
		if (world.getTotalWorldTime() % GTUtility.DATA_NET_RESET_RATE == 0) {
			this.isOnlyComputer = true;
			this.dataNet.clear();
		}
		tryUseEnergy();
		if (this.isOnlyComputer) {
			if (task != null && world.isAreaLoaded(pos, 16)) {
				task.update();
				if (!task.isFinished()) {
					return;
				}
				onTaskComplete();
			}
			if (world.getTotalWorldTime() % GTUtility.DATA_NET_SEARCH_RATE == 0) {
				if (!world.isAreaLoaded(pos, 16))
					return;
				task = AabbUtil.createBatchTask(world, new BoundingBox(this.pos, 256), this.pos, RotationList.ALL, DATA_FILTER, 64, false, false, true);
				task.update();
			}
		}
	}

	/**
	 * Called when a task is completed, parses the task results to the data network
	 * filtering out cables, simultaneity updates tiles on the newly parsed network.
	 */
	private void onTaskComplete() {
		this.nodeCount = 0;
		this.energyCost = 0;
		for (BlockPos resultPos : task.getResults()) {
			if (!world.isBlockLoaded(resultPos) || world.getBlockState(resultPos).getBlock() == GTBlocks.dataCable) {
				continue;
			}
			TileEntity tile = world.getTileEntity(resultPos);
			if (tile != this && tile instanceof GTTileComputerCube) {
				((GTTileComputerCube) tile).disableComputer();
			}
			if (tile instanceof IGTDataNetObject) {
				this.energyCost = this.energyCost + ((IGTDataNetObject) tile).getCost();
			}
			if (tile instanceof GTTileBaseDataNode) {
				((GTTileBaseDataNode) tile).setComputer(this.energy > 0 ? this : null);
				this.dataNet.add(resultPos);
				this.nodeCount++;
			}
		}
		this.updateGui();
	}

	/**
	 * Called when multiple computers conflict on a single network.
	 */
	public void disableComputer() {
		this.isOnlyComputer = false;
		this.setActive(false);
	}

	/**
	 * Get the amount of nodes on this computers network.
	 * 
	 * @return int - the amount of nodes.
	 */
	public int getNodeCount() {
		return this.nodeCount;
	}

	/**
	 * Get the current energy cost of maintaining the current network
	 * 
	 * @return int - the cost, returns 0 if no network.
	 */
	public int getEnergyCost() {
		return this.energyCost;
	}

	/**
	 * Checks if the data network on this computer is null or empty.
	 * 
	 * @return true if data networks exists and has at least one entry.
	 */
	public boolean hasDataNetwork() {
		return this.dataNet != null && !this.dataNet.isEmpty();
	}

	/**
	 * Getter for the network connected to this computer
	 * 
	 * @return an unmodifiable BlockPos Set of each node's block position.
	 */
	public Set<BlockPos> getDataNetwork() {
		return Collections.unmodifiableSet(this.dataNet);
	}

	public void updateGui() {
		this.getNetwork().updateTileGuiField(this, "nodeCount");
		this.getNetwork().updateTileGuiField(this, "energyCost");
	}

	@Override
	public int getCost() {
		return 0;
	}

	@Override
	public void getData(Map<String, Boolean> data) {
		data.put("Nodes in network: " + this.getNodeCount(), false);
	}
}
