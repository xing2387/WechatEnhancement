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

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
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
            //            XposedHelpers.findAndHookMethod(Instrumentation.class, "callApplicationOnCreate", Application.class, new XC_MethodHook() {
//            XposedHelpers.findAndHookMethod(ContextWrapper.class, "attachBaseContext", Context.class, new XC_MethodHook() {

            XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
//                    if (param.thisObject.getClass().getCanonicalName().equals("com.tencent.mm.plugin.appbrand.ui.AppBrandUI")) {
                    String clazzName = "com.tencent.mm.plugin.profile.ui.ContactInfoUI";
                    if (param.thisObject.getClass().getCanonicalName().equals(clazzName)) {
                        log("Created.");

                        Activity activity = (Activity) param.thisObject;
                        String wechatId = activity.getIntent().getStringExtra("Contact_User");
                        ClipboardManager cmb = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                        cmb.setText(wechatId);
                        Toast.makeText(activity, "微信ID:" + wechatId + "已复制到剪切板", Toast.LENGTH_LONG).show();
                        log("ContactInfoUI: " + wechatId);

                        log("lpparam.classLoader: " + lpparam.classLoader.getClass().getCanonicalName() + ",,, " + lpparam.classLoader.toString());

                        Class a = param.thisObject.getClass();
                        Class b = null;
                        try {
                            b = XposedHelpers.findClass(clazzName, lpparam.classLoader);
                        } catch (Exception e) {
                            Log.e("EdXposed-Bridge", "ContactInfoUI afterHookedMethod: ", e);
                        }
                        log("class found in lpparam.classLoader: " + (b != null) + "\n" +
                                "\t\t\t\t\t is same clazz: " + (a == b));

                        ClassLoader loader = param.thisObject.getClass().getClassLoader();
                        log("ClassLoaer: " + loader.toString());
                        log("Same with package: " + (loader == lpparam.classLoader));
                        loader = loader.getParent();
                        while (loader != null) {
                            if (loader == lpparam.classLoader) {
                                log("Package class loader is one of the parent.");
                                return;
                            }
                            loader = loader.getParent();
                        }
                        log("Package class loader is not one of the parent.");
                    }

//                    Log.d(TAG, "findAndHookMethod: ContactInfoUI ");
//                    XposedHelpers.findAndHookMethod("com.tencent.mm.plugin.profile.ui.ContactInfoUI",
//                            lpparam.classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
//                                @Override
//                                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
//                                    Log.d(TAG, "ContactInfoUI beforeHookedMethod: ");
//                                }
//
//                                @Override
//                                protected void afterHookedMethod(MethodHookParam param) {
//                                    Log.d(TAG, "ContactInfoUI afterHookedMethod: ");
//                                }
//                            });


//                            Context context = (Context) param.args[0];
//                            String processName = lpparam.processName;
//                            //Only hook important process
//                            if (!processName.equals(HookParams.WECHAT_PACKAGE_NAME) &&
//                                    !processName.equals(HookParams.WECHAT_PACKAGE_NAME + ":tools")
//                            ) {
//                                return;
//                            }
//                            String versionName = getVersionName(context, HookParams.WECHAT_PACKAGE_NAME);
//                            log("Found wechat version:" + versionName);
//                            Log.d(TAG, "afterHookedMethod: " + "Found wechat version:" + versionName);
//                            if (!HookParams.hasInstance()) {
//                                SearchClasses.init(context, lpparam, versionName);
//                                loadPlugins(lpparam);
//                            }
                }
            });
        } catch (Error | Exception e) {
        }
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


    private void loadPlugins(LoadPackageParam lpparam) {
        for (IPlugin plugin : plugins) {
            try {
                plugin.hook(lpparam);
            } catch (Error | Exception e) {
                log("loadPlugins error" + e);
            }
        }
    }

}
