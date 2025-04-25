package com.crack.vapp.ui;

import static com.crack.vapp.core.InstallApp.installApp;

import android.content.pm.ApplicationInfo;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.crack.vapp.BaseData;
import com.crack.vapp.R;
import com.crack.vapp.Utils.AppInfoUtils;

import java.util.List;

public class MyAppAdapter extends RecyclerView.Adapter<MyAppAdapter.ViewHolder> {

    private List<ApplicationInfo> appList;

    public MyAppAdapter(List<ApplicationInfo> appList) {
        this.appList = appList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app_add, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ApplicationInfo appInfo = appList.get(position);
        holder.appIcon.setImageDrawable(appInfo.loadIcon(holder.itemView.getContext().getPackageManager()));
        holder.appName.setText(appInfo.loadLabel(holder.itemView.getContext().getPackageManager()));

        // 设置安装按钮点击事件
        holder.installButton.setOnClickListener(v -> {
            // 打印应用路径
            System.out.println("应用路径: " + appInfo.sourceDir);

            installApp(BaseData.baseActivity, appInfo);
            System.out.println("安装完成");
            AppInfoUtils.saveString(BaseData.baseActivity, appInfo.packageName);


        });
    }

    @Override
    public int getItemCount() {
        return appList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView appIcon;
        TextView appName;
        Button installButton;

        public ViewHolder(View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.app_icon);
            appName = itemView.findViewById(R.id.app_name);
            installButton = itemView.findViewById(R.id.install_button);
        }
    }
}