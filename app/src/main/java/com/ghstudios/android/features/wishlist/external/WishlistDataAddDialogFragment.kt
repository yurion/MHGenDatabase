package com.ghstudios.android.features.wishlist.external

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import com.ghstudios.android.data.classes.Wishlist

import com.ghstudios.android.data.database.DataManager
import com.ghstudios.android.mhgendatabase.R
import com.ghstudios.android.util.applyArguments
import com.ghstudios.android.util.first
import com.ghstudios.android.util.toList

enum class WishlistItemType {
    ITEM,
    ARMORSET
}

/**
 * A dialog created to decide which wishlist to add an item to.
 */
class WishlistDataAddDialogFragment : DialogFragment() {
    companion object {
        private const val ARG_WISHLIST_TYPE = "WISHLIST_DATA_TYPE"
        private const val ARG_WISHLIST_DATA_ID = "WISHLIST_DATA_ID"
        private const val ARG_WISHLIST_DATA_WEAPON_NAME = "WISHLIST_DATA_WEAPON_NAME"

        /**
         * Creates a new instance of this dialog to decide which wishlist to add an item to.
         * This constructor is for just items
         * @param id
         * @param name
         */
        @JvmStatic fun newInstance(id: Long, name: String)
                = newInstance(WishlistItemType.ITEM, id, name)

        @JvmStatic fun newInstance(type: WishlistItemType, id: Long, name: String): WishlistDataAddDialogFragment {
            return WishlistDataAddDialogFragment().applyArguments {
                putSerializable(ARG_WISHLIST_TYPE, type)
                putLong(ARG_WISHLIST_DATA_ID, id)
                putString(ARG_WISHLIST_DATA_WEAPON_NAME, name) // currently unused?
            }
        }
    }

    private val TAG = javaClass.simpleName

    // todo: move to viewmodel? Do DialogFragments use ViewModels?
    private lateinit var itemType: WishlistItemType
    private var itemId: Long = -1

    private lateinit var wishlists: List<Wishlist>
    private lateinit var paths: List<String>


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        itemType = arguments?.getSerializable(ARG_WISHLIST_TYPE) as WishlistItemType
        itemId = arguments?.getLong(ARG_WISHLIST_DATA_ID) ?: -1

        // validation, show error in console
        if (itemId < 0) {
            Log.e(TAG, "Item added to wishlist is -1, this means there was an error")
            // todo: show a different error dialog message instead
        }

        val dataManager = DataManager.get()

        wishlists = dataManager.queryWishlists().toList { it.wishlist }
        paths = when (itemType) {
            WishlistItemType.ITEM -> dataManager.queryComponentCreateImprove(itemId)
            WishlistItemType.ARMORSET -> listOf("Create") // todo: localize
        }

        return showSelectWishlistDialog()
    }

    private fun showSelectWishlistDialog(): Dialog {
        val inflater = LayoutInflater.from(this.context)
        val dialogView = inflater.inflate(R.layout.dialog_wishlist_data_add, null)
        val wishlistSelect = dialogView.findViewById<Spinner>(R.id.wishlist_select)
        val wishlistNameEntry = dialogView.findViewById<EditText>(R.id.wishlist_name)
        val pathSelect = dialogView.findViewById<RadioGroup>(R.id.path_select)
        val quantityInput = dialogView.findViewById<EditText>(R.id.add)

        if (wishlists.isEmpty()) {
            // There's no wishlist, so ask the user to enter one
            wishlistNameEntry.visibility = View.VISIBLE
        } else {
            // Bind selectable wishlists
            wishlistSelect.visibility = View.VISIBLE
            wishlistSelect.adapter = ArrayAdapter(
                    this.context,
                    R.layout.support_simple_spinner_dropdown_item,
                    wishlists.map { it.name }.toTypedArray())
        }


        // If there are any "path" items, add them to the path selection area
        // Also select the first one
        if (paths.isNotEmpty()) {
            pathSelect.removeAllViews()
            for (path in paths) {
                pathSelect.addView(RadioButton(context).apply {
                    this.text = path
                    tag = path
                })
            }

            pathSelect.check(pathSelect.getChildAt(0).id)
        }

        // Determine the title for the dialog
        val title = when (itemType) {
            WishlistItemType.ITEM -> getString(R.string.wishlist_add_title)
            WishlistItemType.ARMORSET -> getString(R.string.wishlist_add_set_title)
        }

        // Create the dialog
        val dialog = AlertDialog.Builder(activity)
                .setTitle(title)
                .setView(dialogView)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    // do nothing here, since we want to be able to show errors and cancel closing
                }
                .create()

        // Handles the "ok" input option. We put it here so that we can validate w/o closing the dialog.
        dialog.setOnShowListener {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                try {
                    // Initial pass - validation. Perform before doing anything
                    if (wishlists.isEmpty() && wishlistNameEntry.text.isBlank()) {
                        Toast.makeText(activity, R.string.wishlist_error_name_required, Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    // Validate quantity
                    val quantity = quantityInput.text.toString().toIntOrNull()
                    if (quantity == null) {
                        Toast.makeText(activity, R.string.wishlist_error_quantity_required, Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    if (quantity < 0 || quantity > 99) {
                        Toast.makeText(activity, R.string.wishlist_error_quantity_invalid, Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    // Pull the selected crafting path (Create/Upgrade/Create B/etc)
                    val pathItem = pathSelect.findViewById<RadioButton>(pathSelect.checkedRadioButtonId)
                    val path = pathItem?.tag as? String ?: "Create"

                    // if wishlists are empty, create a new wishlist...and then use that one
                    // otherwise pull the wishlist via its idx
                    val wishlist = when (wishlists.isEmpty()) {
                        true -> {
                            val dataManager = DataManager.get()
                            val wishlistName = wishlistNameEntry.text.toString().trim()
                            val newWishlistId = dataManager.queryAddWishlist(wishlistName)
                            dataManager.queryWishlist(newWishlistId).first { it.wishlist }
                        }
                        false -> {
                            wishlists[wishlistSelect.selectedItemPosition]
                        }
                    }

                    // Delegate result
                    handleResult(wishlist, path, quantity)

                    // Show success message.
                    val message = getString(R.string.wishlist_add_success, wishlist.name)
                    Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()

                    // We're done, exit
                    dialog.dismiss()

                } catch (ex: Exception) {
                    Toast.makeText(activity, R.string.wishlist_error_unknown, Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "ERROR While adding item to wishlist", ex)
                }
            }
        }

        return dialog
    }

    /**
     * Helper that performs the actual logic of adding the item to the selected wishlist and path.
     * The data is assumed to be successfully validated
     * todo: move to something else to handle this sort of business logic.
     */
    private fun handleResult(wishlist: Wishlist, path: String, quantity: Int) {
        val dataManager = DataManager.get()

        // Add to wishlist
        when (itemType) {
            WishlistItemType.ITEM ->
                dataManager.queryAddWishlistData(wishlist.id, itemId, quantity, path)
            WishlistItemType.ARMORSET ->
                dataManager.queryAddWishlistArmorFamily(wishlist.id, itemId, quantity, path)
        }
    }
}
