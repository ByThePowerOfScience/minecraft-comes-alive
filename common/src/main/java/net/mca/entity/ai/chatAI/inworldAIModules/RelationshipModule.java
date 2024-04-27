package net.mca.entity.ai.chatAI.inworldAIModules;

import net.mca.entity.VillagerEntityMCA;
import net.mca.entity.ai.Relationship;
import net.mca.entity.ai.chatAI.inworldAIModules.api.Interaction;
import net.mca.entity.ai.chatAI.inworldAIModules.api.TriggerEvent;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Class to manage relationship updates.
 * Creates Triggers to inform AI of current heart level + status
 * Works relationship update from responses back to heart level
 */
public class RelationshipModule {

    /**
     * Updates the villager's heart level according to the latest interaction RelationshipUpdate
     * @param interaction The new interaction
     * @param player The player in this relationship
     * @param villager The villager whose heart level is getting updated
     */
    public void updateRelationship(Interaction interaction, ServerPlayerEntity player, VillagerEntityMCA villager) {
        Interaction.RelationshipUpdate update = interaction.relationshipUpdate();

        // Get total, with different weights applied to different relationship values
        // TODO: Some **serious*** balancing
        int weightedTotal = 1 * update.trust()
                + 1 * update.respect()
                + 1 * update.familiar()
                + 1 * update.flirtatious()
                + 1 * update.attraction();

        int heartsUpdate = weightedTotal / 10;

        villager.getVillagerBrain().getMemoriesForPlayer(player).modHearts(heartsUpdate);
    }

    /**
     * This method generates a TriggerEvent for the relationship status between a player and a villager.
     * The TriggerEvent contains a single parameter, "status", which is the name of the current relationship status.
     *
     * @param player The player involved in the relationship.
     * @param villager The villager involved in the relationship.
     * @return A TriggerEvent representing the current relationship status.
     */
    public TriggerEvent getRelationshipTrigger(ServerPlayerEntity player, VillagerEntityMCA villager) {
        TriggerEvent.Parameter[] params = {
                new TriggerEvent.Parameter("status", getRelationshipStatus(player, villager).name())};
        return new TriggerEvent("relationship-trigger", params);
    }

    /**
     * Gets the relationship status between the player and villager in terms
     * Depends on if married/engaged/promised or the heart level
     * @param player The player of the relationship
     * @param villager The villager of the relationship
     * @return {@link RelationshipStatus}
     */
    private RelationshipStatus getRelationshipStatus(ServerPlayerEntity player, VillagerEntityMCA villager) {
        if (Relationship.IS_ENGAGED.test(villager, player) || Relationship.IS_MARRIED.test(villager, player)) {
            return RelationshipStatus.LIFE_PARTNER;
        }
        if (Relationship.IS_PROMISED.test(villager, player)) {
            return RelationshipStatus.RELATIONSHIP; // Is the bouquet for confirming a Date or a Relationship? For now, it's a relationship
        }
        int heartLevel = villager.getVillagerBrain().getMemoriesForPlayer(player).getHearts();

        RelationshipStatus returnStatus = RelationshipStatus.UNKNOWN;
        for (RelationshipStatus status : RelationshipStatus.values()) {
            if (status.hasStatus(heartLevel)) {
                returnStatus = status;
            }
        }
        return returnStatus;
    }

    /**
     * Enum of all possible relationship values.
     * Each status has a heart threshold, which is the minimum heart value needed for the relationship to be at that status
     */
    private enum RelationshipStatus {
        UNKNOWN(Integer.MIN_VALUE),
        ARCHENEMY(Integer.MIN_VALUE),
        ENEMY(-75),
        ACQUAINTANCE(-15),
        FRIEND(25),
        CLOSE_FRIEND(100),
        DATE(Integer.MAX_VALUE),
        RELATIONSHIP(Integer.MAX_VALUE),
        LIFE_PARTNER(Integer.MAX_VALUE);

        private final int heartThreshold;

        RelationshipStatus(int heartThreshold) {
            this.heartThreshold = heartThreshold;
        }

        public boolean hasStatus(int hearts) {
            return hearts > heartThreshold;
        }
    }
}
