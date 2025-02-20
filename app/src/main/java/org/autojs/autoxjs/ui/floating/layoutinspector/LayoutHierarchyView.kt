package org.autojs.autojs.ui.floating.layoutinspector

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.AdapterView
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import com.stardust.autojs.core.ozobi.capture.ScreenCapture.Companion.curImgBitmap
import com.stardust.autojs.core.ozobi.capture.ScreenCapture.Companion.isCurImgBitmapValid
import com.stardust.util.ViewUtil
import com.stardust.view.accessibility.NodeInfo
import org.autojs.autoxjs.R
import org.autojs.autoxjs.ui.floating.layoutinspector.LayoutBoundsView
import org.autojs.autoxjs.ui.widget.LevelBeamView
import pl.openrnd.multilevellistview.ItemInfo
import pl.openrnd.multilevellistview.MultiLevelListAdapter
import pl.openrnd.multilevellistview.MultiLevelListView
import pl.openrnd.multilevellistview.NestType
import pl.openrnd.multilevellistview.OnItemClickListener
import java.util.Locale
import java.util.Stack

/**
 * Created by Stardust on 2017/3/10.
 */
open class LayoutHierarchyView : MultiLevelListView {
    interface OnItemLongClickListener {
        fun onItemLongClick(view: View, nodeInfo: NodeInfo)
    }

    private var mAdapter: Adapter? = null
    private var mOnItemLongClickListener: ((view: View, nodeInfo: NodeInfo) -> Unit)? = null
    private var onItemTouchListener: ((view: View, event: MotionEvent) -> Boolean)? = null
    private val mOnItemLongClickListenerProxy =
        AdapterView.OnItemLongClickListener { parent, view, position, id ->
            (view.tag as ViewHolder).nodeInfo?.let {
                mOnItemLongClickListener?.invoke(view, it)
                return@OnItemLongClickListener true
            }
            false
        }
    // Added by ozobi - 2025/02/19 >
    companion object{
        var nightMode = false
    }
    // <

    var boundsPaint: Paint? = null
        private set
    private var mBoundsInScreen: IntArray? = null
    var mStatusBarHeight = 0
    var mClickedNodeInfo: NodeInfo? = null
    private var mClickedView: View? = null
    private var mOriginalBackground: Drawable? = null
    var mShowClickedNodeBounds = false
    private var mClickedColor = -0x664d4c49
    private var mRootNode: NodeInfo? = null
    private val mInitiallyExpandedNodes: MutableSet<NodeInfo?> = HashSet()

    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init()
    }

    fun setShowClickedNodeBounds(showClickedNodeBounds: Boolean) {
        mShowClickedNodeBounds = showClickedNodeBounds
    }

    fun setClickedColor(clickedColor: Int) {
        mClickedColor = clickedColor
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun init() {
        // Added by ozobi - 2025/02/19 >
        if(nightMode){
            setClickedColor(0x11ffffff)
        }
        // <
        mAdapter = Adapter()
        setAdapter(mAdapter)
        nestType = NestType.MULTIPLE
        (getChildAt(0) as ListView).apply {
            setOnTouchListener { view, motionEvent ->
                return@setOnTouchListener onItemTouchListener?.invoke(view, motionEvent) ?: false
            }
            onItemLongClickListener = mOnItemLongClickListenerProxy
        }
        setWillNotDraw(false)
        initPaint()
        setOnItemClickListener(object : OnItemClickListener {
            override fun onItemClicked(
                parent: MultiLevelListView,
                view: View,
                item: Any,
                itemInfo: ItemInfo
            ) {
                setClickedItem(view, item as NodeInfo)
            }

            override fun onGroupItemClicked(
                parent: MultiLevelListView,
                view: View,
                item: Any,
                itemInfo: ItemInfo
            ) {
                setClickedItem(view, item as NodeInfo)
            }
        })
    }

    private fun setClickedItem(view: View, item: NodeInfo) {
        mClickedNodeInfo = item
        if (mClickedView == null) {
            mOriginalBackground = view.background
        } else {
            mClickedView!!.background = mOriginalBackground
        }
        view.setBackgroundColor(mClickedColor)
        mClickedView = view
        // Added by ozobi - 2025/02/19
        if(nightMode){
            view.setBackgroundColor(0xaa999999.toInt())
        }
        // <
        invalidate()
    }

    private fun initPaint() {
        boundsPaint = Paint()
        boundsPaint!!.color = Color.DKGRAY
        boundsPaint!!.style = Paint.Style.STROKE
        boundsPaint!!.isAntiAlias = true
        boundsPaint!!.strokeWidth = 3f
        mStatusBarHeight = ViewUtil.getStatusBarHeight(context)
    }

    fun setRootNode(rootNodeInfo: NodeInfo) {
        mRootNode = rootNodeInfo
        mAdapter!!.setDataItems(listOf(rootNodeInfo))
        mClickedNodeInfo = null
        mInitiallyExpandedNodes.clear()
    }

    fun setOnItemTouchListener(listener: ((view: View, event: MotionEvent) -> Boolean)) {
        onItemTouchListener = listener
    }

    fun setOnItemLongClickListener(onNodeInfoSelectListener: (view: View, nodeInfo: NodeInfo) -> Unit) {
        mOnItemLongClickListener = onNodeInfoSelectListener
    }
    // Added by ozobi - 2024/11/04 >
    fun expandChild(nodeInfo: NodeInfo?){
        if(nodeInfo == null){
            return
        }
        val children = nodeInfo.getChildren()
        for(child in children){
            mInitiallyExpandedNodes.add(child)
            expandChild(child)
        }
    }
    fun expand(){
        mInitiallyExpandedNodes.clear()
        expandChild(mClickedNodeInfo)
        val parents = Stack<NodeInfo?>()
        mClickedNodeInfo?.let { searchNodeParents(it, mRootNode, parents) }
        mInitiallyExpandedNodes.addAll(parents)
        mAdapter?.reloadData()
    }
    // <
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Added by ozobi - 2025/01/13 > 将布局范围分析的背景设置为捕获时的截图
        if (isCurImgBitmapValid && curImgBitmap != null) {
            if (width == curImgBitmap!!.height || height == curImgBitmap!!.width) {
                Log.d("ozobiLog", "异常截图, 不绘制")
            } else {
                canvas.drawBitmap(curImgBitmap!!, 0f, -mStatusBarHeight.toFloat(), null)
            }
        }
        if (mBoundsInScreen == null) {
            mBoundsInScreen = IntArray(4)
            getLocationOnScreen(mBoundsInScreen)
            mStatusBarHeight = mBoundsInScreen!![1]
        }
        if (mShowClickedNodeBounds && mClickedNodeInfo != null) {
            LayoutBoundsView.drawRect(
                canvas,
                mClickedNodeInfo!!.boundsInScreen,
                mStatusBarHeight,
                boundsPaint
            )
        }
    }

    fun setSelectedNode(selectedNode: NodeInfo) {
        mInitiallyExpandedNodes.clear()
        val parents = Stack<NodeInfo?>()
        searchNodeParents(selectedNode, mRootNode, parents)
        mClickedNodeInfo = parents.peek()
        mInitiallyExpandedNodes.addAll(parents)
        mAdapter!!.reloadData()
    }

    // Added by ozobi - 2025/02/20 >
    fun ozobiSetSelectedNode(selectedNode: NodeInfo){
        mClickedNodeInfo = selectedNode
        if(mInitiallyExpandedNodes.contains(mClickedNodeInfo)){
            mInitiallyExpandedNodes.remove(mClickedNodeInfo)
        }else{
            mInitiallyExpandedNodes.add(mClickedNodeInfo)
        }
        mAdapter!!.reloadData()
    }
    // <

    private fun searchNodeParents(
        nodeInfo: NodeInfo,
        rootNode: NodeInfo?,
        stack: Stack<NodeInfo?>
    ): Boolean {
        stack.push(rootNode)
        if (nodeInfo == rootNode) {
            return true
        }
        var found = false
        for (child in rootNode!!.getChildren()) {
            if (searchNodeParents(nodeInfo, child, stack)) {
                found = true
                break
            }
        }
        if (!found) {
            stack.pop()
        }
        return found
    }

    private inner class ViewHolder internal constructor(view: View) {
        var nameView: TextView
        var infoView: TextView
        var arrowView: ImageView
        var levelBeamView: LevelBeamView
        var nodeInfo: NodeInfo? = null

        init {
            infoView = view.findViewById<View>(R.id.dataItemInfo) as TextView
            nameView = view.findViewById<View>(R.id.dataItemName) as TextView
            arrowView = view.findViewById<View>(R.id.dataItemArrow) as ImageView
            levelBeamView = view.findViewById<View>(R.id.dataItemLevelBeam) as LevelBeamView
        }
    }

    private inner class Adapter : MultiLevelListAdapter() {
        override fun getSubObjects(`object`: Any): List<*> {
            return (`object` as NodeInfo).getChildren()
        }

        override fun isExpandable(`object`: Any): Boolean {
            return (`object` as NodeInfo).getChildren().isNotEmpty()
        }

        override fun isInitiallyExpanded(`object`: Any): Boolean {
            return mInitiallyExpandedNodes.contains(`object` as NodeInfo)
        }

        public override fun getViewForObject(
            `object`: Any,
            convertView: View?,
            itemInfo: ItemInfo
        ): View {
            var itemResource = R.layout.layout_hierarchy_view_item
            if(nightMode){
                itemResource = R.layout.layout_hierarchy_view_item_night
                LevelBeamView.levelInfoTextColor = Color.WHITE
            }else{
                LevelBeamView.levelInfoTextColor = Color.BLACK
            }
            val nodeInfo = `object` as NodeInfo
            val viewHolder: ViewHolder
            val convertView1 = if (convertView != null) {
                viewHolder = convertView.tag as ViewHolder
//                convertView.setBackgroundColor(color.toInt())// Added by ozobi - 2025/02/19
                convertView
            } else {
                val convertView2 =
                    LayoutInflater.from(context).inflate(itemResource, null)
                viewHolder = ViewHolder(convertView2)
                convertView2.tag = viewHolder
//                convertView2.setBackgroundColor(color.toInt())// Added by ozobi - 2025/02/19
                convertView2
            }
            // Added by ozobi - 2025/02/19
            if(nightMode){
                viewHolder.levelBeamView.alpha = 0.9f
            }
            // <
            viewHolder.nameView.text = simplifyClassName(nodeInfo.className)
            viewHolder.nodeInfo = nodeInfo
            if (viewHolder.infoView.visibility == VISIBLE) viewHolder.infoView.text =
                getItemInfoDsc(itemInfo)
            if (itemInfo.isExpandable && !isAlwaysExpanded) {
                viewHolder.arrowView.visibility = VISIBLE
                viewHolder.arrowView.setImageResource(if (itemInfo.isExpanded) R.drawable.arrow_up else R.drawable.arrow_down)
            } else {
                viewHolder.arrowView.visibility = GONE
            }
            viewHolder.levelBeamView.setLevel(itemInfo.level)
            if (nodeInfo == mClickedNodeInfo) {
                convertView1?.let { setClickedItem(it, nodeInfo) }
            }
            // Added by ozobi - 2024/11/02 >
            val clickable = nodeInfo.clickable
            var hasDesc = false
            var hasText = false
            if(nodeInfo.desc != null){
                hasDesc = true
            }
            if(nodeInfo.text.isNotEmpty()){
                hasText = true
            }
            viewHolder.levelBeamView.setNodeInfo(clickable,hasDesc,hasText)
            // <
            return convertView1!!
        }

        private fun simplifyClassName(className: CharSequence?): String? {
            if (className == null) return null
            var s = className.toString()
            if (s.startsWith("android.widget.")) {
                s = s.substring(15)
            }
            return s
        }

        private fun getItemInfoDsc(itemInfo: ItemInfo): String {
            val builder = StringBuilder()
            builder.append(
                String.format(
                    Locale.getDefault(), "level[%d], idx in level[%d/%d]",
                    itemInfo.level + 1,  /*Indexing starts from 0*/
                    itemInfo.idxInLevel + 1 /*Indexing starts from 0*/,
                    itemInfo.levelSize
                )
            )
            if (itemInfo.isExpandable) {
                builder.append(String.format(", expanded[%b]", itemInfo.isExpanded))
            }
            return builder.toString()
        }
    }
}