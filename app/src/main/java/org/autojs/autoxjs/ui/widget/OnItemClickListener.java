package org.autojs.autoxjs.ui.widget;

import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

/**
 * Created by Stardust on 2017/3/27.
 */

public interface OnItemClickListener {

    void onItemClick(RecyclerView parent, View item, int position);
}
