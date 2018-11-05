/*
Copyright 2016 shizhefei（LuckyJayce）

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package com.example.lhm.guideview.guide;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout;

import java.util.LinkedList;

public class GuideHelper {
    /**
     * 自动播放最小延迟时间（ms）
     */
    private static final int MIN = 2000;
    /**
     * 自动播放最大延迟时间（ms）
     */
    private static final int MAX = 6000;
    /**
     * activity
     */
    private Activity mActivity;

    /**
     * 要展示的引导页面集合
     */
    private LinkedList<TipPage> mPages = new LinkedList<TipPage>();
    /**
     * Dialog
     */
    private Dialog mBaseDialog;
    /**
     * 是否自动播放
     */
    private boolean mAutoPlay;
    /**
     * dialog中的布局
     */
    private RelativeLayout mLayout;

    public GuideHelper(Activity activity) {
        super();
        this.mActivity = activity;
    }

    public GuideHelper addPage(TipData... tipDatas) {
        return addPage(true, tipDatas);
    }

    /**
     * @param clickDoNext 点击提示界面是否进入显示下一个界面，或者dismiss
     */
    public GuideHelper addPage(boolean clickDoNext, TipData... tipDatas) {
        return addPage(clickDoNext, null, tipDatas);
    }

    public GuideHelper addPage(boolean clickDoNext, OnClickListener onClickPageListener, TipData... tipDatas) {
        mPages.add(new TipPage(clickDoNext, onClickPageListener, tipDatas));
        return this;
    }

    public OnDismissListener onDismissListener;

    public OnDismissListener getOnDismissListener() {
        return onDismissListener;
    }

    public void setOnDismissListener(OnDismissListener onDismissListener) {
        this.onDismissListener = onDismissListener;
    }

    public GuideHelper show() {
        show(true);
        return this;
    }

    public View inflate(int layoutId) {
        return LayoutInflater.from(mActivity).inflate(layoutId, mLayout, false);
    }

    /**
     * @param autoPlay 是否自动播放提示
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public GuideHelper show(boolean autoPlay) {
        this.mAutoPlay = autoPlay;
        //关闭dialog，移除handler消息
        dismiss();
        handler.removeCallbacksAndMessages(null);

        //创建dialog
        mLayout = new InnerChildRelativeLayout(mActivity);
        mBaseDialog = new Dialog(mActivity, android.R.style.Theme_DeviceDefault_Light_DialogWhenLarge_NoActionBar);
        mBaseDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0x99000000));

        //设置沉浸式状态栏
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WindowManager.LayoutParams localLayoutParams = mBaseDialog.getWindow().getAttributes();
            localLayoutParams.flags |= WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
            localLayoutParams.flags |= WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        }

        mBaseDialog.setContentView(mLayout);
        //设置dialog的窗口大小全屏
        mBaseDialog.getWindow()
                .setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        //dialog关闭的时候移除所有消息
        mBaseDialog.setOnDismissListener(new OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {
                handler.removeCallbacksAndMessages(null);
                if (onDismissListener != null) {
                    mBaseDialog.setOnDismissListener(onDismissListener);
                }
            }
        });
        //显示
        mBaseDialog.show();
        startSend();
        return this;
    }

    /**
     * 显示提示界面，这里用view去post，是为了保证view已经显示，才能获取到view的界面画在提示界面上
     * 如果只有自定义view，那么就用handler post
     *
     * @author modify by yale
     * create at 2016/7/14 16:12
     */
    private void startSend() {
        View view = null;
        for (TipPage tipPage : mPages) {
            for (TipData tp : tipPage.tipDatas) {
                if (tp.mTargetViews != null && tp.mTargetViews.length > 0) {
                    view = tp.mTargetViews[0];
                    break;
                }
            }
            if (view != null) {
                break;
            }
        }
        Runnable r = new Runnable() {
            @Override
            public void run() {
                send();
            }
        };
        if (view != null) {
            view.post(r);
        } else {
            handler.post(r);
        }
    }

    private Handler handler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(android.os.Message msg) {
            if (!mPages.isEmpty()) {
                send();
            } else if (currentPage == null || currentPage.clickDoNext) {
                dismiss();
            }
        }
    };

    private TipPage currentPage;

    private void send() {
        currentPage = mPages.poll();
        showIm(mLayout, currentPage.tipDatas);
        if (mAutoPlay) {
            int d = currentPage.tipDatas.length * 1500;
            if (d < MIN) {
                d = 2000;
            } else if (d > MAX) {
                d = MAX;
            }
            handler.sendEmptyMessageDelayed(1, d);
        }
    }

    /**
     * 展示指引布局
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void showIm(final RelativeLayout layout, TipData... tipDatas) {
        //移除掉之前所有的viwe
        layout.removeAllViews();
        //获取layout在屏幕上的位置
        int layoutOffset[] = new int[2];
        layout.getLocationOnScreen(layoutOffset);
        int imageViewId = 89598;
        //循环提示的数据列表
        for (TipData data : tipDatas) {
            imageViewId++;
            if (data.mTargetViews == null || data.mTargetViews.length == 0) {
                RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                        LayoutParams.WRAP_CONTENT);
                switch (data.gravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
                    case Gravity.CENTER_HORIZONTAL:
                        layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
                        break;
                    case Gravity.RIGHT:
                        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, imageViewId);
                        break;
                    case Gravity.LEFT:
                    default:
                        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, imageViewId);
                        break;
                }
                int y = 0;
                switch (data.gravity & Gravity.VERTICAL_GRAVITY_MASK) {
                    case Gravity.CENTER_VERTICAL:
                        layoutParams.addRule(RelativeLayout.CENTER_VERTICAL, imageViewId);
                        break;
                    case Gravity.TOP:
                        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, imageViewId);
                        break;
                    case Gravity.BOTTOM:
                    default:
                        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, imageViewId);
                        break;
                }
                View tipView;
                if (data.tipView != null) {
                    tipView = data.tipView;
                } else {
                    Bitmap bitmap = BitmapFactory.decodeResource(mActivity.getResources(), data.tipImageResourceId);
                    ImageView imageView = new ImageView(mActivity);
                    imageView.setImageBitmap(bitmap);
                    tipView = imageView;
                }
                if (data.onClickListener != null) {
                    tipView.setOnClickListener(data.onClickListener);
                }
                layoutParams.leftMargin += data.offsetX;
                layoutParams.rightMargin -= data.offsetX;
                layoutParams.topMargin += data.offsetY;
                layoutParams.bottomMargin -= data.offsetY;
                layout.addView(tipView, layoutParams);
                continue;
            }

            //循环需要提示的view
            View[] views = data.mTargetViews;
            int[] location = new int[2];
            Rect rect = null;
            for (View view : views) {
                if (view.getVisibility() != View.VISIBLE) {
                    continue;
                }

                //获取view在屏幕的位置，后面会用这个位置绘制到提示的dialog。确保位置完美覆盖在view上
                view.getLocationOnScreen(location);
                //这里避免dialog不是全屏，导致view的绘制位置不对应
                location[1] -= layoutOffset[1];

                //获取view的宽高
                int vWidth = view.getMeasuredWidth();
                int vHeight = view.getMeasuredHeight();

                //如果宽高都小于等于0，再measure试下获取
                if (vWidth <= 0 || vHeight <= 0) {
                    LayoutParams layoutParams = view.getLayoutParams();
                    view.measure(layoutParams.width, layoutParams.height);
                    vWidth = view.getMeasuredWidth();
                    vHeight = view.getMeasuredHeight();
                }

                if (vWidth <= 0 || vHeight <= 0) {
                    continue;
                }

                if (data.needDrawView) {
                    //通过getDrawingCache的方式获取view的视图缓存
                    view.setDrawingCacheEnabled(true);
                    view.buildDrawingCache();
                    Bitmap bitmap = view.getDrawingCache();
                    if (bitmap != null) {
                        bitmap = Bitmap.createBitmap(bitmap);
                    } else {
                        //如果获取不到，则用创建一个view宽高一样的bitmap用canvas把view绘制上去
                        bitmap = Bitmap.createBitmap(vWidth, vHeight, Config.ARGB_8888);
                        Canvas canvas = new Canvas(bitmap);
                        view.draw(canvas);
                    }
                    //释放试图的缓存
                    view.setDrawingCacheEnabled(false);
                    view.destroyDrawingCache();

                    //把需要提示的view的视图设置到imageView上显示
                    ImageView imageView = new ImageView(mActivity);
                    imageView.setScaleType(ScaleType.CENTER_INSIDE);
                    imageView.setImageBitmap(bitmap);
                    imageView.setId(imageViewId);

                    //如果使用者还配置了提示view的背景颜色，那么也设置显示
                    if (data.viewBg != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            imageView.setBackground(data.viewBg);
                        } else {
                            imageView.setBackgroundDrawable(data.viewBg);
                        }
                    }

                    if (data.onClickListener != null) { imageView.setOnClickListener(data.onClickListener); }

                    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                            LayoutParams.WRAP_CONTENT);
                    params.leftMargin = location[0];
                    params.topMargin = location[1];
                    layout.addView(imageView, params);
                }

                if (rect == null) {
                    rect = new Rect(location[0], location[1], location[0] + vWidth, location[1] + vHeight);
                } else {
                    if (rect.left > location[0]) {
                        rect.left = location[0];
                    }
                    if (rect.right < location[0] + vWidth) {
                        rect.right = location[0] + vWidth;
                    }
                    if (rect.top > location[1]) {
                        rect.top = location[1];
                    }
                    if (rect.bottom < location[1] + vHeight) {
                        rect.bottom = location[1] + vHeight;
                    }
                }
            }
            if (rect == null) {
                continue;
            }
            int showViewHeight = 0;
            int showViewWidth = 0;
            View tipView;
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT);
            if (data.tipView != null) {
                tipView = data.tipView;
                tipView.measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                showViewWidth = tipView.getMeasuredWidth();
                showViewHeight = tipView.getMeasuredHeight();
            } else {
                Bitmap bitmap = BitmapFactory.decodeResource(mActivity.getResources(), data.tipImageResourceId);
                showViewHeight = bitmap.getHeight();
                showViewWidth = bitmap.getWidth();
                ImageView tip = new ImageView(mActivity);
                layoutParams.width = showViewWidth;
                layoutParams.height = showViewHeight;
                tip.setImageBitmap(bitmap);
                tipView = tip;
            }
            switch (data.gravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
                case Gravity.CENTER_HORIZONTAL:
                    layoutParams.rightMargin = rect.width() / 2 - showViewWidth / 2;
                    layoutParams.addRule(RelativeLayout.ALIGN_RIGHT, imageViewId);
                    break;
                case Gravity.RIGHT:
                    layoutParams.addRule(RelativeLayout.RIGHT_OF, imageViewId);
                    break;
                case Gravity.LEFT:
                default:
                    layoutParams.rightMargin += rect.width();
                    layoutParams.addRule(RelativeLayout.ALIGN_RIGHT, imageViewId);
                    break;
            }
            int y = 0;
            switch (data.gravity & Gravity.VERTICAL_GRAVITY_MASK) {
                case Gravity.CENTER_VERTICAL:
                    layoutParams.topMargin = rect.height() / 2 - showViewHeight / 2;
                    layoutParams.addRule(RelativeLayout.ALIGN_TOP, imageViewId);
                    break;
                case Gravity.TOP:
                    layoutParams.bottomMargin = rect.height();
                    layoutParams.addRule(RelativeLayout.ALIGN_BOTTOM, imageViewId);
                    break;
                case Gravity.BOTTOM:
                default:
                    layoutParams.topMargin = rect.height();
                    layoutParams.addRule(RelativeLayout.ALIGN_TOP, imageViewId);
                    break;
            }
            layoutParams.leftMargin += data.offsetX;
            layoutParams.rightMargin -= data.offsetX;
            layoutParams.topMargin += data.offsetY;
            layoutParams.bottomMargin -= data.offsetY;
            layout.addView(tipView, layoutParams);
        }

        layout.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (currentPage != null && currentPage.clickDoNext) {
                    if (mPages.isEmpty()) {
                        dismiss();
                    } else {
                        handler.removeCallbacksAndMessages(null);
                        handler.sendEmptyMessage(1);
                    }
                }
                if (currentPage != null && currentPage.onClickPageListener != null) {
                    currentPage.onClickPageListener.onClick(v);
                }
            }
        });
    }

    /**
     * 手动调用展示下一页
     */
    public void nextPage() {
        if (mPages.isEmpty()) {
            dismiss();
        } else {
            handler.removeCallbacksAndMessages(null);
            handler.sendEmptyMessage(1);
        }
    }

    /**
     * 关闭页面
     */
    public void dismiss() {
        if (mBaseDialog != null) { mBaseDialog.dismiss(); }
        mBaseDialog = null;
    }

    /**
     * 传入需展示的view数据
     */
    public static class TipData {
        /**
         * 要高亮的目标view
         */
        View[] mTargetViews;
        /**
         * 默认位置底部居中
         */
        private static final int DEFAULT_GRAVITY = Gravity.BOTTOM | Gravity.CENTER;
        int gravity = DEFAULT_GRAVITY;
        /**
         * 是否需要高亮view
         */
        boolean needDrawView = true;

        OnClickListener onClickListener;
        /**
         * x轴偏移量 向左为负
         */
        private int offsetX;
        /**
         * y轴偏移量 向上为负
         */
        private int offsetY;
        /**
         * 高亮文本的背景
         */
        private Drawable viewBg;
        /**
         * 指引文案的资源id
         */
        private int tipImageResourceId;
        /**
         * 自定义view
         *
         * @author Yale
         * create at 2016/7/14 16:17
         */
        private View tipView;

        public TipData(View tipView, View... targetViews) {
            this(tipView, DEFAULT_GRAVITY, targetViews);
        }

        public TipData(View tipView, int gravity, View... targetViews) {
            this.gravity = gravity;
            this.tipView = tipView;
            this.mTargetViews = targetViews;
        }

        public TipData(int tipImageResourceId, View... targetViews) {
            this(tipImageResourceId, DEFAULT_GRAVITY, targetViews);
        }

        public TipData(int tipImageResourceId, int gravity, View... targetViews) {
            super();
            this.mTargetViews = targetViews;
            this.gravity = gravity;
            this.tipImageResourceId = tipImageResourceId;
        }

        public TipData setLocation(int gravity) {
            this.gravity = gravity;
            return this;
        }

        public TipData setLocation(int gravity, int offsetX, int offsetY) {
            this.gravity = gravity;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            return this;
        }

        public TipData setLocation(int offsetX, int offsetY) {
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            return this;
        }

        public TipData setViewBg(Drawable viewBg) {
            this.viewBg = viewBg;
            return this;
        }

        public TipData setNeedDrawView(boolean needDrawView) {
            this.needDrawView = needDrawView;
            return this;
        }

        public TipData setOnClickListener(OnClickListener onClickListener) {
            this.onClickListener = onClickListener;
            return this;
        }
    }

    private class TipPage {
        /**
         * 其他区域是否可点
         */
        private boolean clickDoNext = true;
        /**
         * 要展示的view数据
         */
        private TipData[] tipDatas;
        /**
         * 文案点击回调事件
         */
        private OnClickListener onClickPageListener;

        public TipPage(boolean clickDoNext, OnClickListener onClickPageListener, TipData[] tipDatas) {
            this.clickDoNext = clickDoNext;
            this.tipDatas = tipDatas;
            this.onClickPageListener = onClickPageListener;
        }
    }

}
