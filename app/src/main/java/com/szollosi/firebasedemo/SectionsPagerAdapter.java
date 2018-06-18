package com.szollosi.firebasedemo;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

class SectionsPagerAdapter extends FragmentPagerAdapter {

  private static final byte TOTAL_SECTION = 2;

  SectionsPagerAdapter(FragmentManager fragmentManager) {
    super(fragmentManager);
  }

  @Override public Fragment getItem(int position) {
    return PlaceholderFragment.newInstance(position + 1);
  }

  @Override public int getCount() {
    return TOTAL_SECTION;
  }
}
