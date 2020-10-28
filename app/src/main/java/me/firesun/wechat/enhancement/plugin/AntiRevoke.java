package me.firesun.wechat.enhancement.plugin;

import android.content.ContentValues;


import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import me.firesun.wechat.enhancement.PreferencesUtils;
import me.firesun.wechat.enhancement.util.HookParams;


public class AntiRevoke implements IPlugin {
    private static Map<Long, Object> msgCacheMap = new HashMap<>();
    private static Object storageInsertClazz;

    private static final List<XC_MethodHook.Unhook> unhookList = new ArrayList<>();

    @Override
    public void hook(final XC_LoadPackage.LoadPackageParam lpparam, final ClassLoader classLoader) {
        for (XC_MethodHook.Unhook unhook : unhookList) {
            unhook.unhook();
        }
        unhookList.clear();
        XC_MethodHook.Unhook unhook = XposedHelpers.findAndHookMethod(HookParams.getInstance().SQLiteDatabaseClassName, classLoader, HookParams.getInstance().SQLiteDatabaseUpdateMethod, String.class, ContentValues.class, String.class, String[].class, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                try {
                    if (!PreferencesUtils.isAntiRevoke()) {
                        return;
                    }

                    if (param.args[0].equals("message")) {
                        ContentValues contentValues = ((ContentValues) param.args[1]);

                        if (contentValues.getAsInteger("type") == 10000 &&
                                !contentValues.getAsString("content").equals("你撤回了一条消息") &&
                                !contentValues.getAsString("content").equals("You've recalled a message") &&
                                !contentValues.getAsString("content").startsWith("<sysmsg type=\"invokeMessage\"><invokeMessage><text><![CDATA[你撤回了一条消息]]") &&
                                !contentValues.getAsString("content").startsWith("<sysmsg type=\"invokeMessage\"><invokeMessage><text><![CDATA[You've recalled a message]]")
                        ) {

                            handleMessageRecall(contentValues);
                            param.setResult(1);
                        }
                    }
                } catch (Error | Exception e) {
                }
            }
        });
        unhookList.add(unhook);

        unhook = XposedHelpers.findAndHookMethod(HookParams.getInstance().SQLiteDatabaseClassName, classLoader, HookParams.getInstance().SQLiteDatabaseDeleteMethod, String.class, String.class, String[].class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                try {
                    if (!PreferencesUtils.isAntiRevoke()) {
                        return;
                    }

                    String[] media = {"ImgInfo2", "voiceinfo", "videoinfo2", "WxFileIndex2"};
                    if (Arrays.asList(media).contains(param.args[0])) {
                        param.setResult(1);
                    }

                } catch (Error | Exception e) {
                }
            }
        });
        unhookList.add(unhook);


        unhook = XposedHelpers.findAndHookMethod(File.class, "delete", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                try {
                    if (!PreferencesUtils.isAntiRevoke()) {
                        return;
                    }
                    String path = ((File) param.thisObject).getAbsolutePath();
                    if ((path.contains("/image2/") || path.contains("/voice2/") || path.contains("/video/")))
                        param.setResult(true);
                } catch (Error | Exception e) {
                }

            }
        });
        unhookList.add(unhook);

        Class msgInfoClass = XposedHelpers.findClass(HookParams.getInstance().MsgInfoClassName, classLoader);
        unhook = XposedHelpers.findAndHookMethod(HookParams.getInstance().MsgInfoStorageClassName, classLoader, HookParams.getInstance().MsgInfoStorageInsertMethod, msgInfoClass, boolean.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                try {
                    if (!PreferencesUtils.isAntiRevoke()) {
                        return;
                    }

                    storageInsertClazz = param.thisObject;
                    Object msg = param.args[0];
                    long msgId = XposedHelpers.getLongField(msg, "field_msgId");
                    msgCacheMap.put(msgId, msg);
                } catch (Error | Exception e) {
                }

            }
        });
        unhookList.add(unhook);
    }

    private void handleMessageRecall(ContentValues contentValues) {
        long msgId = contentValues.getAsLong("msgId");
        Object msg = msgCacheMap.get(msgId);
        long createTime = XposedHelpers.getLongField(msg, "field_createTime");
        XposedHelpers.setIntField(msg, "field_type", contentValues.getAsInteger("type"));
        XposedHelpers.setObjectField(msg, "field_content",
                contentValues.getAsString("content") + "(已被阻止)");

        XposedHelpers.setLongField(msg, "field_createTime", createTime + 1L);
        XposedHelpers.callMethod(storageInsertClazz, HookParams.getInstance().MsgInfoStorageInsertMethod, msg, false);

    }
}
