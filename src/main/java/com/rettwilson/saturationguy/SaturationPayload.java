package com.rettwilson.saturationguy;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server to client: the player's exact current saturation. */
public record SaturationPayload(float saturation) implements CustomPacketPayload {
	public static final Type<SaturationPayload> TYPE =
			new Type<>(Identifier.fromNamespaceAndPath(SaturationGuy.MOD_ID, "saturation"));
	public static final StreamCodec<RegistryFriendlyByteBuf, SaturationPayload> CODEC =
			ByteBufCodecs.FLOAT.map(SaturationPayload::new, SaturationPayload::saturation).cast();

	@Override
	public Type<SaturationPayload> type() {
		return TYPE;
	}
}
