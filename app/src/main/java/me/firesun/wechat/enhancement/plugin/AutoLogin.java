package me.firesun.wechat.enhancement.plugin;


import android.app.Activity;
import android.widget.Button;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import me.firesun.wechat.enhancement.PreferencesUtils;
import me.firesun.wechat.enhancement.util.HookParams;


public class AutoLogin implements IPlugin {
    private static final List<XC_MethodHook.Unhook> unhookList = new ArrayList<>();

    @Override
    public void hook(final XC_LoadPackage.LoadPackageParam lpparam, final ClassLoader classLoader) {
        for (XC_MethodHook.Unhook unhook : unhookList) {
            unhook.unhook();
        }
        unhookList.clear();
        XC_MethodHook.Unhook unhook = XposedHelpers.findAndHookMethod(android.app.Activity.class, "onStart", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                try {
                    if (!PreferencesUtils.isAutoLogin())
                        return;
                    if (!(param.thisObject instanceof Activity)) {
                        return;
                    }
                    Activity activity = (Activity) param.thisObject;
                    if (activity.getClass().getName().equals(HookParams.getInstance().WebWXLoginUIClassName)) {
                        Class clazz = activity.getClass();
                        Field field = XposedHelpers.findFirstFieldByExactType(clazz, Button.class);
                        Button button = (Button) field.get(activity);
                        if (button != null) {
                            button.performClick();
                        }
                    }

                } catch (Error | Exception e) {
                }
            }
        });
        unhookList.add(unhook);
    }

}
