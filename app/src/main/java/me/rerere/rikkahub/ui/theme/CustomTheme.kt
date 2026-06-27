package me.rerere.rikkahub.ui.theme

import androidx.compose.material3.ColorScheme
import com.materialkolor.hct.Hct
import com.materialkolor.palettes.TonalPalette
import com.materialkolor.scheme.DynamicScheme
import com.materialkolor.scheme.SchemeTonalSpot
import kotlinx.serialization.Serializable
import me.rerere.material3.toColorScheme
import kotlin.uuid.Uuid

@Serializable
data class CustomTheme(
    val id: String = Uuid.random().toString(),
    val name: String = "",
    val primaryColorArgb: Long = 0xFF6750A4,
    val secondaryColorArgb: Long? = null,
    val tertiaryColorArgb: Long? = null,
) {
    fun generateColorScheme(dark: Boolean): ColorScheme {
        val sourceHct = Hct.fromInt(primaryColorArgb.toInt())
        val contrastLevel = 0.0
        val baseScheme = SchemeTonalSpot(
            sourceColorHct = sourceHct,
            isDark = dark,
            contrastLevel = contrastLevel,
        )
        val scheme = DynamicScheme(
            sourceColorHct = sourceHct,
            variant = baseScheme.variant,
            isDark = dark,
            contrastLevel = contrastLevel,
            primaryPalette = baseScheme.primaryPalette,
            secondaryPalette = secondaryColorArgb?.let { TonalPalette.fromInt(it.toInt()) }
                ?: baseScheme.secondaryPalette,
            tertiaryPalette = tertiaryColorArgb?.let { TonalPalette.fromInt(it.toInt()) }
                ?: baseScheme.tertiaryPalette,
            neutralPalette = baseScheme.neutralPalette,
            neutralVariantPalette = baseScheme.neutralVariantPalette,
            platform = baseScheme.platform,
            specVersion = baseScheme.specVersion,
            errorPalette = baseScheme.errorPalette,
        )
        return scheme.toColorScheme()
    }
}
