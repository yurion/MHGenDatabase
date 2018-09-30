package com.ghstudios.android.data.classes

import com.ghstudios.android.ITintedIcon
import com.ghstudios.android.mhgendatabase.R

/**
 * Defines an armor family, which is internally a collection of armor.
 * This is the base form, which defines the existance of an armor family.
 * Use the base class for more information.
 */
open class ArmorFamilyBase: ITintedIcon {
    var id: Long = -1
    var name:String? = ""
    var rarity = 1

    override fun getIconResourceString() = "armor_body"
    override fun getColorArrayId() = R.array.rare_colors
    override fun getIconColorIndex() = rarity - 1
}

/**
 * An armor family, which is the result of equipping an entire set of armor.
 */
class ArmorFamily: ArmorFamilyBase() {
    var minDef = 0
    var maxDef = 0
    val skills : MutableList<String> = mutableListOf()
}