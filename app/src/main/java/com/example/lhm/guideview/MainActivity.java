package com.example.lhm.guideview;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.example.lhm.guideview.guide.GuideHelper;
import com.example.lhm.guideview.guide.GuideHelper.TipData;

public class MainActivity extends AppCompatActivity {

    /**
     * 点击开始演示
     */
    private Button mStart;

    /**
     * 第一步
     */
    private Button mFirstStep;
    /**
     * 第二步
     */
    private Button mNextStep;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        setListener();

    }


    private void initView() {
        mStart = findViewById(R.id.button);
        mFirstStep = findViewById(R.id.button2);
        mNextStep = findViewById(R.id.button3);
    }

    private void setListener() {
        mStart.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                showGuide();
            }
        });
    }

    private void showGuide() {

        final GuideHelper guideHelper = new GuideHelper(this);

        //第一页内容
        //下一步
        View nextStep = guideHelper.inflate(R.layout.view_guide_first);
        TipData tipData = new TipData(nextStep, Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
        tipData.setLocation(0, -dip2pixels(this, 70)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //展示下一页
                guideHelper.nextPage();

            }
        });

        //跳过
        View jump = guideHelper.inflate(R.layout.view_guide_jump);
        TipData tipData1 = new TipData(jump, Gravity.RIGHT | Gravity.TOP);
        tipData1.setLocation(-dip2pixels(this, 17), dip2pixels(this, 30));
        tipData1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                guideHelper.dismiss();
            }
        });

        //按钮高亮 传如图片提示语
        TipData tipData2 = new TipData(R.drawable.ic_new_task_guide, Gravity.CENTER_HORIZONTAL | Gravity.TOP,
                mFirstStep);
        tipData2.setLocation(-dip2pixels(this, 35), 0);
        guideHelper.addPage(false, tipData, tipData1, tipData2);

        //第二页内容
        //我知道了
        View view2 = guideHelper.inflate(R.layout.view_guide_first);
        TextView know = (TextView) view2.findViewById(R.id.next);
        know.setText("我知道了");

        TipData tipData4 = new TipData(view2, Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
        tipData4.setLocation(0, -dip2pixels(this, 70));
        tipData4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                guideHelper.dismiss();
            }
        });

        //按钮高亮 传入自定义布局
        View test3 = guideHelper.inflate(R.layout.view_guide_next);
        TipData tipData5 = new TipData(test3, Gravity.CENTER_HORIZONTAL | Gravity.TOP, mNextStep);
        tipData5.setLocation(-dip2pixels(this, 35), 0);
        guideHelper.addPage(false, tipData4, tipData5);
        guideHelper.show(false);

    }

    /**
     * dp ת pixels :将dip值转换为像素值
     */
    public static int dip2pixels(Context ctx, float dpValue) {
        if (null == ctx) {
            return 0;
        }
        final float scale = ctx.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }
}
