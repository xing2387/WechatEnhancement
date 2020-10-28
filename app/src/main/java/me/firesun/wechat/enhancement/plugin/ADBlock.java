package me.firesun.wechat.enhancement.plugin;


import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import me.firesun.wechat.enhancement.PreferencesUtils;
import me.firesun.wechat.enhancement.util.HookParams;


public class ADBlock implements IPlugin {
    private static List<XC_MethodHook.Unhook> unhookList = new ArrayList<>();

    @Override
    public void hook(final XC_LoadPackage.LoadPackageParam lpparam, final ClassLoader classLoader) {
        for (XC_MethodHook.Unhook unhook : unhookList) {
            unhook.unhook();
        }
        unhookList.clear();

        XC_MethodHook.Unhook unhook = XposedHelpers.findAndHookMethod(HookParams.getInstance().XMLParserClassName, classLoader,
                HookParams.getInstance().XMLParserMethod, String.class, String.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        try {
                            if (!PreferencesUtils.isADBlock())
                                return;

                            if (param.args[1].equals("ADInfo"))
                                param.setResult(null);
                        } catch (Error | Exception e) {
                        }

                    }
                });
        unhookList.add(unhook);
    }

}
