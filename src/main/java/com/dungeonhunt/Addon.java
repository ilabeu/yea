package com.stash.hunt.modules;

import com.stash.hunt.Addon;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.ChunkDataEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BlockListSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.*;

import java.util.*;

public class BlockStateDetector extends Module
{
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<Block>> blacklist = sgGeneral.add(new BlockListSetting.Builder()
        .name("blacklist")
        .description("The blocks not to scan.")
        .defaultValue(List.of(Blocks.GLOW_LICHEN, Blocks.LAVA, Blocks.WATER, Blocks.KELP, Blocks.KELP_PLANT, Blocks.REDSTONE_ORE, Blocks.OAK_FENCE, Blocks.CHEST, Blocks.TALL_SEAGRASS,
            Blocks.ACACIA_LEAVES, Blocks.BIRCH_LEAVES, Blocks.CHERRY_LEAVES, Blocks.JUNGLE_LEAVES, Blocks.AZALEA_LEAVES, Blocks.DARK_OAK_LEAVES, Blocks.SPRUCE_LEAVES, Blocks.MANGROVE_LEAVES,
            Blocks.OAK_LEAVES, Blocks.AMETHYST_CLUSTER, Blocks.SMALL_AMETHYST_BUD, Blocks.MEDIUM_AMETHYST_BUD, Blocks.LARGE_AMETHYST_BUD, Blocks.TORCH, Blocks.WALL_TORCH, Blocks.VINE,
            Blocks.CAVE_VINES, Blocks.CAVE_VINES_PLANT, Blocks.RAIL, Blocks.BIG_DRIPLEAF, Blocks.BIG_DRIPLEAF_STEM, Blocks.SMALL_DRIPLEAF, Blocks.POINTED_DRIPSTONE, Blocks.TALL_GRASS,
            Blocks.STONE_BRICK_STAIRS, Blocks.MUSHROOM_STEM, Blocks.BROWN_MUSHROOM_BLOCK, Blocks.RED_MUSHROOM_BLOCK, Blocks.FLOWERING_AZALEA_LEAVES, Blocks.BEE_NEST, Blocks.OAK_STAIRS,
            Blocks.SCULK_VEIN, Blocks.OAK_LOG, Blocks.SNOW, Blocks.GRASS_BLOCK, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILE_SLAB, Blocks.DARK_OAK_FENCE, Blocks.SCULK_SHRIEKER,
            Blocks.SOUL_LANTERN, Blocks.DEEPSLATE_BRICK_STAIRS, Blocks.DEEPSLATE_TILE_WALL, Blocks.DARK_OAK_LOG, Blocks.POLISHED_DEEPSLATE_WALL, Blocks.LADDER, Blocks.DEEPSLATE_REDSTONE_ORE,
            Blocks.NETHER_PORTAL, Blocks.BONE_BLOCK, Blocks.IRON_BARS, Blocks.STONE_BRICK_SLAB, Blocks.TUFF_BRICK_STAIRS, Blocks.FIRE))
        .build()
    );

    private final Setting<Integer> scanBelowY = sgGeneral.add(new IntSetting.Builder()
        .name("scan-below-y")
        .defaultValue(320)
        .description("Will only check below this y value")
        .build()
    );


    public BlockStateDetector()
    {
        super(Addon.CATEGORY, "block-state-detector", "Detects changes in block states from the default state.");
    }

    private final HashMap<ChunkPos, Set<BlockPos>> blockHighlights = new HashMap<>();


    @Override
    public void onActivate()
    {
        blockHighlights.clear();
    }

    @EventHandler
    private void onChunkData(ChunkDataEvent event)
    {
        BlockPos chunkStart = event.chunk().getPos().getStartPos();
        for (int x = 0; x < 16; x++)
        {
            for (int z = 0; z < 16; z++)
            {
                for (int y = -64; y < scanBelowY.get(); y++)
                {
                    BlockPos pos = (new BlockPos(x, y, z)).add(chunkStart);
                    BlockState state = mc.world.getBlockState(pos);
                    if (blacklist.get().contains(state.getBlock())) continue;
                    if (!state.getBlock().getDefaultState().equals(state))
                    {
                        synchronized (blockHighlights)
                        {
                            Set<BlockPos> blocks = blockHighlights.getOrDefault(event.chunk().getPos(), new HashSet<>());
                            blocks.add(pos);
                            blockHighlights.put(event.chunk().getPos(), blocks);
                        }
                    }
                }
            }
        }
    }


    @EventHandler
    private void onRender(Render3DEvent event)
    {
        synchronized (blockHighlights)
        {
            Iterator<Map.Entry<ChunkPos, Set<BlockPos>>> it = blockHighlights.entrySet().iterator();
            while (it.hasNext())
            {
                Map.Entry<ChunkPos, Set<BlockPos>> entry = it.next();
                int renderDistance = Utils.getRenderDistance() + 3;
                if (entry.getKey().getSquaredDistance(mc.player.getChunkPos()) > renderDistance * renderDistance)
                {
                    it.remove();
                    continue;
                }
                for (BlockPos pos : entry.getValue())
                {
                    render(
                        new Box(
                            new Vec3d(pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1),
                            new Vec3d(pos.getX(), pos.getY(), pos.getZ())
                        ),
                        new SettingColor(0, 0, 255, 50),
                        new SettingColor(0, 0, 255, 255),
                        ShapeMode.Both,
                        event
                    );
                }
            }
        }
    }

    private void render(Box box, Color sides, Color lines, ShapeMode shapeMode, Render3DEvent event) {
        event.renderer.box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, sides, lines, shapeMode, 0);
    }
}

