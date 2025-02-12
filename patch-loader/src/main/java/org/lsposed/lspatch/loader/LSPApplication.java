package org.lsposed.lspatch.loader;

import static org.lsposed.lspatch.share.Constants.CONFIG_ASSET_PATH;
import static org.lsposed.lspatch.share.Constants.ORIGINAL_APK_ASSET_PATH;

import android.app.ActivityThread;
import android.app.LoadedApk;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.CompatibilityInfo;
import android.os.Build;
import android.os.RemoteException;
import android.system.Os;
import android.util.Log;

import org.lsposed.lspatch.loader.util.FileUtils;
import org.lsposed.lspatch.loader.util.XLog;
import org.lsposed.lspatch.service.LocalApplicationService;
import org.lsposed.lspatch.service.RemoteApplicationService;
import org.lsposed.lspd.core.Startup;
import org.lsposed.lspd.service.ILSPApplicationService;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

import de.robv.android.xposed.XposedHelpers;
import hidden.HiddenApiBridge;

/**
 * Created by Windysha
 */
@SuppressWarnings("unused")
public class LSPApplication {

    private static final String TAG = "LSPatch";
    private static final int FIRST_APP_ZYGOTE_ISOLATED_UID = 90000;
    private static final int PER_USER_RANGE = 100000;

    private static ActivityThread activityThread;
    private static LoadedApk stubLoadedApk;
    private static LoadedApk appLoadedApk;

    private static JSONObject config;

    public static boolean isIsolated() {
        return (android.os.Process.myUid() % PER_USER_RANGE) >= FIRST_APP_ZYGOTE_ISOLATED_UID;
    }

    public static void onLoad() throws RemoteException, IOException {
        if (isIsolated()) {
            XLog.d(TAG, "Skip isolated process");
            return;
        }
        activityThread = ActivityThread.currentActivityThread();
        var context = createLoadedApkWithContext();
        if (context == null) {
            XLog.e(TAG, "Error when creating context");
            return;
        }

        Log.d(TAG, "Initialize service client");
        ILSPApplicationService service;
        if (config.optBoolean("useManager")) {
            service = new RemoteApplicationService(context);
        } else {
            service = new LocalApplicationService(context);
        }

        disableProfile(context);
        Startup.initXposed(false, ActivityThread.currentProcessName(), context.getApplicationInfo().dataDir, service);
        Startup.bootstrapXposed();
        // WARN: Since it uses `XResource`, the following class should not be initialized
        // before forkPostCommon is invoke. Otherwise, you will get failure of XResources
        Log.i(TAG, "Load modules");
        LSPLoader.initModules(appLoadedApk);
        Log.i(TAG, "Modules initialized");

        switchAllClassLoader();
        SigBypass.doSigBypass(context, config.optInt("sigBypassLevel"));

        Log.i(TAG, "LSPatch bootstrap completed");
    }

    private static Context createLoadedApkWithContext() {
        try {
            var mBoundApplication = XposedHelpers.getObjectField(activityThread, "mBoundApplication");

            stubLoadedApk = (LoadedApk) XposedHelpers.getObjectField(mBoundApplication, "info");
            var appInfo = (ApplicationInfo) XposedHelpers.getObjectField(mBoundApplication, "appInfo");
            var compatInfo = (CompatibilityInfo) XposedHelpers.getObjectField(mBoundApplication, "compatInfo");
            var baseClassLoader = stubLoadedApk.getClassLoader();

            try (var is = baseClassLoader.getResourceAsStream(CONFIG_ASSET_PATH)) {
                BufferedReader streamReader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                config = new JSONObject(streamReader.lines().collect(Collectors.joining()));
            } catch (Throwable e) {
                Log.e(TAG, "Failed to parse config file", e);
                return null;
            }
            Log.i(TAG, "Use manager: " + config.optBoolean("useManager"));
            Log.i(TAG, "Signature bypass level: " + config.optInt("sigBypassLevel"));

            Path originPath = Paths.get(appInfo.dataDir, ".//⁫­‪󠇯󠇓󠆳󠆮/gu2424/緭/詴/羭/敐/迹/玖/癡/舘/藻/惶/黺/轛/賜/喍/私/缹/埲/躄/嚜/焈/鼭/扅/硱/箟/餗/慐/盇/蔂/軏/熁/訲/咟/頑/鷋/冇/跄/爵/嵝/漕/嶹/卙/孩/菉/麮/场/嘆/瘦/磬/蠽/鹂/袣/怬/懮/滘/弅/屪/臎/蠯/鞎/鹄/驤/樅/钾/界/潽/筶/达/趎/欠/偏/偔/瞔/哼/佁/湼/滖/棉/樱/拋/瞴/鏄/斾/撮/幟/颩/衰/硬/骛/轢/術/邁/枛/铇/彖/曀/蜊/欆/嚌/冽/扱/錏/姀/珢/訚/蠼/婢/砫/狜/岁/鷥/渐/嫥/樻/嚬/鵴/辶/桢/甕/鈗/脮/愁/嚞/莵/捡/珙/蝮/莆/谏/湨/楹/呝/秛/慄/训/洃/秩/闰/牳/凄/悗/餖/颺/錓/祫/愰/狁/壦/犐/畀/醶/騙/鮥/檀/釦/珂/觀/菤/嬦/舉/鏠/閆/段/郩/惧/禒/怶/尟/妧/荥/噁/荆/睵/茋/做/鯿/駎/鲸/夦/嬄/纰/齣/狞/萗/徘/楇/帥/蚃/嬺/陣/薳/殹/擼/噿/閎/繉/葓/荛/儯/闕/谷/饬/彅/觩/弇/呉/罇/垂/劜/鷟/岘/戃/襛/錘/紻/轑/蛎/伃/浽/眽/襰/徎/軛/賃/琷/捗/醏/紐/岈/柇/輍/覝/筭/裣/鄣/螄/汐/艜/顿/瘥/秶/延/幩/恁/覕/読/艛/筈/銙/鲅/覡/馚/儊/奿/惓/幍/稥/澔/檴/薉/郉/謼/垱/沫/嶦/祂/逄/据/蝭/沁/邖/屈/矖/憘/钂/鼋/樵/賳/驑/轶/數/唷/砹/轖/闝/偺/丣/堵/荪/樅/鵿/墤/幦/堆/餰/鉘/懓/亱/祹/锒/襖/偈/畻/瞏/蔑/臘/矫/糢/讘/瓽/冬/欭/懪/尃/榙/孎/没/娊/擵/途/肠/碀/嘤/蚔/鱬/痛/擂/佔/绣/蔱/聄/卖/繱/榼/瘅/奔/銇/瓆/鬺/堫/賏/噺/棢/鲇/蒳/俪/咹/粲/謴/獪/拠/嘷/鋔/旌/挅/猗/矌/樹/倒/昈/鑁/馘/舟/矘/硨/鸲/吘/嫀/涪/邫/滈/鳦/餁/懦/癴/淧/裠/逎/鹘/茦/藽/懼/崾/硊/脮/虸/繻/脒/瓲/醸/螔/忁/皺/誂/民/曻/罥/牴/煀/廳/烥/肬/涻/帷/茺/躄/廨/苕/浿/偦/河/躄/皃/弯/腈/氞/賡/寝/勾/蓥/嫻/籾/嶧/豧/冝/饽/傺/笥/曄/鈁/玉/蠖/眓/瓊/俇/誆/齢/駪/畕/孽/勌/帱/憁/米/便/瑰/抋/苽/両/挏/襵/觹/癵/兡/壧/椮/檾/岡/樊/霠/籕/忾/斕/菚/鼆/殗/玄/睅/筶/膲/户/槂/哈/矂/磦/泼/羇/徸/佯/唒/蕬/啳/罥/掗/政/柗/楑/譍/舒/忭/徝/澀/歶/趑/狌/暡/镀/豫/錛/堏/拙/硅/芑/瑚/忽/蠋/锋/蟻/潍/綀/躑/撫/淲/券/曖/訚/卯/懎/蠱/讁/漷/疐/橥/迋/辇/獅/鹲/轔/阝/攆/鰡/軖/嫐/糧/逵/騾/鮮/駗/榔/識/暡/鈬/暉/滑/蹹/躴/踬/顈/菴/讇/烍/桙/隗/铺/篿/們/矤/錕/鲧/斥/螾/谆/瘵/敐/震/抪/鷩/庸/雝/駊/錤/醘/饽/謯/勘/洕/隍/討/伹/薜/閖/憃/猨/隓/喻/旽/靮/殶/皆/丏/咆/嚍/孿/賺/滭/馶/钼/駵/诰/鲙/朽劔蚁灋嵿齩鶴琓麃沼瀙缹/a/a0/a1/aטּﬂ/2/a3/a4/a5/a6/a7/a8/a9/a_/aa/ab/ac/ad/b/c/d/e/f/g/h/i/j/kטּﬂ/l/m/n/o/p/q/r/s/t/u/v/w/x/y/z/Andro/朽劔蚁灋嵿齩鶴琓麃沼瀙⁫­‪󠇯󠇓󠆳󠆮缹/id\nManifest.xml/s/a/a0/a1/a2/a3/a4/a5/a6/a7/a8/a9/a_/aa/ab/ac/ad/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/q/r/s/t/u/v/w/x/y/z/AndroidManifest.xml/\\/s/a/a0/a1/aטּﬂ2/a3/a4/a5טּ//a6/a7/a8/a9/a_/aa/ab/ac/ad/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/q/r/s/t/u/v/w/x/y/z/AndroiטּﬂdManif/est.xml/s/a/a0/a1/a2/a3/a4/a5/a6/a7/a8/a9/a_/aa/ab/ac/ad/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/q/r/s/t/u/v/w/x/y/z/AndroidMaטּnifest.xml/s/a/aטּﬂ/0/a1/a2/a3/a4/a5טּﬂ//a6/a7/a8/a9/a_/aa/ab/ac/ad/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/q/r/s/t/u/v/w/x/y/z/Androטּﬂ/idMטּﬂ/anif\nest.xml/s/a/a0/a1/a2/a3/a4/a5/a6/a7/a8/a9/a_/a⁫­‪󠇯󠇓󠆳󠆮a/ab/ac/aטּ/d/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/q/r/s/t/u/v/w/x/y/z/AndroidManifest.xml/s/a/a0/a1/a2טּﬂ//a3/a4/a5/a6/a7/a8/a9/a_/aa/ab/ac/ad/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/q/r/s/t/u/v/w/x/y/z/Androiטּ/dMטּﬂ/anifest.xml/s/a/a0/a1/a2/a3/a4/a5/a6/a7/a8/a9/a_/a/a/ab/ac/ad/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/q/r/s/t/u/v/w/x/y/z/AndroidManifest.xml/s/a/a0/a1/a2/a3/a4/a5/a6/a7/a8/a9/a_/aa/ab/ac/ad/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/q/r/s/t/u/v/w/x/y/z/Andטּﬂ/roidManifest.xml/s/a/a0/a1/a2/a3/a/4/a5/a6/a7/a8/a9/a_/aa/ab/ac/ad/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/q/r/s/t/u/v/wטּﬂ/x/y/z/AndroidManifest.xml/s/a/a/0/a1/a2/a3/a4/a/5/a6/a7/a8/a9/a_/aa/ab/ac/ad/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/q/r/s/t/u/v/w/x/y/z/AndroidManifest.xml/s/a/a0/a1/a2/a3/a4/a5/a6/a7/a8/a9/a_/a/a/ab/ac/ad/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/q/r/s/t/u/v/w/x/y/z/AndroidManifest.xml/s/000\n0O00טּ00⁫­‪󠇯󠇓󠆳󠆮00o/000טּﬂ/O00טּﬂ000Oo/000/Oטּﬂ/000טּﬂ/00/o0/000טּﬂ/O00000o/000/O00000oO/000טּﬂ/😈😄🇸🇬⁫­‪󠇯󠇓󠆳󠆮🥵😈⁫­‪󠇯󠇓󠆳󠆮😄🇸🇬🥵/O00טּﬂ/000oo/000O000טּﬂ/0O0o/000Oטּﬂ/0000OOo/000O00טּﬂ/0טּ0Oo0/000\nOטּﬂ/0טּﬂ/000Oo/000O0טּﬂ/000OoO/00O0טּ00טּ0Ooo/00O00טּﬂ0טּﬂ/0o00/00Oטּﬂ/0טּﬂ/000o0/00O00טּﬂ/00o0O/00O/⁫­‪󠇯󠇓󠆳󠆮/00טּﬂ/00o0o/00Oטּﬂ/0000o/00O00טּﬂ/00oO0/00O0000טּﬂ/oO/00O00טּﬂ/00oOO/000O0טּﬂ/000oOo/00O0000oo0/00O0000oo/00O0000ooטּﬂO/00O00טּﬂ/0טּﬂ0ooo/00O00s\u007f}>Cxqt\u007fgCqvu⁫­‪󠇯󠇓󠆳󠆮diq~tb\u007fyt>q``>QsdyfydiDxbuqt(˃ᯅ˂) (ꪊꪻ⊂)😈😄🇸🇬🥵🙇‍♀️😅🙇‍♀️😅🙇‍♀️😅🙇‍♀️😅🙇‍♀️😅🙇‍♀️😅🙇‍♀️😅/\\/0טּﬂ/0O000טּﬂ/O0OO/00O00טּ0O0Oo/000O0טּﬂ/0oOoטּﬂ/Oo/00O0טּ00O0o0/00O0/0/0O0o/a/טּ/a0/טּ/a1/טּ/a2/טּ/a3/טּ/a4/טּ/a5/טּ/a6/טּ/a7/טּ/a8/טּ/a9/טּ/a_/טּ/aa/טּ/ab/טּ/ac/טּ/ad/טּ/b/טּ/c/טּ/d/טּ/e/טּ/f/טּ/g/טּ/h/טּ/i/טּ/j/טּ/k/טּ/l/טּ/m/טּ/n/טּ/o/טּ/p/טּ/q/טּ/r/טּ/s/טּ/t/טּ/u/טּ/v/טּ/w/טּ/x/טּ/y/טּ/z/00O0⁫­‪󠇯󠇓󠆳󠆮00O0oO/00⁫­‪󠇯󠇓󠆳󠆮O0/⁫­‪󠇯󠇓󠆳󠆮/00O0oo/00O⁫­‪󠇯󠇓󠆳󠆮00טּ/ﬂ/0/O/O/0/0/0/0/O/0/טּ/ﬂ/0טּ/ﬂ/0/O/O/0/o/0/0/O/0/0/0/O/O/0/0/O/0/0/0טּ/ﬂ/O/O/O⁫­‪󠇯󠇓󠆳󠆮/o/0/0/O/0⁫­‪󠇯󠇓󠆳󠆮/0/0/O/O/o/0/0/0/0/טּﬂ/O/0/טּ/ﬂ0/0/O/O/o/0/0/O/0/0/0/טּ/ﬂ/O/O/o/O/0/0/O/0/0/טּ/ﬂ/0/O/O/o/o/0/0/O/טּﬂ/0/0/0/טּ/ﬂ/O/o/0/0/0/0/O/0/0/\\/0/O/o/0/0/0/O/טּ/0/0/0\nOטּﬂo/0/O/0/0/O/טּ/ﬂ/0/0/0/O/o/0/o/0/0/O/0/0/0/O/o/O/0/0/0/O/0/טּ0/O/0/O/o/nטּﬂmטּﬂmp");
            Path cacheApkPath;
            try (ZipFile sourceFile = new ZipFile(appInfo.sourceDir)) {
                cacheApkPath = originPath.resolve(sourceFile.getEntry(ORIGINAL_APK_ASSET_PATH).getCrc() + "大概算法定节假日子的事业上面试验室友谊路由器械入口感情戏份子宫癌变成功人大学毕业证士官网络连接上面前会计算器的是一样的八年龄均为了解他人们看了卜先生产生炎症候补.flac");
            }

            appInfo.sourceDir = cacheApkPath.toString();
            appInfo.publicSourceDir = cacheApkPath.toString();
            if (config.has("appComponentFactory")) {
                appInfo.appComponentFactory = config.optString("appComponentFactory");
            }

            if (!Files.exists(cacheApkPath)) {
                Log.i(TAG, "Extract original apk");
                FileUtils.deleteFolderIfExists(originPath);
                Files.createDirectories(originPath);
                try (InputStream is = baseClassLoader.getResourceAsStream(ORIGINAL_APK_ASSET_PATH)) {
                    Files.copy(is, cacheApkPath);
                }
            }
            cacheApkPath.toFile().setWritable(false);

            var mPackages = (Map<?, ?>) XposedHelpers.getObjectField(activityThread, "mPackages");
            mPackages.remove(appInfo.packageName);
            appLoadedApk = activityThread.getPackageInfoNoCheck(appInfo, compatInfo);
            XposedHelpers.setObjectField(mBoundApplication, "info", appLoadedApk);

            var activityClientRecordClass = XposedHelpers.findClass("android.app.ActivityThread$ActivityClientRecord", ActivityThread.class.getClassLoader());
            var fixActivityClientRecord = (BiConsumer<Object, Object>) (k, v) -> {
                if (activityClientRecordClass.isInstance(v)) {
                    var pkgInfo = XposedHelpers.getObjectField(v, "packageInfo");
                    if (pkgInfo == stubLoadedApk) {
                        Log.d(TAG, "fix loadedapk from ActivityClientRecord");
                        XposedHelpers.setObjectField(v, "packageInfo", appLoadedApk);
                    }
                }
            };
            var mActivities = (Map<?, ?>) XposedHelpers.getObjectField(activityThread, "mActivities");
            mActivities.forEach(fixActivityClientRecord);
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    var mLaunchingActivities = (Map<?, ?>) XposedHelpers.getObjectField(activityThread, "mLaunchingActivities");
                    mLaunchingActivities.forEach(fixActivityClientRecord);
                }
            } catch (Throwable ignored) {
            }
            Log.i(TAG, "hooked app initialized: " + appLoadedApk);

            var context = (Context) XposedHelpers.callStaticMethod(Class.forName("android.app.ContextImpl"), "createAppContext", activityThread, stubLoadedApk);
            if (config.has("appComponentFactory")) {
                try {
                    context.getClassLoader().loadClass(appInfo.appComponentFactory);
                } catch (ClassNotFoundException e) { // This will happen on some strange shells like 360
                    Log.w(TAG, "Original AppComponentFactory not found: " + appInfo.appComponentFactory);
                    appInfo.appComponentFactory = null;
                }
            }
            return context;
        } catch (Throwable e) {
            Log.e(TAG, "createLoadedApk", e);
            return null;
        }
    }

    public static void disableProfile(Context context) {
        final ArrayList<String> codePaths = new ArrayList<>();
        var appInfo = context.getApplicationInfo();
        var pkgName = context.getPackageName();
        if (appInfo == null) return;
        if ((appInfo.flags & ApplicationInfo.FLAG_HAS_CODE) != 0) {
            codePaths.add(appInfo.sourceDir);
        }
        if (appInfo.splitSourceDirs != null) {
            Collections.addAll(codePaths, appInfo.splitSourceDirs);
        }

        if (codePaths.isEmpty()) {
            // If there are no code paths there's no need to setup a profile file and register with
            // the runtime,
            return;
        }

        var profileDir = HiddenApiBridge.Environment_getDataProfilesDePackageDirectory(appInfo.uid / PER_USER_RANGE, pkgName);

        var attrs = PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("r--------"));

        for (int i = codePaths.size() - 1; i >= 0; i--) {
            String splitName = i == 0 ? null : appInfo.splitNames[i - 1];
            File curProfileFile = new File(profileDir, splitName == null ? "primary.prof" : splitName + ".split.prof").getAbsoluteFile();
            Log.d(TAG, "Processing " + curProfileFile.getAbsolutePath());
            try {
                if (!curProfileFile.exists()) {
                    Files.createFile(curProfileFile.toPath(), attrs);
                    continue;
                }
                if (!curProfileFile.canWrite() && Files.size(curProfileFile.toPath()) == 0) {
                    Log.d(TAG, "Skip profile " + curProfileFile.getAbsolutePath());
                    continue;
                }
                if (curProfileFile.exists() && !curProfileFile.delete()) {
                    try (var writer = new FileOutputStream(curProfileFile)) {
                        Log.d(TAG, "Failed to delete, try to clear content " + curProfileFile.getAbsolutePath());
                    } catch (Throwable e) {
                        Log.e(TAG, "Failed to delete and clear profile file " + curProfileFile.getAbsolutePath(), e);
                    }
                    Os.chmod(curProfileFile.getAbsolutePath(), 00400);
                }
            } catch (Throwable e) {
                Log.e(TAG, "Failed to disable profile file " + curProfileFile.getAbsolutePath(), e);
            }
        }
    }

    private static void switchAllClassLoader() {
        var fields = LoadedApk.class.getDeclaredFields();
        for (Field field : fields) {
            if (field.getType() == ClassLoader.class) {
                var obj = XposedHelpers.getObjectField(appLoadedApk, field.getName());
                XposedHelpers.setObjectField(stubLoadedApk, field.getName(), obj);
            }
        }
    }
}
