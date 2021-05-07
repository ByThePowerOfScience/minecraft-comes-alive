package cobalt.minecraft.world;

import cobalt.core.Cobalt;
import cobalt.minecraft.entity.CEntity;
import cobalt.minecraft.entity.player.CPlayer;
import cobalt.minecraft.util.math.CPos;
import cobalt.minecraft.world.storage.CWorldSavedData;
import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.DimensionSavedDataManager;
import net.minecraft.world.storage.WorldSavedData;

import java.util.Optional;
import java.util.Random;
import java.util.UUID;

public class CWorld {
    @Getter
    private final World mcWorld;

    public final boolean isRemote;
    public final Random rand;

    //TODO
    public Iterable<? extends Entity> loadedEntityList;

    private CWorld(World world) {
        this.mcWorld = world;
        this.isRemote = !world.isClientSide;
        this.rand = world.random;
    }

    public static CWorld fromMC(World world) {
        return new CWorld(world);
    }

    public CWorldSavedData loadData(Class<? extends WorldSavedData> clazz, String dataId) {
        DimensionSavedDataManager dm = ((ServerWorld) mcWorld).getDataStorage();
        return dm.computeIfAbsent(() -> {
            try {
                return (CWorldSavedData) clazz.getDeclaredConstructor(String.class).newInstance(dataId);
            } catch (Exception e) {
                Cobalt.getLog().info(e);
                return null;
            }
        }, dataId);
    }

    public void setData(String dataId, WorldSavedData data) {
        DimensionSavedDataManager dm = ((ServerWorld) mcWorld).getDataStorage();
        dm.set(data);
    }

    public Optional<CPlayer> getPlayerEntityByUUID(UUID uuid) {
        PlayerEntity player = mcWorld.getPlayerByUUID(uuid);
        if (player != null) {
            return Optional.of(CPlayer.fromMC(player));
        } else {
            return Optional.empty();
        }
    }

    public Optional<CEntity> getEntityByUUID(UUID uuid) {
        return Optional.of(CEntity.fromMC(((ServerWorld) mcWorld).getEntity(uuid)));
    }

    public Biome getBiome(CPos pos) {
        return mcWorld.getBiome(pos.getMcPos());
    }

    public void spawnEntity(CEntity entity) {
        ((MobEntity) entity.getMcEntity()).finalizeSpawn((IServerWorld) mcWorld, mcWorld.getCurrentDifficultyAt(entity.getPosition().getMcPos()), SpawnReason.NATURAL, null, null);
        mcWorld.addFreshEntity(entity.getMcEntity());
    }
}
