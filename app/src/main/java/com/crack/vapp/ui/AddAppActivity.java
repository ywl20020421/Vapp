package com.crack.vapp.ui;

import static com.crack.vapp.Utils.AppInfoUtils.getAllNonSystemApps;

import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.crack.vapp.R;

import java.util.List;

public class AddAppActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MyAppAdapter appAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_app);

        // 初始化 RecyclerView
        recyclerView = findViewById(R.id.recycler_view_add_app); // 修改为 recycler_view_add_app
        recyclerView.setLayoutManager(new GridLayoutManager(this, 1)); // 每行 1 个应用图标

        // 加载应用列表
        List<ApplicationInfo> appInfoList = getAllNonSystemApps(this);
        appAdapter = new MyAppAdapter(appInfoList);
        recyclerView.setAdapter(appAdapter);
    }

    // 模拟获取 ApplicationInfo 数据源
    private List<ApplicationInfo> getApplicationInfoList() {
        return getPackageManager().getInstalledApplications(0);
    }
}