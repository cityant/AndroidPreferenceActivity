/*
 * AndroidPreferenceActivity Copyright 2014 Michael Rapp
 *
 * This program is free software: you can redistribute it and/or modify 
 * it under the terms of the GNU Lesser General Public License as published 
 * by the Free Software Foundation, either version 3 of the License, or 
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU 
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public 
 * License along with this program. If not, see
 * <http://www.gnu.org/licenses/>. 
 */
package de.mrapp.android.preference;

import java.util.Collection;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentBreadCrumbs;
import android.app.FragmentTransaction;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import de.mrapp.android.preference.adapter.PreferenceHeaderAdapter;
import de.mrapp.android.preference.fragment.FragmentListener;
import de.mrapp.android.preference.fragment.PreferenceHeaderFragment;
import de.mrapp.android.preference.parser.PreferenceHeaderParser;
import static de.mrapp.android.preference.util.Condition.ensureGreaterThan;
import static de.mrapp.android.preference.util.Condition.ensureAtLeast;
import static de.mrapp.android.preference.util.DisplayUtil.convertDpToPixels;
import static de.mrapp.android.preference.util.DisplayUtil.convertPixelsToDp;

/**
 * An activity, which provides a navigation for multiple groups of preferences,
 * in which each group is represented by an instance of the class
 * {@link PreferenceHeader}. On devices with small screens, e.g. on smartphones,
 * the navigation is designed to use the whole available space and selecting an
 * item causes the corresponding preferences to be shown full screen as well. On
 * devices with large screens, e.g. on tablets, the navigation and the
 * preferences of the currently selected item are shown split screen.
 * 
 * @author Michael Rapp
 *
 * @since 1.0.0
 */
public abstract class PreferenceActivity extends Activity implements
		FragmentListener, OnItemClickListener {

	/**
	 * The name of the extra, which is used to save the class name of the
	 * fragment, which is currently shown, within a bundle.
	 */
	private static final String CURRENT_FRAGMENT_EXTRA = PreferenceActivity.class
			.getSimpleName() + "::CurrentFragment";

	/**
	 * The name of the extra, which is used to save the index of the currently
	 * selected preference header, within a bundle.
	 */
	private static final String SELECTED_PREFERENCE_HEADER_EXTRA = PreferenceActivity.class
			.getSimpleName() + "::SelectedPreferenceHeader";

	/**
	 * The fragment, which contains the preference headers and provides the
	 * navigation to each header's fragment.
	 */
	private PreferenceHeaderFragment preferenceHeaderFragment;

	/**
	 * The parent view of the fragment, which provides the navigation to each
	 * preference header's fragment.
	 */
	private ViewGroup preferenceHeaderParentView;

	/**
	 * The parent view of the fragment, which is used to show the preferences of
	 * the currently selected preference header on devices with a large screen.
	 */
	private ViewGroup preferenceScreenParentView;

	/**
	 * The view, which is used to draw a shadow besides the navigation on
	 * devices with a large screen.
	 */
	private View shadowView;

	/**
	 * The full qualified class name of the fragment, which is currently shown
	 * or null, if no preference header is currently selected.
	 */
	private String currentFragment;

	/**
	 * True, if the back button of the action bar should be shown, false
	 * otherwise.
	 */
	private boolean displayHomeAsUp;

	/**
	 * The color of the shadow, which is drawn besides the navigation on devices
	 * with a large screen.
	 */
	private int shadowColor;

	/**
	 * The bread crumbs, which are used to show the title of the currently
	 * selected fragment.
	 */
	private FragmentBreadCrumbs fragmentBreadCrumbs;

	/**
	 * Shows the fragment, which corresponds to a specific preference header.
	 * 
	 * @param preferenceHeader
	 *            The preference header, the fragment, which should be shown,
	 *            corresponds to, as an instance of the class
	 *            {@link PreferenceHeader}. The preference header may not be
	 *            null
	 */
	private void showPreferenceScreen(final PreferenceHeader preferenceHeader) {
		currentFragment = preferenceHeader.getFragment();
		showPreferenceScreen(currentFragment);
		showBreadCrumbs(preferenceHeader);
	}

	/**
	 * Shows the fragment, which corresponds to a specific class name.
	 * 
	 * @param fragmentName
	 *            The full qualified class name of the fragment, which should be
	 *            shown, as a {@link String}
	 */
	private void showPreferenceScreen(final String fragmentName) {
		Fragment fragment = Fragment.instantiate(this, fragmentName);

		if (isSplitScreen()) {
			replaceFragment(fragment, R.id.preference_screen_parent, 0);
		} else {
			replaceFragment(fragment, R.id.preference_header_parent,
					FragmentTransaction.TRANSIT_FRAGMENT_FADE);
			showActionBarBackButton();
		}
	}

	/**
	 * Shows the fragment, which provides the navigation to each preference
	 * header's fragment.
	 */
	private void showPreferenceHeaders() {
		int transition = 0;

		if (currentFragment != null) {
			transition = FragmentTransaction.TRANSIT_FRAGMENT_CLOSE;
			currentFragment = null;
		}

		replaceFragment(preferenceHeaderFragment,
				R.id.preference_header_parent, transition);
	}

	/**
	 * Replaces the fragment, which is currently contained by a specific parent
	 * view, by an other fragment.
	 * 
	 * @param fragment
	 *            The fragment, which should replace the current fragment, as an
	 *            instance of the class {@link Fragment}. The fragment may not
	 *            be null
	 * @param parentViewId
	 *            The id of the parent view, which contains the fragment, that
	 *            should be replaced, as an {@link Integer} value
	 * @param transition
	 *            The transition, which should be shown when replacing the
	 *            fragment, as an {@link Integer} value or 0, if no transition
	 *            should be shown
	 */
	private void replaceFragment(final Fragment fragment,
			final int parentViewId, final int transition) {
		FragmentTransaction transaction = getFragmentManager()
				.beginTransaction();
		transaction.setTransition(transition);
		transaction.replace(parentViewId, fragment);
		transaction.commit();
	}

	/**
	 * Shows the back button in the activity's action bar.
	 */
	private void showActionBarBackButton() {
		if (getActionBar() != null) {
			displayHomeAsUp = isDisplayHomeAsUpEnabled();
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}

	/**
	 * Hides the back button in the activity's action bar, if it was not
	 * previously shown.
	 */
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	private void hideActionBarBackButton() {
		if (getActionBar() != null && !displayHomeAsUp) {
			getActionBar().setDisplayHomeAsUpEnabled(false);

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				getActionBar().setHomeButtonEnabled(false);
			}
		}
	}

	/**
	 * Returns, whether the back button of the action bar is currently shown, or
	 * not.
	 * 
	 * @return True, if the back button of the action bar is currently shown,
	 *         false otherwise
	 */
	private boolean isDisplayHomeAsUpEnabled() {
		if (getActionBar() != null) {
			return (getActionBar().getDisplayOptions() & ActionBar.DISPLAY_HOME_AS_UP) == ActionBar.DISPLAY_HOME_AS_UP;
		}

		return false;
	}

	/**
	 * Shows the bread crumbs for a specific preference header, depending on
	 * whether the device has a large screen or not. On devices with a large
	 * screen the bread crumbs will be shown above the currently shown fragment,
	 * on devices with a small screen the bread crumbs will be shown as the
	 * action bar's title instead.
	 * 
	 * @param preferenceHeader
	 *            The preference header, the bread crumbs should be shown for,
	 *            as an instance of the class {@link PreferenceHeader}. The
	 *            preference header may not be null
	 */
	private void showBreadCrumbs(final PreferenceHeader preferenceHeader) {
		CharSequence title = preferenceHeader.getBreadCrumbTitle();

		if (title == null) {
			title = preferenceHeader.getTitle();
		}

		if (title == null) {
			title = getTitle();
		}

		showBreadCrumbs(title, preferenceHeader.getBreadCrumbShortTitle());
	}

	/**
	 * Shows the bread crumbs using a specific title and short title, depending
	 * on whether the device has a large screen or not. On devices with a large
	 * screen the bread crumbs will be shown above the currently shown fragment,
	 * on devices with a small screen the bread crumbs will be shown as the
	 * action bar's title instead.
	 * 
	 * @param title
	 *            The title, which should be used by the bread crumbs, as an
	 *            instance of the class {@link CharSequence} or null, if no
	 *            title should be used
	 * @param shortTitle
	 *            The short title, which should be used by the bread crumbs, as
	 *            an instance of the class {@link CharSequence} or null, if no
	 *            short title should be used
	 */
	private void showBreadCrumbs(final CharSequence title,
			final CharSequence shortTitle) {
		if (fragmentBreadCrumbs == null) {
			View breadCrumbsView = findViewById(android.R.id.title);

			try {
				fragmentBreadCrumbs = (FragmentBreadCrumbs) breadCrumbsView;
			} catch (ClassCastException e) {
				return;
			}

			if (fragmentBreadCrumbs == null) {
				if (title != null) {
					setTitle(title);
				}

				return;
			}

			fragmentBreadCrumbs.setMaxVisible(2);
			fragmentBreadCrumbs.setActivity(this);
		}

		fragmentBreadCrumbs.setTitle(title, shortTitle);
		fragmentBreadCrumbs.setParentTitle(null, null, null);
	}

	/**
	 * Returns the parent view of the fragment, which provides the navigation to
	 * each preference header's fragment. On devices with a small screen this
	 * parent view is also used to show a preference header's fragment, when a
	 * header is currently selected.
	 * 
	 * @return The parent view of the fragment, which provides the navigation to
	 *         each preference header's fragment, as an instance of the class
	 *         {@link ViewGroup}. The parent view may not be null
	 */
	public final ViewGroup getPreferenceHeaderParentView() {
		return preferenceHeaderParentView;
	}

	/**
	 * Returns the parent view of the fragment, which is used to show the
	 * preferences of the currently selected preference header on devices with a
	 * large screen.
	 * 
	 * @return The parent view of the fragment, which is used to show the
	 *         preferences of the currently selected preference header, as an
	 *         instance of the class {@link ViewGroup} or null, if the device
	 *         has a small screen
	 */
	public final ViewGroup getPreferenceScreenParentView() {
		return preferenceScreenParentView;
	}

	/**
	 * Returns the view, which is used to draw a shadow besides the navigation
	 * on devices with a large screen.
	 * 
	 * @return The view, which is used to draw a shadow besides the navigation,
	 *         as an instance of the class {@link View} or null, if the device
	 *         has a small screen
	 */
	public final View getShadowView() {
		return shadowView;
	}

	/**
	 * Returns the list view, which is used to show the preference headers.
	 * 
	 * @return The list view, which is used to show the preference header, as an
	 *         instance of the class {@link ListView}. The list view may not be
	 *         null
	 */
	public final ListView getListView() {
		return preferenceHeaderFragment.getListView();
	}

	/**
	 * Returns the adapter, which provides the preference headers for
	 * visualization using the list view.
	 * 
	 * @return The adapter, which provides the preference headers for
	 *         visualization using the list view, as an instance of the class
	 *         {@link PreferenceHeaderAdapter}. The adapter may not be null
	 */
	public final PreferenceHeaderAdapter getListAdapter() {
		return preferenceHeaderFragment.getListAdapter();
	}

	/**
	 * Adds all preference headers, which are specified by a specific XML
	 * resource.
	 * 
	 * @param resourceId
	 *            The resource id of the XML file, which specifies the
	 *            preference headers, as an {@link Integer} value. The resource
	 *            id must correspond to a valid XML resource
	 */
	public final void addPreferenceHeadersFromResource(final int resourceId) {
		getListAdapter().addAllItems(
				PreferenceHeaderParser.fromResource(this, resourceId));
	}

	/**
	 * Adds a new preference header.
	 * 
	 * @param preferenceHeader
	 *            The preference header, which should be added, as an instance
	 *            of the class {@link PreferenceHeader}. The preference header
	 *            may not be null
	 * @return
	 */
	public final void addPreferenceHeader(
			final PreferenceHeader preferenceHeader) {
		getListAdapter().addItem(preferenceHeader);
	}

	/**
	 * Adds all preference headers, which are contained by a specific
	 * collection.
	 * 
	 * @param preferenceHeaders
	 *            The collection, which contains the preference headers, which
	 *            should be added, as an instance of the type {@link Collection}
	 *            or an empty collection, if no preference headers should be
	 *            added
	 */
	public final void addAllPreferenceHeaders(
			final Collection<PreferenceHeader> preferenceHeaders) {
		getListAdapter().addAllItems(preferenceHeaders);
	}

	/**
	 * Removes a specific preference header.
	 * 
	 * @param preferenceHeader
	 *            The preference header, which should be removed, as an instance
	 *            of the class {@link PreferenceHeader}. The preference header
	 *            may not be null
	 * @return True, if the preference header has been removed, false otherwise
	 */
	public final boolean removePreferenceHeader(
			final PreferenceHeader preferenceHeader) {
		return getListAdapter().removeItem(preferenceHeader);
	}

	/**
	 * Removes all preference headers.
	 */
	public final void clearPreferenceHeaders() {
		getListAdapter().clear();
	}

	/**
	 * Returns, whether the preference headers and the corresponding fragments
	 * are shown split screen, or not.
	 * 
	 * @return True, if the preference headers and the corresponding fragments
	 *         are shown split screen, false otherwise
	 */
	public final boolean isSplitScreen() {
		return getPreferenceScreenParentView() != null;
	}

	/**
	 * Returns the color of the shadow, which is drawn besides the navigation on
	 * devices with a large screen.
	 * 
	 * @return The color of the shadow, which is drawn besides the navigation,
	 *         as an {@link Integer} value or -1, if the device has a small
	 *         screen
	 */
	public final int getShadowColor() {
		if (isSplitScreen()) {
			return shadowColor;
		} else {
			return -1;
		}
	}

	/**
	 * Sets the color of the shadow, which is drawn besides the navigation on
	 * devices with a large screen. The color is only set on devices with a
	 * large screen.
	 * 
	 * @param shadowColor
	 *            The color, which should be set, as an {@link Integer} value
	 * @return True, if the color has been set, false otherwise
	 */
	@SuppressWarnings("deprecation")
	public final boolean setShadowColor(final int shadowColor) {
		if (getShadowView() != null) {
			this.shadowColor = shadowColor;
			GradientDrawable gradient = new GradientDrawable(
					Orientation.LEFT_RIGHT, new int[] { shadowColor,
							Color.TRANSPARENT });
			getShadowView().setBackgroundDrawable(gradient);
			return true;
		}

		return false;
	}

	/**
	 * Returns the width of the shadow, which is drawn besides the navigation on
	 * devices with a large screen.
	 * 
	 * @return The width of the shadow, which is drawn besides the navigation,
	 *         in dp as an {@link Integer} value or -1, if the device has a
	 *         small screen
	 */
	public final int getShadowWidth() {
		if (getShadowView() != null) {
			return convertPixelsToDp(this,
					getShadowView().getLayoutParams().width);
		} else {
			return -1;
		}
	}

	/**
	 * Sets the width of the shadow, which is drawn besides the navigation on
	 * devices with a large screen. The width is only set on devices with a
	 * large screen.
	 * 
	 * @param width
	 *            The width, which should be set, in dp as an {@link Integer}
	 *            value. The width must be at least 0
	 * @return True, if the width has been set, false otherwise
	 */
	public final boolean setShadowWidth(final int width) {
		ensureAtLeast(width, 0, "The width must be at least 0");

		if (getShadowView() != null) {
			getShadowView().getLayoutParams().width = convertDpToPixels(this,
					width);
			getShadowView().requestLayout();
			return true;
		}

		return false;
	}

	/**
	 * Returns the background of the parent view of the fragment, which provides
	 * navigation to each preference header's fragment on devices with a large
	 * screen.
	 * 
	 * @return The background of the parent view of the fragment, which provides
	 *         navigation to each preference header's fragment, as an instance
	 *         of the class {@link Drawable} or null, if no background has been
	 *         set or the device has a small screen
	 */
	public final Drawable getNavigationBackground() {
		if (isSplitScreen()) {
			return getPreferenceHeaderParentView().getBackground();
		} else {
			return null;
		}
	}

	/**
	 * Sets the background of the parent view of the fragment, which provides
	 * navigation to each preference header's fragment. The background is only
	 * set on devices with a large screen.
	 * 
	 * @param resourceId
	 *            The resource id of the background, which should be set, as an
	 *            {@link Integer} value. The resource id must correspond to a
	 *            valid drawable resource
	 * @return True, if the background has been set, false otherwise
	 */
	public final boolean setNavigationBackground(final int resourceId) {
		return setNavigationBackground(getResources().getDrawable(resourceId));
	}

	/**
	 * Sets the background color of the parent view of the fragment, which
	 * provides navigation to each preference header's fragment. The background
	 * is only set on devices with a large screen.
	 * 
	 * @param color
	 *            The background color, which should be set, as an
	 *            {@link Integer} value
	 * @return True, if the background has been set, false otherwise
	 */
	public final boolean setNavigationBackgroundColor(final int color) {
		return setNavigationBackground(new ColorDrawable(color));
	}

	/**
	 * Sets the background of the parent view of the fragment, which provides
	 * navigation to each preference header's fragment. The background is only
	 * set on devices with a large screen.
	 * 
	 * @param drawable
	 *            The background, which should be set, as an instance of the
	 *            class {@link Drawable} or null, if no background should be set
	 * @return True, if the background has been set, false otherwise
	 */
	@SuppressWarnings("deprecation")
	public final boolean setNavigationBackground(final Drawable drawable) {
		if (isSplitScreen()) {
			getPreferenceHeaderParentView().setBackgroundDrawable(drawable);
			return true;
		}

		return false;
	}

	/**
	 * Returns the width of the parent view of the fragment, which provides
	 * navigation to each preference header's fragment on devices with a large
	 * screen.
	 * 
	 * @return The width of the parent view of the fragment, which provides
	 *         navigation to each preference header's fragment, in dp as an
	 *         {@link Integer} value or -1, if the device has a small screen
	 */
	public final int getNavigationWidth() {
		if (isSplitScreen()) {
			return convertPixelsToDp(this, getPreferenceHeaderParentView()
					.getLayoutParams().width);
		} else {
			return -1;
		}
	}

	/**
	 * Sets the width of the parent view of the fragment, which provides
	 * navigation to each preference header's fragment. The width is only set on
	 * devices with a large screen.
	 * 
	 * @param width
	 *            The width, which should be set, in dp as an {@link Integer}
	 *            value. The width must be greater than 0
	 * @return True, if the width has been set, false otherwise
	 */
	public final boolean setNavigationWidth(final int width) {
		ensureGreaterThan(width, 0, "The width must be greater than 0");

		if (isSplitScreen()) {
			getPreferenceHeaderParentView().getLayoutParams().width = convertPixelsToDp(
					this, width);
			getPreferenceHeaderParentView().requestLayout();
			return true;
		}

		return false;
	}

	@Override
	public final void onFragmentCreated(final Fragment fragment) {
		onCreatePreferenceHeaders();
		getListView().setOnItemClickListener(this);

		if (isSplitScreen()) {
			getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);

			if (!getListAdapter().isEmpty()) {
				getListView().setItemChecked(0, true);
				showPreferenceScreen(getListAdapter().getItem(0));
			}
		}
	}

	@Override
	public final void onItemClick(final AdapterView<?> parent, final View view,
			final int position, final long id) {
		showPreferenceScreen(getListAdapter().getItem(position));
	}

	@Override
	public final boolean onKeyDown(final int keyCode, final KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && !isSplitScreen()
				&& currentFragment != null) {
			showPreferenceHeaders();
			return true;
		}

		return super.onKeyDown(keyCode, event);
	}

	@Override
	public final boolean onOptionsItemSelected(final MenuItem item) {
		if (item.getItemId() == android.R.id.home && !isSplitScreen()
				&& currentFragment != null) {
			showPreferenceHeaders();
			hideActionBarBackButton();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.preference_activity);
		preferenceHeaderParentView = (ViewGroup) findViewById(R.id.preference_header_parent);
		preferenceScreenParentView = (ViewGroup) findViewById(R.id.preference_screen_parent);
		shadowView = findViewById(R.id.shadow_view);
		preferenceHeaderFragment = new PreferenceHeaderFragment();
		preferenceHeaderFragment.addFragmentListener(this);
		showPreferenceHeaders();
		setShadowColor(getResources().getColor(R.color.shadow));
	}

	@Override
	protected final void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(CURRENT_FRAGMENT_EXTRA, currentFragment);
		outState.putInt(SELECTED_PREFERENCE_HEADER_EXTRA, getListView()
				.getCheckedItemPosition());
	}

	@Override
	protected final void onRestoreInstanceState(final Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		currentFragment = savedInstanceState.getString(CURRENT_FRAGMENT_EXTRA);
		int selectedPreferenceHeader = savedInstanceState
				.getInt(SELECTED_PREFERENCE_HEADER_EXTRA);

		if (currentFragment != null) {
			showPreferenceScreen(currentFragment);
		}

		if (selectedPreferenceHeader != ListView.INVALID_POSITION) {
			getListView().setItemChecked(selectedPreferenceHeader, true);
		}
	}

	/**
	 * The method, which is invoked, when the preference headers should be
	 * created. This method has to be overridden by implementing subclasses to
	 * add the preference headers.
	 */
	protected abstract void onCreatePreferenceHeaders();

}