package com.crack.vapp.test;

import android.widget.Button;

import com.crack.vapp.BaseData;
import com.crack.vapp.R;
import com.crack.vapp.core.HookService;

public class testActivity extends android.app.Activity{
    public void onCreate(android.os.Bundle savedInstanceState)

    {
        BaseData.baseActivity = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        android.widget.Toast.makeText(this,"testActivity",android.widget.Toast.LENGTH_SHORT).show();

        Button button = (Button)findViewById(R.id.button);
        button.setText("启动服务");
        button.setOnClickListener(new android.view.View.OnClickListener(){
            public void onClick(android.view.View v)
            {
               //启动服务 ProxyService
                android.content.Intent intent = new android.content.Intent(testActivity.this, testService.class);
                startService(intent);
            }
        });

        Button button2 = (Button)findViewById(R.id.button2);
        button2.setText("启动Hook Service");
        button2.setOnClickListener(new android.view.View.OnClickListener(){
            public void onClick(android.view.View v)
            {
                HookService.hook();
            }
        });

    }
}
