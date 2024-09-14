/*
 * Copyright © 2024 Mark Raynsford <code@io7m.com> https://www.io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.io7m.exfilac.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import com.io7m.jmulticlose.core.CloseableCollection
import com.io7m.jmulticlose.core.CloseableCollectionType
import com.io7m.jmulticlose.core.ClosingResourceFailedException

/**
 * The main fragment is solely responsible for managing fragments for tabs. The Android
 * ViewPager and ViewPager2 are totally broken and frequently just outright ignore requests
 * to scroll between tabs. Therefore, we simply replace the current fragment whenever the
 * user clicks on a tab.
 */

class EFFragmentMain : EFScreenFragment() {

  private var fragmentNow: Fragment? = null
  private lateinit var tabContent: FrameLayout
  private lateinit var tabLayout: TabLayout

  private var subscriptions: CloseableCollectionType<ClosingResourceFailedException> =
    CloseableCollection.create()

  override fun onBackPressed(): EFBackResult {
    return EFBackResult.BACK_PROPAGATE_UP
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    val view =
      inflater.inflate(R.layout.main, container, false)

    this.tabLayout =
      view.findViewById(R.id.mainTabs)
    this.tabContent =
      view.findViewById(R.id.mainTabsContent)

    /*
     * Naturally, because even the simplest things on Android are completely broken, it's
     * not enough to just track the index of the selected tab: Android will screw up that
     * approach by always calling `onTabSelected` with `0` whenever the view is created. Instead,
     * what you have to do is track whether a tab has been _unselected_, and then if and only
     * if one has, track the selected tab. Otherwise, you'll always set the tab to `0` whenever
     * the view is recreated.
     */

    this.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
      override fun onTabSelected(tab: TabLayout.Tab?) {
        if (tab != null && EFTabModel.tabUnselected != null) {
          EFTabModel.tabSelected = tab.position
          EFTabModel.tabUnselected = null
          this@EFFragmentMain.switchToTab(tab.position)
        }
      }

      override fun onTabUnselected(tab: TabLayout.Tab?) {
        if (tab != null) {
          EFTabModel.tabUnselected = tab.position
        }
      }

      override fun onTabReselected(tab: TabLayout.Tab?) {
        // Nothing
      }
    })
    return view
  }

  override fun onStart() {
    super.onStart()

    /*
     * Restore the selected tab.
     */

    this.switchToTab(EFTabModel.tabSelected ?: 0)
    this.subscriptions = CloseableCollection.create()
  }

  private fun switchToTab(
    tabIndex: Int
  ) {
    this.tabLayout.selectTab(this.tabLayout.getTabAt(tabIndex))
    this.setTabFragment(tabIndex)
  }

  private fun setTabFragment(
    tabIndex: Int
  ) {
    when (tabIndex) {
      0 -> this.switchFragment(EFFragmentTabBuckets())
      1 -> this.switchFragment(EFFragmentTabUploads())
      2 -> this.switchFragment(EFFragmentTabStatus())
      3 -> this.switchFragment(EFFragmentTabSettings())
      else -> throw IllegalStateException("Unexpected tab index: $tabIndex")
    }
  }

  override fun onStop() {
    super.onStop()
    this.subscriptions.close()
  }

  private fun switchFragment(
    fragment: Fragment
  ) {
    this.fragmentNow = fragment
    this.parentFragmentManager.beginTransaction()
      .replace(R.id.mainTabsContent, fragment)
      .commit()
  }
}
