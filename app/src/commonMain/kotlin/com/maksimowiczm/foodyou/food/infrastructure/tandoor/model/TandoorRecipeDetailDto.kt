package com.maksimowiczm.foodyou.food.infrastructure.tandoor.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

@Serializable
internal data class TandoorRecipeDetailDto(
    val id: Int,
    val name: String,
    val description: String? = null,
    val servings: Int = 1,
    @SerialName("servings_text") val servingsText: String? = null,
    @SerialName("source_url") val sourceUrl: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    val steps: List<TandoorStepDto> = emptyList(),
)

@Serializable
internal data class TandoorStepDto(
    val id: Int,
    val instruction: String = "",
    val ingredients: List<TandoorIngredientDto> = emptyList(),
)

@Serializable
internal data class TandoorIngredientDto(
    val id: Int,
    val amount: Double = 0.0,
    @Serializable(with = NullableFlexibleUnitSerializer::class)
    val unit: TandoorUnitDto? = null,
    val food: TandoorFoodDto,
    val note: String? = null,
    @SerialName("no_amount") val noAmount: Boolean = false,
    val conversions: List<TandoorConversionDto> = emptyList(),
)

@Serializable
internal data class TandoorUnitDto(
    val id: Int = 0,
    val name: String = "",
    @SerialName("base_unit") val baseUnit: String? = null,
)

@Serializable
internal data class TandoorFoodDto(
    val id: Int,
    val name: String,
    val properties: List<TandoorFoodPropertyDto> = emptyList(),
    @SerialName("properties_food_amount") val propertiesFoodAmount: Double = 100.0,
    @Serializable(with = NullableFlexibleUnitSerializer::class)
    @SerialName("properties_food_unit") val propertiesFoodUnit: TandoorUnitDto? = null,
)

@Serializable
internal data class TandoorFoodPropertyDto(
    @SerialName("property_type") val propertyType: TandoorPropertyTypeDto? = null,
    @SerialName("property_amount") val propertyAmount: Double = 0.0,
)

@Serializable
internal data class TandoorPropertyTypeDto(
    val id: Int = 0,
    val name: String = "",
    val unit: String = "",
    @SerialName("open_data_slug") val openDataSlug: String = "",
)

/**
 * Tandoor API is inconsistent about unit fields: sometimes a full object
 * `{"id":1,"name":"g","base_unit":"g"}`, sometimes a bare string `"g"`, sometimes null.
 * This serializer normalises all three forms into [TandoorUnitDto]?.
 */
internal object NullableFlexibleUnitSerializer : KSerializer<TandoorUnitDto?> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("NullableFlexibleUnit")

    override fun serialize(encoder: Encoder, value: TandoorUnitDto?) {
        if (value == null) {
            encoder.encodeNull()
        } else {
            encoder.encodeSerializableValue(TandoorUnitDto.serializer(), value)
        }
    }

    override fun deserialize(decoder: Decoder): TandoorUnitDto? {
        val json = (decoder as JsonDecoder).decodeJsonElement()
        return when {
            json is JsonNull -> null
            json is JsonPrimitive && json.isString ->
                TandoorUnitDto(name = json.content, baseUnit = json.content)
            json is JsonObject ->
                decoder.json.decodeFromJsonElement(TandoorUnitDto.serializer(), json)
            else -> null
        }
    }
}

/** Non-nullable variant used for [TandoorConversionDto.unit]. */
internal object FlexibleUnitSerializer : KSerializer<TandoorUnitDto> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("FlexibleUnit")

    override fun serialize(encoder: Encoder, value: TandoorUnitDto) {
        encoder.encodeSerializableValue(TandoorUnitDto.serializer(), value)
    }

    override fun deserialize(decoder: Decoder): TandoorUnitDto {
        val json = (decoder as JsonDecoder).decodeJsonElement()
        return when {
            json is JsonPrimitive && json.isString ->
                TandoorUnitDto(name = json.content, baseUnit = json.content)
            json is JsonObject ->
                decoder.json.decodeFromJsonElement(TandoorUnitDto.serializer(), json)
            else -> TandoorUnitDto()
        }
    }
}

@Serializable
internal data class TandoorConversionDto(
    val amount: Double = 0.0,
    @Serializable(with = FlexibleUnitSerializer::class)
    val unit: TandoorUnitDto = TandoorUnitDto(),
)
