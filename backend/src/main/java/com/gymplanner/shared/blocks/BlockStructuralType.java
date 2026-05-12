package com.gymplanner.shared.blocks;

/**
 * Defines how exercises inside a block are executed. This is different from BlockPurpose,
 * which describes why the block exists in the training day.
 */
public enum BlockStructuralType {
    STANDARD,
    CIRCUIT,
    PYRAMID,
    REVERSE_PYRAMID,
    DROP_SET,
    REST_PAUSE,
    CLUSTER
}
