package com.crack.vapp.ui;

import android.content.pm.ApplicationInfo;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageButton;

import com.crack.vapp.BaseData;
import com.crack.vapp.R;
import com.crack.vapp.Utils.AppInfoUtils;
import com.crack.vapp.core.handle;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private HomeAppAdapter homeAppAdapter;
    private ImageButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        BaseData.baseActivity = this;

        // 初始化 RecyclerView
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3)); // 每行 3 个应用图标

        // 添加分割线
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(dividerItemDecoration);

        homeAppAdapter = new HomeAppAdapter(getApplicationInfoList()); // 数据源
        recyclerView.setAdapter(homeAppAdapter);

        // 初始化悬浮按钮
        fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, AddAppActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 每次进入前台时刷新数据
        refreshRecyclerViewData();
    }

    // 获取 ApplicationInfo 数据源
    private List<ApplicationInfo> getApplicationInfoList() {
        List<ApplicationInfo> applicationInfos = AppInfoUtils.getSavedAppInfos(this);
        return applicationInfos != null ? applicationInfos : new ArrayList<>();
    }

    // 刷新 RecyclerView 数据
    private void refreshRecyclerViewData() {
        List<ApplicationInfo> updatedAppList = getApplicationInfoList();
        homeAppAdapter.updateData(updatedAppList); // 更新适配器数据
    }
}

// RecyclerView 适配器
class HomeAppAdapter extends RecyclerView.Adapter<HomeAppAdapter.ViewHolder> {

    private List<ApplicationInfo> appList;

    public HomeAppAdapter(List<ApplicationInfo> appList) {
        this.appList = appList;
    }

    @Override
    public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
        View view = android.view.LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ApplicationInfo appInfo = appList.get(position);
        holder.appIcon.setImageDrawable(appInfo.loadIcon(holder.itemView.getContext().getPackageManager()));
        holder.appName.setText(appInfo.loadLabel(holder.itemView.getContext().getPackageManager()));
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 打印应用路径
                System.out.println("应用路径: " + appInfo.sourceDir);
                handle.begin(appInfo);
            }
        });
    }

    @Override
    public int getItemCount() {
        return appList.size();
    }

    // 更新数据源并刷新 RecyclerView
    public void updateData(List<ApplicationInfo> newData) {
        this.appList.clear(); // 清空旧数据
        this.appList.addAll(newData); // 添加新数据
        notifyDataSetChanged(); // 通知适配器刷新
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        android.widget.ImageView appIcon;
        android.widget.TextView appName;

        public ViewHolder(View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.app_icon);
            appName = itemView.findViewById(R.id.app_name);
        }
    }
}
