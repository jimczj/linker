package com.github.jzoom.linker;

import android.app.Activity;
import android.util.Log;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.provider.Settings;
import android.content.ComponentName;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.LogManager;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/** LinkerPlugin */
public class LinkerPlugin implements MethodCallHandler, PluginRegistry.ActivityResultListener {

  final MethodChannel channel;
  final Activity activity;

  public LinkerPlugin(MethodChannel channel, Activity activity) {
    this.channel = channel;
    this.activity = activity;
  }

  /** Plugin registration. */
  public static void registerWith(Registrar registrar) {
    final MethodChannel channel = new MethodChannel(registrar.messenger(), "linker");
    LinkerPlugin linkerPlugin = new LinkerPlugin(channel, registrar.activity());
    registrar.addActivityResultListener(linkerPlugin);
    channel.setMethodCallHandler(linkerPlugin);
  }

  private Result result;

  private int requestCode;

  static void setValueToIntent(String name, Object value, Intent map) {
    if (value instanceof String) {
      map.putExtra(name, (String) value);
    } else if (value instanceof Integer) {
      map.putExtra(name, ((Integer) value).intValue());
    } else if (value instanceof Float) {
      map.putExtra(name, ((Float) value).floatValue());
    } else if (value instanceof Double) {
      map.putExtra(name, ((Double) value).doubleValue());
    } else if (value instanceof Boolean) {
      map.putExtra(name, ((Boolean) value).booleanValue());
    } else {
      throw new RuntimeException("Unsupported type:" + value.getClass());
    }
  }

  Map<String, Object> bundle2Map(Bundle bundle) {
    if (bundle == null) {
      return null;
    }
    Map<String, Object> data = new HashMap<>();

    Set<String> set = bundle.keySet();
    for (String key : set) {
      /// Notice the value must be double/int/boolean/String
      data.put(key, bundle.get(key));
    }

    return data;

  }

  private Map intent2map(Intent intent) {
    Map map = new HashMap();
    map.put("action", intent);
    map.put("extras", intent.getExtras());

    return map;

  }

  private Intent parseIntent(Map data) {
    Map<String, Object> extras = (Map<String, Object>) data.get("extras");
    Intent intent = null;
    String uri = (String) data.get("uri");
    String action = (String) data.get("action");
    // intent 协议处理
    if (uri != null && (uri.startsWith("intent:") || uri.startsWith("android-app:"))) {
      try {
        intent = Intent.parseUri(uri, Intent.URI_INTENT_SCHEME);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        return intent;
      } catch (Exception e) {
      }
    }
    if (action != null) {
      if (uri != null) {
        intent = new Intent(action, Uri.parse(uri));
      } else {
        intent = new Intent(action);
      }
    }

    if (intent == null) {
      intent = new Intent();
    }

    String packageName = (String) data.get("packageName");
    String className = (String) data.get("className");
    if (packageName != null && className != null) {
      intent.setClassName(packageName, className);
    }

    if (extras != null) {
      for (Map.Entry<String, Object> entry : extras.entrySet()) {
        String key = entry.getKey();
        Object value = entry.getValue();
        if (value == null) {
          continue;
        }
        setValueToIntent(key, value, intent);
      }
    }
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
    return intent;

  }

  @Override
  public void onMethodCall(MethodCall call, Result result) {
    String method = call.method;
    if ("startActivityForResult".equals(method)) {
      synchronized (this) {
        if (this.result != null) {
          this.result.error("cancel", "The last result is not finished", "");
          this.result = null;
        }

        Map data = (Map) call.arguments;
        this.result = result;
        this.requestCode = (Integer) data.get("requestCode");
        Intent intent = parseIntent(data);
        try {
          activity.startActivityForResult(intent, requestCode);
        } catch (Throwable e) {
          result.error(e.getClass().getName(), e.getMessage(), "Cannot start activity");
          this.result = null;
        }
      }
    } else if ("startActivity".equals(method)) {
      Map data = (Map) call.arguments;
      Intent intent = parseIntent(data);
      try {
        activity.startActivity(intent);
        result.success(true);
      } catch (Throwable e) {
        result.error(e.getClass().getName(), e.getMessage(), "Cannot start activity");
      }

    } else if ("openSetting".equals(method)) {
      Intent intent = new Intent();
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      if (Build.VERSION.SDK_INT >= 9) {
        intent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
        intent.setData(Uri.fromParts("package", getPackageName(), null));
      } else if (Build.VERSION.SDK_INT <= 8) {
        intent.setAction(Intent.ACTION_VIEW);
        intent.setClassName("com.android.settings", "com.android.settings.InstalledAppDetails");
        intent.putExtra("com.android.settings.ApplicationPkgName", getPackageName());
      }
      activity.startActivity(intent);
      result.success(true);
    } else if ("openBackgroundSetting".equals(method)) {
      Intent intent = new Intent(Settings.ACTION_SETTINGS);
      activity.startActivity(intent);
      result.success(true);
      // result.success(goSettingPage());
    } else {
      result.notImplemented();
    }
  }

  private boolean goSettingPage() {
    try {
      if (isHuawei()) {
        goHuaweiSetting();
        return true;
      }
      if (isHonor()) {
        goHonorSetting();
        return true;
      }
      if (isOppo()) {
        goOPPOSetting();
        return true;
      }
      if (isVIVO()) {
        goVIVOSetting();
        return true;
      }
      if (isLeTV()) {
        goLetvSetting();
        return true;
      }
      if (isMeizu()) {
        goMeizuSetting();
        return true;
      }
      if (isSamsung()) {
        goSamsungSetting();
        return true;
      }
      if (isSmartisan()) {
        goSmartisanSetting();
        return true;
      }
    } catch (Exception e) {
    }
    return false;
  }

  // 后台权限跳转页面
  private void goHuaweiSetting() {
    try {
      showActivity("com.huawei.systemmanager",
          "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity");
    } catch (Exception e) {
      showActivity("com.huawei.systemmanager",
          "com.huawei.systemmanager.optimize.bootstart.BootStartActivity");
    }
  }

  private void goHonorSetting() {
    try {

      showActivity("com.hihonor.devicemanager");
    } catch (Exception e) {
      Log.e("jimtest", e.getMessage());
    }
  }

  private void goXiaomiSetting() {
    showActivity("com.miui.securitycenter",
        "com.miui.permcenter.autostart.AutoStartManagementActivity");
  }

  private void goOPPOSetting() {
    try {
      showActivity("com.coloros.phonemanager");
    } catch (Exception e1) {
      try {
        showActivity("com.oppo.safe");
      } catch (Exception e2) {
        try {
          showActivity("com.coloros.oppoguardelf");
        } catch (Exception e3) {
          showActivity("com.coloros.safecenter");
        }
      }
    }
  }

  private void goVIVOSetting() {
    try {
      showActivity("com.iqoo.secure");
    } catch (Exception e) {
    }
  }

  private void goMeizuSetting() {
    showActivity("com.meizu.safe");
  }

  private void goSamsungSetting() {
    try {
      showActivity("com.samsung.android.sm_cn");
    } catch (Exception e) {
      showActivity("com.samsung.android.sm");
    }
  }

  private void goLetvSetting() {
    showActivity("com.letv.android.letvsafe",
        "com.letv.android.letvsafe.AutobootManageActivity");
  }

  private void goSmartisanSetting() {
    showActivity("com.smartisanos.security");
  }

  /**
   * 跳转到指定应用的首页
   */
  private void showActivity(String packageName) {
    Intent intent = activity.getPackageManager().getLaunchIntentForPackage(packageName);
    activity.startActivity(intent);
  }

  /**
   * 跳转到指定应用的指定页面
   */
  private void showActivity(String packageName, String activityDir) {
    Intent intent = new Intent();
    intent.setComponent(new ComponentName(packageName, activityDir));
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    activity.startActivity(intent);
  }

  // 是否是华为
  public static boolean isHuawei() {
    return Build.BRAND != null && Build.BRAND.toLowerCase().equals("huawei");
  }

  // 是否是荣耀
  public static boolean isHonor() {
    return Build.BRAND != null && Build.BRAND.toLowerCase().equals("honor");
  }

  // 是否是小米
  public static boolean isXiaomi() {
    return Build.BRAND != null && Build.BRAND.toLowerCase().equals("xiaomi");
  }

  public static boolean isOppo() {
    return Build.BRAND != null && Build.BRAND.toLowerCase().equals("oppo");
  }

  public static boolean isVIVO() {
    return Build.BRAND != null && Build.BRAND.toLowerCase().equals("vivo");
  }

  public static boolean isMeizu() {
    return Build.BRAND != null && Build.BRAND.toLowerCase().equals("meizu");
  }

  public static boolean isSamsung() {
    return Build.BRAND != null && Build.BRAND.toLowerCase().equals("samsung");
  }

  public static boolean isLeTV() {
    return Build.BRAND != null && Build.BRAND.toLowerCase().equals("letv");
  }

  public static boolean isSmartisan() {
    return Build.BRAND != null && Build.BRAND.toLowerCase().equals("smartisan");
  }

  private String getPackageName() {
    return activity.getPackageName();
  }

  @Override
  public boolean onActivityResult(int requestCode, int resultCode, Intent intent) {
    synchronized (this) {
      if (requestCode == this.requestCode && result != null) {
        Map<String, Object> data = new HashMap<>();
        data.put("resultCode", resultCode);
        data.put("requestCode", requestCode);
        if (intent != null) {
          data.put("intent", intent2map(intent));
        }

        result.success(data);
        this.result = null;
        return true;
      }
    }
    return false;
  }

}
