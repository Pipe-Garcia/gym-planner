package com.gymplanner.shared.blocks;

/**
 * Describes the training-day role of a block. BlockPurpose.WARMUP is a warmup block;
 * SetKind.WARMUP is only a warmup set inside an exercise.
 */
public enum BlockPurpose {
    WARMUP,
    ACTIVATION,
    MAIN_LIFT,
    ACCESSORY,
    CONDITIONING,
    CORE,
    COOLDOWN,
    OTHER
}
