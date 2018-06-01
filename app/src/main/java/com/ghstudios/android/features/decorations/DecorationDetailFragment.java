package com.ghstudios.android.features.decorations;

import android.arch.lifecycle.ViewModelProviders;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.ghstudios.android.AppSettings;
import com.ghstudios.android.ClickListeners.ItemClickListener;
import com.ghstudios.android.ClickListeners.SkillClickListener;
import com.ghstudios.android.MHUtils;
import com.ghstudios.android.components.ColumnLabelTextCell;
import com.ghstudios.android.components.IconLabelTextCell;
import com.ghstudios.android.components.ItemRecipeCell;
import com.ghstudios.android.components.TitleBarCell;
import com.ghstudios.android.data.classes.Component;
import com.ghstudios.android.data.classes.Decoration;
import com.ghstudios.android.data.classes.Item;
import com.ghstudios.android.features.wishlist.WishlistDataAddDialogFragment;
import com.ghstudios.android.mhgendatabase.R;

import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DecorationDetailFragment extends Fragment {
    private static final String ARG_DECORATION_ID = "DECORATION_ID";

    private static final String DIALOG_WISHLIST_ADD = "wishlist_add";

    @BindView(R.id.titlebar) TitleBarCell titleView;
    @BindView(R.id.rare) ColumnLabelTextCell rareView;
    @BindView(R.id.buy) ColumnLabelTextCell buyView;
    @BindView(R.id.sell) ColumnLabelTextCell sellView;
    @BindView(R.id.slots) ColumnLabelTextCell slotsReqView;
    @BindView(R.id.skill_list) LinearLayout skillListView;
    @BindView(R.id.recipe_list) LinearLayout recipeListView;

    // stored to allow add to wishlist to work
    long decorationId;
    String decorationName;

    public static DecorationDetailFragment newInstance(long decorationId) {
        Bundle args = new Bundle();
        args.putLong(ARG_DECORATION_ID, decorationId);
        DecorationDetailFragment f = new DecorationDetailFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        // Check for a Item ID as an argument, and find the item
        Bundle args = getArguments();
        if (args == null) {
            return;
        }

        decorationId = args.getLong(ARG_DECORATION_ID, -1);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_decoration_detail,
                container, false);

        ButterKnife.bind(this, view);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (decorationId == -1) {
            return;
        }

        DecorationViewModel viewModel = ViewModelProviders.of(this).get(DecorationViewModel.class);
        viewModel.setDecoration(decorationId);

        viewModel.getDecorationData().observe(this, this::populateDecoration);
        viewModel.getComponentData().observe(this, this::populateRecipes);
    }

    /**
     * Updates the UI to set the decoration data.
     * @param decoration
     */
    private void populateDecoration(Decoration decoration) {
        decorationId = decoration.getId();
        decorationName = decoration.getName();

        String cellImage = "icons_items/" + decoration.getFileLocation();
        String cellRare = "" + decoration.getRarity();
        String cellBuy = "" + decoration.getBuy() + "z";
        String cellSell = "" + decoration.getSell() + "z";
        String cellSlotsReq = "" + decoration.getSlotsString();

        if (cellBuy.equals("0z")) {
            cellBuy = "-";
        }
        if (cellSell.equals("0z")) {
            cellSell = "-";
        }

        Drawable image = MHUtils.loadAssetDrawable(getContext(), cellImage);

        titleView.setIconDrawable(image);
        titleView.setTitleText(decorationName);
        titleView.setAltTitleText(decoration.getJpnName());
        titleView.setAltTitleEnabled(AppSettings.isJapaneseEnabled());

        rareView.setValueText(cellRare);
        buyView.setValueText(cellBuy);
        sellView.setValueText(cellSell);
        slotsReqView.setValueText(cellSlotsReq);

        skillListView.removeAllViews();

        addSkillListItem(decoration.getSkill1Id(), decoration.getSkill1Name(), decoration.getSkill1Point());
        if (decoration.getSkill2Point() != 0) {
            addSkillListItem(decoration.getSkill2Id(), decoration.getSkill2Name(), decoration.getSkill2Point());
        }
    }

    private void populateRecipes(Map<String, List<Component>> recipes) {
        recipeListView.removeAllViews();

        for (List<Component> recipe : recipes.values()) {
            ItemRecipeCell cell = new ItemRecipeCell(getContext());
            cell.setTitleText(recipe.get(0).getType());

            for (Component component : recipe) {
                Item item = component.getComponent();
                Drawable itemIcon = MHUtils.loadAssetDrawable(getContext(), item.getItemImage());

                View itemCell = cell.addItem(itemIcon, item.getName(), component.getQuantity());
                itemCell.setOnClickListener(new ItemClickListener(getContext(), item));
            }

            recipeListView.addView(cell);
        }
    }

    private void addSkillListItem(long skillId, String skillName, int points) {
        IconLabelTextCell skillItem = new IconLabelTextCell(getContext());
        skillItem.setLabelText(skillName);
        skillItem.setValueText(String.valueOf(points));
        skillItem.setOnClickListener(new SkillClickListener(getContext(), skillId));

        skillListView.addView(skillItem);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_add_to_wishlist, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add_to_wishlist:
                FragmentManager fm = this.getFragmentManager();
                WishlistDataAddDialogFragment dialogCopy = WishlistDataAddDialogFragment
                        .newInstance(decorationId, decorationName);
                dialogCopy.show(fm, DIALOG_WISHLIST_ADD);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
