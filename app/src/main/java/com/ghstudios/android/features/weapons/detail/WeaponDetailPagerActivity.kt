package com.ghstudios.android.features.weapons.detail

import android.arch.lifecycle.ViewModelProviders
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import com.ghstudios.android.AssetLoader

import com.ghstudios.android.data.classes.Weapon
import com.ghstudios.android.data.DataManager
import com.ghstudios.android.mhgendatabase.R
import com.ghstudios.android.features.wishlist.external.WishlistDataAddDialogFragment
import com.ghstudios.android.BasePagerActivity
import com.ghstudios.android.MenuSection

class WeaponDetailPagerActivity : BasePagerActivity() {
    companion object {
        /**
         * A key for passing a weapon ID as a long
         */
        const val EXTRA_WEAPON_ID = "com.daviancorp.android.android.ui.detail.weapon_id"

        private const val DIALOG_WISHLIST_ADD = "wishlist_add"
    }

    internal var weaponId: Long = 0
    internal var name: String? = null

    val viewModel by lazy {
        ViewModelProviders.of(this).get(WeaponDetailViewModel::class.java)
    }

    override fun onAddTabs(tabs: BasePagerActivity.TabAdder) {
        weaponId = intent.getLongExtra(EXTRA_WEAPON_ID, -1)

        // This shouldn't ever be null, but no clue what to do on failure.
        val w = viewModel.loadWeapon(weaponId)!!
        name = w.name

        val wtype = w.wtype!!

        // Set activity title to display weapon type
        title = AssetLoader.localizeWeaponType(wtype)

        // All weapons have a detail tab
        tabs.addTab(R.string.weapon_detail_tab_detail) {
            getDetailForWeaponType(wtype, weaponId)
        }

        // Certain weapon types may have a different second tab
        if (wtype == Weapon.HUNTING_HORN) {
            tabs.addTab(R.string.weapon_detail_tab_melodies) {
                WeaponSongFragment.newInstance(weaponId)
            }
        } else if (wtype == Weapon.LIGHT_BOWGUN || wtype == Weapon.HEAVY_BOWGUN) {
            tabs.addTab(R.string.weapon_detail_tab_ammo) {
                WeaponDetailAmmoFragment.newInstance(weaponId)
            }
        } else if (wtype == Weapon.BOW) {
            tabs.addTab(R.string.weapon_detail_tab_coatings) {
                WeaponDetailCoatingFragment.newInstance(weaponId)
            }
        }

        // All weapons have a family tab
        tabs.addTab(R.string.weapon_detail_tab_family) {
            WeaponTreeFragment.newInstance(weaponId)
        }
    }

    /**
     * Helper that gets the "Detail tab contents" for the weapon type
     * @param wtype
     * @return
     */
    private fun getDetailForWeaponType(wtype: String, weaponId: Long): Fragment {
        when (wtype) {
            Weapon.LIGHT_BOWGUN, Weapon.HEAVY_BOWGUN ->
                return WeaponBowgunDetailFragment.newInstance(weaponId)
            Weapon.BOW -> return WeaponBowDetailFragment.newInstance(weaponId)
            else -> return WeaponBladeDetailFragment.newInstance(weaponId)
        }
    }

    override fun getSelectedSection(): Int {
        return MenuSection.WEAPONS
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_add_to_wishlist, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add_to_wishlist -> {
                val fm = supportFragmentManager
                val dialogCopy = WishlistDataAddDialogFragment
                        .newInstance(weaponId, name!!)
                dialogCopy.show(fm, DIALOG_WISHLIST_ADD)
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }
}
