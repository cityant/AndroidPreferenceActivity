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
	 * When starting this activity, the invoking Intent can contain this extra
	 * string to specify which fragment should be initially displayed.
	 */
	public static final String EXTRA_SHOW_FRAGMENT = ":android:show_fragment";

	/**
	 * When starting this activity and using <code>EXTRA_SHOW_FRAGMENT</code>,
	 * this extra can also be specified to supply a bundle of arguments to pass
	 * to that fragment when it is instantiated during the initial creation of
	 * the activity.
	 */
	public static final String EXTRA_SHOW_FRAGMENT_ARGUMENTS = ":android:show_fragment_args";

	/**
	 * When starting this activity and using <code>EXTRA_SHOW_FRAGMENT</code>,
	 * this extra can also be specify to supply the title to be shown for that
	 * fragment.
	 */
	public static final String EXTRA_SHOW_FRAGMENT_TITLE = ":android:show_fragment_title";

	/**
	 * When starting this activity and using <code>EXTRA_SHOW_FRAGMENT</code>,
	 * this extra can also be specify to supply the short title to be shown for
	 * that fragment.
	 */
	public static final String EXTRA_SHOW_FRAGMENT_SHORT_TITLE = ":android:show_fragment_short_title";

	/**
	 * When starting this activity, the invoking intent can contain this extra
	 * boolean that the header list should not be displayed. This is most often
	 * used in conjunction with <code>EXTRA_SHOW_FRAGMENT</code> to launch the
	 * activity to display a specific fragment that the user has navigated to.
	 */
	public static final String EXTRA_NO_HEADERS = ":android:no_headers";

	/**
	 * The name of the extra, which is used to save the class name of the
	 * fragment, which is currently shown, within a bundle.
	 */
	private static final String CURRENT_FRAGMENT_EXTRA = PreferenceActivity.class
			.getSimpleName() + "::CurrentFragment";

	/**
	 * The name of the extra, which is used to save the parameters, which have
	 * been passed when the currently shown fragment has been created, within a
	 * bundle.
	 */
	private static final String CURRENT_BUNDLE_EXTRA = PreferenceActivity.class
			.getSimpleName() + "::CurrentBundle";

	/**
	 * The name of the extra, which is used to save the title, which is
	 * currently used by the bread crumbs, within a bundle.
	 */
	private static final String CURRENT_TITLE_EXTRA = PreferenceActivity.class
			.getSimpleName() + "::CurrentTitle";

	/**
	 * The name of the extra, which is used to save the short title, which is
	 * currently used by the bread crumbs, within a bundle.
	 */
	private static final String CURRENT_SHORT_TITLE_EXTRA = PreferenceActivity.class
			.getSimpleName() + "::CurrentShortTitle";

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
	 * The view group, which contains all views, e.g. the preferences itself and
	 * the bread crumbs, which are shown when a preference header is selected on
	 * devices with a large screen.
	 */
	private ViewGroup preferenceScreenContainer;

	/**
	 * The view, which is used to draw a separator between the bread crumbs and
	 * the preferences on devices with a large screen.
	 */
	private View breadCrumbsSeperator;

	/**
	 * The view, which is used to draw a shadow besides the navigation on
	 * devices with a large screen.
	 */
	private View shadowView;

	/**
	 * The preference header, which is currently selected, or null, if no
	 * preference header is currently selected.
	 */
	private PreferenceHeader currentPreferenceHeader;

	/**
	 * The title, which is currently used by the bread crumbs or null, if no
	 * bread crumbs are currently shown.
	 */
	private CharSequence currentTitle;

	/**
	 * The short title, which is currently used by the bread crumbs or null, if
	 * no bread crumbs are currently shown.
	 */
	private CharSequence currentShortTitle;

	/**
	 * True, if the back button of the action bar should be shown, false
	 * otherwise.
	 */
	private boolean displayHomeAsUp;

	/**
	 * True, if the behavior of the action bar's back button is overridden to
	 * return to the navigation when a preference header is currently selected
	 * on devices with a small screen.
	 */
	private boolean overrideBackButton;

	/**
	 * True, if the fragment, which provides navigation to each preference
	 * header's fragment on devices with a large screen, is currently hidden or
	 * not.
	 */
	private boolean navigationHidden;

	/**
	 * The color of the separator, which is drawn between the bread crumbs and
	 * the preferences on devices with a large screen.
	 */
	private int breadCrumbsSeparatorColor;

	/**
	 * The color of the shadow, which is drawn besides the navigation on devices
	 * with a large screen.
	 */
	private int shadowColor;

	/**
	 * The bread crumbs, which are used to show the title of the currently
	 * selected fragment on devices with a large screen.
	 */
	private FragmentBreadCrumbs breadCrumbs;

	/**
	 * Handles the intent, which has been used to start the activity.
	 */
	private void handleIntent() {
		String initialFragment = getIntent()
				.getStringExtra(EXTRA_SHOW_FRAGMENT);
		Bundle initialArguments = getIntent().getBundleExtra(
				EXTRA_SHOW_FRAGMENT_ARGUMENTS);
		int initialTitle = getIntent()
				.getIntExtra(EXTRA_SHOW_FRAGMENT_TITLE, 0);
		int initialShortTitle = getIntent().getIntExtra(
				EXTRA_SHOW_FRAGMENT_SHORT_TITLE, 0);

		if (initialFragment != null) {
			for (int i = 0; i < getListAdapter().getCount(); i++) {
				PreferenceHeader preferenceHeader = getListAdapter().getItem(i);

				if (preferenceHeader.getFragment().equals(initialFragment)) {
					showPreferenceScreen(preferenceHeader, initialArguments);
					getListView().setItemChecked(i, true);

					if (initialTitle != 0) {
						CharSequence title = getText(initialTitle);
						CharSequence shortTitle = (initialShortTitle != 0) ? getText(initialShortTitle)
								: null;
						showBreadCrumbs(title, shortTitle);
					}
				}
			}
		}

		hideNavigation(getIntent().getBooleanExtra(EXTRA_NO_HEADERS, false));
	}

	/**
	 * Shows the fragment, which corresponds to a specific preference header.
	 * 
	 * @param preferenceHeader
	 *            The preference header, the fragment, which should be shown,
	 *            corresponds to, as an instance of the class
	 *            {@link PreferenceHeader}. The preference header may not be
	 *            null
	 * @param params
	 *            Optional parameters, which are passed to the fragment, as an
	 *            instance of the class Bundle or null, if the preference
	 *            header's extras should be used instead
	 */
	private void showPreferenceScreen(final PreferenceHeader preferenceHeader,
			final Bundle params) {
		currentPreferenceHeader = preferenceHeader;

		if (preferenceHeader.getIntent() != null) {
			startActivity(preferenceHeader.getIntent());
		} else {
			Bundle parameters = (params != null) ? params : preferenceHeader
					.getExtras();
			showPreferenceScreen(preferenceHeader.getFragment(), parameters);
			showBreadCrumbs(preferenceHeader);
		}
	}

	/**
	 * Shows the fragment, which corresponds to a specific class name.
	 * 
	 * @param fragmentName
	 *            The full qualified class name of the fragment, which should be
	 *            shown, as a {@link String}
	 * @param params
	 *            Optional parameters, which are passed to the fragment, as an
	 *            instance of the class {@link Bundle} or null, if no parameters
	 *            should be passed
	 */
	private void showPreferenceScreen(final String fragmentName,
			final Bundle params) {
		Fragment fragment = Fragment.instantiate(this, fragmentName, params);

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

		if (isPreferenceHeaderSelected()) {
			transition = FragmentTransaction.TRANSIT_FRAGMENT_CLOSE;
			currentPreferenceHeader = null;
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
		this.currentTitle = title;
		this.currentShortTitle = title;

		if (getBreadCrumbs() != null) {
			getBreadCrumbs().setTitle(title, shortTitle);
			getBreadCrumbs().setParentTitle(null, null, null);
		} else if (title != null) {
			setTitle(title);
		}
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
	 * Returns the view group, which contains all views, e.g. the preferences
	 * itself and the bread crumbs, which are shown when a preference header is
	 * selected on devices with a large screen.
	 * 
	 * @return The view group, which contains all views, which are shown when a
	 *         preference header is selected, as an instance of the class
	 *         {@link ViewGroup} or null, if the device has a small screen
	 */
	public final ViewGroup getPreferenceScreenContainer() {
		return preferenceScreenContainer;
	}

	/**
	 * Returns the view, which is used to draw a separator between the bread
	 * crumbs and the preferences on devices with a large screen.
	 * 
	 * @return The view, which is used to draw a separator between the bread
	 *         crumbs and the preferences, as an instance of the class
	 *         {@link View} or null, if the device has a small display
	 */
	public final View getBreadCrumsSeparator() {
		return breadCrumbsSeperator;
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
	 * Returns the bread crumbs, which are used to show the title of the
	 * currently selected fragment on devices with a large screen.
	 * 
	 * @return The bread crumbs, which are used to show the title of the
	 *         currently selected fragment or null, if the device has a small
	 *         screen
	 */
	public final FragmentBreadCrumbs getBreadCrumbs() {
		return breadCrumbs;
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
	 * Returns, whether the fragment, which provides navigation to each
	 * preference header's fragment, is currently hidden or not.
	 * 
	 * @return True, if the fragment, which provides navigation to each
	 *         preference header's fragment is currently hidden, false otherwise
	 */
	public final boolean isNavigationHidden() {
		return navigationHidden;
	}

	/**
	 * Hides or shows the fragment, which provides navigation to each preference
	 * header's fragment.
	 * 
	 * @param hideNavigation
	 *            True, if the fragment, which provides navigation to each
	 *            preference header's fragment, should be hidden, false
	 *            otherwise
	 */
	public final void hideNavigation(final boolean hideNavigation) {
		this.navigationHidden = hideNavigation;

		if (isSplitScreen()) {
			getPreferenceHeaderParentView().setVisibility(
					hideNavigation ? View.GONE : View.VISIBLE);
			getShadowView().setVisibility(
					hideNavigation ? View.GONE : View.VISIBLE);
		} else {
			onBackPressed();
		}
	}

	/**
	 * Returns, whether a preference header is currently selected, or not.
	 * 
	 * @return True, if a preference header is currently selected, false
	 *         otherwise
	 */
	public final boolean isPreferenceHeaderSelected() {
		return currentPreferenceHeader != null;
	}

	/**
	 * Returns the color of the separator, which is drawn between the bread
	 * crumbs and the preferences on devices with a large screen.
	 * 
	 * @return The color of the separator as an {@link Integer} value or -1, if
	 *         the device has a small screen
	 */
	public final int getBreadCrumbsSeparatorColor() {
		if (getBreadCrumsSeparator() != null) {
			return breadCrumbsSeparatorColor;
		} else {
			return -1;
		}
	}

	/**
	 * Sets the color of the separator, which is drawn between the bread crumbs
	 * and the preferences on devices with a large screen. The color is only set
	 * on devices with a large screen.
	 * 
	 * @param separatorColor
	 *            The color, which should be set, as an {@link Integer} value
	 * @return True, if the color has been set, false otherwise
	 */
	public final boolean setBreadCrumbsSeparatorColor(final int separatorColor) {
		if (getBreadCrumsSeparator() != null) {
			this.breadCrumbsSeparatorColor = separatorColor;
			getBreadCrumsSeparator().setBackgroundColor(separatorColor);
			return true;
		}

		return false;
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
	 * Returns the background of the view group, which contains all views, which
	 * are shown when a preference header is selected on devices with a large
	 * screen.
	 * 
	 * @return The background of the view group, which contains all views, which
	 *         are shown when a preference header is selected or null, if no
	 *         background has been set or device has a small screen
	 */
	public final Drawable getPreferenceScreenBackground() {
		if (getPreferenceScreenContainer() != null) {
			return getPreferenceScreenContainer().getBackground();
		} else {
			return null;
		}
	}

	/**
	 * Sets the background of the view group, which contains all views, which
	 * are shown when a preference header is selected. The background is only
	 * set on devices with a large screen.
	 * 
	 * @param resourceId
	 *            The resource id of the background, which should be set, as an
	 *            {@link Integer} value. The resource id must correspond to a
	 *            valid drawable resource
	 * @return True, if the background has been set, false otherwise
	 */
	public final boolean setPreferenceScreenBackground(final int resourceId) {
		return setPreferenceScreenBackground(getResources().getDrawable(
				resourceId));
	}

	/**
	 * Sets the background color of the view group, which contains all views,
	 * which are shown when a preference header is selected. The background is
	 * only set on devices with a large screen.
	 * 
	 * @param color
	 *            The background color, which should be set, as an
	 *            {@link Integer} value
	 * @return True, if the background has been set, false otherwise
	 */
	public final boolean setPreferenceScreenBackgroundColor(final int color) {
		return setPreferenceScreenBackground(new ColorDrawable(color));
	}

	/**
	 * Sets the background of the view group, which contains all views, which
	 * are shown when a preference header is selected. The background is only
	 * set on devices with a large screen.
	 * 
	 * @param drawable
	 *            The background, which should be set, as an instance of the
	 *            class {@link Drawable} or null, if no background should be set
	 * @return True, if the background has been set, false otherwise
	 */
	@SuppressWarnings("deprecation")
	public final boolean setPreferenceScreenBackground(final Drawable drawable) {
		if (getPreferenceScreenContainer() != null) {
			getPreferenceScreenContainer().setBackgroundDrawable(drawable);
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

	/**
	 * Returns, whether the behavior of the action bar's back button is
	 * overridden to return to the navigation when a preference header is
	 * currently selected on devices with a small screen, or not.
	 * 
	 * @return True, if the behavior of the action bar's back button is
	 *         overridden, false otherwise
	 */
	public final boolean isBackButtonOverridden() {
		return overrideBackButton;
	}

	/**
	 * Sets, whether the behavior of the action bar's back button should be
	 * overridden to return to the navigation when a preference header is
	 * currently selected on devices with a small screen, or not.
	 * 
	 * @param overrideBackButton
	 *            True, if the behavior of the action bar's back button should
	 *            be overridden, false otherwise
	 */
	public final void overrideBackButton(final boolean overrideBackButton) {
		this.overrideBackButton = overrideBackButton;

		if (isPreferenceHeaderSelected()) {
			showActionBarBackButton();
		}
	}

	@Override
	public final void onFragmentCreated(final Fragment fragment) {
		onCreatePreferenceHeaders();
		getListView().setOnItemClickListener(this);

		if (isSplitScreen()) {
			getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);

			if (!getListAdapter().isEmpty()) {
				getListView().setItemChecked(0, true);
				showPreferenceScreen(getListAdapter().getItem(0), null);
			}
		}

		handleIntent();
	}

	@Override
	public final void onItemClick(final AdapterView<?> parent, final View view,
			final int position, final long id) {
		showPreferenceScreen(getListAdapter().getItem(position), null);
	}

	@Override
	public final boolean onKeyDown(final int keyCode, final KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && !isSplitScreen()
				&& isPreferenceHeaderSelected() && !isNavigationHidden()) {
			showPreferenceHeaders();
			return true;
		}

		return super.onKeyDown(keyCode, event);
	}

	@Override
	public final boolean onOptionsItemSelected(final MenuItem item) {
		if (item.getItemId() == android.R.id.home && !isSplitScreen()
				&& isPreferenceHeaderSelected() && isBackButtonOverridden()
				&& !isNavigationHidden()) {
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
		preferenceScreenContainer = (ViewGroup) findViewById(R.id.preference_screen_container);
		breadCrumbs = (FragmentBreadCrumbs) findViewById(R.id.bread_crumbs_view);
		breadCrumbs.setMaxVisible(2);
		breadCrumbs.setActivity(this);
		breadCrumbsSeperator = findViewById(R.id.bread_crumbs_separator);
		shadowView = findViewById(R.id.shadow_view);
		preferenceHeaderFragment = new PreferenceHeaderFragment();
		preferenceHeaderFragment.addFragmentListener(this);
		overrideBackButton(true);
		showPreferenceHeaders();
		setBreadCrumbsSeparatorColor(getResources().getColor(
				R.color.bread_crumb_separator));
		setShadowColor(getResources().getColor(R.color.shadow));
	}

	@Override
	protected final void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(CURRENT_FRAGMENT_EXTRA,
				currentPreferenceHeader.getFragment());
		outState.putBundle(CURRENT_BUNDLE_EXTRA,
				currentPreferenceHeader.getExtras());
		outState.putCharSequence(CURRENT_TITLE_EXTRA, currentTitle);
		outState.putCharSequence(CURRENT_SHORT_TITLE_EXTRA, currentShortTitle);
		outState.putInt(SELECTED_PREFERENCE_HEADER_EXTRA, getListView()
				.getCheckedItemPosition());
	}

	@Override
	protected final void onRestoreInstanceState(final Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		String currentFragment = savedInstanceState
				.getString(CURRENT_FRAGMENT_EXTRA);
		Bundle currentBundle = savedInstanceState
				.getBundle(CURRENT_BUNDLE_EXTRA);
		CharSequence title = savedInstanceState
				.getCharSequence(CURRENT_TITLE_EXTRA);
		CharSequence shortTitle = savedInstanceState
				.getCharSequence(CURRENT_SHORT_TITLE_EXTRA);
		int selectedPreferenceHeader = savedInstanceState
				.getInt(SELECTED_PREFERENCE_HEADER_EXTRA);

		if (currentFragment != null) {
			showPreferenceScreen(currentFragment, currentBundle);
			showBreadCrumbs(title, shortTitle);
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