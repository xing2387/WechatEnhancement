package me.firesun.wechat.enhancement;

import android.app.Activity;
import android.app.Application;
import android.app.Instrumentation;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import me.firesun.wechat.enhancement.plugin.ADBlock;
import me.firesun.wechat.enhancement.plugin.AntiRevoke;
import me.firesun.wechat.enhancement.plugin.AntiSnsDelete;
import me.firesun.wechat.enhancement.plugin.AutoLogin;
import me.firesun.wechat.enhancement.plugin.HideModule;
import me.firesun.wechat.enhancement.plugin.IPlugin;
import me.firesun.wechat.enhancement.plugin.Limits;
import me.firesun.wechat.enhancement.plugin.LuckMoney;
import me.firesun.wechat.enhancement.util.HookParams;
import me.firesun.wechat.enhancement.util.SearchClasses;

import static de.robv.android.xposed.XposedBridge.log;


public class Main implements IXposedHookLoadPackage {

    private static final String TAG = "Main";

    private static IPlugin[] plugins = {
            new ADBlock(),
            new AntiRevoke(),
            new AntiSnsDelete(),
            new AutoLogin(),
            new HideModule(),
            new LuckMoney(),
            new Limits(),
    };

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) {

        if (!lpparam.packageName.equals(HookParams.WECHAT_PACKAGE_NAME)) {
            return;
        }

        try {
            Class clazz = XposedHelpers.findClass("com.tencent.tinker.loader.NewClassLoaderInjector", lpparam.classLoader);
            Method method = XposedHelpers.findMethodBestMatch(clazz, "inject", Application.class,
                    ClassLoader.class, File.class, Boolean.class, List.class);
            XposedBridge.hookMethod(method, new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    log("after inject ClassLoader: " + param.getResult());
                    Context context = (Context) param.args[0];
                    doHook(context, lpparam, (ClassLoader) param.getResult());
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "NewClassLoaderInjector handleLoadPackage: ", e);
        }


        XposedHelpers.findAndHookMethod(Instrumentation.class, "callApplicationOnCreate", Application.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                Context context = (Context) param.args[0];
                doHook(context, lpparam, lpparam.classLoader);
            }
        });

    }

    private void doHook(Context context, LoadPackageParam lpparam, ClassLoader classLoader) {
        String processName = lpparam.processName;
        //Only hook important process
        if (!processName.equals(HookParams.WECHAT_PACKAGE_NAME) &&
                !processName.equals(HookParams.WECHAT_PACKAGE_NAME + ":tools")
        ) {
            return;
        }
        String versionName = getVersionName(context, HookParams.WECHAT_PACKAGE_NAME);
        log("Found wechat version:" + versionName);
        Log.d(TAG, "afterHookedMethod: " + "Found wechat version:" + versionName);
        HookParams.getInstance().setClassLoader(classLoader);
        SearchClasses.init(context, lpparam, classLoader, versionName);
        loadPlugins(lpparam, classLoader);
    }

    private String getVersionName(Context context, String packageName) {
        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packInfo = packageManager.getPackageInfo(packageName, 0);
            return packInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
        }
        return "";
    }


    private void loadPlugins(LoadPackageParam lpparam, ClassLoader classLoader) {
        for (IPlugin plugin : plugins) {
            try {
                plugin.hook(lpparam, classLoader);
            } catch (Error | Exception e) {
                log("loadPlugins error" + e);
            }
        }
    }

}
