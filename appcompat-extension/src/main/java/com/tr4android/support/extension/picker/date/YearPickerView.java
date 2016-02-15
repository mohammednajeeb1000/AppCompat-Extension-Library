/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tr4android.support.extension.picker.date;

import android.content.Context;
import android.content.res.Resources;
import android.support.v4.view.accessibility.AccessibilityEventCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.tr4android.appcompat.extension.R;
import com.tr4android.support.extension.picker.CompatUtils;

import java.util.Calendar;

/**
 * Displays a selectable list of years.
 */
class YearPickerView extends ListView {
    private final YearAdapter mAdapter;
    private final int mViewSize;
    private final int mChildSize;

    private OnYearSelectedListener mOnYearSelectedListener;

    private long mCurrentTimeMillis;

    public YearPickerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0); //TODO: look into R.attr.listViewStyle
    }

    public YearPickerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        final LayoutParams frame = new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        setLayoutParams(frame);

        final Resources res = context.getResources();
        mViewSize = res.getDimensionPixelOffset(R.dimen.datepicker_view_animator_height);
        mChildSize = res.getDimensionPixelOffset(R.dimen.datepicker_year_label_height);

        setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final int year = mAdapter.getYearForPosition(position);
                mAdapter.setSelection(year);

                if (mOnYearSelectedListener != null) {
                    mOnYearSelectedListener.onYearChanged(YearPickerView.this, year);
                }
            }
        });

        mAdapter = new YearAdapter(getContext());
        setAdapter(mAdapter);
    }

    public void setOnYearSelectedListener(OnYearSelectedListener listener) {
        mOnYearSelectedListener = listener;
    }

    public void setDate(long currentTimeMillis) {
        mCurrentTimeMillis = currentTimeMillis;
    }

    /**
     * Sets the currently selected year. Jumps immediately to the new year.
     *
     * @param year the target year
     */
    public void setYear(final int year) {
        mAdapter.setSelection(year);

        post(new Runnable() {
            @Override
            public void run() {
                final int position = mAdapter.getPositionForYear(year);
                if (position >= 0 && position < getCount()) {
                    setSelectionCentered(position);
                }
            }
        });
    }

    public void setSelectionCentered(int position) {
        final int offset = mViewSize / 2 - mChildSize / 2;
        setSelectionFromTop(position, offset);
    }

    public void setRange(Calendar min, Calendar max) {
        mAdapter.setRange(min, max);
    }

    private static class YearAdapter extends BaseAdapter {
        private static final int ITEM_LAYOUT = R.layout.year_label_text_view;
        private static final int ITEM_TEXT_APPEARANCE =
                R.style.TextAppearance_Material_DatePicker_List_YearLabel;
        private static final int ITEM_TEXT_SELECTED_APPEARANCE =
                R.style.TextAppearance_Material_DatePicker_List_YearLabel_Selected;
        private static final int ITEM_IS_SELECTED = R.id.date_picker_year_item_is_selected;

        private final LayoutInflater mInflater;

        private int mSelectedYear;
        private int mMinYear;
        private int mCount;

        public YearAdapter(Context context) {
            mInflater = LayoutInflater.from(context);
        }

        public void setRange(Calendar minDate, Calendar maxDate) {
            final int minYear = minDate.get(Calendar.YEAR);
            final int count = maxDate.get(Calendar.YEAR) - minYear + 1;

            if (mMinYear != minYear || mCount != count) {
                mMinYear = minYear;
                mCount = count;
                notifyDataSetInvalidated();
            }
        }

        public boolean setSelection(int year) {
            if (mSelectedYear != year) {
                mSelectedYear = year;
                notifyDataSetChanged();
                return true;
            }
            return false;
        }

        @Override
        public int getCount() {
            return mCount;
        }

        @Override
        public Integer getItem(int position) {
            return getYearForPosition(position);
        }

        @Override
        public long getItemId(int position) {
            return getYearForPosition(position);
        }

        public int getPositionForYear(int year) {
            return year - mMinYear;
        }

        public int getYearForPosition(int position) {
            return mMinYear + position;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final TextView v;
            final boolean hasNewView = convertView == null;
            if (hasNewView) {
                v = (TextView) mInflater.inflate(ITEM_LAYOUT, parent, false);
            } else {
                v = (TextView) convertView;
            }

            final int year = getYearForPosition(position);
            final boolean selected = mSelectedYear == year;

            if (hasNewView || v.getTag(ITEM_IS_SELECTED) != selected) {
                final int textAppearanceResId;
                if (selected && ITEM_TEXT_SELECTED_APPEARANCE != 0) {
                    textAppearanceResId = ITEM_TEXT_SELECTED_APPEARANCE;
                } else {
                    textAppearanceResId = ITEM_TEXT_APPEARANCE;
                }
                CompatUtils.setTextAppearance(v, textAppearanceResId);
                v.setTag(ITEM_IS_SELECTED, selected);
            }

            v.setText(Integer.toString(year));
            return v;
        }

        @Override
        public int getItemViewType(int position) {
            return 0;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return true;
        }

        @Override
        public boolean isEnabled(int position) {
            return true;
        }
    }

    public int getFirstPositionOffset() {
        final View firstChild = getChildAt(0);
        if (firstChild == null) {
            return 0;
        }
        return firstChild.getTop();
    }

    /** @hide */
    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);

        // There are a bunch of years, so don't bother.
        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            AccessibilityEventCompat.asRecord(event).setFromIndex(0);
            AccessibilityEventCompat.asRecord(event).setToIndex(0);
        }
    }

    /**
     * The callback used to indicate the user changed the year.
     */
    public interface OnYearSelectedListener {
        /**
         * Called upon a year change.
         *
         * @param view The view associated with this listener.
         * @param year The year that was set.
         */
        void onYearChanged(YearPickerView view, int year);
    }
}